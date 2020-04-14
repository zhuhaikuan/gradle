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

package org.gradle.plugin.devel.internal.precompiled;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.api.internal.plugins.PluginImplementation;
import org.gradle.api.plugins.UnknownPluginException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.configuration.CompileOperationFactory;
import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.groovy.scripts.internal.CompileOperation;
import org.gradle.groovy.scripts.internal.CompiledScript;
import org.gradle.groovy.scripts.internal.ScriptCompilationHandler;
import org.gradle.initialization.ClassLoaderScopeRegistry;
import org.gradle.internal.exceptions.LocationAwareException;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.plugin.management.PluginRequest;
import org.gradle.plugin.management.internal.PluginRequestInternal;
import org.gradle.plugin.management.internal.PluginRequests;
import org.gradle.plugin.use.PluginId;
import org.gradle.plugin.use.internal.PluginDependencyResolutionServices;
import org.gradle.plugin.use.internal.PluginResolverFactory;
import org.gradle.plugin.use.internal.PluginsAwareScript;
import org.gradle.plugin.use.resolve.internal.PluginResolution;
import org.gradle.plugin.use.resolve.internal.PluginResolutionResult;
import org.gradle.plugin.use.resolve.internal.PluginResolveContext;
import org.gradle.plugin.use.resolve.internal.PluginResolver;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

@CacheableTask
abstract class GeneratePluginAdaptersTask extends DefaultTask {
    private final ScriptCompilationHandler scriptCompilationHandler;
    private final CompileOperationFactory compileOperationFactory;
    private final ServiceRegistry serviceRegistry;
    private final FileSystemOperations fileSystemOperations;
    private final ClassLoaderScope classLoaderScope;
    private final PluginResolver pluginResolver;
    private final DependencyResolutionServices dependencyResolutionServices;

    private final Project project = getProject();

    @Inject
    public GeneratePluginAdaptersTask(ScriptCompilationHandler scriptCompilationHandler,
                                      ClassLoaderScopeRegistry classLoaderScopeRegistry,
                                      CompileOperationFactory compileOperationFactory,
                                      ServiceRegistry serviceRegistry,
                                      FileSystemOperations fileSystemOperations,
                                      PluginResolverFactory pluginResolverFactory,
                                      PluginDependencyResolutionServices dependencyResolutionServices) {
        this.scriptCompilationHandler = scriptCompilationHandler;
        this.compileOperationFactory = compileOperationFactory;
        this.serviceRegistry = serviceRegistry;
        this.classLoaderScope = classLoaderScopeRegistry.getCoreAndPluginsScope();
        this.fileSystemOperations = fileSystemOperations;
        this.pluginResolver = pluginResolverFactory.create();
        this.dependencyResolutionServices = dependencyResolutionServices;
    }

    @PathSensitive(PathSensitivity.RELATIVE)
    @InputFiles
    @SkipWhenEmpty
    abstract DirectoryProperty getExtractedPluginRequestsClassesDirectory();

    @OutputDirectory
    abstract DirectoryProperty getPluginAdapterSourcesOutputDirectory();

    @Internal
    abstract ListProperty<PrecompiledGroovyScript> getScriptPlugins();

    @TaskAction
    void generatePluginAdapters() {
        fileSystemOperations.delete(spec -> spec.delete(getPluginAdapterSourcesOutputDirectory()));
        getPluginAdapterSourcesOutputDirectory().get().getAsFile().mkdirs();

        // TODO: Use worker API?
        for (PrecompiledGroovyScript scriptPlugin : getScriptPlugins().get()) {
            PluginRequests pluginRequests = getValidPluginRequests(scriptPlugin);
            generateScriptPluginAdapter(scriptPlugin, pluginRequests);
        }
    }

    private PluginRequests getValidPluginRequests(PrecompiledGroovyScript scriptPlugin) {
        CompiledScript<PluginsAwareScript, ?> pluginsBlock = loadCompiledPluginsBlocks(scriptPlugin);
        if (!pluginsBlock.getRunDoesSomething()) {
            return PluginRequests.EMPTY;
        }
        PluginRequests pluginRequests = extractPluginRequests(pluginsBlock, scriptPlugin);
        for (PluginRequestInternal pluginRequest : pluginRequests) {
            if (pluginRequest.getVersion() != null) {
                PluginConfigurer pluginConfigurer = new PluginConfigurer(pluginRequest, scriptPlugin.getSource());
                pluginResolver.resolve(pluginRequest, pluginConfigurer);
                pluginConfigurer.configurePluginDependencies();
            }
        }
        return pluginRequests;
    }

    private PluginRequests extractPluginRequests(CompiledScript<PluginsAwareScript, ?> pluginsBlock, PrecompiledGroovyScript scriptPlugin) {
        try {
            PluginsAwareScript pluginsAwareScript = pluginsBlock.loadClass().getDeclaredConstructor().newInstance();
            pluginsAwareScript.setScriptSource(scriptPlugin.getSource());
            pluginsAwareScript.init("dummy", serviceRegistry);
            pluginsAwareScript.run();
            return pluginsAwareScript.getPluginRequests();
        } catch (Exception e) {
            throw new IllegalStateException("Could not execute plugins block", e);
        }
    }

    private CompiledScript<PluginsAwareScript, ?> loadCompiledPluginsBlocks(PrecompiledGroovyScript scriptPlugin) {
        CompileOperation<?> pluginsCompileOperation = compileOperationFactory.getPluginsBlockCompileOperation(scriptPlugin.getScriptTarget());
        File compiledPluginRequestsDir = getExtractedPluginRequestsClassesDirectory().get().dir(scriptPlugin.getId()).getAsFile();
        return scriptCompilationHandler.loadFromDir(scriptPlugin.getSource(), scriptPlugin.getContentHash(),
            classLoaderScope, compiledPluginRequestsDir, compiledPluginRequestsDir, pluginsCompileOperation, PluginsAwareScript.class);
    }

