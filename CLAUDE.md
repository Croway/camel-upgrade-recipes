# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an **Apache Camel Upgrade Recipes** project that provides automated code migration recipes using OpenRewrite. It helps users migrate Apache Camel applications across major and minor versions (3.x → 4.x and between 4.x versions).

**Key Characteristics:**
- Multi-module Maven project with two modules: `camel-upgrade-recipes` (core) and `camel-spring-boot-upgrade-recipes`
- Uses OpenRewrite's AST transformation framework
- Supports Java, XML, YAML, and Properties file migrations
- Tests require downloading multiple Camel versions for classpath validation

## Build and Development Commands

### Building
```bash
# Full build with tests
mvn clean install

# Build without tests (for faster iterations)
mvn clean install -DskipTests
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CamelAPIsTest

# Run a specific test method
mvn test -Dtest=CamelAPIsTest#testSpecificMethod

# Run test suites (for comprehensive migration path testing)
mvn test -Dtest=CamelUpdateLatestTestSuite
mvn test -Dtest=CamelUpdate410LtsTestSuite
```

### Code Quality
```bash
# Apply OpenRewrite best practices to this codebase
mvn -Popenrewrite rewrite:run

# This is run automatically in CI on pull requests
```

## Architecture Overview

### Module Structure

1. **camel-upgrade-recipes** (core module)
   - Contains Java recipe implementations
   - YAML recipe definitions in `src/main/resources/META-INF/rewrite/`
   - Organized by Camel version: `camel40/`, `camel41/`, `camel44/`, etc.

2. **camel-spring-boot-upgrade-recipes**
   - Depends on core module
   - Contains Spring Boot-specific property migrations
   - YAML-only recipes (no Java implementations)

### Recipe Organization Pattern

Recipes follow a **composable, version-based architecture**:

```
latest.yaml (4.15)
  ├─ includes 4.15.yaml
  ├─ includes 4.14.yaml
  ├─ includes 4.13.yaml
  └─ ... (all intermediate versions)
     └─ includes 4.0.yaml
```

**Key principle:** To migrate from version X to Y, apply all recipes between X and Y. The YAML files in `META-INF/rewrite/` compose these chains.

### Visitor Pattern Architecture

All Java-based transformations use OpenRewrite's visitor pattern with three abstract base classes:

1. **AbstractCamelJavaVisitor** - Base for Java code transformations
   - Wraps all `visit*` methods with error handling
   - Provides caching for `MethodMatcher` and `Pattern` objects
   - Child classes override `doVisit*` methods instead of `visit*`

2. **AbstractCamelXmlVisitor** - Base for XML DSL transformations
3. **AbstractCamelYamlVisitor** - Base for YAML DSL transformations

**Critical implementation detail:** All visitor methods are wrapped in `executeVisitWithCatch()` to ensure a single recipe failure doesn't break the entire migration.

### Recipe Implementation Patterns

#### Java Recipes
Located in packages like `org.apache.camel.upgrade.camel40.java`:

```java
@Value
@EqualsAndHashCode(callSuper = false)
public class CamelAPIsRecipe extends Recipe {
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {
            // Override doVisit* methods
        });
    }
}
```

**Important:** Use `RecipesUtil.newVisitor()` which wraps the visitor with `Preconditions.check(new UsesType<>("org.apache.camel..*"))` to skip files without Camel imports (performance optimization).

#### YAML Recipes
Located in `src/main/resources/META-INF/rewrite/`:

- Use OpenRewrite's declarative YAML format
- Support variable substitution via Maven resource filtering (e.g., `@camel-latest-version@`)
- Compose Java recipes and OpenRewrite's built-in recipes

#### Custom Recipes
Located in `org.apache.camel.upgrade.customRecipes`:

- Parameterized, reusable recipe components
- Examples: `MoveGetterToPluginHelper`, `PropertiesAndYamlKeyUpdate`, `ReplacePropertyInComponentXml`
- Use `@Option` annotation for recipe parameters

### Test Architecture

Tests use OpenRewrite's `RewriteTest` interface with a before/after pattern:

```java
public class CamelAPIsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_0)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v3_18, "camel-api", "camel-core-model"))
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void testApiChange() {
        rewriteRun(java(
            "// before code",
            "// after code"
        ));
    }
}
```

