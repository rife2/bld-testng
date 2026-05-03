/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * Implements the TestNgOperationTests class.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseUtilityClass"})
class TestNgOperationTest {

    private static final String BAR = "bar";
    private static final String FOO = "foo";

    @BeforeAll
    static void beforeAll() {
        var level = Level.ALL;
        var logger = Logger.getLogger(TestNgOperation.class.getName());
        var consoleHandler = new ConsoleHandler();

        consoleHandler.setLevel(level);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);
    }

    @Nested
    @DisplayName("Argument Parsing Tests")
    class ArgumentParsingTests {

        @Test
        void targetCollectionForArgExcludeGroups() {
            var args = new ArrayList<>(List.of("-excludegroups=unit,integration"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.excludeGroups()).containsExactlyInAnyOrder("unit", "integration");
        }

        @Test
        void targetCollectionForArgGroups() {
            var args = new ArrayList<>(List.of("-groups=unit,integration"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.groups()).containsExactlyInAnyOrder("unit", "integration");
        }

        @Test
        void targetCollectionForArgLogLevelParsed() {
            var args = new ArrayList<>(List.of("-log=5"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.options().get("-log")).isEqualTo("5");
            assertThat(args).isEmpty();
        }

        @Test
        void targetCollectionForArgMethods() {
            var args = new ArrayList<>(List.of("-methods=com.foo.Test.m1,com.foo.Test.m2"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.methods()).containsExactlyInAnyOrder("com.foo.Test.m1", "com.foo.Test.m2");
        }

        @Test
        void targetCollectionForArgSuites() {
            var args = new ArrayList<>(List.of("-suites=suite1.xml,suite2.xml"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.suites()).containsExactlyInAnyOrder("suite1.xml", "suite2.xml");
        }

        @Test
        void targetCollectionForArgTestClass() {
            var args = new ArrayList<>(List.of("-testclass=com.foo.Test1,com.foo.Test2"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.testClasses()).containsExactlyInAnyOrder("com.foo.Test1", "com.foo.Test2");
        }

        @Test
        void targetCollectionForArgTestNames() {
            var args = new ArrayList<>(List.of("-testnames=foo,bar"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.testNames()).containsExactlyInAnyOrder("foo", "bar");
        }

        @Test
        void targetCollectionForArgUnknownBreaksLoop() {
            var args = new ArrayList<>(List.of("-unknown=value", "-groups=should-not-parse"));
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return args;
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            assertThat(op.groups()).isEmpty(); // loop broke before -groups
        }
    }

    @Nested
    @DisplayName("Directory Tests")
    class DirectoryTests {

        private final File foo = new File("FOO");

        @Test
        void directoryAsFile() {
            var op = new TestNgOperation().directory(foo);
            assertThat(op.options().get("-d")).isEqualTo(foo.getAbsolutePath());
        }

        @Test
        void directoryAsPath() {
            var op = new TestNgOperation().directory(foo.toPath());
            assertThat(op.options().get("-d")).isEqualTo(foo.getAbsolutePath());
        }

        @Test
        void directoryAsString() {
            var op = new TestNgOperation().directory(FOO);
            assertThat(op.options().get("-d")).isEqualTo(FOO);
        }
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        void execute() {
            var pkg = "com.example";
            var op = new TestNgOperation()
                    .fromProject(new BaseProjectBlueprint(
                            new File("examples/example-project"),
                            pkg,
                            "examples",
                            "Examples"))
                    .packages(pkg);
            assertThatCode(op::execute).doesNotThrowAnyException();
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void executeWithDefaultSuiteFailsOnIOException() throws IOException {
            var readOnlyDir = Files.createTempDirectory("readonly");
            if (readOnlyDir.toFile().setReadOnly()) {
                var project = new Project() {
                    @Override
                    public File buildDirectory() {
                        return readOnlyDir.toFile();
                    }
                };

                var op = new TestNgOperation().fromProject(project); // no suites/packages/methods

                assertThatThrownBy(op::executeConstructProcessCommandList)
                        .isInstanceOf(RuntimeException.class)
                        .hasCauseInstanceOf(IOException.class);

                var ignored = readOnlyDir.toFile().setWritable(true); // cleanup
                Files.deleteIfExists(readOnlyDir);
            }
        }

        @Test
        void executeWithInvalidLogLevel() {
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return List.of("-log=abc");
                }
            };
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            assertThatThrownBy(op::executeConstructProcessCommandList).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void executeWithInvalidSuite() {
            assertThatThrownBy(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng.xml")
                            .execute())
                    .as("with suites")
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithInvalidTestNames() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath("lib/test/*", "build/main", "build/test")
                            .testNames("foo", "bar")
                            .execute())
                    .as("with run classpath")
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithMethods() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .methods("rife.bld.extension.TestNgExampleTests.foo")
                            .execute())
                    .as("with methods")
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithRunClasspath() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath("lib/test/*", "build/main", "build/test")
                            .log(2)
                            .execute())
                    .as("with run classpath")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithRunClasspathAsList() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath(List.of("lib/test/*", "build/main", "build/test"))
                            .log(2)
                            .execute())
                    .as("with run classpath as list")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithSuite() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .execute())
                    .as("with suite")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithSuiteAndLog() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .log(2)
                            .execute())
                    .as("with suite log")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithTestClass() {
            assertThatThrownBy(() ->
                    new TestNgOperation().fromProject(new Project())
                            .testClass("rife.bld.extension.TestNgExampleTests")
                            .execute())
                    .as("with testClass")
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithTestClassAndMethod() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .testClass("rife.bld.extension.TestNgExampleTests")
                            .methods("rife.bld.extension.TestNgExampleTests.foo")
                            .execute())
                    .as("with methods")
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithTestName() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath("lib/test/*", "build/main", "build/test")
                            .testNames("exclude fail")
                            .log(2)
                            .execute())
                    .as("with run classpath")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithTestNames() {
            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng3.xml")
                            .testClasspath("lib/test/*", "build/main", "build/test")
                            .testNames("hello", "message")
                            .log(2)
                            .execute())
                    .as("with run classpath")
                    .doesNotThrowAnyException();
        }

        @Test
        void executeWithUnknownArgBreaks() {
            var project = new Project() {
                @Override
                public List<String> arguments() {
                    return List.of("-unknown=value", "-suites=foo.xml");
                }
            };
            var op = new TestNgOperation().fromProject(project);
            var args = op.executeConstructProcessCommandList();
            assertThat(args).doesNotContain("-suites=foo.xml");
        }

        @Test
        void executeWithoutProject() {
            var op = new TestNgOperation();
            assertThatThrownBy(op::execute)
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithoutTests() {
            var op = new TestNgOperation().fromProject(new Project());
            assertThatThrownBy(op::execute)
                    .isInstanceOf(ExitStatusException.class);
        }

        @Test
        void writeDefaultSuiteGeneratesFile() throws IOException {
            var project = new Project();
            var op = new TestNgOperation().fromProject(project).packages("com.example");
            op.executeConstructProcessCommandList();
            var generated = Path.of(project.buildDirectory().getAbsolutePath(), "testng-generated.xml");
            assertThat(generated).exists();
            assertThat(Files.readString(generated)).contains("<suite", "com.example");
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        void alwaysRunListenersFalse() {
            var op = new TestNgOperation().alwaysRunListeners(false);
            assertThat(op.options().get("-alwaysrunlisteners")).isEqualTo("false");
        }

        @Test
        void alwaysRunListenersTrue() {
            var op = new TestNgOperation().alwaysRunListeners(true);
            assertThat(op.options().get("-alwaysrunlisteners")).isEqualTo("true");
        }

        @Test
        @EnabledOnOs(OS.LINUX)
        void checkAllParameters() throws IOException {
            var args = Files.readAllLines(Paths.get("src", "test", "resources", "testng-args.txt"));
            assertThat(args).isNotEmpty();

            var params = new TestNgOperation()
                    .fromProject(new BaseProjectBlueprint(new File("examples"), "com.example", "examples", "Examples"))
                    .alwaysRunListeners(true) // -alwaysrunlisteners
                    .failurePolicy(TestNgOperation.FailurePolicy.SKIP) // -configfailurepolicy
                    .directory("dir") // -d
                    .dataProviderThreadCount(1) // -dataproviderthreadcount
                    .dependencyInjectorFactory("injectorfactory") // -dependencyinjectorfactory
                    .excludeGroups("group") // -excludegroups
                    .failWhenEverythingSkipped(true) // -failwheneverythingskipped
                    .generateResultsPerSuite(true) // -generateResultsPerSuite
                    .groups("group1", "group2") // -groups
                    .ignoreMissedTestName(true) // -ignoreMissedTestNames
                    .includeAllDataDrivenTestsWhenSkipping(true) // -includeAllDataDrivenTestsWhenSkipping
                    .jUnit(true) // -junit
                    .listener("listener") // -listener
                    .listenerComparator("comparator") // -listenercomparator
                    .listenerFactory("factory") // -listenerfactory
                    .log(1) // -log
                    .methods("methods") // -methods
                    .methodSelectors("selector") // -methodselectors
                    .mixed(true) // -mixed
                    .objectFactory("objectFactory") // -objectfactory
                    .overrideIncludedMethods("method") // -overrideincludedmethods
                    .parallel(TestNgOperation.Parallel.TESTS) // -parallel
                    .propagateDataProviderFailureAsTestFailure(true) // -propagateDataProviderFailureAsTestFailure
                    .reporter("reporter") // -reporter
                    .shareThreadPoolForDataProviders(true) // -shareThreadPoolForDataProviders
                    .sourceDir("src/test") // -sourcedir
                    .spiListenersToSkip("listener") // -spilistenerstoskip
                    .suiteName("name") // -suitename
                    .suiteThreadPoolSize(1) // -suitethreadpoolsize
                    .testClass("class") // -testclass
                    .testJar("jar") // -testjar
                    .testName("name") // -testname
                    .testNames("names") // -testnames
                    .testRunFactory("runFactory") // -testrunfactory
                    .threadCount(1) // -threadcount
                    .threadPoolFactoryClass("poolClass") // -threadpoolfactoryclass
                    .useDefaultListeners(true) // -usedefaultlisteners
                    .useGlobalThreadPool(true) // -useGlobalThreadPool
                    .xmlPathInJar("jarPath") // -xmlpathinjar
                    .executeConstructProcessCommandList();

            try (var softly = new AutoCloseableSoftAssertions()) {
                for (var p : args) {
                    var found = params.stream().anyMatch(a -> a.equals(p) || a.startsWith(p + " "));
                    softly.assertThat(found).as("%s not found.", p).isTrue();
                }
            }
        }

        @Test
        void classpath() {
            var op = new TestNgOperation().testClasspath(FOO, BAR);
            assertThat(op.testClasspath()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void dataProviderThreadCount() {
            var op = new TestNgOperation().dataProviderThreadCount(1);
            assertThat(op.options().get("-dataproviderthreadcount")).isEqualTo("1");
        }

        @Test
        void dataProviderThreadCountInvalid() {
            assertThatThrownBy(() -> new TestNgOperation().dataProviderThreadCount(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void dependencyInjectorFactory() {
            var op = new TestNgOperation().dependencyInjectorFactory(FOO);
            assertThat(op.options().get("-dependencyinjectorfactory")).isEqualTo(FOO);
        }

        @Test
        void excludeGroups() {
            var op = new TestNgOperation().excludeGroups(FOO, BAR, FOO);
            assertThat(op.excludeGroups()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void excludeGroupsAsList() {
            var op = new TestNgOperation().excludeGroups(List.of(FOO, BAR));
            assertThat(op.excludeGroups()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void excludeGroupsRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().excludeGroups(List.of("")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void failWheneverEverythingSkippedFalse() {
            var op = new TestNgOperation().failWhenEverythingSkipped(false);
            assertThat(op.options().get("-failwheneverythingskipped")).isEqualTo("false");
        }

        @Test
        void failWheneverEverythingSkippedTrue() {
            var op = new TestNgOperation().failWhenEverythingSkipped(true);
            assertThat(op.options().get("-failwheneverythingskipped")).isEqualTo("true");
        }

        @Test
        void failurePolicyContinue() {
            var op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.CONTINUE);
            assertThat(op.options().get("-configfailurepolicy")).isEqualTo("continue");
        }

        @Test
        void failurePolicySkip() {
            var op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.SKIP);
            assertThat(op.options().get("-configfailurepolicy")).isEqualTo("skip");
        }

        @Test
        void fromProjectRespectsExistingDirectory() {
            var custom = new File("custom-out");
            var op = new TestNgOperation()
                    .directory(custom)
                    .fromProject(new Project());
            assertThat(op.options().get("-d")).isEqualTo(custom.getAbsolutePath());
        }

        @Test
        void generateResultsPerSuiteFalse() {
            var op = new TestNgOperation().generateResultsPerSuite(false);
            assertThat(op.options().get("-generateResultsPerSuite")).isEqualTo("false");
        }

        @Test
        void generateResultsPerSuiteTrue() {
            var op = new TestNgOperation().generateResultsPerSuite(true);
            assertThat(op.options().get("-generateResultsPerSuite")).isEqualTo("true");
        }

        @Test
        void groups() {
            var op = new TestNgOperation().groups(FOO, BAR, FOO);
            assertThat(op.groups()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void groupsAsList() {
            var op = new TestNgOperation().groups(List.of(FOO, BAR));
            assertThat(op.groups()).hasSize(2).contains(FOO, BAR);
        }

        @Test
        void groupsRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().groups(List.of("")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void ignoreMissedTestNameFalse() {
            var op = new TestNgOperation().ignoreMissedTestName(false);
            assertThat(op.options().get("-ignoreMissedTestNames")).isEqualTo("false");
        }

        @Test
        void ignoreMissedTestNameTrue() {
            var op = new TestNgOperation().ignoreMissedTestName(true);
            assertThat(op.options().get("-ignoreMissedTestNames")).isEqualTo("true");
        }

        @Test
        void includeAllDataDrivenTestsWhenSkippingFalse() {
            var op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(false);
            assertThat(op.options().get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("false");
        }

        @Test
        void includeAllDataDrivenTestsWhenSkippingTrue() {
            var op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(true);
            assertThat(op.options().get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("true");
        }

        @Test
        void jUnitFalse() {
            var op = new TestNgOperation().jUnit(false);
            assertThat(op.options().get("-junit")).isEqualTo("false");
        }

        @Test
        void jUnitTrue() {
            var op = new TestNgOperation().jUnit(true);
            assertThat(op.options().get("-junit")).isEqualTo("true");
        }

        @Test
        void jar() {
            var op = new TestNgOperation().testJar(FOO);
            assertThat(op.options().get("-testjar")).isEqualTo(FOO);
        }

        @Test
        void listener() {
            var ops = new TestNgOperation().listener(FOO, BAR);
            assertThat(ops.options().get("-listener")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void listenerAsList() {
            var ops = new TestNgOperation().listener(List.of(FOO, BAR));
            assertThat(ops.options().get("-listener")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void listenerRejectsNullElements() {
            assertThatThrownBy(() -> new TestNgOperation().listener(Arrays.asList("foo", null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void log() {
            var op = new TestNgOperation().log(1);
            assertThat(op.options().get("-log")).isEqualTo("1");
        }

        @Test
        void logInvalid() {
            assertThatThrownBy(() -> new TestNgOperation().log(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void methodDetectors() {
            var op = new TestNgOperation().methodSelectors(FOO, BAR);
            assertThat(op.options().get("-methodselectors")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void methodDetectorsAsList() {
            var op = new TestNgOperation().methodSelectors(List.of(FOO, BAR));
            assertThat(op.options().get("-methodselectors")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void methods() {
            var op = new TestNgOperation().methods(FOO, BAR, FOO);
            assertThat(op.methods()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void methodsAsList() {
            var op = new TestNgOperation().methods(List.of(FOO, BAR, BAR));
            assertThat(op.methods()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void methodsRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().methods(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void mixedFalse() {
            var op = new TestNgOperation().mixed(false);
            assertThat(op.options().get("-mixed")).isEqualTo("false");
        }

        @Test
        void mixedTrue() {
            var op = new TestNgOperation().mixed(true);
            assertThat(op.options().get("-mixed")).isEqualTo("true");
        }

        @Test
        void name() {
            var op = new TestNgOperation().testName(FOO);
            assertThat(op.options().get("-testname")).isEqualTo(FOO);
        }

        @Test
        void objectFactory() {
            var ops = new TestNgOperation().objectFactory(FOO, BAR);
            assertThat(ops.options().get("-objectfactory")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void objectFactoryAsList() {
            var ops = new TestNgOperation().objectFactory(List.of(FOO, BAR));
            assertThat(ops.options().get("-objectfactory")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void overrideIncludedMethods() {
            var ops = new TestNgOperation().overrideIncludedMethods(FOO, BAR);
            assertThat(ops.options().get("-overrideincludedmethods")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void overrideIncludedMethodsAsList() {
            var ops = new TestNgOperation().overrideIncludedMethods(List.of(FOO, BAR));
            assertThat(ops.options().get("-overrideincludedmethods")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void packages() {
            var op = new TestNgOperation().packages(FOO, BAR);
            assertThat(op.packages()).contains(FOO).contains(BAR);
        }

        @Test
        void packagesAsList() {
            var op = new TestNgOperation().packages(List.of(FOO, BAR));
            assertThat(op.packages()).contains(FOO).contains(BAR);
        }

        @Test
        void packagesRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().packages(List.of("")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void parallelClasses() {
            var op = new TestNgOperation().parallel(TestNgOperation.Parallel.CLASSES);
            assertThat(op.options().get("-parallel")).isEqualTo("classes");
        }

        @Test
        void parallelMethods() {
            var op = new TestNgOperation().parallel(TestNgOperation.Parallel.METHODS);
            assertThat(op.options().get("-parallel")).isEqualTo("methods");
        }

        @Test
        void parallelTests() {
            var op = new TestNgOperation().parallel(TestNgOperation.Parallel.TESTS);
            assertThat(op.options().get("-parallel")).isEqualTo("tests");
        }

        @Test
        void propagateDataProviderFailureAsTestFailureFalse() {
            var op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(false);
            assertThat(op.options().get("-propagateDataProviderFailureAsTestFailure")).isEqualTo("false");
        }

        @Test
        void propagateDataProviderFailureAsTestFailureTrue() {
            var op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(true);
            assertThat(op.options().get("-propagateDataProviderFailureAsTestFailure")).isEqualTo("true");
        }

        @Test
        void reported() {
            var op = new TestNgOperation().reporter(FOO);
            assertThat(op.options().get("-reporter")).isEqualTo(FOO);
        }

        @Test
        void runFactory() {
            var op = new TestNgOperation().testRunFactory(FOO);
            assertThat(op.options().get("-testrunfactory")).isEqualTo(FOO);
        }

        @Test
        void shareThreadPoolForDataProviders() {
            var op = new TestNgOperation();
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isNull();
        }

        @Test
        void shareThreadPoolForDataProvidersFalse() {
            var op = new TestNgOperation().shareThreadPoolForDataProviders(false);
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isEqualTo("false");
        }

        @Test
        void shareThreadPoolForDataProvidersTrue() {
            var op = new TestNgOperation().shareThreadPoolForDataProviders(true);
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isEqualTo("true");
        }

        @Test
        void spiListenersToSkip() {
            var ops = new TestNgOperation().spiListenersToSkip(FOO, BAR);
            assertThat(ops.options().get("-spilistenerstoskip")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void spiListenersToSkipAsList() {
            var ops = new TestNgOperation().spiListenersToSkip(List.of(FOO, BAR));
            assertThat(ops.options().get("-spilistenerstoskip")).isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void suiteNameRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().suiteName(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testClassRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().testClass(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testClasspathRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().testClasspath(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void testNames() {
            var ops = new TestNgOperation().testNames(FOO, BAR);
            assertThat(ops.testNames()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void testNamesAsList() {
            var ops = new TestNgOperation().testNames(List.of(FOO, BAR));
            assertThat(ops.testNames()).containsExactlyInAnyOrder(BAR, FOO);
        }

        @Test
        void testNamesRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().testNames(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void threadCount() {
            var op = new TestNgOperation().threadCount(1);
            assertThat(op.options().get("-threadcount")).isEqualTo("1");
        }

        @Test
        void threadCountInvalid() {
            assertThatThrownBy(() -> new TestNgOperation().threadCount(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void threadPoolFactoryClass() {
            var op = new TestNgOperation().threadPoolFactoryClass(FOO);
            assertThat(op.options().get("-threadpoolfactoryclass")).isEqualTo(FOO);
        }

        @Test
        void useDefaultListenersFalse() {
            var op = new TestNgOperation().useDefaultListeners(false);
            assertThat(op.options().get("-usedefaultlisteners")).isEqualTo("false");
        }

        @Test
        void useDefaultListenersTrue() {
            var op = new TestNgOperation().useDefaultListeners(true);
            assertThat(op.options().get("-usedefaultlisteners")).isEqualTo("true");
        }

        @Test
        void useGlobalThreadPool() {
            var op = new TestNgOperation();
            assertThat(op.options().get("-useGlobalThreadPool")).isNull();
        }

        @Test
        void useGlobalThreadPoolFalse() {
            var op = new TestNgOperation().useGlobalThreadPool(false);
            assertThat(op.options().get("-useGlobalThreadPool")).isEqualTo("false");
        }

        @Test
        void useGlobalThreadPoolTrue() {
            var op = new TestNgOperation().useGlobalThreadPool(true);
            assertThat(op.options().get("-useGlobalThreadPool")).isEqualTo("true");
        }

        @Test
        void verbose() {
            var op = new TestNgOperation().verbose(1);
            assertThat(op.options().get("-log")).isEqualTo("1");
        }

        @Nested
        @DisplayName("Source Dir Tests")
        class SourceDirTests {

            private final File bar = new File(BAR);
            private final File foo = new File(FOO);
            private final String foobar = String.format("%s;%s", foo.getAbsolutePath(), bar.getAbsolutePath());

            @Test
            void sourceDirAsFileArray() {
                var op = new TestNgOperation().sourceDir(foo, bar);
                assertThat(op.options().get("-sourcedir")).isEqualTo(foobar);
            }

            @Test
            void sourceDirAsFileList() {
                var op = new TestNgOperation().sourceDirFiles(List.of(foo, bar));
                assertThat(op.options().get("-sourcedir")).isEqualTo(foobar);
            }

            @Test
            void sourceDirAsPathArray() {
                var op = new TestNgOperation().sourceDir(foo.toPath(), bar.toPath());
                assertThat(op.options().get("-sourcedir")).isEqualTo(foobar);
            }

            @Test
            void sourceDirAsPathList() {
                var op = new TestNgOperation().sourceDirPaths(List.of(foo.toPath(), bar.toPath()));
                assertThat(op.options().get("-sourcedir")).isEqualTo(foobar);
            }

            @Test
            void sourceDirAsStringArray() {
                var op = new TestNgOperation().sourceDir(FOO, BAR);
                assertThat(op.options().get("-sourcedir")).isEqualTo(FOO + ';' + BAR);
            }

            @Test
            void sourceDirAsStringList() {
                var op = new TestNgOperation().sourceDir(List.of(FOO, BAR));
                assertThat(op.options().get("-sourcedir")).isEqualTo(FOO + ';' + BAR);
            }
        }

        @Nested
        @DisplayName("XML Path In Jar Tests")
        class XmlPathInJarTests {

            private final File foo = new File(FOO);

            @Test
            void xmlPathInJarAsFile() {
                var op = new TestNgOperation().xmlPathInJar(foo);
                assertThat(op.options().get("-xmlpathinjar")).isEqualTo(foo.getAbsolutePath());
            }

            @Test
            void xmlPathInJarAsPath() {
                var op = new TestNgOperation().xmlPathInJar(foo.toPath());
                assertThat(op.options().get("-xmlpathinjar")).isEqualTo(foo.getAbsolutePath());
            }

            @Test
            void xmlPathInJarAsString() {
                var op = new TestNgOperation().xmlPathInJar(FOO);
                assertThat(op.options().get("-xmlpathinjar")).isEqualTo(FOO);
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void xmlPathInJarRejectsNullFile() {
                assertThatThrownBy(() -> new TestNgOperation().xmlPathInJar((File) null))
                        .isInstanceOf(NullPointerException.class);
            }

            @Test
            @SuppressWarnings("DataFlowIssue")
            void xmlPathInJarRejectsNullPath() {
                assertThatThrownBy(() -> new TestNgOperation().xmlPathInJar((Path) null))
                        .isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Nested
    @DisplayName("Suite Tests")
    class SuiteTests {

        @Test
        void suiteName() {
            var op = new TestNgOperation().suiteName(FOO);
            assertThat(op.options().get("-suitename")).isEqualTo("\"" + FOO + '\"');
        }

        @Test
        void suiteThreadPoolSize() {
            var op = new TestNgOperation().suiteThreadPoolSize(1);
            assertThat(op.options().get("-suitethreadpoolsize")).isEqualTo("1");
        }

        @Test
        void suiteThreadPoolSizeInvalid() {
            assertThatThrownBy(() -> new TestNgOperation().suiteThreadPoolSize(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void suites() {
            var op = new TestNgOperation().suites(FOO, BAR);
            assertThat(op.suites()).contains(FOO).contains(BAR);
        }

        @Test
        void suitesAsList() {
            var op = new TestNgOperation().suites(List.of(FOO, BAR));
            assertThat(op.suites()).contains(FOO).contains(BAR);
        }

        @Test
        void suitesRejectsEmpty() {
            assertThatThrownBy(() -> new TestNgOperation().suites(List.of("")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}