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
package org.apache.camel.upgrade.camel31.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe migrating changes from Camel 3.0 to 3.1, for more details see the
 * <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_1.html">documentation</a>.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class CamelAPIsRecipe extends Recipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelAPIsRecipe.class);

    // Method matchers for Exchange property migrations
    private static final String MATCHER_EXCHANGE_SET_PROPERTY = "org.apache.camel.Exchange setProperty(java.lang.String, java.lang.Object)";
    private static final String MATCHER_EXCHANGE_GET_PROPERTY = "org.apache.camel.Exchange getProperty(java.lang.String, java.lang.Class)";

    @Override
    public String getDisplayName() {
        return "Camel API changes for Camel 3.1";
    }

    @Override
    public String getDescription() {
        return "Apache Camel API migration from version 3.0 to 3.1. " +
               "Migrates Exchange property-based API calls to dedicated method calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {

            @Override
            protected J.MethodInvocation doVisitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);

                // Handle Exchange.ROUTE_STOP property → exchange.setRouteStop(true)
                if (getMethodMatcher(MATCHER_EXCHANGE_SET_PROPERTY).matches(mi)) {
                    mi = migrateExchangeSetProperty(mi, ctx);
                }

                // Handle Exchange.ROLLBACK_ONLY property → exchange.setRollbackOnly(true)
                // Handle Exchange.ROLLBACK_ONLY_LAST property → exchange.setRollbackOnlyLast(true)
                if (getMethodMatcher(MATCHER_EXCHANGE_SET_PROPERTY).matches(mi)) {
                    mi = migrateRollbackProperties(mi, ctx);
                }

                return mi;
            }

            /**
             * Migrates exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE) → exchange.setRouteStop(true)
             */
            private J.MethodInvocation migrateExchangeSetProperty(J.MethodInvocation mi, ExecutionContext ctx) {
                if (mi.getArguments().size() != 2) {
                    return mi;
                }

                Expression firstArg = mi.getArguments().get(0);
                Expression secondArg = mi.getArguments().get(1);

                // Check if first argument is Exchange.ROUTE_STOP
                String firstArgStr = firstArg.toString();
                if (firstArgStr.contains("Exchange.ROUTE_STOP") || firstArgStr.contains("\"Exchange.ROUTE_STOP\"")) {
                    // Convert to exchange.setRouteStop(true)
                    if (mi.getSelect() != null) {
                        J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                            .builder("#{any(org.apache.camel.Exchange)}.setRouteStop(true)")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getSelect());
                        return result.withPrefix(mi.getPrefix());
                    }
                }

                return mi;
            }

            /**
             * Migrates rollback property-based calls to method calls
             * - exchange.setProperty(Exchange.ROLLBACK_ONLY, Boolean.TRUE) → exchange.setRollbackOnly(true)
             * - exchange.setProperty(Exchange.ROLLBACK_ONLY_LAST, Boolean.TRUE) → exchange.setRollbackOnlyLast(true)
             */
            private J.MethodInvocation migrateRollbackProperties(J.MethodInvocation mi, ExecutionContext ctx) {
                if (mi.getArguments().size() != 2) {
                    return mi;
                }

                Expression firstArg = mi.getArguments().get(0);
                String firstArgStr = firstArg.toString();

                if (mi.getSelect() != null) {
                    // Check for ROLLBACK_ONLY
                    if (firstArgStr.contains("Exchange.ROLLBACK_ONLY") && !firstArgStr.contains("ROLLBACK_ONLY_LAST")) {
                        J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                            .builder("#{any(org.apache.camel.Exchange)}.setRollbackOnly(true)")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getSelect());
                        return result.withPrefix(mi.getPrefix());
                    }

                    // Check for ROLLBACK_ONLY_LAST
                    if (firstArgStr.contains("Exchange.ROLLBACK_ONLY_LAST")) {
                        J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                            .builder("#{any(org.apache.camel.Exchange)}.setRollbackOnlyLast(true)")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getSelect());
                        return result.withPrefix(mi.getPrefix());
                    }
                }

                return mi;
            }

            @Override
            protected J.FieldAccess doVisitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = super.doVisitFieldAccess(fieldAccess, ctx);

                // Add informational comment for deprecated constants
                String fieldName = fa.getSimpleName();
                if ("ROUTE_STOP".equals(fieldName) ||
                    "ROLLBACK_ONLY".equals(fieldName) ||
                    "ROLLBACK_ONLY_LAST".equals(fieldName) ||
                    "ERRORHANDLER_HANDLED".equals(fieldName) ||
                    "CREATED_TIMESTAMP".equals(fieldName)) {

                    // These constants are deprecated - the migration happens at the setProperty call site
                    // Just log for awareness
                    LOGGER.debug("Found deprecated Exchange constant: {}. " +
                               "If used with setProperty(), it will be migrated to a method call.", fieldName);
                }

                return fa;
            }
        });
    }
}