    private void generateScriptPluginAdapter(PrecompiledGroovyScript scriptPlugin, PluginRequests pluginRequests) {
        String targetClass = scriptPlugin.getTargetClassName();
        File outputFile = getPluginAdapterSourcesOutputDirectory().file(scriptPlugin.getGeneratedPluginClassName() + ".java").get().getAsFile();

        StringBuilder applyPlugins = new StringBuilder();
        if (!pluginRequests.isEmpty()) {
            for (PluginRequest pluginRequest : pluginRequests) {
                applyPlugins.append("        target.getPluginManager().apply(\"").append(pluginRequest.getId().getId()).append("\");\n");
            }
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outputFile.toURI())))) {
            writer.println("//CHECKSTYLE:OFF");
            writer.println("import org.gradle.util.GradleVersion;");
            writer.println("import org.gradle.groovy.scripts.BasicScript;");
            writer.println("import org.gradle.groovy.scripts.ScriptSource;");
            writer.println("import org.gradle.groovy.scripts.TextResourceScriptSource;");
            writer.println("import org.gradle.internal.resource.StringTextResource;");
            writer.println("/**");
            writer.println(" * Precompiled " + scriptPlugin.getId() + " script plugin.");
            writer.println(" **/");
            writer.println("public class " + scriptPlugin.getGeneratedPluginClassName() + " implements org.gradle.api.Plugin<" + targetClass + "> {");
            writer.println("    private static final String MIN_SUPPORTED_GRADLE_VERSION = \"5.0\";");
            writer.println("    public void apply(" + targetClass + " target) {");
            writer.println("        assertSupportedByCurrentGradleVersion();");
            writer.println("        " + applyPlugins + "");
            writer.println("        try {");
            writer.println("            Class<? extends BasicScript> precompiledScriptClass = Class.forName(\"" + scriptPlugin.getClassName() + "\").asSubclass(BasicScript.class);");
            writer.println("            BasicScript script = precompiledScriptClass.getDeclaredConstructor().newInstance();");
            writer.println("            script.setScriptSource(scriptSource(precompiledScriptClass));");
            writer.println("            script.init(target, " + scriptPlugin.serviceRegistryAccessCode() + ");");
            writer.println("            script.run();");
            writer.println("        } catch (Exception e) {");
            writer.println("            throw new RuntimeException(e);");
            writer.println("        }");
            writer.println("  }");
            writer.println("  private static ScriptSource scriptSource(Class<?> scriptClass) {");
            writer.println("      return new TextResourceScriptSource(new StringTextResource(scriptClass.getSimpleName(), \"\"));");
            writer.println("  }");
            writer.println("  private static void assertSupportedByCurrentGradleVersion() {");
            writer.println("      if (GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version(MIN_SUPPORTED_GRADLE_VERSION)) < 0) {");
            writer.println("          throw new RuntimeException(\"Precompiled Groovy script plugins require Gradle \"+MIN_SUPPORTED_GRADLE_VERSION+\" or higher\");");
            writer.println("      }");
            writer.println("  }");
            writer.println("}");
            writer.println("//CHECKSTYLE:ON");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private class PluginConfigurer implements PluginResolutionResult {
        private final PluginRequestInternal pluginRequest;
        private final ScriptSource scriptSource;

        private final StringBuilder notFoundMessageBuilder = new StringBuilder();
        private PluginResolution pluginResolution;

        public PluginConfigurer(PluginRequestInternal pluginRequest, ScriptSource scriptSource) {
            this.pluginRequest = pluginRequest;
            this.scriptSource = scriptSource;
        }

        @Override
        public void notFound(String sourceDescription, String notFoundMessage) {
            notFoundMessageBuilder.append("- ").append(notFoundMessage).append(".\n");
        }

        @Override
        public void notFound(String sourceDescription, String notFoundMessage, String notFoundDetail) {
            notFoundMessageBuilder.append("- ").append(notFoundMessage).append(". ").append(notFoundDetail).append(".\n");
        }

        @Override
        public void found(String sourceDescription, PluginResolution pluginResolution) {
            this.pluginResolution = pluginResolution;
        }

        @Override
        public boolean isFound() {
            return pluginResolution != null;
        }

        void configurePluginDependencies() {
            if (!isFound()) {
                throw new LocationAwareException(new UnknownPluginException(String.format("Plugin %s not found:\n%s", pluginRequest, notFoundMessageBuilder)),
                    scriptSource.getResource().getLocation().getDisplayName(),
                    pluginRequest.getLineNumber());
            }
            pluginResolution.execute(new PluginResolveContext() {
                @Override
                public void addLegacy(PluginId pluginId, String m2RepoUrl, Object dependencyNotation) {
                    throw new UnsupportedOperationException("External plugin should be resolved to an artifact");
                }

                @Override
                public void addLegacy(PluginId pluginId, Object dependencyNotation) {
                    dependencyResolutionServices.getResolveRepositoryHandler().forEach(r -> {
                        project.getRepositories().add(r);
                    });
                    project.getDependencies().add("implementation", dependencyNotation);
                }

                @Override
                public void add(PluginImplementation<?> plugin) {
                    throw new UnsupportedOperationException("External plugin should be resolved to an artifact");
                }

                @Override
                public void addFromDifferentLoader(PluginImplementation<?> plugin) {
                    throw new UnsupportedOperationException("External plugin should be resolved to an artifact");
                }
            });
        }
    }
}
