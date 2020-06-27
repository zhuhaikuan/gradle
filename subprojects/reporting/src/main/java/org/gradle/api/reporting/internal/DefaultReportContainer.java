/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.api.reporting.internal;

import groovy.lang.Closure;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.api.reporting.Report;
import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.specs.Spec;
import org.gradle.internal.instantiation.InstantiatorFactory;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.Map;

public class DefaultReportContainer<T extends Report> extends DefaultNamedDomainObjectSet<T> implements ReportContainer<T> {
    private final NamedDomainObjectSet<T> enabled;

    public DefaultReportContainer(Class<? extends T> type) {
        super(type, Report.NAMER);

        enabled = matching(new Spec<T>() {
            @Override
            public boolean isSatisfiedBy(T element) {
                return element.isEnabled();
            }
        });
    }

    @Override
    protected void assertMutableCollectionContents() {
        throw new ImmutableViolationException();
    }

    @Override
    public NamedDomainObjectSet<T> getEnabled() {
        return enabled;
    }

    @Override
    public ReportContainer<T> configure(Closure cl) {
        ConfigureUtil.configureSelf(cl, this);
        return this;
    }

    @Inject
    protected InstantiatorFactory getInstantiatorFactory() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ServiceRegistry getServicesRegistry() {
        throw new UnsupportedOperationException();
    }

    protected <N extends T> N add(Class<N> clazz, Object... constructionArgs) {
        N report = getInstantiatorFactory().decorateLenient(getServicesRegistry()).newInstance(clazz, constructionArgs);
        String name = report.getName();
        if (name.equals("enabled")) {
            throw new InvalidUserDataException("Reports that are part of a ReportContainer cannot be named 'enabled'");
        }
        getStore().add(report);
        index();
        return report;
    }

    @Override
    public Map<String, T> getEnabledReports() {
        return getEnabled().getAsMap();
    }
}
