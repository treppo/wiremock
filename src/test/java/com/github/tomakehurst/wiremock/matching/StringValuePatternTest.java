/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.matching;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.reflect.ClassPath;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StringValuePatternTest {

    @Test
    public void allSubclassesHaveWorkingToString() throws Exception {
        ImmutableSet<ClassPath.ClassInfo> allClasses = ClassPath
            .from(Thread.currentThread().getContextClassLoader())
            .getAllClasses();

        FluentIterable<Class<?>> classes = from(allClasses)
            .filter(new Predicate<ClassPath.ClassInfo>() {
                @Override
                public boolean apply(ClassPath.ClassInfo input) {
                    return input.getPackageName().startsWith("com.github.tomakehurst.wiremock.matching");
                }
            })
            .transform(new Function<ClassPath.ClassInfo, Class<?>>() {
                @Override
                public Class<?> apply(ClassPath.ClassInfo input) {
                    try {
                        return input.load();
                    } catch (Throwable e) {
                        return Object.class;
                    }
                }
            })
            .filter(Predicates.assignableFrom(StringValuePattern.class))
            .filter(new Predicate<Class<?>>() {
                @Override
                public boolean apply(Class<?> input) {
                    return !Modifier.isAbstract(input.getModifiers());
                }
            });


        for (Class<?> clazz: classes) {
            Constructor<?> constructor = findConstructorWithStringParamInFirstPosition(clazz);
            Stopwatch stopwatch = Stopwatch.createStarted();
            assertThat(constructor
                .getParameterAnnotations()[0][0]
                .annotationType()
                .getSimpleName(), is("JsonProperty"));
            stopwatch.stop();
        }

    }

    private Constructor<?> findConstructorWithStringParamInFirstPosition(Class<?> clazz) {
        return Iterables.find(asList(clazz.getConstructors()), new Predicate<Constructor<?>>() {
            @Override
            public boolean apply(Constructor<?> input) {
                return input.getParameterTypes().length > 0 && input.getParameterTypes()[0].equals(String.class);
            }
        });
    }
}