**Critical testing detail:** The `maven-dependency-plugin` downloads specific Camel versions into `target/test-classes/META-INF/rewrite/classpath/` during the `process-test-resources` phase. This allows type-safe AST transformations to validate against actual Camel APIs.

### Adding a New Recipe

1. **Create Java visitor class** (if needed):
   ```java
   // In camel-upgrade-recipes/src/main/java/org/apache/camel/upgrade/camel4XX/
   package org.apache.camel.upgrade.camel4XX.java;

   @Value
   @EqualsAndHashCode(callSuper = false)
   public class MyNewRecipe extends Recipe {
       @Override
       public String getDisplayName() { return "My Migration"; }

       @Override
       public String getDescription() { return "Migrates X to Y"; }

       @Override
       public TreeVisitor<?, ExecutionContext> getVisitor() {
           return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {
               @Override
               protected J.MethodInvocation doVisitMethodInvocation(
                   J.MethodInvocation method, ExecutionContext ctx) {
                   // transformation logic
               }
           });
       }
   }
   ```

2. **Add YAML recipe definition** (or update existing):
   ```yaml
   # In camel-upgrade-recipes/src/main/resources/META-INF/rewrite/4.XX.yaml
   ---
   type: specs.openrewrite.org/v1beta/recipe
   name: org.apache.camel.upgrade.camel4XX.CamelMigrationRecipe
   displayName: Migrate to Camel 4.XX
   recipeList:
     - org.apache.camel.upgrade.camel4XX.java.MyNewRecipe
     - org.apache.camel.upgrade.camel4XX.xml.MyXmlRecipe
   ```

3. **Update version-specific YAML files** to include the new recipe in the migration chain

4. **Write tests**:
   ```java
   // In camel-upgrade-recipes/src/test/java/org/apache/camel/upgrade/
   public class CamelUpdate4XXTest implements RewriteTest {
       @Override
       public void defaults(RecipeSpec spec) {
           CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_XX)
               .parser(CamelTestUtil.parserFromClasspath(
                   CamelTestUtil.CamelVersion.v4_YY, "camel-api"));
       }

       @Test
       void testMyMigration() {
           rewriteRun(java(
               """
               // old code
               """,
               """
               // expected new code
               """
           ));
       }
   }
   ```

5. **Add test dependencies** to `pom.xml` if new Camel components are needed for classpath validation

### RecipesUtil Helper Methods

When manipulating AST, use `RecipesUtil` helper methods:

- `createAnnotation()` - Create/modify annotations
- `createMultilineComment()` - Add migration hints as comments
- `createIdentifier()` - Create identifier nodes
- `createTypeCast()` - Create type cast expressions
- `getMethodMatcher()` - Get cached method matcher (from parent visitor)
- `getPattern()` - Get cached regex pattern (from parent visitor)

### Version Management

When updating to a new Camel version:

1. Update version properties in parent `pom.xml`:
   ```xml
   <camel4.XX-version>4.XX.0</camel4.XX-version>
   <camel-latest-version>4.XX.0</camel-latest-version>
   ```

2. Add test dependencies if needed (in `camel-upgrade-recipes/pom.xml`)

3. Create new YAML recipe file: `4.XX.yaml`

4. Update `latest.yaml` to include the new version

5. Add enum value to `CamelTestUtil.CamelVersion`

## Project-Specific Notes

### Not for Camel-Quarkus
This project is **not** for migrating Camel-Quarkus applications. Direct users to the Quarkus migration guide instead.

### Test Jar Artifact
The project produces a `test-jar` artifact used by downstream projects (e.g., quarkus-updates). Changes to test infrastructure may affect external consumers.

### Resource Filtering
YAML files use Maven resource filtering. Variables like `@camel-latest-version@` are replaced during build. Ensure the POM's `<resources>` section has `<filtering>true</filtering>`.

### OpenRewrite Dependencies Scope
All OpenRewrite dependencies are `provided` scope - they're not packaged into the JAR. The OpenRewrite Maven plugin provides them at runtime.

