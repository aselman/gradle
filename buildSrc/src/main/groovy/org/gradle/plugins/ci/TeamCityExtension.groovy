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

import org.gradle.util.ConfigureUtil

class TeamCityExtension {
    String baseUrl
    String username
    String password
    String rootProjectId
    List<BuildProject> buildProjects = []
    List<GitVcsRoot> vcsRoots = []

    def buildProjec(Closure closure) {
        BuildProject buildProject = new BuildProject()
        ConfigureUtil.configure(closure, buildProject)
        buildProjects << buildProject
    }

    def vcsRoot(Closure closure) {
        closure.resolveStrategy = 1
        GitVcsRoot gitVcsRoot = new GitVcsRoot()
        ConfigureUtil.configure(closure, gitVcsRoot)
        vcsRoots << gitVcsRoot
    }
}
