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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Test for component renames and removals in Camel 3.0.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3-migration-guide.html">Camel 3.0 Migration Guide</a>
 */
public class ComponentChangesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_0);
    }

    /**
     * Test http4 component rename to http
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_renamed_components
     */
    @DocumentExample
    @Test
    void testHttp4ComponentRename() {
        rewriteRun(java(
            """
            import org.apache.camel.component.http4.HttpComponent;

            public class MyRoute {
                HttpComponent component;
            }
            """,
            """
            import org.apache.camel.component.http.HttpComponent;

            public class MyRoute {
                HttpComponent component;
            }
            """
        ));
    }

    /**
     * Test netty4 component rename to netty
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_renamed_components
     */
    @Test
    void testNetty4ComponentRename() {
        rewriteRun(java(
            """
            import org.apache.camel.component.netty4.NettyComponent;

            public class MyRoute {
                NettyComponent component;
            }
            """,
            """
            import org.apache.camel.component.netty.NettyComponent;

            public class MyRoute {
                NettyComponent component;
            }
            """
        ));
    }

    /**
     * Test mongodb3 component rename to mongodb
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_renamed_components
     */
    @Test
    void testMongodb3ComponentRename() {
        rewriteRun(java(
            """
            import org.apache.camel.component.mongodb3.MongoDbComponent;

            public class MyRoute {
                MongoDbComponent component;
            }
            """,
            """
            import org.apache.camel.component.mongodb.MongoDbComponent;

            public class MyRoute {
                MongoDbComponent component;
            }
            """
        ));
    }

    /**
     * Test quartz2 component rename to quartz
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_renamed_components
     */
    @Test
    void testQuartz2ComponentRename() {
        rewriteRun(java(
            """
            import org.apache.camel.component.quartz2.QuartzComponent;

            public class MyRoute {
                QuartzComponent component;
            }
            """,
            """
            import org.apache.camel.component.quartz.QuartzComponent;

            public class MyRoute {
                QuartzComponent component;
            }
            """
        ));
    }

    /**
     * Test AWS component split
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_aws
     */
    @Test
    void testAwsComponentSplit() {
        rewriteRun(pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-aws</artifactId>
                        <version>2.25.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-aws-s3</artifactId>
                        <version>3.0.x</version>
                    </dependency>
                </dependencies>
            </project>
            """
        ));
    }

    /**
     * Test removal of deprecated camel-jibx
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_removed_components
     */
    @Test
    void testCamelJibxRemoval() {
        rewriteRun(pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-jibx</artifactId>
                        <version>2.25.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-core</artifactId>
                        <version>2.25.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-core</artifactId>
                        <version>3.0.x</version>
                    </dependency>
                </dependencies>
            </project>
            """
        ));
    }

    /**
     * Test removal of deprecated camel-linkedin
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_removed_components
     */
    @Test
    void testCamelLinkedinRemoval() {
        rewriteRun(pomXml(
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-linkedin</artifactId>
                        <version>2.25.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                </dependencies>
            </project>
            """
        ));
    }
}
