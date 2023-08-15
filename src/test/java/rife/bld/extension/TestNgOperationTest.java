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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void testClass() {
        var op = new TestNgOperation().testClass(FOO, BAR);
        assertThat(op.options().get(TestNgOperation.TEST_CLASS_ARG)).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testDataProviderThreadCount() {
        var op = new TestNgOperation().dataProviderThreadCount(1);
        assertThat(op.options().get("-dataproviderthreadcount")).isEqualTo("1");
    }

    @Test
    void testDirectory() {
        var op = new TestNgOperation().directory(FOO);
        assertThat(op.options().get("-d")).isEqualTo(FOO);
    }

    @Test
    void testExcludeGroups() {
        var op = new TestNgOperation().excludeGroups(FOO, BAR);
        assertThat(op.options().get("-excludegroups")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testExecute() {
        assertThatThrownBy(() ->
                new TestNgOperation().fromProject(new Project()).packages("rife.bld.extension")
                        .testClass("rife.bld.extension.TestNGSimpleTest")
                        .execute()).isInstanceOf(ExitStatusException.class);
    }

    @Test
    void testFailurePolicy() {
        var op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.CONTINUE);
        assertThat(op.options().get("-configfailurepolicy")).isEqualTo("continue");

        op = new TestNgOperation().failurePolicy(TestNgOperation.FailurePolicy.SKIP);
        assertThat(op.options().get("-configfailurepolicy")).isEqualTo("skip");
    }

    @Test
    void testGroups() {
        var op = new TestNgOperation().groups(FOO, BAR);
        assertThat(op.options().get("-groups")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testJar() {
        var op = new TestNgOperation().testJar(FOO);
        assertThat(op.options().get("-testjar")).isEqualTo(FOO);
    }

    @Test
    void testMethodDetectors() {
        var op = new TestNgOperation().methodSelectors(FOO, BAR);
        assertThat(op.options().get("-methodselectors")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testMethods() {
        var op = new TestNgOperation().methods(FOO, BAR);
        assertThat(op.options().get("-methods")).isEqualTo(String.format("%s,%s", FOO, BAR));
    }

    @Test
    void testName() {
        var op = new TestNgOperation().testName(FOO);
        assertThat(op.options().get("-testname")).isEqualTo("\"" + FOO + '\"');
    }

    @Test
    void testNames() {
        var ops = new TestNgOperation().testNames(FOO, BAR);
        assertThat(ops.options().get("-testnames")).isEqualTo(String.format("\"%s\",\"%s\"", FOO, BAR));
    }

    @Test
    void testParallel() {
        var op = new TestNgOperation().parallel(TestNgOperation.Parallel.TESTS);
        assertThat(op.options().get("-parallel")).isEqualTo("tests");

        op = new TestNgOperation().parallel(TestNgOperation.Parallel.METHODS);
        assertThat(op.options().get("-parallel")).isEqualTo("methods");

        op = new TestNgOperation().parallel(TestNgOperation.Parallel.CLASSES);
        assertThat(op.options().get("-parallel")).isEqualTo("classes");
    }

    @Test
    void testSourceDir() {
        var op = new TestNgOperation().sourceDir(FOO, BAR);
        assertThat(op.options().get("-sourcedir")).isEqualTo(String.format("%s;%s", FOO, BAR));
    }

    @Test
    void testSuiteName() {
        var op = new TestNgOperation().suiteName(FOO);
        assertThat(op.options().get("-suitename")).isEqualTo("\"" + FOO + '\"');
    }

    @Test
    void testThreadCount() {
        var op = new TestNgOperation().threadCount(1);
        assertThat(op.options().get("-threadcount")).isEqualTo("1");
    }

    @Test
    void testXmlPathInJar() {
        var op = new TestNgOperation().xmlPathInJar(FOO);
        assertThat(op.options().get("-xmlpathinjar")).isEqualTo(FOO);
    }
}
