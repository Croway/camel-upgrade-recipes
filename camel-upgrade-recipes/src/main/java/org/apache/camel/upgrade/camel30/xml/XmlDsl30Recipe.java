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
package org.apache.camel.upgrade.camel30.xml;

import org.apache.camel.upgrade.AbstractCamelXmlVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;

/**
 * Recipe for XML DSL changes from Camel 2.x to 3.0.
 * Based on <a href="https://camel.apache.org/manual/camel-3-migration-guide.html">Camel 3.0 Migration Guide</a>
 */
public class XmlDsl30Recipe extends Recipe {

    // Map of XPath matchers to new tag names for simple renames
    private static final Map<XPathMatcher, String> TAG_RENAMES = new HashMap<>();
    static {
        // <custom> load balancer → <customLoadBalancer>
        TAG_RENAMES.put(new XPathMatcher("//loadBalance/custom"), "customLoadBalancer");
    }

    // Map of XPath matchers to attribute renames
    private static final Map<XPathMatcher, Map<String, String>> ATTRIBUTE_RENAMES = new HashMap<>();
    static {
        // <setHeader headerName="x"> → <setHeader name="x">
        Map<String, String> setHeaderAttrs = new HashMap<>();
        setHeaderAttrs.put("headerName", "name");
        ATTRIBUTE_RENAMES.put(new XPathMatcher("//setHeader"), setHeaderAttrs);

        // <setProperty propertyName="x"> → <setProperty name="x">
        Map<String, String> setPropertyAttrs = new HashMap<>();
        setPropertyAttrs.put("propertyName", "name");
        ATTRIBUTE_RENAMES.put(new XPathMatcher("//setProperty"), setPropertyAttrs);
    }

    // Map for nested element renames (completionSize → completionSizeExpression)
    private static final Map<XPathMatcher, String> NESTED_ELEMENT_RENAMES = new HashMap<>();
    static {
        NESTED_ELEMENT_RENAMES.put(new XPathMatcher("//aggregate/completionSize"), "completionSizeExpression");
        NESTED_ELEMENT_RENAMES.put(new XPathMatcher("//aggregate/completionTimeout"), "completionTimeoutExpression");
    }

    @Override
    public String getDisplayName() {
        return "Camel XML DSL changes for 3.0";
    }

    @Override
    public String getDescription() {
        return "Apache Camel XML DSL migration from version 2.x to 3.0.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AbstractCamelXmlVisitor() {
            @Override
            public Xml.Tag doVisitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.doVisitTag(tag, ctx);

                // Apply tag renames
                for (Map.Entry<XPathMatcher, String> entry : TAG_RENAMES.entrySet()) {
                    if (entry.getKey().matches(getCursor()) && !entry.getValue().equals(t.getName())) {
                        return t.withName(entry.getValue());
                    }
                }

                // Apply nested element renames
                for (Map.Entry<XPathMatcher, String> entry : NESTED_ELEMENT_RENAMES.entrySet()) {
                    if (entry.getKey().matches(getCursor()) && !entry.getValue().equals(t.getName())) {
                        return t.withName(entry.getValue());
                    }
                }

                // Handle <hystrix> → <circuitBreaker> rename
                if ("hystrix".equals(t.getName()) && new XPathMatcher("//*").matches(getCursor())) {
                    return t.withName("circuitBreaker");
                }

                // Apply attribute renames (these don't change the tag name, so we can batch them)
                for (Map.Entry<XPathMatcher, Map<String, String>> entry : ATTRIBUTE_RENAMES.entrySet()) {
                    if (entry.getKey().matches(getCursor())) {
                        for (Map.Entry<String, String> attrRename : entry.getValue().entrySet()) {
                            Xml.Tag renamed = renameAttribute(t, attrRename.getKey(), attrRename.getValue());
                            if (renamed != t) {
                                t = renamed;
                            }
                        }
                    }
                }

                return t;
            }

            /**
             * Renames an attribute on a tag, only if it exists
             */
            private Xml.Tag renameAttribute(Xml.Tag tag, String oldName, String newName) {
                // Check if the attribute exists first
                boolean hasAttribute = tag.getAttributes().stream()
                    .anyMatch(attr -> attr instanceof Xml.Attribute
                        && oldName.equals(((Xml.Attribute) attr).getKeyAsString()));

                if (!hasAttribute) {
                    return tag;
                }

                return tag.withAttributes(
                    tag.getAttributes().stream()
                        .map(attr -> {
                            if (attr instanceof Xml.Attribute) {
                                Xml.Attribute attribute = (Xml.Attribute) attr;
                                if (oldName.equals(attribute.getKeyAsString())) {
                                    return attribute.withKey(
                                        ((Xml.Ident) attribute.getKey()).withName(newName)
                                    );
                                }
                            }
                            return attr;
                        })
                        .toList()
                );
            }
        };
    }
}
