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
    /**
     * The run options.
     */
    protected final Map<String, String> options = new ConcurrentHashMap<>();
    /**
     * The suite packages to run.
     */
    protected final Set<String> packages = new HashSet<>();
    /**
     * The suites to run.
     */
    protected final Set<String> suites = new HashSet<>();
    /**
     * The classpath entries used for running tests.
     */
    protected final Set<String> testClasspath = new HashSet<>();
    private BaseProject project;

    /**
     * Should Method Invocation Listeners be run even for skipped methods.
     *
     * <p>Default is {@code true}</p>
     */
    public TestNgOperation alwaysRunListeners(Boolean isAlwaysRunListeners) {
        options.put("-alwaysrunlisteners", String.valueOf(isAlwaysRunListeners));
        return this;
    }

    /**
     * This sets the default maximum number of threads to use for data providers when running tests in parallel.
     * It will only take effect if the parallel mode has been selected (for example,with the
     * {@link #parallel(Parallel) parallel} option). This can be overridden in the suite definition.
     */
    public TestNgOperation dataProviderThreadCount(int count) {
        options.put("-dataproviderthreadcount", String.valueOf(count));
        return this;
    }

    /**
     * The dependency injector factory implementation that TestNG should use.
     */
    public TestNgOperation dependencyInjectorFactory(String injectorFactory) {
        options.put("-dependencyinjectorfactory", injectorFactory);
        return this;
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
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
     * Part of the {@link #execute execute} operation, constructs the command list to use for building the process.
     */
    @Override
    protected List<String> executeConstructProcessCommandList() {
        if (project == null) {
            LOGGER.severe("A project must be specified.");
        } else if (packages.isEmpty() && suites.isEmpty()) {
            LOGGER.severe("At least one package or XML suite is required.");
        }

        if (!options.containsKey("-d")) {
            options.put("d", Path.of(project.buildDirectory().getPath(), "test-output").toString());
        }

        List<String> args = new ArrayList<>();
        args.add(javaTool());

        args.add("-cp");
        if (testClasspath.isEmpty()) {
            args.add(String.format("%s:%s:%s", Path.of(project.libTestDirectory().getPath(), "*"),
                    project.buildMainDirectory(), project.buildTestDirectory()));
        } else {
            args.add(String.join(":", testClasspath));
        }

        args.add("org.testng.TestNG");

        options.forEach((k, v) -> {
            args.add(k);
            args.add(v);
        });

        if (!suites.isEmpty()) {
            args.addAll(suites);
        } else if (!options.containsKey(TEST_CLASS_ARG)) {
            try {
                args.add(writeDefaultSuite().getPath());
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "An IO error occurred while accessing the default testng.xml file", ioe);
            }
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.join(" ", args));
            LOGGER.info(String.format("Report will be saved in file://%s", new File(options.get("-d"))));
        }

        return args;
    }

    /**
     * Configures the {@link BaseProject}.
     */
    @Override
    public TestNgOperation fromProject(BaseProject project) {
        this.project = project;
        directory(Path.of(project.buildDirectory().getPath(), "test-output").toString());
        return this;
    }

    /**
     * Should TestNG fail execution if all tests were skipped and nothing was run.
     */
    public TestNgOperation failWhenEverythingSkipped(Boolean isFailAllSkipped) {
        options.put("-failwheneverythingskipped", String.valueOf(isFailAllSkipped));
        return this;
    }

    /**
     * Whether TestNG should continue to execute the remaining tests in the suite or skip them if in a {@code @Before*}
     * method.
     */
    public TestNgOperation failurePolicy(FailurePolicy policy) {
        options.put("-configfailurepolicy", policy.name().toLowerCase(Locale.getDefault()));
        return this;
    }

    /**
     * Should TestNG consider failures in Data Providers as test failures.
     *
     * <p>Default is {@code false}</p>.
     */
    public TestNgOperation generateResultsPerSuite(Boolean resultsPerSuite) {
        options.put("-generateResultsPerSuite", String.valueOf(resultsPerSuite));
        return this;
    }

    /**
     * The list of groups you want to run.
     *
     * <p>For example: {@code "windows", "linux", "regression}</p>
     */
    public TestNgOperation groups(String... group) {
        options.put("-groups", String.join(",", group));
        return this;
    }

    /**
     * Ignore missed test names given by {@link #testNames(String...) testNames} and continue to run existing tests,
     * if any.
     *
     * <p>Default is {@code false}</p>
     */
    public TestNgOperation ignoreMissedTestName(Boolean isIgnoreMissedTestNames) {
        options.put("-ignoreMissedTestNames", String.valueOf(isIgnoreMissedTestNames));
        return this;
    }

    /**
     * Should TestNG report all iterations of a data driven test as individual skips, in-case of upstream failures.
     *
     * <p>Default is {@code false}</p>
     */
    public TestNgOperation includeAllDataDrivenTestsWhenSkipping(Boolean isIncludeDrivenTestsWhenSkipping) {
        options.put("-includeAllDataDrivenTestsWhenSkipping", String.valueOf(isIncludeDrivenTestsWhenSkipping));
        return this;
    }

    /**
     * Enables or disables the JUnit mode.
     *
     * <p>Default is {@code false}</p>
     */
    public TestNgOperation jUnit(Boolean isJunit) {
        options.put("-junit", String.valueOf(isJunit));
        return this;
    }

    /**
     * The list of {@code .class} files or list of class names implementing {@code ITestListener} or
     * {@code ISuiteListener}
     */
    public TestNgOperation listener(String... listener) {
        options.put("-listener", String.join(",", listener));
        return this;
    }

    /**
     * Set the Level of verbosity.
     *
     * @see #verbose(int)
     */
    public TestNgOperation log(int level) {
        options.put("-log", String.valueOf(level));
        return this;
    }

    /**
     * Specifies the list of {@code .class} files or class names implementing {@code IMethodSelector}.
     *
     * <p>For example: {@code "com.example.Selector1:3", "com.example.Selector2:2"}</p>
     */
    public TestNgOperation methodSelectors(String... selector) {
        options.put("-methodselectors", String.join(",", selector));
        return this;
    }

    /**
     * Lets you specify individual methods to run.
     *
     * <p>For example: {@code "com.example.Foo.f1", "com.example.Bar.f2"}</p>
     */
    public TestNgOperation methods(String... method) {
        options.put("-methods", String.join(",", method));
        return this;
    }

    /**
     * Mixed mode autodetects the type of current test and run it with appropriate runner.
     *
     * <p>Default is {@code false}</p>
     */
    public TestNgOperation mixed(Boolean isMixed) {
        options.put("-mixed", String.valueOf(isMixed));
        return this;
    }

    /**
     * The list of {@code .class} files or class names implementing {@code ITestRunnerFactory}.
     */
    public TestNgOperation objectFactory(String... factory) {
        options.put("-objectfactory", String.join(",", factory));
        return this;
    }

    /**
     * The list of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     */
    public TestNgOperation overrideIncludedMethods(String... method) {
        options.put("-overrideincludedmethods", String.join(",", method));
        return this;
    }

    /**
     * The list of packages to include in this test.
     * If the package name ends with .* then subpackages are included too.
     * Required if no {@link #suites(String... suites)} specified.
     *
     * <p>For example: {@code "com.example", "test.sample.*"}</p>
     */
    public TestNgOperation packages(String... name) {
        packages.addAll(Arrays.stream(name).toList());
        return this;
    }

    /**
     * If specified, sets the default mechanism used to determine how to use parallel threads when running tests.
     * If not set, default mechanism is not to use parallel threads at all.
     * This can be overridden in the suite definition.
     *
     * @see Parallel
     */
    public TestNgOperation parallel(Parallel mechanism) {
        options.put("-parallel", mechanism.name().toLowerCase(Locale.getDefault()));
        return this;
    }

    /**
     * Specifies the port number.
     */
    public TestNgOperation port(int port) {
        options.put("-port", String.valueOf(port));
        return this;
    }

    /**
     * Should TestNG consider failures in Data Providers as test failures.
     *
     * <p>Default is {@code false}</p>
     */
    public TestNgOperation propagateDataProviderFailureAsTestFailure(Boolean isPropagateDataProviderFailure) {
        options.put("-propagateDataProviderFailureAsTestFailure", String.valueOf(isPropagateDataProviderFailure));
        return this;
    }

    /**
     * Specifies the extended configuration for custom report listener.
     */
    public TestNgOperation reporter(String reporter) {
        options.put("-reporter", reporter);
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
     * Specifies the List of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     */
    public TestNgOperation spiListenersToSkip(String... listenerToSkip) {
        options.put("-spilistenerstoskip", String.join(",", listenerToSkip));
        return this;
    }

    /**
     * This specifies the default name of the test suite, if not specified in the suite definition file or source code.
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different suite name.
     */
    public TestNgOperation suiteName(String name) {
        options.put("-suitename", '"' + name + '"');
        return this;
    }

    /**
     * Specifies the size of the thread pool to use to run suites.
     * Required if no {@link #packages(String...)} specified.
     */
    public TestNgOperation suiteThreadPoolSize(int poolSize) {
        options.put("-suitethreadpoolsize", String.valueOf(poolSize));
        return this;
    }

    /**
     * Specifies the suites to run.
     *
     * <p>For example: {@code "testng.xml", "testng2.xml"}</p>
     */
    public TestNgOperation suites(String... suite) {
        suites.addAll(Arrays.stream(suite).toList());
        return this;
    }

    /**
     * Create a test file and delete it on exit.
     */
    private File tempFile() throws IOException {
        var temp = File.createTempFile("testng", ".xml");
        temp.deleteOnExit();
        return temp;
    }

    /**
     * Specifies the list of class files.
     *
     * <p>For example: {@code "org.foo.Test1","org.foo.test2"}</p>
     */
    public TestNgOperation testClass(String... aClass) {
        options.put("-testclass", String.join(",", aClass));
        return this;
    }

    /**
     * Specifies the classpath entries used to run tests.
     */
    public TestNgOperation testClasspath(String... entry) {
        testClasspath.addAll(Arrays.stream(entry).toList());
        return this;
    }

    /**
     * Specifies a jar file that contains test classes. If a {@code testng.xml} file is found at the root of that
     * jar file, it will be used, otherwise, all the test classes found in this jar file will be considered test
     * classes.
     */
    public TestNgOperation testJar(String jar) {
        options.put("-testjar", jar);
        return this;
    }

    /**
     * This specifies the default name of test, if not specified in the suite definition file or source code.
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different test name.
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
     * Specifies the factory used to create tests.
     */
    public TestNgOperation testRunFactory(String factory) {
        options.put("-testrunfactory", factory);
        return this;
    }

    /**
     * This sets the default maximum number of threads to use for running tests in parallel. It will only take effect
     * if the parallel mode has been selected (for example, with the {@link #parallel(Parallel) parallel} option).
     * This can be overridden in the suite definition.
     */
    public TestNgOperation threadCount(int count) {
        options.put("-threadcount", String.valueOf(count));
        return this;
    }

    /**
     * Specifies the thread pool executor factory implementation that TestNG should use.
     */
    public TestNgOperation threadPoolFactoryClass(String factoryClass) {
        options.put("-threadpoolfactoryclass", factoryClass);
        return this;
    }

    /**
     * Whether to use the default listeners
     *
     * <p>Default is {@code true}</p>
     */
    public TestNgOperation useDefaultListeners(Boolean isDefaultListener) {
        options.put("-usedefaultlisteners", String.valueOf(isDefaultListener));
        return this;
    }

    /**
     * Set the Level of verbosity.
     *
     * @see #log(int)
     */
    public TestNgOperation verbose(int level) {
        options.put("-verbose", String.valueOf(level));
        return this;
    }

    private File writeDefaultSuite() throws IOException {
        var temp = tempFile();
        try (var bufWriter = Files.newBufferedWriter(Paths.get(temp.getPath()))) {
            bufWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">" +
                    "<suite name=\"bld Default Suite\" verbose=\"2\">" +
                    "<test name=\"All Packages\">" +
                    "<packages>");
            for (var p : packages) {
                bufWriter.write(String.format("<package name=\"%s\"/>", p));
            }
            bufWriter.write("</packages></test></suite>");
        }
        return temp;
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}). The default is {@code testng.xml}, which means a file called
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