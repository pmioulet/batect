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

package batect.config

import batect.os.PathResolutionContext
import kotlinx.serialization.Serializable

sealed class ImageSource {
    abstract val imagePullPolicy: ImagePullPolicy
}

data class BuildImage(
    val buildDirectory: Expression,
    val pathResolutionContext: PathResolutionContext,
    val buildArgs: Map<String, Expression> = emptyMap(),
    val dockerfilePath: String = "Dockerfile",
    override val imagePullPolicy: ImagePullPolicy = ImagePullPolicy.IfNotPresent,
    val targetStage: String? = null
) : ImageSource()

@Serializable
data class PullImage(
    val imageName: String,
    override val imagePullPolicy: ImagePullPolicy = ImagePullPolicy.IfNotPresent
) : ImageSource()
