/*
   Copyright 2017-2020 Charles Korn.

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

package batect.ioc

import batect.docker.client.DockerContainerType
import org.kodein.di.Copy
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.bind
import org.kodein.di.instance

class DockerConfigurationKodeinFactory(
    private val baseKodein: DirectDI
) {
    fun create(containerType: DockerContainerType): DirectDI = DI.direct {
        extend(baseKodein, copy = Copy.All)
        bind<DockerContainerType>() with instance(containerType)
    }
}
