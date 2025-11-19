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
package org.apache.camel.upgrade.camel30.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Recipe migrating changes from Camel 2.x to 3.0, for more details see the
 * <a href="https://camel.apache.org/manual/camel-3-migration-guide.html">documentation</a>.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class CamelAPIsRecipe extends Recipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelAPIsRecipe.class);

    // Method matchers for API changes
    private static final String MATCHER_CONTEXT_GET_PROPERTIES = "org.apache.camel.CamelContext getProperties()";
    private static final String MATCHER_REGISTRY_PUT = "org.apache.camel.spi.Registry put(java.lang.String, java.lang.Object)";
    private static final String MATCHER_SIMPLE_REGISTRY_PUT = "org.apache.camel.impl.SimpleRegistry put(java.lang.String, java.lang.Object)";
    private static final String MATCHER_CONTEXT_START_ROUTE = "org.apache.camel.CamelContext startRoute(java.lang.String)";
    private static final String MATCHER_CONTEXT_STOP_ROUTE = "org.apache.camel.CamelContext stopRoute(java.lang.String)";
    private static final String MATCHER_CONTEXT_SUSPEND_ROUTE = "org.apache.camel.CamelContext suspendRoute(java.lang.String)";
    private static final String MATCHER_CONTEXT_RESUME_ROUTE = "org.apache.camel.CamelContext resumeRoute(java.lang.String)";
    private static final String MATCHER_CONTEXT_GET_ROUTE_STATUS = "org.apache.camel.CamelContext getRouteStatus(java.lang.String)";

    @Override
    public String getDisplayName() {
        return "Camel API changes for Camel 3.0";
    }

    @Override
    public String getDescription() {
        return "Apache Camel API migration from version 2.x to 3.0. Removal of deprecated APIs and package changes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {

            @Override
            protected J.MethodInvocation doVisitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);

                // CamelContext.getProperties() → CamelContext.getGlobalOptions()
                if (getMethodMatcher(MATCHER_CONTEXT_GET_PROPERTIES).matches(mi)) {
                    mi = mi.withName(mi.getName().withSimpleName("getGlobalOptions"));
                }

                // Registry.put() → Registry.bind()
                // SimpleRegistry.put() → SimpleRegistry.bind()
                if (getMethodMatcher(MATCHER_REGISTRY_PUT).matches(mi) ||
                    getMethodMatcher(MATCHER_SIMPLE_REGISTRY_PUT).matches(mi)) {
                    mi = mi.withName(mi.getName().withSimpleName("bind"));
                }

                // CamelContext.startRoute() → CamelContext.getRouteController().startRoute()
                if (getMethodMatcher(MATCHER_CONTEXT_START_ROUTE).matches(mi)) {
                    return wrapWithRouteController(mi, ctx);
                }

                // CamelContext.stopRoute() → CamelContext.getRouteController().stopRoute()
                if (getMethodMatcher(MATCHER_CONTEXT_STOP_ROUTE).matches(mi)) {
                    return wrapWithRouteController(mi, ctx);
                }

                // CamelContext.suspendRoute() → CamelContext.getRouteController().suspendRoute()
                if (getMethodMatcher(MATCHER_CONTEXT_SUSPEND_ROUTE).matches(mi)) {
                    return wrapWithRouteController(mi, ctx);
                }

                // CamelContext.resumeRoute() → CamelContext.getRouteController().resumeRoute()
                if (getMethodMatcher(MATCHER_CONTEXT_RESUME_ROUTE).matches(mi)) {
                    return wrapWithRouteController(mi, ctx);
                }

                // CamelContext.getRouteStatus() → CamelContext.getRouteController().getRouteStatus()
                if (getMethodMatcher(MATCHER_CONTEXT_GET_ROUTE_STATUS).matches(mi)) {
                    return wrapWithRouteController(mi, ctx);
                }

                return mi;
            }

            /**
             * Wraps route control methods with .getRouteController()
             * e.g., context.startRoute("foo") → context.getRouteController().startRoute("foo")
             */
            private J.MethodInvocation wrapWithRouteController(J.MethodInvocation mi, ExecutionContext ctx) {
                if (mi.getSelect() == null) {
                    return mi;
                }

                String methodName = mi.getSimpleName();
                List<Expression> args = mi.getArguments();

                // Build template based on number of arguments
                StringBuilder templateBuilder = new StringBuilder();
                templateBuilder.append("#{any(org.apache.camel.CamelContext)}.getRouteController().");
                templateBuilder.append(methodName);
                templateBuilder.append("(");

                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) templateBuilder.append(", ");
                    templateBuilder.append("#{any()}");
                }
                templateBuilder.append(")");

                Object[] params = new Object[args.size() + 1];
                params[0] = mi.getSelect();
                for (int i = 0; i < args.size(); i++) {
                    params[i + 1] = args.get(i);
                }

                J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                    .builder(templateBuilder.toString())
                    .build()
                    .apply(getCursor(), mi.getCoordinates().replace(), params);

                return result.withPrefix(mi.getPrefix());
            }

            @Override
            protected J.Annotation doVisitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.doVisitAnnotation(annotation, ctx);

                // Handle @Consume, @Produce, @EndpointInject annotations
                // Remove 'ref' attribute or convert to uri="ref:xxx"
                if (a.getType() != null) {
                    String annotationType = a.getType().toString();
                    if (annotationType.equals("org.apache.camel.Consume") ||
                        annotationType.equals("org.apache.camel.Produce") ||
                        annotationType.equals("org.apache.camel.EndpointInject")) {

                        // Check if annotation has 'ref' attribute
                        if (a.getArguments() != null) {
                            for (Expression arg : a.getArguments()) {
                                if (arg instanceof J.Assignment) {
                                    J.Assignment assignment = (J.Assignment) arg;
                                    J.Identifier variable = (J.Identifier) assignment.getVariable();

                                    if ("ref".equals(variable.getSimpleName())) {
                                        // Convert ref="myBean" to uri="ref:myBean"
                                        // Note: This is a simplified transformation
                                        // In a full implementation, we would reconstruct the annotation
                                        LOGGER.info("Found deprecated 'ref' attribute in {} annotation. " +
                                                   "Please manually convert to uri=\"ref:beanName\"", annotationType);
                                    }
                                }
                            }
                        }
                    }
                }

                return a;
            }
        });
    }
}
