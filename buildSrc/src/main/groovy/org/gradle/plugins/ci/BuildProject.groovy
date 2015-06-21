/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugins.ci

import groovy.transform.ToString
import org.gradle.util.ConfigureUtil

@ToString
class BuildProject implements Parameterised, Named {
    List<BuildProject> subProjects = []
    List<BuildType> buildTypes = []

    def buildProject(Closure closure) {
        BuildProject buildProject = new BuildProject()
        ConfigureUtil.configure(closure, buildProject)
        this.subProjects << buildProject
    }

    def buildType(Closure closure) {
        BuildType buildType = new BuildType()
        ConfigureUtil.configure(closure, buildType)
        this.buildTypes << buildType
    }
}
