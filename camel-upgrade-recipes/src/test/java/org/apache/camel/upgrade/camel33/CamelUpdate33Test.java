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
package org.apache.camel.upgrade.camel33;

import org.apache.camel.upgrade.CamelTestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Test for {@link org.apache.camel.upgrade.camel33.java.CamelAPIsRecipe}.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_3.html">Camel 3.3 Upgrade Guide</a>
 */
@Disabled
public class CamelUpdate33Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_3)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v3_2, "camel-infinispan", "camel-undertow", "camel-core", "camel-api", "camel-support", "camel-main"))
            .typeValidationOptions(TypeValidation.none());
    }

    /**
     * Test Karaf component removals
     * https://camel.apache.org/manual/camel-3x-upgrade-guide-3_3.html#_camel_karaf
     */
    @Test
    void testKarafComponentRemoval_Undertow() {
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
                        <artifactId>camel-undertow</artifactId>
                        <version>3.2.0</version>
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
            </project>
            """
        ));
    }

    @Test
    void testKarafComponentRemoval_JGroups() {
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
                        <artifactId>camel-jgroups</artifactId>
                        <version>3.2.0</version>
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
            </project>
            """
        ));
    }

    @Test
    void testKarafComponentRemoval_JGroupsRaft() {
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
                        <artifactId>camel-jgroups-raft</artifactId>
                        <version>3.2.0</version>
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
            </project>
            """
        ));
    }
}
