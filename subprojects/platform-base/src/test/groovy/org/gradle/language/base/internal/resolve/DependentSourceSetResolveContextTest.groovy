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

package org.gradle.language.base.internal.resolve
import org.gradle.api.artifacts.component.LibraryComponentIdentifier
import org.gradle.language.base.internal.DependentSourceSetInternal
import spock.lang.Specification
import spock.lang.Unroll

class DependentSourceSetResolveContextTest extends Specification {
    def "resolve context can be created from a java source set"() {
        given:
        def project = ':foo'
        def sourceset = Mock(DependentSourceSetInternal)

        when:
        def context = new DependentSourceSetResolveContext(project, sourceset)

        then:
        context.projectPath == project
        context.dependencies.empty
        context.allDependencies.empty
    }

    @Unroll
    def "context name for project #path and library #library is #contextName"() {
        // keeping this test in case we need to change the context name again
        given:
        def sourceset = Mock(DependentSourceSetInternal)

        when:
        sourceset.parentName >> library
        def context = new DependentSourceSetResolveContext(path, sourceset)

        then:
        context.projectPath == path
        context.name == contextName

        where:
        path       | library  | contextName
        ':myPath'  | 'myLib'  | LibraryComponentIdentifier.API_CONFIGURATION_NAME
        ':myPath'  | 'myLib2' | LibraryComponentIdentifier.API_CONFIGURATION_NAME
        ':myPath2' | 'myLib'  | LibraryComponentIdentifier.API_CONFIGURATION_NAME
    }
}
