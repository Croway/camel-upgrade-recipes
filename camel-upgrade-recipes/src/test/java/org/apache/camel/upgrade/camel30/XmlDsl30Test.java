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

import static org.openrewrite.xml.Assertions.xml;

/**
 * Test for {@link org.apache.camel.upgrade.camel30.xml.XmlDsl30Recipe}.
 * Tests based on <a href="https://camel.apache.org/manual/camel-3-migration-guide.html">Camel 3.0 Migration Guide</a>
 */
public class XmlDsl30Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v3_0);
    }

    /**
     * Test <setHeader headerName="x"> → <setHeader name="x">
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_setheader_and_setproperty_in_xml_dsl
     */
    @DocumentExample
    @Test
    void testSetHeaderAttributeRename() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <setHeader headerName="foo">
                    <simple>Hello ${body}</simple>
                </setHeader>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <setHeader name="foo">
                    <simple>Hello ${body}</simple>
                </setHeader>
            </route>
            """
        ));
    }

    /**
     * Test <setProperty propertyName="x"> → <setProperty name="x">
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_setheader_and_setproperty_in_xml_dsl
     */
    @Test
    void testSetPropertyAttributeRename() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <setProperty propertyName="myProp">
                    <simple>myValue</simple>
                </setProperty>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <setProperty name="myProp">
                    <simple>myValue</simple>
                </setProperty>
            </route>
            """
        ));
    }

    /**
     * Test <completionSize> → <completionSizeExpression>
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_aggregate_eip_in_xml_dsl
     */
    @Test
    void testAggregateCompletionSizeRename() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <aggregate>
                    <correlationExpression>
                        <header>id</header>
                    </correlationExpression>
                    <completionSize>
                        <header>mySize</header>
                    </completionSize>
                    <to uri="mock:result"/>
                </aggregate>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <aggregate>
                    <correlationExpression>
                        <header>id</header>
                    </correlationExpression>
                    <completionSizeExpression>
                        <header>mySize</header>
                    </completionSizeExpression>
                    <to uri="mock:result"/>
                </aggregate>
            </route>
            """
        ));
    }

    /**
     * Test <completionTimeout> → <completionTimeoutExpression>
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_aggregate_eip_in_xml_dsl
     */
    @Test
    void testAggregateCompletionTimeoutRename() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <aggregate>
                    <correlationExpression>
                        <header>id</header>
                    </correlationExpression>
                    <completionTimeout>
                        <header>myTimeout</header>
                    </completionTimeout>
                    <to uri="mock:result"/>
                </aggregate>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <aggregate>
                    <correlationExpression>
                        <header>id</header>
                    </correlationExpression>
                    <completionTimeoutExpression>
                        <header>myTimeout</header>
                    </completionTimeoutExpression>
                    <to uri="mock:result"/>
                </aggregate>
            </route>
            """
        ));
    }

    /**
     * Test <custom> → <customLoadBalancer>
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_threads_delay_sample_throttle_eip_in_xml
     */
    @Test
    void testCustomLoadBalancerRename() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <loadBalance>
                    <custom ref="myLoadBalancer"/>
                    <to uri="mock:a"/>
                </loadBalance>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <loadBalance>
                    <customLoadBalancer ref="myLoadBalancer"/>
                    <to uri="mock:a"/>
                </loadBalance>
            </route>
            """
        ));
    }

    /**
     * Test <hystrix> → <circuitBreaker>
     * https://camel.apache.org/manual/camel-3-migration-guide.html#_hystrix_eip
     */
    @Test
    void testHystrixToCircuitBreaker() {
        rewriteRun(xml(
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <hystrix>
                    <to uri="http://someservice"/>
                    <onFallback>
                        <to uri="mock:fallback"/>
                    </onFallback>
                </hystrix>
            </route>
            """,
            """
            <route xmlns="http://camel.apache.org/schema/spring">
                <from uri="direct:start"/>
                <circuitBreaker>
                    <to uri="http://someservice"/>
                    <onFallback>
                        <to uri="mock:fallback"/>
                    </onFallback>
                </circuitBreaker>
            </route>
            """
        ));
    }
}
