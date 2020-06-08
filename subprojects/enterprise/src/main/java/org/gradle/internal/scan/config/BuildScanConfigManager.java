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

package org.gradle.internal.scan.config;

import org.gradle.StartParameter;
import org.gradle.internal.Factory;
import org.gradle.util.VersionNumber;

import static org.gradle.internal.enterprise.core.GradleEnterprisePluginPresence.OLD_SCAN_PLUGIN_VERSION_MESSAGE;

/**
 * This is the meeting point between Gradle and the build scan plugin during initialization. This is effectively build scoped.
 */
public class BuildScanConfigManager implements BuildScanConfigProvider, BuildScanPluginApplied {

    private static final VersionNumber FIRST_VERSION_AWARE_OF_UNSUPPORTED = VersionNumber.parse("1.11");

    private final BuildScanPluginCompatibility compatibility;
    private final Factory<BuildScanConfig.Attributes> configAttributes;

    private final Requestedness requestedness;
    private boolean collected;

    BuildScanConfigManager(
        StartParameter startParameter,
        BuildScanPluginCompatibility compatibility,
        Factory<BuildScanConfig.Attributes> configAttributes
    ) {
        this.compatibility = compatibility;
        this.configAttributes = configAttributes;

        if (startParameter.isNoBuildScan()) {
            requestedness = Requestedness.DISABLED;
        } else if (startParameter.isBuildScan()) {
            requestedness = Requestedness.ENABLED;
        } else {
            requestedness = Requestedness.DEFAULTED;
        }
    }

    @Override
    public BuildScanConfig collect(BuildScanPluginMetadata pluginMetadata) {
        if (collected) {
            throw new IllegalStateException("Configuration has already been collected.");
        }

        VersionNumber pluginVersion = VersionNumber.parse(pluginMetadata.getVersion()).getBaseVersion();
        if (pluginVersion.compareTo(BuildScanPluginCompatibility.FIRST_GRADLE_ENTERPRISE_PLUGIN_VERSION) < 0) {
            throw new UnsupportedBuildScanPluginVersionException(OLD_SCAN_PLUGIN_VERSION_MESSAGE);
        }

        collected = true;
        BuildScanConfig.Attributes configAttributes = this.configAttributes.create();
        String unsupportedReason = compatibility.unsupportedReason();

        if (unsupportedReason != null) {
            if (isPluginAwareOfUnsupported(pluginVersion)) {
                return requestedness.toConfig(unsupportedReason, configAttributes);
            } else {
                throw new UnsupportedBuildScanPluginVersionException(unsupportedReason);
            }
        }

        return requestedness.toConfig(null, configAttributes);
    }

    private boolean isPluginAwareOfUnsupported(VersionNumber pluginVersion) {
        return pluginVersion.compareTo(FIRST_VERSION_AWARE_OF_UNSUPPORTED) >= 0;
    }

    @Override
    public boolean isBuildScanPluginApplied() {
        return collected;
    }

    private enum Requestedness {

        DEFAULTED(false, false),
        ENABLED(true, false),
        DISABLED(false, true);

        private final boolean enabled;
        private final boolean disabled;

        Requestedness(boolean enabled, boolean disabled) {
            this.enabled = enabled;
            this.disabled = disabled;
        }

        BuildScanConfig toConfig(final String unsupported, final BuildScanConfig.Attributes attributes) {
            return new BuildScanConfig() {
                @Override
                public boolean isEnabled() {
                    return enabled;
                }

                @Override
                public boolean isDisabled() {
                    return disabled;
                }

                @Override
                public String getUnsupportedMessage() {
                    return unsupported;
                }

                @Override
                public Attributes getAttributes() {
                    return attributes;
                }
            };
        }
    }

}
