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

package org.gradle.internal.fingerprint.impl;

import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.changedetection.state.CachingResourceHasher;
import org.gradle.api.internal.changedetection.state.LineEndingAwareClasspathResourceHasher;
import org.gradle.api.internal.changedetection.state.ResourceSnapshotterCacheService;
import org.gradle.api.internal.changedetection.state.RuntimeClasspathResourceHasher;
import org.gradle.internal.execution.fingerprint.FileCollectionFingerprinter;
import org.gradle.internal.execution.fingerprint.FileCollectionSnapshotter;
import org.gradle.internal.fingerprint.DirectorySensitivity;
import org.gradle.internal.fingerprint.LineEndingNormalization;
import org.gradle.internal.fingerprint.hashing.ResourceHasher;
import org.gradle.internal.hash.StreamHasher;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class FileCollectionFingerprinterRegistrations {
    private final List<FileCollectionFingerprinter> registrants;

    public FileCollectionFingerprinterRegistrations(StringInterner stringInterner, FileCollectionSnapshotter fileCollectionSnapshotter, ResourceSnapshotterCacheService resourceSnapshotterCacheService, StreamHasher streamHasher) {
        this.registrants = stream(DirectorySensitivity.values())
            .flatMap(directorySensitivity ->
                stream(LineEndingNormalization.values()).flatMap(lineEndingNormalization -> {
                        ResourceHasher normalizedContentHasher = normalizedContentHasher(lineEndingNormalization, streamHasher, resourceSnapshotterCacheService);
                        return Stream.of(
                            new AbsolutePathFileCollectionFingerprinter(directorySensitivity, lineEndingNormalization, fileCollectionSnapshotter, normalizedContentHasher),
                            new RelativePathFileCollectionFingerprinter(stringInterner, directorySensitivity, lineEndingNormalization, fileCollectionSnapshotter, normalizedContentHasher),
                            new NameOnlyFileCollectionFingerprinter(directorySensitivity, lineEndingNormalization, fileCollectionSnapshotter, normalizedContentHasher)
                        );
                    }
                )
            ).collect(Collectors.toList());
        this.registrants.addAll(
            stream(LineEndingNormalization.values())
                .map(lineEndingNormalization -> {
                        ResourceHasher normalizedContentHasher = normalizedContentHasher(lineEndingNormalization, streamHasher, resourceSnapshotterCacheService);
                        return new IgnoredPathFileCollectionFingerprinter(fileCollectionSnapshotter, lineEndingNormalization, normalizedContentHasher);
                    }
                )
                .collect(Collectors.toList())
        );
    }

    public List<? extends FileCollectionFingerprinter> getRegistrants() {
        return registrants;
    }

    private static ResourceHasher normalizedContentHasher(LineEndingNormalization lineEndingNormalization, StreamHasher streamHasher, ResourceSnapshotterCacheService resourceSnapshotterCacheService) {
        ResourceHasher lineEndingAwareResourceHasher = new LineEndingAwareClasspathResourceHasher(new RuntimeClasspathResourceHasher(), lineEndingNormalization, streamHasher);
        return new CachingResourceHasher(lineEndingAwareResourceHasher, resourceSnapshotterCacheService);
    }
}
