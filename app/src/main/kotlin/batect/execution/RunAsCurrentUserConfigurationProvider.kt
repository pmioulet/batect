/*
    Copyright 2017-2021 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package batect.execution

import batect.config.CacheMount
import batect.config.Container
import batect.config.RunAsCurrentUserConfig
import batect.docker.ContainerDirectory
import batect.docker.ContainerFile
import batect.docker.DockerContainer
import batect.docker.DockerException
import batect.docker.DockerVolumeMount
import batect.docker.DockerVolumeMountSource
import batect.docker.UserAndGroup
import batect.docker.client.ContainersClient
import batect.docker.client.DockerContainerType
import batect.os.NativeMethods
import batect.os.OperatingSystem
import batect.os.SystemInfo
import java.nio.file.FileSystem
import java.nio.file.Files

class RunAsCurrentUserConfigurationProvider(
    private val systemInfo: SystemInfo,
    private val nativeMethods: NativeMethods,
    private val fileSystem: FileSystem,
    private val containerType: DockerContainerType,
    private val containersClient: ContainersClient
) {
    private val userId: Int by lazy {
        when (systemInfo.operatingSystem) {
            OperatingSystem.Windows -> 0
            else -> nativeMethods.getUserId()
        }
    }

    private val groupId: Int by lazy {
        when (systemInfo.operatingSystem) {
            OperatingSystem.Windows -> 0
            else -> nativeMethods.getGroupId()
        }
    }

    private val userName: String by lazy {
        when (systemInfo.operatingSystem) {
            OperatingSystem.Windows -> "root"
            else -> nativeMethods.getUserName()
        }
    }

    private val groupName: String by lazy {
        when (systemInfo.operatingSystem) {
            OperatingSystem.Windows -> "root"
            else -> nativeMethods.getGroupName()
        }
    }

    fun determineUserAndGroup(container: Container): UserAndGroup? = when (container.runAsCurrentUserConfig) {
        is RunAsCurrentUserConfig.RunAsDefaultContainerUser -> null
        is RunAsCurrentUserConfig.RunAsCurrentUser -> UserAndGroup(userId, groupId)
    }

    fun applyConfigurationToContainer(container: Container, dockerContainer: DockerContainer) {
        when (container.runAsCurrentUserConfig) {
            is RunAsCurrentUserConfig.RunAsDefaultContainerUser -> return
            is RunAsCurrentUserConfig.RunAsCurrentUser -> applyConfigurationToContainer(container.runAsCurrentUserConfig, container, dockerContainer)
        }
    }

    private fun applyConfigurationToContainer(configuration: RunAsCurrentUserConfig.RunAsCurrentUser, container: Container, dockerContainer: DockerContainer) {
        if (containerType == DockerContainerType.Windows) {
            throw RunAsCurrentUserConfigurationException("Container '${container.name}' has run as current user enabled, but this is not supported for Windows containers.")
        }

        try {
            uploadFilesForConfiguration(configuration, dockerContainer)
            uploadHomeDirectoryForConfiguration(configuration, container, dockerContainer)
            uploadCacheDirectories(container, dockerContainer)
        } catch (e: DockerException) {
            throw RunAsCurrentUserConfigurationException("Could not apply 'run as current user' configuration to container '${container.name}': ${e.message}", e)
        }
    }

    private fun uploadFilesForConfiguration(configuration: RunAsCurrentUserConfig.RunAsCurrentUser, dockerContainer: DockerContainer) {
        val passwdContents = generatePasswdFile(configuration)
        val groupContents = generateGroupFile()

        containersClient.upload(
            dockerContainer,
            setOf(
                ContainerFile("passwd", 0, 0, passwdContents.toByteArray(Charsets.UTF_8)),
                ContainerFile("group", 0, 0, groupContents.toByteArray(Charsets.UTF_8))
            ),
            "/etc"
        )
    }

    private fun uploadHomeDirectoryForConfiguration(configuration: RunAsCurrentUserConfig.RunAsCurrentUser, container: Container, dockerContainer: DockerContainer) {
        if (!configuration.homeDirectory.startsWith("/")) {
            throw RunAsCurrentUserConfigurationException("Container '${container.name}' has an invalid home directory configured: '${configuration.homeDirectory}' is not an absolute path.")
        }

        uploadDirectory(configuration.homeDirectory, dockerContainer)
    }

    private fun uploadCacheDirectories(container: Container, dockerContainer: DockerContainer) {
        val cacheMounts = container.volumeMounts.filterIsInstance<CacheMount>()

        cacheMounts.forEach { cacheMount ->
            if (!cacheMount.containerPath.startsWith("/")) {
                throw RunAsCurrentUserConfigurationException("Container '${container.name}' has an invalid cache mount configured: '${cacheMount.containerPath}' is not an absolute path.")
            }

            uploadDirectory(cacheMount.containerPath, dockerContainer)
        }
    }

    private fun uploadDirectory(path: String, dockerContainer: DockerContainer) {
        containersClient.upload(
            dockerContainer,
            setOf(ContainerDirectory(leafDirectoryNameOf(path), userId.toLong(), groupId.toLong())),
            parentDirectoryOf(path)
        )
    }

    private fun parentDirectoryOf(path: String): String {
        val segments = path.splitToPathSegments()
        return if (segments.size <= 2) "/" else segments.dropLast(1).joinToString("/")
    }

    private fun leafDirectoryNameOf(path: String): String {
        return path.splitToPathSegments().last()
    }

    private fun generatePasswdFile(runAsCurrentUserConfig: RunAsCurrentUserConfig.RunAsCurrentUser): String {
        val homeDirectory = runAsCurrentUserConfig.homeDirectory

        return if (userId == 0) {
            "root:x:0:0:root:$homeDirectory:/bin/sh"
        } else {
            """
                |root:x:0:0:root:/root:/bin/sh
                |$userName:x:$userId:$groupId:$userName:$homeDirectory:/bin/sh
            """.trimMargin()
        }
    }

    private fun generateGroupFile(): String {
        val rootGroup = "root:x:0:root"

        return if (groupId == 0) {
            rootGroup
        } else {
            """
                |$rootGroup
                |$groupName:x:$groupId:$userName
            """.trimMargin()
        }
    }

    fun createMissingVolumeMountDirectories(mounts: Set<DockerVolumeMount>, container: Container) {
        if (containerType == DockerContainerType.Linux && container.runAsCurrentUserConfig is RunAsCurrentUserConfig.RunAsDefaultContainerUser) {
            return
        }

        mounts.map { it.source }.filterIsInstance<DockerVolumeMountSource.LocalPath>().forEach { mount ->
            val path = fileSystem.getPath(mount.path)

            if (!Files.exists(path)) {
                Files.createDirectories(path)
            }
        }

        mounts.forEach { mount ->
            val parentMount = mounts.findClosestParentMount(mount)

            if (parentMount != null && parentMount.source is DockerVolumeMountSource.LocalPath) {
                val source = parentMount.source as DockerVolumeMountSource.LocalPath
                val thisPath = mount.containerPath.splitToPathSegments()
                val parentPath = parentMount.containerPath.splitToPathSegments()

                val directoriesToCreate = parentPath.relativePathTo(thisPath)
                val path = fileSystem.getPath(source.path, *directoriesToCreate.toTypedArray())
                Files.createDirectories(path)
            }
        }
    }

    private fun Set<DockerVolumeMount>.findClosestParentMount(mount: DockerVolumeMount): DockerVolumeMount? {
        val thisPath = mount.containerPath.splitToPathSegments()

        return this
            .filter { it != mount }
            .associateWith { it.containerPath.splitToPathSegments() }
            .filterValues { it.size < thisPath.size }
            .filterValues { it == thisPath.subList(0, it.lastIndex + 1) }
            .entries
            .maxByOrNull { it.value.size }
            ?.key
    }

    private fun List<String>.relativePathTo(other: List<String>) = other.subList(this.lastIndex + 1, other.lastIndex + 1)

    // This method assumes that paths can't contain forward slashes except as path separators, which appears to be true for Linux.
    private fun String.splitToPathSegments() = this.split('/')
}

class RunAsCurrentUserConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
