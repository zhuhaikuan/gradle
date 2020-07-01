/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.api.internal.file.collections;

import org.gradle.api.Task;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.Factory;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.util.DeferredUtil;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Consumer;

public class UnpackingVisitor {
    private final Consumer<FileCollectionInternal> visitor;
    private final PathToFileResolver resolver;
    private final Factory<PatternSet> patternSetFactory;

    public UnpackingVisitor(Consumer<FileCollectionInternal> visitor, PathToFileResolver resolver, Factory<PatternSet> patternSetFactory) {
        this.visitor = visitor;
        this.resolver = resolver;
        this.patternSetFactory = patternSetFactory;
    }

    public void add(@Nullable Object element) {
        if (element instanceof FileCollectionInternal) {
            // FileCollection is-a Iterable, Buildable and TaskDependencyContainer, so check before checking for these things
            visitor.accept((FileCollectionInternal) element);
            return;
        }
        if (element instanceof DirectoryTree) {
            visitor.accept(new FileTreeAdapter((MinimalFileTree) element, patternSetFactory));
            return;
        }
        if (element instanceof ProviderInternal) {
            // ProviderInternal is-a TaskDependencyContainer, so check first
            ProviderInternal<?> provider = (ProviderInternal<?>) element;
            visitor.accept(new ProviderBackedFileCollection(provider, resolver, patternSetFactory));
            return;
        }

        if (element instanceof Task) {
            visitor.accept((FileCollectionInternal) ((Task) element).getOutputs().getFiles());
        } else if (element instanceof TaskOutputs) {
            visitor.accept((FileCollectionInternal) ((TaskOutputs) element).getFiles());
        } else if (DeferredUtil.isNestableDeferred(element)) {
            Object deferredResult = DeferredUtil.unpackNestableDeferred(element);
            if (deferredResult != null) {
                add(deferredResult);
            }
        } else if (element instanceof Path) {
            // Path is-a Iterable, so check before checking for Iterable
            visitor.accept(new FileCollectionAdapter(new ListBackedFileSet(((Path) element).toFile()), patternSetFactory));
        } else if (element instanceof Iterable) {
            Iterable<?> iterable = (Iterable) element;
            for (Object item : iterable) {
                add(item);
            }
        } else if (element instanceof Object[]) {
            Object[] array = (Object[]) element;
            for (Object value : array) {
                add(value);
            }
        } else if (element != null) {
            // Treat everything else as a single file
            visitor.accept(new FileCollectionAdapter(new ListBackedFileSet(resolver.resolve(element)), patternSetFactory));
        }
    }
}
