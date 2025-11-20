/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.upgrade.camel33.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.HashMap;
import java.util.Map;

/**
 * Recipe migrating changes from Camel 3.2 to 3.3, for more details see the
 * <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_3.html">documentation</a>.
 *
 * This recipe handles:
 * - camel-main: Methods relocated from BaseMainSupport to MainConfigurationProperties
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class CamelAPIsRecipe extends Recipe {

    // Method matchers for camel-main API relocation from BaseMainSupport to MainConfigurationProperties
    private static final Map<String, String> MAIN_RELOCATED_METHODS = new HashMap<>();

    static {
        // Methods relocated to MainConfigurationProperties
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport getConfigurationClasses()",
            "configure().getConfigurationClasses()"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport setConfigurationClasses(java.lang.Class[])",
            "configure().setConfigurationClasses"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport addConfigurationClass(java.lang.Class)",
            "configure().addConfigurationClass"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport addConfiguration(java.lang.Object)",
            "configure().addConfiguration"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport getConfigurations()",
            "configure().getConfigurations()"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport setConfigurations(java.util.List)",
            "configure().setConfigurations"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport getRouteBuilderClasses()",
            "configure().getRouteBuilderClasses()"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport setRouteBuilderClasses(java.lang.Class[])",
            "configure().setRouteBuilderClasses"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport getRouteBuilders()",
            "configure().getRouteBuilders()"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport getRoutesBuilders()",
            "configure().getRoutesBuilders()"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport setRoutesBuilders(java.util.List)",
            "configure().setRoutesBuilders"
        );
        MAIN_RELOCATED_METHODS.put(
            "org.apache.camel.main.BaseMainSupport addRoutesBuilder(org.apache.camel.RoutesBuilder)",
            "configure().addRoutesBuilder"
        );
    }

    @Override
    public String getDisplayName() {
        return "Camel Java API changes for Camel 3.3";
    }

    @Override
    public String getDescription() {
        return "Apache Camel Java API migration from version 3.2 to 3.3. " +
               "Handles camel-main API relocation from BaseMainSupport to MainConfigurationProperties.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {

            @Override
            protected J.MethodInvocation doVisitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);

                // Handle camel-main API relocation: BaseMainSupport methods to MainConfigurationProperties
                for (Map.Entry<String, String> entry : MAIN_RELOCATED_METHODS.entrySet()) {
                    if (getMethodMatcher(entry.getKey()).matches(mi)) {
                        String newMethod = entry.getValue();

                        // Only transform if we have the select (the object on which method is called)
                        if (mi.getSelect() != null) {
                            try {
                                // Build template pattern based on whether it's a setter or getter
                                String templatePattern;
                                if (newMethod.endsWith("()")) {
                                    // Getter method: main.getXXX() -> main.configure().getXXX()
                                    templatePattern = "#{any()}.configure()." + newMethod.substring(newMethod.indexOf('.') + 1);
                                } else {
                                    // Setter/add method: main.setXXX(args) -> main.configure().setXXX(args)
                                    String methodName = newMethod.substring(newMethod.indexOf('.') + 1);
                                    if (mi.getArguments().size() == 1) {
                                        templatePattern = "#{any()}.configure()." + methodName + "(#{any()})";
                                    } else {
                                        // Skip complex cases
                                        return mi;
                                    }
                                }

                                J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                                    .builder(templatePattern)
                                    .build()
                                    .apply(
                                        getCursor(),
                                        mi.getCoordinates().replace(),
                                        mi.getSelect(),
                                        mi.getArguments().isEmpty() ? new Object[0] : new Object[]{mi.getArguments().get(0)}
                                    );

                                return result != null ? result.withPrefix(mi.getPrefix()) : mi;
                            } catch (Exception e) {
                                // If transformation fails, return unchanged
                                return mi;
                            }
                        }
                        break;
                    }
                }

                return mi;
            }
        });
    }
}
