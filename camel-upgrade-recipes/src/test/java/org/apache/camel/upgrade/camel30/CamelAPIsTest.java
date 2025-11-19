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
package org.apache.camel.upgrade.camel30;

import org.apache.camel.upgrade.CamelTestUtil;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Test for {@link org.apache.camel.upgrade.camel30.java.CamelAPIsRecipe}.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3-migration-guide.html">Camel 3.0 Migration Guide</a>
 */
public class CamelAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_0)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v2_25, "camel-core", "camel-test"))
            .typeValidationOptions(TypeValidation.none());
    }

    /**
     * Test CamelContext.getProperties() → CamelContext.getGlobalOptions()
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_configuring_global_options_on_camelcontext
     */
    @DocumentExample
    @Test
    void testGetPropertiesToGetGlobalOptions() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void configure(CamelContext context) {
                    context.getProperties().put("CamelJacksonEnableTypeConverter", "true");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void configure(CamelContext context) {
                    context.getGlobalOptions().put("CamelJacksonEnableTypeConverter", "true");
                }
            }
            """
        ));
    }

    /**
     * Test Registry.put() → Registry.bind()
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_generic_information
     */
    @Test
    void testRegistryPutToBind() {
        rewriteRun(java(
            """
            import org.apache.camel.impl.SimpleRegistry;

            public class TestRegistry {
                public void setup() {
                    SimpleRegistry registry = new SimpleRegistry();
                    registry.put("myBean", new Object());
                }
            }
            """,
            """
            import org.apache.camel.support.SimpleRegistry;

            public class TestRegistry {
                public void setup() {
                    SimpleRegistry registry = new SimpleRegistry();
                    registry.bind("myBean", new Object());
                }
            }
            """
        ));
    }

    /**
     * Test route control methods moved to RouteController
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_controlling_routes
     */
    @Test
    void testStartRouteMovedToRouteController() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void start(CamelContext context) throws Exception {
                    context.startRoute("myRoute");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void start(CamelContext context) throws Exception {
                    context.getRouteController().startRoute("myRoute");
                }
            }
            """
        ));
    }

    @Test
    void testStopRouteMovedToRouteController() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void stop(CamelContext context) throws Exception {
                    context.stopRoute("myRoute");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void stop(CamelContext context) throws Exception {
                    context.getRouteController().stopRoute("myRoute");
                }
            }
            """
        ));
    }

    @Test
    void testSuspendRouteMovedToRouteController() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void suspend(CamelContext context) throws Exception {
                    context.suspendRoute("myRoute");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void suspend(CamelContext context) throws Exception {
                    context.getRouteController().suspendRoute("myRoute");
                }
            }
            """
        ));
    }

    @Test
    void testResumeRouteMovedToRouteController() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void resume(CamelContext context) throws Exception {
                    context.resumeRoute("myRoute");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;

            public class TestRoute {
                public void resume(CamelContext context) throws Exception {
                    context.getRouteController().resumeRoute("myRoute");
                }
            }
            """
        ));
    }

    @Test
    void testGetRouteStatusMovedToRouteController() {
        rewriteRun(java(
            """
            import org.apache.camel.CamelContext;
            import org.apache.camel.ServiceStatus;

            public class TestRoute {
                public ServiceStatus getStatus(CamelContext context) {
                    return context.getRouteStatus("myRoute");
                }
            }
            """,
            """
            import org.apache.camel.CamelContext;
            import org.apache.camel.ServiceStatus;

            public class TestRoute {
                public ServiceStatus getStatus(CamelContext context) {
                    return context.getRouteController().getRouteStatus("myRoute");
                }
            }
            """
        ));
    }

    /**
     * Test package relocation from org.apache.camel.impl to org.apache.camel.support
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_migrating_custom_components
     */
    @Test
    void testDefaultComponentPackageMove() {
        rewriteRun(java(
            """
            import org.apache.camel.impl.DefaultComponent;

            public class MyComponent extends DefaultComponent {
            }
            """,
            """
            import org.apache.camel.support.DefaultComponent;

            public class MyComponent extends DefaultComponent {
            }
            """
        ));
    }

    @Test
    void testHelperClassPackageMove() {
        rewriteRun(java(
            """
            import org.apache.camel.util.ExchangeHelper;

            public class TestHelper {
                public void use() {
                    ExchangeHelper.toString(null);
                }
            }
            """,
            """
            import org.apache.camel.support.ExchangeHelper;

            public class TestHelper {
                public void use() {
                    ExchangeHelper.toString(null);
                }
            }
            """
        ));
    }
}
