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

package org.gradle.internal;

import java.util.concurrent.atomic.AtomicLong;

public class Thing {
    private static final long TIME_ZERO = System.nanoTime();
    private static final AtomicLong COUNTER = new AtomicLong();

    public static void log(String message) {
//        double duration = (System.nanoTime() - TIME_ZERO) / 1000000000.0;
//        System.out.println(String.format("[time=%s] [count=%s] [thread=%s] %s", duration, COUNTER.incrementAndGet(), Thread.currentThread().getId(), message));
    }
}
