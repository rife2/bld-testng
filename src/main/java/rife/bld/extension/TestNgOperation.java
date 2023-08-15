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

import rife.bld.BaseProject;
import rife.bld.operations.AbstractProcessOperation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Run tests with <a href="https;//testng.org/">TestNG</a>.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestNgOperation extends AbstractProcessOperation<TestNgOperation> {
    public static final String TEST_CLASS_ARG = "-testclass";
    private static final Logger LOGGER = Logger.getLogger(TestNgOperation.class.getName());
    private final List<String> args = new ArrayList<>();
    private final Map<String, String> options = new ConcurrentHashMap<>();
    private final List<String> packages = new ArrayList<>();
    private final List<String> suites = new ArrayList<>();
    private BaseProject project;

    /**
     * This sets the default maximum number of threads to use for data providers when running tests in parallel.
     * It will only take effect if the parallel mode has been selected (for example, with the parallel option).
     * This can be overridden in the suite definition.
     */
    public TestNgOperation dataProviderThreadCount(int count) {
        options.put("-dataproviderthreadcount", String.valueOf(count));
        return this;
    }

    /**
     * The directory where the reports will be generated (defaults to {@code build/test-output}).
     */
    public TestNgOperation directory(String directoryPath) {
        options.put("-d", directoryPath);
        return this;
    }

    /**
     * The list of groups you want to be excluded from this run.
     */
    public TestNgOperation excludeGroups(String... group) {
        options.put("-excludegroups", String.join(",", group));
        return this;
    }

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 1.5
     */
    @Override
    protected List<String> executeConstructProcessCommandList() {
        if (project == null) {
            LOGGER.severe("A project must be specified.");
        } else if (packages.isEmpty() && suites.isEmpty()) {
            LOGGER.severe("At least one suite or package is required.");
        }

        if (!options.containsKey("-d")) {
            options.put("d", Path.of(project.buildDirectory().getPath(), "test-output").toString());
        }

        args.clear();
        args.add(javaTool());
        args.add("-cp");
        args.add(String.format("%s:%s:%s", Path.of(project.libTestDirectory().getPath(), "*"),
                project.buildMainDirectory(), project.buildTestDirectory()));
        args.add("org.testng.TestNG");

        options.forEach((k, v) -> {
            args.add(k);
            args.add(v);
        });

        if (!options.containsKey(TEST_CLASS_ARG)) {
            try {
                var temp = tempFile();
                try (var bufWriter = Files.newBufferedWriter(Paths.get(temp.getPath()))) {
                    bufWriter.write("<suite name=\"bld Default Suite\" verbose=\"2\"><test name=\"All Packages\"><packages>");
                    for (var p : packages) {
                        bufWriter.write(String.format("<package name=\"%s\"/>", p));
                    }
                    bufWriter.write("</packages></test></suite>");
                    args.add(temp.getPath());
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "An IO error occurred while accessing the default testng.xml file", ioe);
            }
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.join(" ", args));
        }

        return args;
    }

    /**
     * Configures a PMD operation from a {@link BaseProject}.
     */
    @Override
    public TestNgOperation fromProject(BaseProject project) {
        this.project = project;
        directory(Path.of(project.buildDirectory().getPath(), "test-output").toString());
        return this;
    }

    /**
     * Whether TestNG should continue to execute the remaining tests in the suite or skip them if an @Before* method
     */
    public TestNgOperation failurePolicy(FailurePolicy policy) {
        options.put("-configfailurepolicy", policy.name().toLowerCase(Locale.getDefault()));
        return this;
    }

    /**
     * The list of groups you want to run (e.g. "{@code "windows", "linux", "regression}").
     */
    public TestNgOperation groups(String... group) {
        options.put("-groups", String.join(",", group));
        return this;
    }

    /**
     * Lets you specify method selectors on the command line.
     * For example: {@code "com.example.Selector1:3", "com.example.Selector2:2"}
     */
    public TestNgOperation methodSelectors(String... detector) {
        options.put("-methodselectors", String.join(",", detector));
        return this;
    }

    /**
     * Lets you specify individual methods to run.
     * For example: {@code "com.example.Foo.f1", "com.example.Bar.f2"}
     */
    public TestNgOperation methods(String... method) {
        options.put("-methods", String.join(",", method));
        return this;
    }

    protected Map<String, String> options() {
        return options;
    }

    /**
     * The list of packages to include in this test. For example: {@code "com.example", "test.sample.*"}
     * If the package name ends with .* then subpackages are included too.
     */
    public TestNgOperation packages(String... name) {
        packages.addAll(Arrays.stream(name).toList());
        return this;
    }

    /**
     * If specified, sets the default mechanism used to determine how to use parallel threads when running tests.
     * If not set, default mechanism is not to use parallel threads at all.
     * This can be overridden in the suite definition.
     */

    public TestNgOperation parallel(Parallel mechanism) {
        options.put("-parallel", mechanism.name().toLowerCase(Locale.getDefault()));
        return this;
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     */
    public TestNgOperation sourceDir(String... directory) {
        options.put("-sourcedir", String.join(";", directory));
        return this;
    }

    /**
     * This specifies the suite name for a test suite defined on the command line. This option is ignored if the
     * suite.xml file or the source code specifies a different suite name.
     */
    public TestNgOperation suiteName(String name) {
        options.put("-suitename", '"' + name + '"');
        return this;
    }

    /**
     * Specifies the suites to run. For example: {@code "testng.xml", "testng2.xml"}
     */
    public void suites(String... suite) {
        suites.addAll(Arrays.stream(suite).toList());
    }

    private File tempFile() throws IOException {
        var temp = File.createTempFile("testng", ".xml");
        temp.deleteOnExit();
        return temp;
    }

    /**
     * A list of class files separated by commas (e.g. {@code "org.foo.Test1","org.foo.test2"}).
     */
    public TestNgOperation testClass(String... aClass) {
        options.put("-testclass", String.join(",", aClass));
        return this;
    }

    /**
     * Specifies a jar file that contains test classes. If a testng.xml file is found at the root of that jar file,
     * it will be used, otherwise, all the test classes found in this jar file will be considered test classes.
     */
    public TestNgOperation testJar(String jar) {
        options.put("-testjar", jar);
        return this;
    }

    /**
     * This specifies the name for a test defined on the command line. This option is ignored if the suite.xml file or
     * the source code specifies a different test name.
     */
    public TestNgOperation testName(String name) {
        options.put("-testname", '"' + name + '"');
        return this;
    }

    /**
     * Only tests defined in a {@code <test>} tag matching one of these names will be run.
     */
    public TestNgOperation testNames(String... name) {
        options.put("-testnames", Arrays.stream(name).map(s -> '"' + s + '"').collect(Collectors.joining(",")));
        return this;
    }

    /**
     * This sets the default maximum number of threads to use for running tests in parallel. It will only take effect
     * if the parallel mode has been selected (for example, with the -parallel option). This can be overridden in the
     * suite definition.
     */
    public TestNgOperation threadCount(int count) {
        options.put("-threadcount", String.valueOf(count));
        return this;
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"|). The default is {@code testng.xml}, which means a file called
     * {@code testng.xml} at the root of the jar file. This option will be ignored unless a test jar is specified.
     */
    public TestNgOperation xmlPathInJar(String path) {
        options.put("-xmlpathinjar", path);
        return this;
    }

    /**
     * Parallel Mechanisms
     */
    public enum Parallel {
        METHODS, TESTS, CLASSES
    }

    /**
     * Failure Policies
     */
    public enum FailurePolicy {
        SKIP, CONTINUE
    }
}