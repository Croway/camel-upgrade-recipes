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
package org.apache.camel.upgrade.camel31;

import org.apache.camel.upgrade.CamelTestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Test for {@link org.apache.camel.upgrade.camel31.java.CamelAPIsRecipe}.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_1.html">Camel 3.1 Upgrade Guide</a>
 */
public class CamelAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_1)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v3_0, "camel-api", "camel-support"))
            .typeValidationOptions(TypeValidation.none());
    }

    /**
     * Test Exchange.ROUTE_STOP property migration to setRouteStop() method
     * https://camel.apache.org/manual/camel-3x-upgrade-guide-3_1.html#_exchange_route_stop
     */
    @DocumentExample
    @Test
    @Disabled
    void testRouteStopPropertyToMethod() {
        rewriteRun(java(
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                }
            }
            """,
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setRouteStop(true);
                }
            }
            """
        ));
    }

    /**
     * Test Exchange.ROLLBACK_ONLY property migration to setRollbackOnly() method
     * https://camel.apache.org/manual/camel-3x-upgrade-guide-3_1.html#_exchange_rollback_only_and_exchange_rollback_only_last
     */
    @Test
    @Disabled
    void testRollbackOnlyPropertyToMethod() {
        rewriteRun(java(
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setProperty(Exchange.ROLLBACK_ONLY, Boolean.TRUE);
                }
            }
            """,
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setRollbackOnly(true);
                }
            }
            """
        ));
    }

    /**
     * Test Exchange.ROLLBACK_ONLY_LAST property migration to setRollbackOnlyLast() method
     * https://camel.apache.org/manual/camel-3x-upgrade-guide-3_1.html#_exchange_rollback_only_and_exchange_rollback_only_last
     */
    @Test
    @Disabled
    void testRollbackOnlyLastPropertyToMethod() {
        rewriteRun(java(
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setProperty(Exchange.ROLLBACK_ONLY_LAST, Boolean.TRUE);
                }
            }
            """,
            """
            import org.apache.camel.Exchange;

            public class TestProcessor {
                public void process(Exchange exchange) {
                    exchange.setRollbackOnlyLast(true);
                }
            }
            """
        ));
    }
}