### Classpath Directory Exclusion
Test classpath JARs in `target/test-classes/META-INF/rewrite/classpath/` are excluded from the final artifact via `maven-jar-plugin` configuration.

## Creating Recipes from Camel Upgrade Guides

### Overview

When a new Camel version is released, the official upgrade guide at `https://camel.apache.org/manual/camel-4x-upgrade-guide-4_X.md` documents breaking changes. This section explains how to systematically convert that documentation into automated migration recipes.

### Step-by-Step Process

#### 1. Analyze the Upgrade Guide

Read the upgrade guide (e.g., `camel-4x-upgrade-guide-4_7.md`) and categorize changes:

**Common Change Patterns:**

| Pattern | Example from Guide | Recipe Type | Complexity |
|---------|-------------------|-------------|------------|
| **Class/Interface Renamed** | `TransformerKey` moved from `org.apache.camel.impl.engine` → `org.apache.camel.spi` | YAML: `ChangeType` | Simple |
| **Dependency Renamed** | `camel-langchain-chat` → `camel-langchain4j-chat` | YAML: `ChangeDependencyGroupIdAndArtifactId` | Simple |
| **Dependency Removed** | `camel-cloudevents` merged into `camel-api` | YAML: `RemoveDependency` | Simple |
| **Property Key Renamed** | `route.streamCaching` → `route.streamCache` | YAML: `ChangePropertyKey` | Simple |
| **XML/YAML DSL Tag Renamed** | `<failover/>` → `<failoverLoadBalancer/>` | Java: XmlVisitor or YamlVisitor | Medium |
| **Method Call Changed** | `exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST, ...)` → `exchange.getMessage(HttpMessage.class).getRequest()` | Java: JavaVisitor | Complex |
| **API Structure Changed** | Bean `property` (sequence) → `properties` (mapping) | Java: YamlVisitor | Complex |

#### 2. Create the Version-Specific YAML File

Create `camel-upgrade-recipes/src/main/resources/META-INF/rewrite/4.X.yaml`:

```yaml
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# ... (standard Apache header)
#

#####
# Rules coming from https://camel.apache.org/manual/camel-4x-upgrade-guide-4_X.html
#####

#####
# Update the Camel project from 4.Y to 4.X
#####
---
type: specs.openrewrite.org/v1beta/recipe
name: org.apache.camel.upgrade.camel4X.CamelMigrationRecipe
displayName: Migrates `camel 4.Y` application to `camel 4.X`
description: Migrates `camel 4.Y` application to `camel 4.X`.
recipeList:
  # List all sub-recipes here
  - org.apache.camel.upgrade.camel4X.renamedClasses
  - org.apache.camel.upgrade.camel4X.XmlDsl4XRecipe  # if XML changes exist
  - org.apache.camel.upgrade.camel4X.YamlDsl4XRecipe  # if YAML changes exist
  - org.apache.camel.upgrade.camel4X.Java4XRecipes   # if Java API changes exist
  - org.apache.camel.upgrade.camel4X.removedDependencies
```

#### 3. Implement Simple Changes (YAML-Only)

For **class renames**, **dependency changes**, and **property renames**, add sub-recipes directly in the YAML:

```yaml
---
# Example: Class renames
type: specs.openrewrite.org/v1beta/recipe
name: org.apache.camel.upgrade.camel4X.renamedClasses
displayName: Renamed classes for API
description: Renamed classes for API.
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: org.apache.camel.impl.engine.TransformerKey
      newFullyQualifiedTypeName: org.apache.camel.spi.TransformerKey
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: org.apache.camel.impl.engine.ValidatorKey
      newFullyQualifiedTypeName: org.apache.camel.spi.ValidatorKey
---
# Example: Dependency changes
type: specs.openrewrite.org/v1beta/recipe
name: org.apache.camel.upgrade.camel4X.renamedDependencies
displayName: Renamed dependencies
description: Renamed dependencies.
recipeList:
  - org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId:
      oldGroupId: org.apache.camel
      oldArtifactId: camel-langchain-chat
      newGroupId: org.apache.camel
      newArtifactId: camel-langchain4j-chat
      newVersion: 4.X.0
      overrideManagedVersion: true
---
# Example: Property renames
type: specs.openrewrite.org/v1beta/recipe
name: org.apache.camel.upgrade.camel4X.propertyRenames
displayName: Renamed properties in YAML DSL
description: Property key changes.
recipeList:
  - org.openrewrite.yaml.ChangePropertyKey:
      oldPropertyKey: route.streamCaching
      newPropertyKey: route.streamCache
```

