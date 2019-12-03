# AutoAnnotator

[![Actions Status](https://github.com/marco-eckstein/autoannotator/workflows/Java%20CI/badge.svg)](
https://github.com/marco-eckstein/autoannotator/actions?query=workflow%3A"Java+CI"
)

[![Maven Central Status](
https://maven-badges.herokuapp.com/maven-central/com.marcoeckstein/autoannotator/badge.svg
)](
https://mvnrepository.com/artifact/com.marcoeckstein/autoannotator
)

AutoAnnotator is a metaprogramming tool for automatically annotating types with validation constraints,
JPA annotations and arbitrary other annotations. It is written in Kotlin but works for Java and possibly
other JVM languages as well.

## Purpose

The main purpose of AutoAnnotator is avoidance of boilerplate annotations when working with JPA POJOs.

Given this POJO

Kotlin:

```kotlin
import java.time.ZonedDateTime
import javax.persistence.Entity

@Entity
internal class AutoAnnotatedPojo(
    val nonNullString: String,
    val nullableString: String?,
    val zonedDateTime: ZonedDateTime
)
```

Java:

```java
import java.time.ZonedDateTime;
import javax.persistence.Entity;

@Entity
public class AutoAnnotatedPojo {

    @javax.annotation.Nonnull
    private String nonNullString = "";

    private String nullableString;

    @javax.annotation.Nonnull
    private ZonedDateTime zonedDateTime = ZonedDateTime.now();
}
```

you can use AutoAnnotator to manipulate your bytecode as if you had written:

Kotlin:

```kotlin
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

@Entity
internal class AutoAnnotatedPojo(
    @field:[NotNull NotBlank]
    val nonNullString: String,

    @field:Pattern(regexp = """(?s).*\S.*""", message = "must be null or not blank")
    val nullableString: String?,

    @field:[NotNull Column(columnDefinition = "timestamp with time zone")]
    val zonedDateTime: ZonedDateTime
)
```

`@javax.validation.constraints.NotNull` can be useful even in Kotlin, because your JPA provider may be able 
to infer a `NOT NULL` database constraint from it. E.g., Hibernate does this by default.

In Kotlin, you probably do not want blank strings because `null` is the idiomatic way of representing a 
missing value.

Java:

```java
import org.jetbrains.annotations.NotNull;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Entity
public class AutoAnnotatedPojo {
    @javax.annotation.Nonnull
    @NotNull
    @NotBlank
    private String nonNullString = "";

    @Pattern(regexp = "(?s).*\\S.*", message = "must be null or not blank")
    private String nullableString;

    @javax.annotation.Nonnull
    @NotNull
    @Column(columnDefinition = "timestamp with time zone")
    private ZonedDateTime zonedDateTime = ZonedDateTime.now();
}
```

For more examples, please see the tests in the `autoannotator-core` module.

## Usage

### 1. Dependencies

Maven:

```xml
<dependency>
    <groupId>com.marcoeckstein</groupId>
    <artifactId>autoannotator-api</artifactId>
    <version>${version}</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
implementation("com.marcoeckstein:autoannotator-api:$version")
```

The `autoannotator-api` module has no compile time dependencies to libraries with the annotations it may use
(depending on the configuration). It is expected that your project's classpath contains libraries with these
annotations. E.g., if you use `javax.validation.constraints.NotNull`, your project must have a dependency to
``javax.validation:validation-api`` or a substitute. If you have a JPA project, you probably have all
required dependencies.

### 2. Configuration

AutoAnnotator is configured inside your sources, allowing for type safety and autocompletion.
Your project needs to provide a single parameterless function annotated with `@AutoAnnotatorConfigSource`
that returns an `AutoAnnotatorConfig`.

Kotlin:

```kotlin
import com.marcoeckstein.autoannotator.api.AnnotationInfo
import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfig
import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfigSource
import com.marcoeckstein.autoannotator.api.ClassFilter
import com.marcoeckstein.autoannotator.api.ClassOptions
import java.time.ZonedDateTime
import javax.persistence.Column

@AutoAnnotatorConfigSource
fun get() = AutoAnnotatorConfig(
    ClassFilter(packagePrefix = "mypackage.domain.model"),
    ClassOptions(
        annotationsByFieldType = mapOf(
            ZonedDateTime::class.qualifiedName!! to setOf(
                AnnotationInfo(
                    clazz = Column::class, 
                    members = mapOf(Column::columnDefinition to "timestamp with time zone")
                )
            )
        )
    )
)
```

Java:

```java
import com.marcoeckstein.autoannotator.api.AnnotationInfo;
import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfig;
import com.marcoeckstein.autoannotator.api.AutoAnnotatorConfigSource;
import com.marcoeckstein.autoannotator.api.ClassFilter;
import com.marcoeckstein.autoannotator.api.ClassOptions;
import java.time.ZonedDateTime;
import javax.persistence.Column;

public class Config {

    @AutoAnnotatorConfigSource
    public static AutoAnnotatorConfig get() {
        return new AutoAnnotatorConfig(
            new ClassFilter(packageName),
            new ClassOptions(
                ImmutableMap.of(
                    ZonedDateTime.class.getName(),
                    ImmutableSet.of(
                        new AnnotationInfo(
                            javax.persistence.Column.class, // clazz
                            ImmutableMap.of("columnDefinition", "timestamp with time zone") // members
                        )
                    )
                )
            )
        );
    }
}
```

### 3. Build lifecycle

AutoAnnotator needs to run after your project has compiled.

#### Maven

The default phase is `process-classes`, which comes directly after `compile` in the default lifecycle, but
`compile` should also work.

```xml
<plugin>
    <groupId>com.marcoeckstein</groupId>
    <artifactId>autoannotator-maven-plugin</artifactId>
    <version>${version}</version>
    <executions>
        <execution>
            <phase>process-classes</phase><!-- Default -->
            <goals>
                <goal>annotate</goal><!-- The only goal -->
            </goals>
        </execution>
    </executions>
</plugin>
```

If you want to auto-annotate classes in your test source directory, you have to choose the phase and edit
the plugin configuration accordingly:

```xml
<plugin>
    <groupId>com.marcoeckstein</groupId>
    <artifactId>autoannotator-maven-plugin</artifactId>
    <version>${version}</version>
    <configuration>
        <annotateTestClasses>true</annotateTestClasses>
    </configuration>
    <executions>
        <execution>
            <phase>process-test-classes</phase><!-- Anything from test-compile should work. -->
            <goals>
                <goal>annotate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

For details and other configuration parameters, see the plugin parameter descriptions.

You should also configure your IDE to run this goal after building. E.g., in IntelliJ IDEA's Maven tab,
right-click the plugin's goal and check "Execute After Build".

#### CLI (Command-line interface)

If you do not use Maven, you can use the `autoannotator-cli` module at
`"com.marcoeckstein:autoannotator-api:$version"`. The main class is `com.marcoeckstein.autoannotator.cli.Main`. 
Logging is configured via 
[`org.slf4j.simpleLogger.*` properties](http://www.slf4j.org/api/org/slf4j/impl/SimpleLogger.html).
For options documentation, call the CLI with `--help`.

##### Classpath

The Java process running the CLI needs a classpath that includes:

- The `autoannotator-cli` jar-with-dependencies (aka "fat JAR")
- Your project's classpath, or at least:
    - All classes you want to annotate
    - All types of fields in those classes (even if not annotated)
    - All annotations you want to add or update
    - The class with the `@AutoAnnotatorConfigSource` function
    - All classes used in the `@AutoAnnotatorConfigSource` function\
      I.e., writing the string `"mypackage.MyClass"` inside that function generally requires fewer classes than
      the expressions `mypackage.MyClass::class.qualifiedName!!` (Kotlin) 
      or `mypackage.MyClass.class.getName()` (Java).

If you wanted to use `autoannotator-cli` in Maven, you could do this:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>...</version>
    <executions>
        <execution>
            <configuration>
                <target name="autoannotator">
                    <property name="maven.compile.classpath" refid="maven.compile.classpath"/>
                    <java taskname="autoannotator"
                          dir="${basedir}"
                          fork="true"
                          failonerror="true"
                          classpathref="maven.plugin.classpath"
                          classpath="${maven.compile.classpath}"
                          classname="com.marcoeckstein.autoannotator.cli.Main">
                        <sysproperty key="org.slf4j.simpleLogger.defaultLogLevel" value="info"/>
                    </java>
                </target>
            </configuration>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.marcoeckstein</groupId>
            <artifactId>autoannotator-cli</artifactId>
            <version>...</version>
            <classifier>jar-with-dependencies</classifier>
        </dependency>
    </dependencies>
</plugin>
```

## Development

Due to limitations of the Dokka plugin, there are some caveats:

- For development, JDK 8 is required. A higher JDK version cannot be used.
  If you use Windows and have a different JDK installed, you can set environment variable `JAVA8_HOME` and 
  run the PowerShell script `mvn-runner.ps1`.
- The following warnings are to be expected:
    - `dokka-maven-plugin:0.10.0:dokka`:
      - "Can't find node by signature \`org.jetbrains.annotations.NotNull\`, referenced at..."
    - `dokka-maven-plugin:0.10.0:javadoc`:
      - "null:-1:-1: Tag @see cannot be used in inline documentation."
  
### mvn-runner.ps1
 
This interactive PowerShell script - if used with goal `deploy`-  expects you to have the following data in 
your `<USERHOME>/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>Sonatype Staging</id>
            <username>USERNAME</username>
            <password>${sonatype.password}</password>
        </server>
        <server>
            <id>Sonatype Snapshots</id>
            <username>USERNAME</username>
            <password>${sonatype.password}</password>
        </server>
        ...
    </servers>
    ...
</settings>
```
