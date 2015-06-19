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

import org.gradle.api.Plugin
import org.gradle.api.Project

import static groovyx.net.http.ContentType.*

class TeamCityJobPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("teamCity", TeamCityExtension)

        project.task('printTC') << {
            TeamCityExtension teamCityExtension = project.extensions.teamCity
            BuildProject template = teamCityExtension.template

            println "Template is: ${template.name}"
            template.subProjects.each { BuildProject p ->
                println "SubProject is: ${p.name}"

            }
        }
    }

    def httpHeaders = ["Accept": "application/json"]

    def printProjects(client, resp) {
        def project = resp.data
        def projectIds = project.projects.project.collect { it.id }

        project.projects.project.each {
            println """
        project{
         "name": "$it.name"
         "description": "$it.description"
    """
            projectIds.each {
                def subResp = client.get(path: "projects/id:$it", headers: httpHeaders)
                printProjects(client, subResp)
            }

            printBuildTypes(client, it.id)
            println "}"
        }
    }


    def printBuildTypes(client, projectId) {
        def resp = client.get(path: "projects/$projectId/buildTypes", headers: httpHeaders)
        def configs = resp.data.buildType

//    https://builds.gradle.org/httpAuth/app/rest/projects/Gradle_Master_Checkpoints/buildTypes/id:Gradle_Master_Checkpoints_Stage0Foundation
        configs.each {
            println """
        buildConfig{
            vcsRootId: ""
            settings: []
            parameters: []
            steps: []
            snapshotDependencies: []
            artifactDependencies:[]
            agentRequirements: []
        }
        """
        }
    }
}
