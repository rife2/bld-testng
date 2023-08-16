/*
 *  Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package rife.bld.extension;

import org.junit.jupiter.api.Test;
import rife.bld.Project;
import rife.bld.operations.exceptions.ExitStatusException;

import static org.assertj.core.api.Assertions.*;

/**
 * Implements the TestNgOperationTest class.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestNgOperationTest {
    private static final String BAR = "bar";
    private static final String FOO = "foo";

    @Test
    void testAlwaysRunListeners() {
        var op = new TestNgOperation().alwaysRunListeners(false);
        assertThat(op.options.get("-alwaysrunlisteners")).isEqualTo("false");

        op = new TestNgOperation().alwaysRunListeners(true);
        assertThat(op.options.get("-alwaysrunlisteners")).isEqualTo("true");
    }

    @Test
    void testClass() {
        var op = new TestNgOperation().testClass(FOO, BAR);
        assertThat(op.options.get(TestNgOperation.TEST_CLASS_ARG)).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testDataProviderThreadCount() {
        var op = new TestNgOperation().dataProviderThreadCount(1);
        assertThat(op.options.get("-dataproviderthreadcount")).isEqualTo("1");
    }

    @Test
    void testDependencyInjectorFactory() {
        var op = new TestNgOperation().dependencyInjectorFactory(FOO);
        assertThat(op.options.get("-dependencyinjectorfactory")).isEqualTo(FOO);
    }

    @Test
    void testDirectory() {
        var op = new TestNgOperation().directory(FOO);
        assertThat(op.options.get("-d")).isEqualTo(FOO);
    }

    @Test
    void testExcludeGroups() {
        var op = new TestNgOperation().excludeGroups(FOO, BAR);
        assertThat(op.options.get("-excludegroups")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testExecute() {
        assertThatThrownBy(() ->
                new TestNgOperation().fromProject(new Project())
                        .testClass("rife.bld.extension.TestNgSimpleTest")
                        .execute()).isInstanceOf(ExitStatusException.class);

        assertThatThrownBy(() ->
                new TestNgOperation().fromProject(new Project())
                        .suites("src/test/resources/testng.xml")
                        .execute()).isInstanceOf(ExitStatusException.class);

        assertThatCode(() ->
                new TestNgOperation().fromProject(new Project())
                        .testClass("rife.bld.extension.TestNgSimpleTest")
                        .methods("rife.bld.extension.TestNgSimpleTest.verifyHello")
                        .execute())
                .doesNotThrowAnyException();

        assertThatCode(() ->
                new TestNgOperation().fromProject(new Project())
                        .suites("src/test/resources/testng2.xml")
                        .execute())
                .doesNotThrowAnyException();

        assertThatCode(() ->
                new TestNgOperation().fromProject(new Project())
                        .suites("src/test/resources/testng3.xml")
                        .log(2)
                        .execute())
                .doesNotThrowAnyException();
    }

    @Test
    void testFailurePolicy() {
        var op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.CONTINUE);
        assertThat(op.options.get("-configfailurepolicy")).isEqualTo("continue");

        op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.SKIP);
        assertThat(op.options.get("-configfailurepolicy")).isEqualTo("skip");
    }

    @Test
    void testGenerateResultsPerSuite() {
        var op = new TestNgOperation().generateResultsPerSuite(false);
        assertThat(op.options.get("-generateResultsPerSuite")).isEqualTo("false");

        op = new TestNgOperation().generateResultsPerSuite(true);
        assertThat(op.options.get("-generateResultsPerSuite")).isEqualTo("true");
    }

    @Test
    void testGroups() {
        var op = new TestNgOperation().groups(FOO, BAR);
        assertThat(op.options.get("-groups")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testIgnoreMissedTestName() {
        var op = new TestNgOperation().ignoreMissedTestName(false);
        assertThat(op.options.get("-ignoreMissedTestNames")).isEqualTo("false");

        op = new TestNgOperation().ignoreMissedTestName(true);
        assertThat(op.options.get("-ignoreMissedTestNames")).isEqualTo("true");
    }

    @Test
    void testIncludeAllDataDrivenTestsWhenSkipping() {
        var op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(false);
        assertThat(op.options.get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("false");

        op = new TestNgOperation().includeAllDataDrivenTestsWhenSkipping(true);
        assertThat(op.options.get("-includeAllDataDrivenTestsWhenSkipping")).isEqualTo("true");
    }

    @Test
    void testJar() {
        var op = new TestNgOperation().testJar(FOO);
        assertThat(op.options.get("-testjar")).isEqualTo(FOO);
    }

    @Test
    void testJunit() {
        var op = new TestNgOperation().jUnit(false);
        assertThat(op.options.get("-junit")).isEqualTo("false");

        op = new TestNgOperation().jUnit(true);
        assertThat(op.options.get("-junit")).isEqualTo("true");
    }

    @Test
    void testListener() {
        var ops = new TestNgOperation().listener(FOO, BAR);
        assertThat(ops.options.get("-listener")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testMethodDetectors() {
        var op = new TestNgOperation().methodSelectors(FOO, BAR);
        assertThat(op.options.get("-methodselectors")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testMethods() {
        var op = new TestNgOperation().methods(FOO, BAR);
        assertThat(op.options.get("-methods")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testMixed() {
        var op = new TestNgOperation().mixed(false);
        assertThat(op.options.get("-mixed")).isEqualTo("false");

        op = new TestNgOperation().mixed(true);
        assertThat(op.options.get("-mixed")).isEqualTo("true");
    }

    @Test
    void testName() {
        var op = new TestNgOperation().testName(FOO);
        assertThat(op.options.get("-testname")).isEqualTo("\"" + FOO + '\"');
    }

    @Test
    void testNames() {
        var ops = new TestNgOperation().testNames(FOO, BAR);
        assertThat(ops.options.get("-testnames")).isEqualTo(String.format("\"%s\",\"%s\"", FOO, BAR));
    }

    @Test
    void testObjectFactory() {
        var ops = new TestNgOperation().objectFactory(FOO, BAR);
        assertThat(ops.options.get("-objectfactory")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testOverrideIncludedMethods() {
        var ops = new TestNgOperation().overrideIncludedMethods(FOO, BAR);
        assertThat(ops.options.get("-overrideincludedmethods")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testPackages() {
        var op = new TestNgOperation().packages(FOO, BAR);
        assertThat(op.packages).contains(FOO).contains(BAR);
    }

    @Test
    void testParallel() {
        var op = new TestNgOperation().parallel(TestNgOperation.Parallel.TESTS);
        assertThat(op.options.get("-parallel")).isEqualTo("tests");

        op = new TestNgOperation().parallel(TestNgOperation.Parallel.METHODS);
        assertThat(op.options.get("-parallel")).isEqualTo("methods");

        op = new TestNgOperation().parallel(TestNgOperation.Parallel.CLASSES);
        assertThat(op.options.get("-parallel")).isEqualTo("classes");
    }

    @Test
    void testPort() {
        var op = new TestNgOperation().port(1);
        assertThat(op.options.get("-port")).isEqualTo("1");
    }

    @Test
    void testPropagateDataProviderFailureAsTestFailure() {
        var op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(false);
        assertThat(op.options.get("-propagateDataProviderFailureAsTestFailure")).isEqualTo("false");

        op = new TestNgOperation().propagateDataProviderFailureAsTestFailure(true);
        assertThat(op.options.get("-propagateDataProviderFailureAsTestFailure")).isEqualTo("true");
    }

    @Test
    void testReported() {
        var op = new TestNgOperation().reporter(FOO);
        assertThat(op.options.get("-reporter")).isEqualTo(FOO);
    }

    @Test
    void testRunFactory() {
        var op = new TestNgOperation().testRunFactory(FOO);
        assertThat(op.options.get("-testrunfactory")).isEqualTo(FOO);
    }

    @Test
    void testSourceDir() {
        var op = new TestNgOperation().sourceDir(FOO, BAR);
        assertThat(op.options.get("-sourcedir")).isEqualTo(String.format("%s;%s", FOO, BAR));
    }

    @Test
    void testSpiListenersToSkip() {
        var ops = new TestNgOperation().spiListenersToSkip(FOO, BAR);
        assertThat(ops.options.get("-spilistenerstoskip")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testSuiteName() {
        var op = new TestNgOperation().suiteName(FOO);
        assertThat(op.options.get("-suitename")).isEqualTo("\"" + FOO + '\"');
    }

    @Test
    void testSuiteThreadPoolSize() {
        var op = new TestNgOperation().suiteThreadPoolSize(1);
        assertThat(op.options.get("-suitethreadpoolsize")).isEqualTo("1");
    }

    @Test
    void testSuites() {
        var op = new TestNgOperation().suites(FOO, BAR);
        assertThat(op.suites).contains(FOO).contains(BAR);
    }

    @Test
    void testThreadCount() {
        var op = new TestNgOperation().threadCount(1);
        assertThat(op.options.get("-threadcount")).isEqualTo("1");
    }

    @Test
    void testThreadPoolFactoryClass() {
        var op = new TestNgOperation().threadPoolFactoryClass(FOO);
        assertThat(op.options.get("-threadpoolfactoryclass")).isEqualTo(FOO);
    }

    @Test
    void testUseDefaultListeners() {
        var op = new TestNgOperation().useDefaultListeners(false);
        assertThat(op.options.get("-usedefaultlisteners")).isEqualTo("false");

        op = new TestNgOperation().useDefaultListeners(true);
        assertThat(op.options.get("-usedefaultlisteners")).isEqualTo("true");
    }

    @Test
    void testVerbose() {
        var op = new TestNgOperation().log(1);
        assertThat(op.options.get("-log")).isEqualTo("1");

        op = new TestNgOperation().verbose(1);
        assertThat(op.options.get("-verbose")).isEqualTo("1");
    }

    @Test
    void testXmlPathInJar() {
        var op = new TestNgOperation().xmlPathInJar(FOO);
        assertThat(op.options.get("-xmlpathinjar")).isEqualTo(FOO);
    }
}
