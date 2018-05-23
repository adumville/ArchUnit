/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.lang.syntax;

import java.util.Collections;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.GivenClass;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;

import java.lang.Class;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;

public final class ArchRuleDefinition {
    private ArchRuleDefinition() {
    }

    /**
     * @see Creator#all(ClassesTransformer)
     */
    @PublicAPI(usage = ACCESS)
    public static <TYPE> GivenObjects<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).all(classesTransformer);
    }

    /**
     * @see Creator#no(ClassesTransformer)
     */
    @PublicAPI(usage = ACCESS)
    public static <TYPE> GivenObjects<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
        return priority(MEDIUM).no(classesTransformer);
    }

    @PublicAPI(usage = ACCESS)
    public static Creator priority(Priority priority) {
        return new Creator(priority);
    }

    @PublicAPI(usage = ACCESS)
    public static GivenClasses classes() {
        //returns a creator
        return priority(MEDIUM).classes();
    }


    @PublicAPI(usage = ACCESS)
    public static GivenClasses noClasses() {
        return priority(MEDIUM).noClasses();
    }

    public static GivenClass theClass(Class<?> clazz) {
        return priority(MEDIUM).theClass(clazz);
    }

    public static GivenClass theClass(String className) {
        return priority(MEDIUM).theClass(className);
    }

    public static final class Creator {
        private final Priority priority;

        private Creator(Priority priority) {
            this.priority = priority;
        }

        @PublicAPI(usage = ACCESS)
        public GivenClasses classes() {
            return new GivenClassesInternal(priority, ClassesIdentityTransformer.classes());
        }

        @PublicAPI(usage = ACCESS)
        public GivenClasses noClasses() {
            return new GivenClassesInternal(
                    priority,
                    ClassesIdentityTransformer.classes().as("no classes"),
                    ArchRuleDefinition.<JavaClass>negateCondition());
        }

        /**
         * Takes a {@link ClassesTransformer} to specify how the set of objects of interest is to be created
         * from {@link JavaClasses} (which are the general input obtained from a {@link ClassFileImporter}).
         *
         * @param <TYPE>             The target type to which the later used {@link ArchCondition ArchCondition&lt;TYPE&gt;}
         *                           will have to refer to
         * @param classesTransformer Transformer specifying how the imported {@link JavaClasses} are to be transformed
         * @return {@link GivenObjects} to guide the creation of an {@link ArchRule}
         */
        @PublicAPI(usage = ACCESS)
        public <TYPE> GivenObjects<TYPE> all(ClassesTransformer<TYPE> classesTransformer) {
            return new GivenObjectsInternal<>(priority, classesTransformer);
        }

        /**
         * Same as {@link #all(ClassesTransformer)}, but negates the following condition.
         */
        @PublicAPI(usage = ACCESS)
        public <TYPE> GivenObjects<TYPE> no(ClassesTransformer<TYPE> classesTransformer) {
            return new GivenObjectsInternal<>(
                    priority,
                    classesTransformer.as("no " + classesTransformer.getDescription()),
                    ArchRuleDefinition.<TYPE>negateCondition());
        }

        public GivenClass theClass(final Class<?> clazz) {
            ClassesTransformer<JavaClass> theClass = new AbstractClassesTransformer<JavaClass>("the class " + clazz.getName()) {
                @Override
                public Iterable<JavaClass> doTransform(JavaClasses classes) {
                    return Collections.singleton(classes.get(clazz));
                }
            };
            return new GivenClassInternal(priority, theClass, Functions.<ArchCondition<JavaClass>>identity());
        }

        public GivenClass theClass(final String className) {
            final Class<?> classInSystem;
            //there are de
            try {
                Class.forName(className); //TODO: is this cheating???
            } catch (java.lang.ClassNotFoundException exception) {

            }
            classInSystem = Class.forName(className);
            ClassesTransformer<JavaClass> theClass = new AbstractClassesTransformer<JavaClass>("the class " + classInSystem.getName()) {
                @Override
                public Iterable<JavaClass> doTransform(JavaClasses classes) {
                    return Collections.singleton(classes.get(classInSystem));
                }
            };
            return new GivenClassInternal(priority, theClass, Functions.<ArchCondition<JavaClass>>identity());


//            Class theClass = (Class) (clazz + ".class");
//            JavaClass theClass = new ClassFileImporter().importClass(clazz);

            /**GivenClassInternal takes in a priority and a transformer
             *
             * I would like to be able to look at all the classes in the set from the top level
             * and get the one corresponding to the String clazz, then return a GivenClass
             * based on this information
             */
        }


        public GivenClass noClass(final Class<?> clazz) {
            return null; //todo: fix this
        }


    }


    private static <T> Function<ArchCondition<T>, ArchCondition<T>> negateCondition() {
        return new Function<ArchCondition<T>, ArchCondition<T>>() {
            @Override
            public ArchCondition<T> apply(ArchCondition<T> condition) {
                return never(condition).as(condition.getDescription());
            }
        };
    }
}
