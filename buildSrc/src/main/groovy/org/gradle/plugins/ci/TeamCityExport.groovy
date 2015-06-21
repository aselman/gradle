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

import groovyx.net.http.RESTClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class TeamCityExport extends DefaultTask {

    @OutputDirectory
    File exportDir = project.file("${project.buildDir}/teamcity/")

    @TaskAction
    def export() {

        RESTClient client = TeamCityApi.restClient('https://builds.gradle.org', System.getenv('TC_USERNAME'), System.getenv('TC_PASSWORD'))
        ApiResponse projectsResp = client.get(
            contentType: TeamCityApi.APPLICATION_JSON,
            path: "projects/_root",
            headers: TeamCityApi.httpHeaders,
        )
        File file = new File(exportDir, "Export.groovy")
        dumpVcsRoots('_root', client, file)
        dumpProjects(projectsResp, client, file)
    }

    def dumpVcsRoots(String projectId, RESTClient client, File file) {
        ApiResponse allVcsRootsResp = client.get(
            contentType: TeamCityApi.APPLICATION_JSON,
            path: "vcs-roots",
            headers: TeamCityApi.httpHeaders,
        )

        def vcsJson = allVcsRootsResp.slurp()
        vcsJson."vcs-root"?.each { root ->
            ApiResponse vcsRootResp = client.get(
                contentType: TeamCityApi.APPLICATION_JSON,
                path: "vcs-roots/$root.id",
                headers: TeamCityApi.httpHeaders,
            )

            def vcsRoot = vcsRootResp.slurp()
            List<String> params = vcsRoot['properties']?.property?.collect { prop ->
                """["name" :'${prop.name}', "value":${propertyValue(prop.value)}]"""
            }

            file << """
        vcsRoot {
            name = "${vcsRoot.name}"
            id = "${vcsRoot.id}"
            ${params ? "parameters = [${params.join(',\n')}]" : ''}
        }
"""
        }

    }

    def dumpProjects(ApiResponse apiResponse, RESTClient client, File file) {
        def projects = apiResponse.slurp().projects?.project
        projects.each { project ->
            ApiResponse projectResp = client.get(
                contentType: TeamCityApi.APPLICATION_JSON,
                path: "projects/${project.id}",
                headers: TeamCityApi.httpHeaders,
            )

            def detailedProject = projectResp.slurp()
            List<String> buildTypesList = []

            detailedProject?.buildTypes?.buildType?.each { buildType ->
                List<String> buildStepList = []
                ApiResponse buildTypeResp = client.get(
                    contentType: TeamCityApi.APPLICATION_JSON,
                    path: "buildTypes/${buildType.id}",
                    headers: TeamCityApi.httpHeaders,
                )
                def types = buildTypeResp.slurp()
                types.steps?.step?.each { buildStep ->
                    List<String> stepProps = []
                    buildStep['properties']?.property?.each { prop ->
                        stepProps << """["name" :'${prop.name}', "value":${propertyValue(prop.value)}]"""
                    }

                    buildStepList << """
        buildStep {
            type = "${buildStep.type}"
            name = "${buildStep.name}"
            description = "${buildStep.description}"
            ${stepProps ? "parameters = [${stepProps.join(',\n')}]" : ''}
        }
"""
                }

                buildTypesList << """
    buildType{
        name = "${buildType.name}"
        description = "${buildType.description}"
        ${buildStepList ? buildStepList.join() : ''}
    }
"""
            }



            file << """
buildProject {
    name = "${project.name}"
    description = "${project.description}"
"""

            dumpProjects(projectResp, client, file)

            file << """
    ${buildTypesList ? buildTypesList.join() : ''}
}
"""


        }
    }

    String propertyValue(String string) {
        return '\"\"\"' + string + '\"\"\"'
    }
}

