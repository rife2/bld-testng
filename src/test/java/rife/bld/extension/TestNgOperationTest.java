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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                    .alwaysRunListeners(true)
                    .dataProviderThreadCount(1)
                    .dependencyInjectorFactory("injectorfactory")
                    .directory("dir")
                    .excludeGroups("group")
                    .failWhenEverythingSkipped(true)
                    .failurePolicy(TestNgOperation.FailurePolicy.SKIP)
                    .generateResultsPerSuite(true)
                    .groups("group1", "group2")
                    .ignoreMissedTestName(true)
                    .includeAllDataDrivenTestsWhenSkipping(true)
                    .listener("listener")
                    .listenerComparator("comparator")
                    .listenerFactory("factory")
                    .log(1)
                    .methodSelectors("selector")
                    .methods("methods")
                    .mixed(true)
                    .objectFactory("objectFactory")
                    .overrideIncludedMethods("method")
                    .parallel(TestNgOperation.Parallel.TESTS)
                    .propagateDataProviderFailureAsTestFailure(true)
                    .reporter("reporter")
                    .shareThreadPoolForDataProviders(true)
                    .spiListenersToSkip("listenter")
                    .suiteName("name")
                    .suiteThreadPoolSize(1)
                    .testClass("class")
                    .testJar("jar")
                    .testName("name")
                    .testNames("names")
                    .testRunFactory("runFactory")
                    .threadCount(1)
                    .threadPoolFactoryClass("poolClass")
                    .useDefaultListeners(true)
                    .useGlobalThreadPool(true)
                    .xmlPathInJar("jarPath")
                    .executeConstructProcessCommandList();

            try (var softly = new AutoCloseableSoftAssertions()) {
                for (var p : args) {
                    var found = false;
                    for (var a : params) {
                        if (a.startsWith(p)) {
                            found = true;
                            break;
                        }
                    }
                    softly.assertThat(found).as("%s not found.", p).isTrue();
                }
            }

        }

        @Test
        void classpath() {
            var op = new TestNgOperation().testClasspath(FOO, BAR);
            assertThat(op.testClasspath()).containsExactly(BAR, FOO);
        }

        @Test
        void dataProviderThreadCount() {
            var op = new TestNgOperation().dataProviderThreadCount(1);
            assertThat(op.options().get("-dataproviderthreadcount")).isEqualTo("1");
        }

        @Test
        void dependencyInjectorFactory() {
            var op = new TestNgOperation().dependencyInjectorFactory(FOO);
            assertThat(op.options().get("-dependencyinjectorfactory")).isEqualTo(FOO);
        }

        @Test
        void excludeGroups() {
            var op = new TestNgOperation().excludeGroups(FOO, BAR, FOO);
            assertThat(op.excludeGroups()).containsExactly(BAR, FOO);
        }

        @Test
        void excludeGroupsAsList() {
            var op = new TestNgOperation().excludeGroups(List.of(FOO, "", BAR));
            assertThat(op.excludeGroups()).containsExactly(BAR, FOO);
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
            assertThat(op.groups()).containsExactly(BAR, FOO);
        }

        @Test
        void groupsAsList() {
            var op = new TestNgOperation().groups(List.of(FOO, BAR, ""));
            assertThat(op.groups()).hasSize(2).contains(FOO, BAR);
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
        void log() {
            var op = new TestNgOperation().log(1);
            assertThat(op.options().get("-log")).isEqualTo("1");
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
            assertThat(op.methods()).containsExactly(BAR, FOO);
        }

        @Test
        void methodsAsList() {
            var op = new TestNgOperation().methods(List.of(FOO, BAR, BAR, ""));
            assertThat(op.methods()).containsExactly(BAR, FOO);
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
        void port() {
            var op = new TestNgOperation().port(1);
            assertThat(op.options().get("-port")).isEqualTo("1");
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
        void shareThreadPoolForDataProvidersFalse() {
            var op = new TestNgOperation().shareThreadPoolForDataProviders(false);
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isNull();
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
        void testNames() {
            var ops = new TestNgOperation().testNames(FOO, BAR);
            assertThat(ops.testNames()).containsExactly(BAR, FOO);
        }

        @Test
        void testNamesAsList() {
            var ops = new TestNgOperation().testNames(List.of(FOO, BAR));
            assertThat(ops.testNames()).containsExactly(BAR, FOO);
        }

        @Test
        void threadCount() {
            var op = new TestNgOperation().threadCount(1);
            assertThat(op.options().get("-threadcount")).isEqualTo("1");
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
        void useGlobalThreadPoolFalse() {
            var op = new TestNgOperation().useGlobalThreadPool(false);
            assertThat(op.options().get("-useGlobalThreadPool")).isNull();
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
        void suites() {
            var op = new TestNgOperation().suites(FOO, BAR);
            assertThat(op.suites()).contains(FOO).contains(BAR);
        }

        @Test
        void suitesAsList() {
            var op = new TestNgOperation().suites(List.of(FOO, BAR));
            assertThat(op.suites()).contains(FOO).contains(BAR);
        }
    }
}