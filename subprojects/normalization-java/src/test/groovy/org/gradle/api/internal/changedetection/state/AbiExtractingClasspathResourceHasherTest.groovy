/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.api.internal.changedetection.state

import org.gradle.internal.fingerprint.hashing.ConfigurableNormalizer
import org.gradle.internal.hash.HashCode
import org.gradle.internal.hash.Hashing
import org.gradle.internal.normalization.java.ApiClassExtractor
import spock.lang.Specification

class AbiExtractingClasspathResourceHasherTest extends Specification {

    def "api class extractors affect the configuration hash"() {
        def apiClassExtractor1 = Mock(ApiClassExtractor)
        def apiClassExtractor2 = Mock(ApiClassExtractor)

        def resourceHasher1 = new AbiExtractingClasspathResourceHasher(apiClassExtractor1)
        def resourceHasher2 = new AbiExtractingClasspathResourceHasher(apiClassExtractor2)

        when:
        def configurationHash1 = configurationHashOf(resourceHasher1)
        def configurationHash2 = configurationHashOf(resourceHasher2)

        then:
        configurationHash1 != configurationHash2

        1 * apiClassExtractor1.appendConfigurationToHasher(_) >> { args -> args[0].putString("first") }
        1 * apiClassExtractor2.appendConfigurationToHasher(_) >> { args -> args[0].putString("second") }
    }

    private static HashCode configurationHashOf(ConfigurableNormalizer normalizer) {
        def hasher = Hashing.md5().newHasher()
        normalizer.appendConfigurationToHasher(hasher)
        return hasher.hash()
    }
}
