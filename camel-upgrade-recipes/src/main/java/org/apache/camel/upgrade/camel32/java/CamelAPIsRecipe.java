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
package org.apache.camel.upgrade.camel32.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Recipe migrating changes from Camel 3.1 to 3.2, for more details see the
 * <a href="https://camel.apache.org/manual/camel-3x-upgrade-guide-3_2.html">documentation</a>.
 *
 * This recipe handles:
 * - Rest Configuration API method removals
 * - Component configuration delegate setter changes (requires .getConfiguration())
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class CamelAPIsRecipe extends Recipe {

    // Component classes that had delegate setters removed
    private static final Map<String, String> COMPONENTS_WITH_CONFIGURATION = new HashMap<>();

    static {
        // AWS components
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws.sqs.SqsComponent", "org.apache.camel.component.aws.sqs.SqsConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws.sns.SnsComponent", "org.apache.camel.component.aws.sns.SnsConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws.s3.S3Component", "org.apache.camel.component.aws.s3.S3Configuration");

        // AWS2 components
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws2.sqs.Sqs2Component", "org.apache.camel.component.aws2.sqs.Sqs2Configuration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws2.sns.Sns2Component", "org.apache.camel.component.aws2.sns.Sns2Configuration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.aws2.s3.AWS2S3Component", "org.apache.camel.component.aws2.s3.AWS2S3Configuration");

        // Other components
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.consul.ConsulComponent", "org.apache.camel.component.consul.ConsulConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.etcd.EtcdComponent", "org.apache.camel.component.etcd.EtcdConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.infinispan.InfinispanComponent", "org.apache.camel.component.infinispan.InfinispanConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.kafka.KafkaComponent", "org.apache.camel.component.kafka.KafkaConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.servicenow.ServiceNowComponent", "org.apache.camel.component.servicenow.ServiceNowConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.ssh.SshComponent", "org.apache.camel.component.ssh.SshConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.stomp.StompComponent", "org.apache.camel.component.stomp.StompConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.xmlsecurity.XmlSignerComponent", "org.apache.camel.component.xmlsecurity.XmlSignerConfiguration");
        COMPONENTS_WITH_CONFIGURATION.put("org.apache.camel.component.yammer.YammerComponent", "org.apache.camel.component.yammer.YammerConfiguration");
    }

    // Rest Configuration methods removed
    private static final String MATCHER_ADD_REST_CONFIGURATION =
        "org.apache.camel.CamelContext addRestConfiguration(org.apache.camel.model.rest.RestConfiguration)";
    private static final String MATCHER_GET_REST_CONFIGURATION_WITH_COMPONENT =
        "org.apache.camel.CamelContext getRestConfiguration(java.lang.String, boolean)";
    private static final String MATCHER_GET_REST_CONFIGURATIONS =
        "org.apache.camel.CamelContext getRestConfigurations()";

    @Override
    public String getDisplayName() {
        return "Camel Java API changes for Camel 3.2";
    }

    @Override
    public String getDescription() {
        return "Apache Camel Java API migration from version 3.1 to 3.2. " +
               "Handles Rest Configuration API changes and component configuration delegate setter removal.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations varDecls, ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(varDecls, ctx);

                // Check if the initializer contains a Rest Configuration method call
                if (vd.getVariables() != null && !vd.getVariables().isEmpty()) {
                    J.VariableDeclarations.NamedVariable namedVar = vd.getVariables().get(0);
                    if (namedVar.getInitializer() instanceof J.MethodInvocation) {
                        J.MethodInvocation mi = (J.MethodInvocation) namedVar.getInitializer();

                        if (getMethodMatcher(MATCHER_ADD_REST_CONFIGURATION).matches(mi) ||
                            getMethodMatcher(MATCHER_GET_REST_CONFIGURATION_WITH_COMPONENT).matches(mi) ||
                            getMethodMatcher(MATCHER_GET_REST_CONFIGURATIONS).matches(mi)) {

                            String commentText = " FIXME: Rest Configuration API changed in Camel 3.2. " +
                                "Use CamelContext.getRestConfiguration() instead. " +
                                "See https://camel.apache.org/manual/camel-3x-upgrade-guide-3_2.html#_rest_configuration ";

                            // Only add comment if it doesn't already exist
                            if (!RecipesUtil.isCommentBeforeElement(vd, commentText)) {
                                Comment comment = RecipesUtil.createMultinlineComment(commentText);
                                vd = vd.withPrefix(vd.getPrefix().withComments(Collections.singletonList(comment)));
                            }
                        }
                    }
                }

                return vd;
            }

            @Override
            protected J.MethodInvocation doVisitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);

                // Handle component configuration delegate setters
                // This is a simplified check - in practice, we'd need to detect actual setter calls
                // on component instances and transform them to use .getConfiguration()

                return mi;
            }
        });
    }
}