#### 4. Implement XML DSL Changes (Java Required)

For XML tag renames, create a Java visitor in `camel-upgrade-recipes/src/main/java/org/apache/camel/upgrade/camel4X/`:

**Pattern for XML tag renames:**

```java
package org.apache.camel.upgrade.camel4X;

import org.apache.camel.upgrade.AbstractCamelXmlVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;
import java.util.Map;

/**
 * Recipe based on <a href="https://camel.apache.org/manual/camel-4x-upgrade-guide-4_X.html#_dsl">DSL Changes</a>
 */
public class XmlDsl4XRecipe extends Recipe {

    private static final Map<XPathMatcher, String> transformations = Map.of(
        new XPathMatcher("//loadBalance/failover"), "failoverLoadBalancer",
        new XPathMatcher("//loadBalance/random"), "randomLoadBalancer",
        new XPathMatcher("//loadBalance/sticky"), "stickyLoadBalancer"
    );

    @Override
    public String getDisplayName() {
        return "Camel XML DSL changes for 4.X";
    }

    @Override
    public String getDescription() {
        return "Apache Camel XML DSL migration from version 4.Y to 4.X.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AbstractCamelXmlVisitor() {
            @Override
            public Xml.Tag doVisitTag(final Xml.Tag tag, final ExecutionContext ctx) {
                Xml.Tag t = super.doVisitTag(tag, ctx);

                // Apply transformations
                return transformations.entrySet().stream()
                    .filter(e -> e.getKey().matches(getCursor()))
                    .map(e -> t.withName(e.getValue()))
                    .findAny()
                    .orElse(t);
            }
        };
    }
}
```

#### 5. Implement YAML DSL Changes (Java Required)

For YAML structure changes, create a YAML visitor:

**Pattern for YAML key renames:**

```java
package org.apache.camel.upgrade.camel4X;

import org.apache.camel.upgrade.AbstractCamelYamlVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.tree.Yaml;
import java.util.Map;

public class YamlDsl4XRecipe extends Recipe {

    private static final Map<String, String> keyRenames = Map.of(
        "failover", "failoverLoadBalancer",
        "random", "randomLoadBalancer",
        "sticky", "stickyLoadBalancer"
    );

    @Override
    public String getDisplayName() {
        return "Camel YAML DSL changes for 4.X";
    }

    @Override
    public String getDescription() {
        return "Apache Camel YAML DSL migration from version 4.Y to 4.X.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AbstractCamelYamlVisitor() {
            @Override
            protected void clearLocalCache() {
                // nothing to do for simple renames
            }

            @Override
            public Yaml.Mapping.Entry doVisitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.doVisitMappingEntry(entry, ctx);

                if (e.getKey() instanceof Yaml.Scalar) {
                    String keyValue = ((Yaml.Scalar) e.getKey()).getValue();
                    String newValue = keyRenames.get(keyValue);

                    if (newValue != null) {
                        return e.withKey(((Yaml.Scalar) e.getKey()).withValue(newValue));
                    }
                }

                return e;
            }
        };
    }
}
```

#### 6. Implement Java API Changes (Complex)

For method call changes, create a Java visitor:

**Pattern for method transformations:**

