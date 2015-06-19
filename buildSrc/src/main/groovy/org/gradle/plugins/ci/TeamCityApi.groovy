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

class TeamCityApi {
    static final String CONTENT_TYPE = 'application/json'
    static Map httpHeaders = ["Accept": CONTENT_TYPE]

    static applyFromTemplate(TeamCityExtension teamCityExtension) {
        teamCityExtension.buildProjects.each { BuildProject buildProject ->
            createProject(teamCityExtension.baseUrl, teamCityExtension.username, teamCityExtension.password, teamCityExtension.rootProjectId, buildProject)
        }

    }

    static createProject(String baseUrl, String username, String password, String parentId, BuildProject buildProject) {
        def client = restClient(baseUrl, username, password)
        def resp = client.post(
            contentType: CONTENT_TYPE,
            path: "projects",
            headers: httpHeaders,
            body: """
{
    "name": "${buildProject.name}",
    "description": "${buildProject.description}",
    "parentProject": {
        "id": "${parentId}"
    }
}
"""
        )

        String projectId = resp.data.id
        buildProject.subProjects.each { BuildProject subProject ->
            createProject(baseUrl, username, password, projectId, subProject)
        }
    }

    static RESTClient restClient(String baseUrl, String username, String password) {
        def client = new RESTClient("$baseUrl/httpAuth/app/rest/")
        client.auth.basic(username, password)
        client
    }
}
