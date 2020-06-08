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

package org.gradle.internal.enterprise.impl;

import org.gradle.api.provider.ProviderFactory;
import org.gradle.internal.enterprise.GradleEnterprisePluginBuildState;
import org.gradle.internal.enterprise.GradleEnterprisePluginCheckInResultHandler;
import org.gradle.internal.enterprise.GradleEnterprisePluginCheckInService;
import org.gradle.internal.enterprise.GradleEnterprisePluginConfig;
import org.gradle.internal.enterprise.GradleEnterprisePluginEndOfBuildListener;
import org.gradle.internal.enterprise.GradleEnterprisePluginMetadata;
import org.gradle.internal.enterprise.GradleEnterprisePluginRequiredServices;
import org.gradle.internal.enterprise.GradleEnterprisePluginService;
import org.gradle.internal.enterprise.GradleEnterprisePluginServiceFactory;
import org.gradle.internal.enterprise.core.GradleEnterprisePluginEndOfBuildNotifier;
import org.gradle.internal.enterprise.core.GradleEnterprisePluginPresence;
import org.gradle.internal.scan.config.BuildScanPluginApplied;
import org.gradle.internal.scan.eob.DefaultBuildScanEndOfBuildNotifier;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class GradleEnterprisePluginManager implements GradleEnterprisePluginCheckInService, GradleEnterprisePluginPresence, GradleEnterprisePluginEndOfBuildNotifier {

    // Used just for testing
    public static final String UNSUPPORTED_TOGGLE = "org.gradle.internal.unsupported-scan-plugin";
    public static final String UNSUPPORTED_TOGGLE_MESSAGE = "Build scan support disabled by secret toggle";

    private final GradleEnterprisePluginConfig config;
    private final GradleEnterprisePluginRequiredServices requiredServices;
    private final GradleEnterprisePluginBuildState buildState;

    private final BuildScanPluginApplied buildScanPluginApplied;
    private final DefaultBuildScanEndOfBuildNotifier buildScanEndOfBuildNotifier;

    private final ProviderFactory providerFactory;

    private boolean present;
    private GradleEnterprisePluginService pluginService;

    @Inject
    public GradleEnterprisePluginManager(
        GradleEnterprisePluginConfig config,
        GradleEnterprisePluginRequiredServices requiredServices,
        GradleEnterprisePluginBuildState buildState,
        BuildScanPluginApplied buildScanPluginApplied,
        DefaultBuildScanEndOfBuildNotifier buildScanEndOfBuildNotifier,
        ProviderFactory providerFactory
    ) {
        this.config = config;
        this.requiredServices = requiredServices;
        this.buildState = buildState;
        this.buildScanPluginApplied = buildScanPluginApplied;
        this.buildScanEndOfBuildNotifier = buildScanEndOfBuildNotifier;
        this.providerFactory = providerFactory;
    }

    @Override
    public void checkIn(GradleEnterprisePluginMetadata pluginMetadata, GradleEnterprisePluginServiceFactory serviceFactory, GradleEnterprisePluginCheckInResultHandler resultHandler) {
        if (!unsupported(pluginMetadata, resultHandler)) {
            present = true;
            doCheckIn(serviceFactory, resultHandler);
        }
    }

    @Override
    public boolean isPresent() {
        return present || buildScanPluginApplied.isBuildScanPluginApplied();
    }

    public void init() {
    }

    private boolean unsupported(@SuppressWarnings("unused") GradleEnterprisePluginMetadata pluginMetadata, GradleEnterprisePluginCheckInResultHandler resultHandler) {
        if (Boolean.getBoolean(UNSUPPORTED_TOGGLE)) {
            resultHandler.unsupported(UNSUPPORTED_TOGGLE_MESSAGE);
            return true;
        } else {
            return false;
        }
    }

    private void doCheckIn(GradleEnterprisePluginServiceFactory serviceFactory, GradleEnterprisePluginCheckInResultHandler resultHandler) {
        pluginService = serviceFactory.create(config, requiredServices, buildState);
        resultHandler.supported(providerFactory.provider(() -> pluginService));
    }

    @Override
    public void buildComplete(@Nullable Throwable buildFailure) {
        if (pluginService != null) {
            pluginService.getEndOfBuildListener().buildFinished(new GradleEnterprisePluginEndOfBuildListener.BuildResult() {
                @Nullable
                @Override
                public Throwable getFailure() {
                    return buildFailure;
                }
            });
        }

        buildScanEndOfBuildNotifier.fireBuildComplete(buildFailure);
    }
}