```java
package org.apache.camel.upgrade.camel4X;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.camel.upgrade.AbstractCamelJavaVisitor;
import org.apache.camel.upgrade.RecipesUtil;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class Java4XRecipes extends Recipe {

    private static final String MATCHER_GET_HEADER =
        "org.apache.camel.Message getHeader(java.lang.String, java.lang.Class)";
    private static final String MATCHER_GET_IN =
        "org.apache.camel.Exchange getIn()";

    @Override
    public String getDisplayName() {
        return "Camel Java API changes for 4.X";
    }

    @Override
    public String getDescription() {
        return "Apache Camel Java API migration from version 4.Y to 4.X.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return RecipesUtil.newVisitor(new AbstractCamelJavaVisitor() {
            @Override
            protected J.MethodInvocation doVisitMethodInvocation(
                J.MethodInvocation method, ExecutionContext ctx) {

                J.MethodInvocation mi = super.doVisitMethodInvocation(method, ctx);

                // Example: exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST, ...)
                // becomes: exchange.getMessage(HttpMessage.class).getRequest()
                if (mi.getSelect() instanceof J.MethodInvocation &&
                    getMethodMatcher(MATCHER_GET_IN).matches((J.MethodInvocation) mi.getSelect(), false) &&
                    getMethodMatcher(MATCHER_GET_HEADER).matches(mi) &&
                    mi.toString().contains("Exchange.HTTP_SERVLET_REQUEST")) {

                    J.MethodInvocation result = (J.MethodInvocation) JavaTemplate
                        .builder("#{any(org.apache.camel.Exchange)}.getMessage(HttpMessage.class).getRequest()")
                        .build()
                        .apply(getCursor(), mi.getCoordinates().replace(),
                               ((J.MethodInvocation) mi.getSelect()).getSelect())
                        .withPrefix(mi.getPrefix());

                    doAfterVisit(new AddImport<>("org.apache.camel.http.common.HttpMessage", null, false));
                    return result;
                }

                return mi;
            }
        });
    }
}
```

#### 7. Write Tests

Create `camel-upgrade-recipes/src/test/java/org/apache/camel/upgrade/CamelUpdate4XTest.java`:

```java
package org.apache.camel.upgrade;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;
import static org.openrewrite.yaml.Assertions.yaml;

public class CamelUpdate4XTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        CamelTestUtil.recipe(spec, CamelTestUtil.CamelVersion.v4_X)
            .parser(CamelTestUtil.parserFromClasspath(
                CamelTestUtil.CamelVersion.v4_Y,
                "camel-api", "camel-core-model"  // Add required components
            ))
            .typeValidationOptions(TypeValidation.none());
    }

    @DocumentExample  // Mark one test as the primary example
    @Test
    void testApiChanges() {
        rewriteRun(java(
            """
            // Before code (from upgrade guide)
            import org.apache.camel.impl.engine.TransformerKey;

            public class Test {
                TransformerKey key;
            }
            """,
            """
            // After code (expected result)
            import org.apache.camel.spi.TransformerKey;

            public class Test {
                TransformerKey key;
            }
            """
        ));
    }

    @Test
    void testXmlDslChange() {
        rewriteRun(xml(
            """
            <route>
                <loadBalance>
                    <failover/>
                    <to uri="mock:a"/>
                </loadBalance>
            </route>
            """,
            """
            <route>
                <loadBalance>
                    <failoverLoadBalancer/>
                    <to uri="mock:a"/>
                </loadBalance>
            </route>
            """
        ));
    }

    @Test
    void testDependencyRemoval() {
        rewriteRun(pomXml(
            """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.camel</groupId>
                        <artifactId>camel-cloudevents</artifactId>
                        <version>4.Y.0</version>
                    </dependency>
                </dependencies>
            </project>
            """,
            """
            <project>
                <dependencies>
                </dependencies>
            </project>
            """
        ));
    }
}
```

**Test Naming Convention:**
- Each test method should reference the upgrade guide section via JavaDoc link
- Use descriptive names: `testApiChanges()`, `xmlDslLoadBalanceFailover()`, etc.

#### 8. Update Version Management

**Add to parent `pom.xml`:**
```xml
<properties>
    <camel4.X-version>4.X.0</camel4.X-version>
    <camel-latest-version>4.X.0</camel-latest-version>
</properties>
```

**Add test dependencies** (if new components are involved):
```xml
<!-- In camel-upgrade-recipes/pom.xml -->
<artifactItem>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-NEW-COMPONENT</artifactId>
    <version>${camel4.X-version}</version>
    <outputDirectory>${rewrite-tmp-classpath}</outputDirectory>
</artifactItem>
```

**Add to `CamelTestUtil.CamelVersion` enum:**
```java
public enum CamelVersion {
    // ... existing versions
    v4_X(4, X, 0);
```

