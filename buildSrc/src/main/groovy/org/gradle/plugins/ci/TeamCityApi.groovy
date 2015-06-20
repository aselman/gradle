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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.gradle.plugins.ci.requests.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TeamCityApi {
    static final String APPLICATION_JSON = 'application/json'
    static Map httpHeaders = ["Accept": APPLICATION_JSON]
    private static final Logger LOGGER = LoggerFactory.getLogger(TeamCityApi.class);

    static applyFromTemplate(TeamCityExtension teamCityExtension) {

        teamCityExtension.buildProjects.each { BuildProject buildProject ->
            maybeDeleteProject(teamCityExtension.baseUrl, teamCityExtension.username, teamCityExtension.password, buildProject.name)
            createProject(teamCityExtension.baseUrl, teamCityExtension.username, teamCityExtension.password, teamCityExtension.rootProjectId, buildProject)
        }

    }

    static createProject(String baseUrl, String username, String password, String parentId, BuildProject buildProject) {
        def client = restClient(baseUrl, username, password)
        CreateProjectRequest createProjectRequest = new CreateProjectRequest(
            name: buildProject.name,
            description: buildProject.description,
            parentProject: new ParentProject(id: parentId),
        )
        ApiResponse resp = client.post(
            contentType: APPLICATION_JSON,
            path: "projects",
            headers: httpHeaders,
            body: JsonOutput.toJson(createProjectRequest)
        )
        def json = new JsonSlurper().parseText(resp.body)
        String projectId = json.id

        if (buildProject.configurationParameters) {
            createConfigurationParameters(baseUrl, username, password, projectId, buildProject)
        }

        if (buildProject.buildTypes) {
            createBuildTypes(baseUrl, username, password, projectId, buildProject)
        }

        buildProject.subProjects.each { BuildProject subProject ->
            createProject(baseUrl, username, password, projectId, subProject)
        }
    }

    static createBuildTypes(String baseUrl, String username, String password, String projectId, BuildProject buildProject) {
        buildProject.buildTypes.each { BuildType buildType ->
            CreateBuildTypeRequest createBuildTypeRequest = new CreateBuildTypeRequest()
            createBuildTypeRequest.name = buildType.name
            createBuildTypeRequest.description = buildType.description
            createBuildTypeRequest.project = new ParentProject(id: projectId)

            def settingTupples = buildType.settings.collect { Map map ->
                new PropertyTuple(name: map.name, value: map.value)
            }

            createBuildTypeRequest.settings = new Settings(settingTupples)

            def client = restClient(baseUrl, username, password)
            ApiResponse resp = client.post(
                contentType: APPLICATION_JSON,
                path: "buildTypes",
                headers: httpHeaders,
                body: JsonOutput.toJson(createBuildTypeRequest))
        }
    }

    static maybeDeleteProject(String baseUrl, String username, String password, String projectId) {
        def client = restClient(baseUrl, username, password)
        LOGGER.info("Deleting project: $projectId")
        try {
            ApiResponse resp = client.delete(
                contentType: APPLICATION_JSON,
                path: "projects/${projectId}",
                headers: httpHeaders,
            )
        } catch (ApiException e) {
            if (e.response.body.contains('jetbrains.buildServer.server.rest.errors.NotFoundException')) {
                LOGGER.info("Project $projectId not found")
            }
        }
        LOGGER.info("Deleting project: $projectId complete")

    }

    static createConfigurationParameters(String baseUrl, String username, String password, String projectId, BuildProject buildProject) {
        LOGGER.info("Creating configuration parameters for project: $buildProject.name")
        def client = restClient(baseUrl, username, password)
        CreateParameterRequest createParameterRequest = new CreateParameterRequest()
        createParameterRequest.property = buildProject.configurationParameters.collect { Map map ->
            new PropertyTuple(name: map.name, value: map.value)
        }

        def json = JsonOutput.toJson(createParameterRequest)

        ApiResponse resp = client.put(
            contentType: APPLICATION_JSON,
            path: "projects/${projectId}/parameters",
            headers: httpHeaders,
            body: json
        )
        LOGGER.info("Creating configuration parameters for project: $buildProject.name complete")

    }

    static RESTClient restClient(String baseUrl, String username, String password) {
        def client = new RESTClient("$baseUrl/httpAuth/app/rest/")
        client.parser.'application/json' = client.parser.'text/plain'
        client.auth.basic(username, password)
        def responseHandler = { resp, reader ->
            new ApiResponse(body: reader?.text, status: resp.status)
        }
        client.handler.failure = { resp, reader ->
            ApiResponse response = new ApiResponse(body: reader.text, status: resp.status)
            throw new ApiException(response)
        }
        client.handler.success = responseHandler
        client
    }

}
