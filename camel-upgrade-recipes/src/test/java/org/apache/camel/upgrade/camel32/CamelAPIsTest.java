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
package org.apache.camel.upgrade.camel32;

import org.apache.camel.upgrade.CamelTestUtil;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Test for {@link org.apache.camel.upgrade.camel32.java.CamelAPIsRecipe}.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_2.html">Camel 3.2 Upgrade Guide</a>
 */
public class CamelAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_2)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v3_1, "camel-core", "camel-api", "camel-support"))
            .typeValidationOptions(TypeValidation.none());
    }

    /**
     * NOTE: JndiRegistry was removed in Camel 3.0, not 3.2.
     * The upgrade guide mentioning it in 3.2 is misleading.
     * See camel30/CamelAPIsTest for the actual migration test.
     */

    /**
     * Test Rest Configuration API changes
     * https://camel.apache.org/manual/camel-3x-upgrade-guide-3_2.html#_rest_configuration
     */
    @DocumentExample
    @Test
    void testRestConfigurationApiChange() {
        // This test verifies that a comment is added for manual migration
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;
            import org.apache.camel.model.rest.RestConfiguration;

            public class Test {
                void method(CamelContext context) {
                    RestConfiguration config = context.getRestConfiguration("jetty", true);
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;
            import org.apache.camel.model.rest.RestConfiguration;

            public class Test {
                void method(CamelContext context) {
                    /* FIXME: Rest Configuration API changed in Camel 3.2. Use CamelContext.getRestConfiguration() instead. See https://camel.apache.org/manual/camel-3x-upgrade-guide-3_2.html#_rest_configuration */RestConfiguration config = context.getRestConfiguration("jetty", true);
                }
            }
            """
        ));
    }
}