#### 9. Update Latest Recipe Chain

Edit `camel-upgrade-recipes/src/main/resources/META-INF/rewrite/latest.yaml`:

```yaml
recipeList:
  - org.apache.camel.upgrade.camel4X.CamelMigrationRecipe  # Add new version
  - org.apache.camel.upgrade.camel4Y.CamelMigrationRecipe  # Previous versions...
  # ... rest of the chain
  - org.openrewrite.maven.UpgradeDependencyVersion:
      groupId: 'org.apache.camel'
      artifactId: '*'
      newVersion: @camel-latest-version@  # Will be filtered to 4.X.0
```

#### 10. Verify the Recipe Works

```bash
# Build and test
mvn clean install

# Test on a sample Camel 4.Y project
cd /path/to/test/project
mvn -U org.openrewrite.maven:rewrite-maven-plugin:dryRun \
  -Drewrite.recipeArtifactCoordinates=org.apache.camel.upgrade:camel-upgrade-recipes:LATEST \
  -Drewrite.activeRecipes=org.apache.camel.upgrade.camel4X.CamelMigrationRecipe

# Review the diff
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.apache.camel.upgrade:camel-upgrade-recipes:LATEST \
  -Drewrite.activeRecipes=org.apache.camel.upgrade.camel4X.CamelMigrationRecipe
```

### Decision Matrix: When to Use What

| Upgrade Guide Says... | Recipe Approach | File Location |
|----------------------|-----------------|---------------|
| "Class X moved to package Y" | YAML: `ChangeType` | `4.X.yaml` |
| "Dependency renamed" | YAML: `ChangeDependencyGroupIdAndArtifactId` | `4.X.yaml` |
| "Dependency removed/merged" | YAML: `RemoveDependency` | `4.X.yaml` |
| "Property key renamed" | YAML: `ChangePropertyKey` | `4.X.yaml` |
| "XML tag `<foo>` renamed to `<bar>`" | Java: `XmlDsl4XRecipe` with `XPathMatcher` | `camel4X/XmlDsl4XRecipe.java` |
| "YAML key renamed" | Java: `YamlDsl4XRecipe` with key mapping | `camel4X/YamlDsl4XRecipe.java` |
| "Method X.foo() changed to X.bar()" | Java: `MethodMatcher` + template replacement | `camel4X/Java4XRecipes.java` |
| "Complex structural change" | Java: Custom visitor with AST manipulation | `camel4X/Java4XRecipes.java` |

### Common Pitfalls

1. **Forgetting to add imports**: Use `doAfterVisit(new AddImport<>(...))` when transformations introduce new types
2. **Not testing negative cases**: Add tests that verify unrelated code isn't changed (e.g., `xmlDslLoadBalanceRandomKeep()` in CamelUpdate47Test)
3. **Missing XPath context**: XML matchers need full path context (e.g., `//loadBalance/failover`, not just `//failover`)
4. **YAML indentation issues**: When manipulating YAML, preserve `prefix` spacing
5. **Type validation failures**: Set `.typeValidationOptions(TypeValidation.none())` in tests if classpath isn't perfect
6. **Forgetting to update `latest.yaml`**: Always add new version to the migration chain

### Recipe Generation Checklist

- [ ] Read upgrade guide and categorize all changes
- [ ] Create `4.X.yaml` with main recipe and sub-recipes
- [ ] Implement simple changes (class renames, dependency changes) in YAML
- [ ] Create Java visitors for XML/YAML/Java changes (if needed)
- [ ] Write comprehensive tests covering all changes
- [ ] Add version to `pom.xml` properties
- [ ] Add test dependencies for new components
- [ ] Update `CamelTestUtil.CamelVersion` enum
- [ ] Update `latest.yaml` to include new version
- [ ] Build and test locally: `mvn clean install`
- [ ] Test against a real project migrating from previous version
- [ ] Document any limitations or manual steps in comments

## Release Process (Committers Only)

Releases follow Apache Software Foundation procedures. See README.adoc for detailed steps. Key points:

- Update release notes before releasing
- Use `./mvnw release:prepare -Prelease`
- Scripts in `release-utils/scripts/` handle signing and distribution
- Requires ASF credentials and GPG key
