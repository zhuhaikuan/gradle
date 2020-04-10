/*
 * Copyright 2020 the original author or authors.
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
package org.gradle.internal.compiler.java;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ClassNameCollector implements TaskListener {
    private final Function<File, Optional<String>> relativize;
    private final BiConsumer<String, Set<String>> onFile;
    private String currentClass;
    private final Set<String> symbols = new TreeSet<>();

    public ClassNameCollector(Function<File, Optional<String>> relativize, BiConsumer<String, Set<String>> onFile) {
        this.relativize = relativize;
        this.onFile = onFile;
    }

    public void finish() {
        flush();
    }

    @Override
    public void started(TaskEvent e) {

    }

    @Override
    public void finished(TaskEvent e) {
        JavaFileObject sourceFile = e.getSourceFile();
        if (isSourceFile(sourceFile)) {
            File asSourceFile = new File(sourceFile.getName());
            if (isClassGenerationPhase(e)) {
                processSourceFile(e, asSourceFile);
            } else if (isPackageInfoFile(e, asSourceFile)) {
                processPackageInfo(asSourceFile);
            }
        }
    }

    private static boolean isSourceFile(JavaFileObject sourceFile) {
        return sourceFile != null && sourceFile.getKind() == JavaFileObject.Kind.SOURCE;
    }

    private void processSourceFile(TaskEvent e, File sourceFile) {
        Optional<String> relativePath = findRelativePath(sourceFile);
        if (relativePath.isPresent()) {
            String key = relativePath.get();
            TypeElement typeElement = e.getTypeElement();
            Name name = typeElement.getQualifiedName();
            if (typeElement instanceof Symbol.TypeSymbol) {
                Symbol.TypeSymbol symbol = (Symbol.TypeSymbol) typeElement;
                name = symbol.flatName();
            }
            String symbol = normalizeName(name);
            registerMapping(key, symbol);
        }
    }

    private void processPackageInfo(File sourceFile) {
        Optional<String> relativePath = findRelativePath(sourceFile);
        if (relativePath.isPresent()) {
            String key = relativePath.get();
            String pkgInfo = key.substring(0, key.lastIndexOf(".java")).replace('/', '.');
            registerMapping(key, pkgInfo);
        }
    }

    private Optional<String> findRelativePath(File sourceFile) {
        return relativize.apply(sourceFile);
    }

    private static String normalizeName(Name name) {
        String symbol = name.toString();
        if (symbol.endsWith("module-info")) {
            symbol = "module-info";
        }
        return symbol;
    }

    private static boolean isPackageInfoFile(TaskEvent e, File asSourceFile) {
        return e.getKind() == TaskEvent.Kind.ANALYZE && "package-info.java".equals(asSourceFile.getName());
    }

    private static boolean isClassGenerationPhase(TaskEvent e) {
        return e.getKind() == TaskEvent.Kind.GENERATE;
    }

    public void registerMapping(String key, String symbol) {
        if (!key.equals(currentClass)) {
            flush();
            currentClass = key;
        }
        symbols.add(symbol);
    }

    private void flush() {
        if (currentClass != null) {
            onFile.accept(currentClass, symbols);
            currentClass = null;
            symbols.clear();
        }
    }

}
