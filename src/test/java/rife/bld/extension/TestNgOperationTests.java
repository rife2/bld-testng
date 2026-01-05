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

import static org.assertj.core.api.Assertions.*;

/**
 * Implements the TestNgOperationTests class.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestNgOperationTests {

    private static final String BAR = "bar";
    private static final String FOO = "foo";

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
            assertThatThrownBy(() ->
                    new TestNgOperation().fromProject(new Project())
                            .testClass("rife.bld.extension.TestNgExampleTests")
                            .execute())
                    .as("with testClass").isInstanceOf(ExitStatusException.class);

            assertThatThrownBy(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng.xml")
                            .execute())
                    .as("with suites").isInstanceOf(ExitStatusException.class);

            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .methods("rife.bld.extension.TestNgExampleTests.foo")
                            .execute())
                    .as("with methods").isInstanceOf(ExitStatusException.class);

            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .execute())
                    .as("suite 2").doesNotThrowAnyException();

            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .log(2)
                            .execute())
                    .as("suite 2 - log ").doesNotThrowAnyException();

            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath("lib/test/*", "build/main", "build/test")
                            .log(2)
                            .execute())
                    .as("with run classpath").doesNotThrowAnyException();

            assertThatCode(() ->
                    new TestNgOperation().fromProject(new Project())
                            .suites("src/test/resources/testng2.xml")
                            .testClasspath(List.of("lib/test/*", "build/main", "build/test"))
                            .log(2)
                            .execute())
                    .as("with run classpath as list").doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Options Tests")
    class OptionsTests {

        @Test
        void alwaysRunListeners() {
            var op = new TestNgOperation().alwaysRunListeners(false);
            assertThat(op.options().get("-alwaysrunlisteners")).isEqualTo("false");

            op = new TestNgOperation().alwaysRunListeners(true);
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
                    .verbose(1)
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
            var op = new TestNgOperation().excludeGroups(FOO, BAR);
            assertThat(op.options().get("-excludegroups")).isEqualTo(String.format("%s,%s", FOO, BAR));

            op = new TestNgOperation().excludeGroups(List.of(FOO, BAR));
            assertThat(op.options().get("-excludegroups")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void failWheneverEverythingSkipped() {
            var op = new TestNgOperation().failWhenEverythingSkipped(false);
            assertThat(op.options().get("-failwheneverythingskipped")).isEqualTo("false");

            op = new TestNgOperation().failWhenEverythingSkipped(true);
            assertThat(op.options().get("-failwheneverythingskipped")).isEqualTo("true");
        }

        @Test
        void failurePolicy() {
            var op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.CONTINUE);
            assertThat(op.options().get("-configfailurepolicy")).isEqualTo("continue");

            op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.SKIP);
            assertThat(op.options().get("-configfailurepolicy")).isEqualTo("skip");
        }

        @Test
        void generateResultsPerSuite() {
            var op = new TestNgOperation().generateResultsPerSuite(false);
            assertThat(op.options().get("-generateResultsPerSuite")).isEqualTo("false");

            op = new TestNgOperation().generateResultsPerSuite(true);
            assertThat(op.options().get("-generateResultsPerSuite")).isEqualTo("true");
        }

        @Test
        void groups() {
            var op = new TestNgOperation().groups(FOO, BAR);
            assertThat(op.options().get("-groups")).isEqualTo(String.format("%s,%s", FOO, BAR));

            op.groups(List.of("group3", "group4"));
            assertThat(op.options().get("-groups")).isEqualTo("group3,group4");

        }

        @Test
        void ignoreMissedTestName() {
            var op = new TestNgOperation().ignoreMissedTestName(false);
            assertThat(op.options().get("-ignoreMissedTestNames")).isEqualTo("false");

            op = new TestNgOperation().ignoreMissedTestName(true);
            assertThat(op.options().get("-ignoreMissedTestNames")).isEqualTo("true");
        }

        @Test
        void includeAllDataDrivenTestsWhenSkipping() {
            var op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(false);
            assertThat(op.options().get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("false");

            op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(true);
            assertThat(op.options().get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("true");
        }

        @Test
        void jar() {
            var op = new TestNgOperation().testJar(FOO);
            assertThat(op.options().get("-testjar")).isEqualTo(FOO);
        }

        @Test
        void junit() {
            var op = new TestNgOperation().jUnit(false);
            assertThat(op.options().get("-junit")).isEqualTo("false");

            op = new TestNgOperation().jUnit(true);
            assertThat(op.options().get("-junit")).isEqualTo("true");
        }

        @Test
        void listener() {
            var ops = new TestNgOperation().listener(FOO, BAR);
            assertThat(ops.options().get("-listener")).isEqualTo(String.format("%s,%s", FOO, BAR));

            ops = new TestNgOperation().listener(List.of(FOO, BAR));
            assertThat(ops.options().get("-listener")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void methodDetectors() {
            var op = new TestNgOperation().methodSelectors(FOO, BAR);
            assertThat(op.options().get("-methodselectors")).isEqualTo(String.format("%s,%s", FOO, BAR));

            op = new TestNgOperation().methodSelectors(List.of(FOO, BAR));
            assertThat(op.options().get("-methodselectors")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void methods() {
            var op = new TestNgOperation().methods(FOO, BAR);
            assertThat(op.methods()).containsExactly(BAR, FOO);

            new TestNgOperation().methods(List.of(FOO, BAR));
            assertThat(op.methods()).containsExactly(BAR, FOO);
        }

        @Test
        void mixed() {
            var op = new TestNgOperation().mixed(false);
            assertThat(op.options().get("-mixed")).isEqualTo("false");

            op = new TestNgOperation().mixed(true);
            assertThat(op.options().get("-mixed")).isEqualTo("true");
        }

        @Test
        void name() {
            var op = new TestNgOperation().testName(FOO);
            assertThat(op.options().get("-testname")).isEqualTo("\"" + FOO + '\"');
        }

        @Test
        void names() {
            var ops = new TestNgOperation().testNames(FOO, BAR);
            assertThat(ops.options().get("-testnames")).isEqualTo(String.format("\"%s\",\"%s\"", FOO, BAR));

            new TestNgOperation().testNames(List.of(FOO, BAR));
            assertThat(ops.options().get("-testnames")).as("as list").isEqualTo(String.format("\"%s\",\"%s\"", FOO, BAR));
        }

        @Test
        void objectFactory() {
            var ops = new TestNgOperation().objectFactory(FOO, BAR);
            assertThat(ops.options().get("-objectfactory")).isEqualTo(String.format("%s,%s", FOO, BAR));

            ops = new TestNgOperation().objectFactory(List.of(FOO, BAR));
            assertThat(ops.options().get("-objectfactory")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void overrideIncludedMethods() {
            var ops = new TestNgOperation().overrideIncludedMethods(FOO, BAR);
            assertThat(ops.options().get("-overrideincludedmethods")).isEqualTo(String.format("%s,%s", FOO, BAR));

            ops = new TestNgOperation().overrideIncludedMethods(List.of(FOO, BAR));
            assertThat(ops.options().get("-overrideincludedmethods")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void packages() {
            var op = new TestNgOperation().packages(FOO, BAR);
            assertThat(op.packages()).contains(FOO).contains(BAR);

            op = new TestNgOperation().packages(List.of(FOO, BAR));
            assertThat(op.packages()).as("as list").contains(FOO).contains(BAR);
        }

        @Test
        void parallel() {
            var op = new TestNgOperation().parallel(TestNgOperation.Parallel.TESTS);
            assertThat(op.options().get("-parallel")).isEqualTo("tests");

            op = new TestNgOperation().parallel(TestNgOperation.Parallel.METHODS);
            assertThat(op.options().get("-parallel")).isEqualTo("methods");

            op = new TestNgOperation().parallel(TestNgOperation.Parallel.CLASSES);
            assertThat(op.options().get("-parallel")).isEqualTo("classes");
        }

        @Test
        void port() {
            var op = new TestNgOperation().port(1);
            assertThat(op.options().get("-port")).isEqualTo("1");
        }

        @Test
        void propagateDataProviderFailureAsTestFailure() {
            var op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(false);
            assertThat(op.options().get("-propagateDataProviderFailureAsTestFailure")).isEqualTo("false");

            op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(true);
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
            var op = new TestNgOperation().shareThreadPoolForDataProviders(true);
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isEqualTo("true");

            op = new TestNgOperation().shareThreadPoolForDataProviders(false);
            assertThat(op.options().get("-shareThreadPoolForDataProviders")).isNull();
        }

        @Test
        void spiListenersToSkip() {
            var ops = new TestNgOperation().spiListenersToSkip(FOO, BAR);
            assertThat(ops.options().get("-spilistenerstoskip")).isEqualTo(String.format("%s,%s", FOO, BAR));

            ops = new TestNgOperation().spiListenersToSkip(List.of(FOO, BAR));
            assertThat(ops.options().get("-spilistenerstoskip")).as("as list").isEqualTo(String.format("%s,%s", FOO, BAR));
        }

        @Test
        void testClass() {
            var op = new TestNgOperation().testClass(FOO, BAR);
            assertThat(op.options().get("-testclass")).isEqualTo(String.format("%s,%s", FOO, BAR));

            new TestNgOperation().testClass(List.of(FOO, BAR));
            assertThat(op.options().get("-testclass")).as("as list")
                    .isEqualTo(String.format("%s,%s", FOO, BAR));
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
        void useDefaultListeners() {
            var op = new TestNgOperation().useDefaultListeners(false);
            assertThat(op.options().get("-usedefaultlisteners")).isEqualTo("false");

            op = new TestNgOperation().useDefaultListeners(true);
            assertThat(op.options().get("-usedefaultlisteners")).isEqualTo("true");
        }

        @Test
        void useGlobalThreadPool() {
            var op = new TestNgOperation().useGlobalThreadPool(true);
            assertThat(op.options().get("-useGlobalThreadPool")).isEqualTo("true");

            op = new TestNgOperation().useGlobalThreadPool(false);
            assertThat(op.options().get("-useGlobalThreadPool")).isNull();
        }

        @Test
        void verbose() {
            var op = new TestNgOperation().log(1);
            assertThat(op.options().get("-log")).isEqualTo("1");

            op = new TestNgOperation().verbose(1);
            assertThat(op.options().get("-verbose")).isEqualTo("1");
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

            op = new TestNgOperation().suites(List.of(FOO, BAR));
            assertThat(op.suites()).as("as list").contains(FOO).contains(BAR);
        }
    }
}
