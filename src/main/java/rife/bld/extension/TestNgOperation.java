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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.BaseProject;
import rife.bld.extension.tools.ClasspathUtils;
import rife.bld.extension.tools.TextUtils;
import rife.bld.operations.TestOperation;
import rife.bld.operations.exceptions.ExitStatusException;

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
public class TestNgOperation extends TestOperation<TestNgOperation, List<String>> {

    private static final Logger LOGGER = Logger.getLogger(TestNgOperation.class.getName());
    /**
     * The methods to run.
     */
    private final Set<String> methods_ = new HashSet<>();
    /**
     * The run options.
     */
    private final Map<String, String> options_ = new ConcurrentHashMap<>();
    /**
     * The suite packages to run.
     */
    private final Set<String> packages_ = new HashSet<>();
    /**
     * The suites to run.
     */
    private final Set<String> suites_ = new HashSet<>();
    /**
     * The classpath entries used for running tests.
     */
    private final Set<String> testClasspath_ = new HashSet<>();
    private BaseProject project_;

    @Override
    public void execute() throws IOException, InterruptedException, ExitStatusException {
        if (project_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("A project must be specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (packages_.isEmpty() && suites_.isEmpty() && methods_.isEmpty()) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.severe("At least an XML suite, package or method is required.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else {
            super.execute();
        }
    }

    /**
     * Part of the {@link #execute execute} operation, constructs the command list to use for building the process.
     *
     * @return the command list
     */
    @Override
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CHECKED", "PATH_TRAVERSAL_IN"})
    protected List<String> executeConstructProcessCommandList() {
        final List<String> args = new ArrayList<>();

        if (project_ != null) {
            if (!options_.containsKey("-d")) {
                options_.put("-d", new File(project_.buildDirectory(), "test-output").getAbsolutePath());
            }

            args.add(javaTool());
            args.addAll(javaOptions());

            args.add("-cp");
            if (testClasspath_.isEmpty()) {
                args.add(
                        ClasspathUtils.buildClasspath(
                                ClasspathUtils.joinClasspathJar(
                                        project_.testClasspathJars(),
                                        project_.compileClasspathJars(),
                                        project_.providedClasspathJars()
                                ),
                                project_.buildMainDirectory().getAbsolutePath(),
                                project_.buildTestDirectory().getAbsolutePath())
                );
            } else {
                args.add(String.join(File.pathSeparator, testClasspath_));
            }

            args.add("org.testng.TestNG");

            options_.forEach((k, v) -> {
                args.add(k);
                args.add(v);
            });

            if (!suites_.isEmpty()) {
                args.addAll(suites_);
            } else if (!options_.containsKey("-testclass")) {
                try {
                    args.add(writeDefaultSuite().getAbsolutePath());
                } catch (IOException ioe) {
                    if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                        LOGGER.severe("An IO error occurred while accessing the default testng.xml file: "
                                + ioe.getMessage());
                    }
                    throw new RuntimeException(ioe);
                }
            }

            if (!methods_.isEmpty()) {
                args.add("-methods");
                args.add(String.join(",", methods_));
            }

            if (LOGGER.isLoggable(Level.FINE) && !silent()) {
                LOGGER.fine(String.join(" ", args));
            }

            if (LOGGER.isLoggable(Level.INFO) && !silent()) {
                LOGGER.info(String.format("Report will be saved in file://%s",
                        new File(options_.get("-d")).toURI().getPath()));
            }
        }

        return args;
    }

    /**
     * Configures the {@link BaseProject}.
     *
     * @param project the project
     * @return this operation instance
     */
    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TestNgOperation fromProject(BaseProject project) {
        project_ = project;
        directory(Path.of(project.buildDirectory().getPath(), "test-output").toString());
        return this;
    }

    /**
     * Should Method Invocation Listeners be run even for skipped methods.
     *
     * <p>Default is {@code true}</p>
     *
     * @param isAlwaysRunListeners {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation alwaysRunListeners(Boolean isAlwaysRunListeners) {
        options_.put("-alwaysrunlisteners", String.valueOf(isAlwaysRunListeners));
        return this;
    }

    /**
     * This sets the default maximum number of threads to use for data providers when running tests in parallel.
     * It will only take effect if the parallel mode has been selected (for example, with the
     * {@link #parallel(Parallel) parallel} option). This can be overridden in the suite definition.
     *
     * @param count the count
     * @return this operation instance
     */
    public TestNgOperation dataProviderThreadCount(int count) {
        if (count >= 0) {
            options_.put("-dataproviderthreadcount", String.valueOf(count));
        }
        return this;
    }

    /**
     * The dependency injector factory implementation that TestNG should use.
     *
     * @param injectorFactory the injector factory
     * @return this operation instance
     */
    public TestNgOperation dependencyInjectorFactory(String injectorFactory) {
        addOption("-dependencyinjectorfactory", injectorFactory);
        return this;
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
     *
     * @param directoryPath the directory path
     * @return this operation instance
     */
    public TestNgOperation directory(String directoryPath) {
        addOption("-d", directoryPath);
        return this;
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
     *
     * @param directoryPath the directory path
     * @return this operation instance
     */
    public TestNgOperation directory(File directoryPath) {
        return directory(directoryPath.getAbsolutePath());
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
     *
     * @param directoryPath the directory path
     * @return this operation instance
     */
    public TestNgOperation directory(Path directoryPath) {
        return directory(directoryPath.toFile());
    }

    /**
     * The list of groups you want to be excluded from this run.
     *
     * @param group one or more groups
     * @return this operation instance
     * @see #excludeGroups(Collection) #excludeGroups(Collection)
     */
    public TestNgOperation excludeGroups(String... group) {
        return excludeGroups(List.of(group));
    }

    /**
     * The list of groups you want to be excluded from this run.
     *
     * @param group the list of groups
     * @return this operation instance
     * @see #excludeGroups(String...) #excludeGroups(String...)
     */
    public TestNgOperation excludeGroups(Collection<String> group) {
        options_.put("-excludegroups", String.join(",", group.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * Should TestNG fail execution if all tests were skipped and nothing was run.
     *
     * @param isFailAllSkipped {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation failWhenEverythingSkipped(Boolean isFailAllSkipped) {
        options_.put("-failwheneverythingskipped", String.valueOf(isFailAllSkipped));
        return this;
    }

    /**
     * Whether TestNG should continue to execute the remaining tests in the suite or skip them if in a {@code @Before*}
     * method.
     *
     * @param policy the policy
     * @return this operation instance
     */
    public TestNgOperation failurePolicy(FailurePolicy policy) {
        options_.put("-configfailurepolicy", policy.name().toLowerCase());
        return this;
    }

    /**
     * Should TestNG generate results on a per-suite basis by creating a subdirectory for each suite and dumping
     * results into it.
     *
     * <p>Default is {@code false}</p>.
     *
     * @param resultsPerSuite {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation generateResultsPerSuite(Boolean resultsPerSuite) {
        options_.put("-generateResultsPerSuite", String.valueOf(resultsPerSuite));
        return this;
    }

    /**
     * The list of groups you want to run.
     *
     * <p>For example: {@code "windows", "linux", "regression}</p>
     *
     * @param group one or more groups
     * @return this operation instance
     * @see #groups(Collection) #groups(Collection)
     */
    public TestNgOperation groups(String... group) {
        return groups(List.of(group));
    }

    /**
     * The list of groups you want to run.
     *
     * <p>For example: {@code "windows", "linux", "regression}</p>
     *
     * @param group the list of groups
     * @return this operation instance
     * @see #groups(String...) #groups(String...)
     */
    public TestNgOperation groups(Collection<String> group) {
        options_.put("-groups", String.join(",", group.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * Ignore missed test names given by {@link #testNames(String...) testNames} and continue to run existing tests,
     * if any.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isIgnoreMissedTestNames {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation ignoreMissedTestName(Boolean isIgnoreMissedTestNames) {
        options_.put("-ignoreMissedTestNames", String.valueOf(isIgnoreMissedTestNames));
        return this;
    }

    /**
     * Should TestNG report all iterations of a data driven test as individual skips, in-case of upstream failures.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isIncludeDrivenTestsWhenSkipping {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation includeAllDataDrivenTestsWhenSkipping(Boolean isIncludeDrivenTestsWhenSkipping) {
        options_.put("-includeAllDataDrivenTestsWhenSkipping", String.valueOf(isIncludeDrivenTestsWhenSkipping));
        return this;
    }

    /**
     * Enables or disables the JUnit mode.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isJunit {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation jUnit(Boolean isJunit) {
        options_.put("-junit", String.valueOf(isJunit));
        return this;
    }

    /**
     * The list of {@code .class} files or list of class names implementing {@code ITestListener} or
     * {@code ISuiteListener}
     *
     * @param listener one or more listeners
     * @return this operation instance
     * @see #listener(Collection) #listener(Collection)
     */
    public TestNgOperation listener(String... listener) {
        return listener(List.of(listener));
    }

    /**
     * The list of {@code .class} files or list of class names implementing {@code ITestListener} or
     * {@code ISuiteListener}
     *
     * @param listener the list of listeners
     * @return this operation instance
     * @see #listener(String...) #listener(String...)
     */
    public TestNgOperation listener(Collection<String> listener) {
        options_.put("-listener", String.join(",", listener.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * An implementation of {@code ListenerComparator} that will be used by TestNG to determine order of execution for
     * listeners.
     *
     * @param listenerComparator the listener comparator
     * @return this operation instance
     */
    public TestNgOperation listenerComparator(String listenerComparator) {
        options_.put("-listenercomparator", listenerComparator);
        return this;
    }

    /**
     * The factory used to create TestNG listeners.
     *
     * @param listenerFactory the listener factory
     * @return this operation instance
     */
    public TestNgOperation listenerFactory(String listenerFactory) {
        options_.put("-listenerfactory", listenerFactory);
        return this;
    }

    /**
     * Set the Level of verbosity.
     *
     * @param level the level
     * @return this operation instance
     * @see #verbose(int) #verbose(int)
     */
    public TestNgOperation log(int level) {
        if (level >= 0) {
            options_.put("-log", String.valueOf(level));
        }
        return this;
    }

    /**
     * Specifies the list of {@code .class} files or class names implementing {@code IMethodSelector}.
     *
     * <p>For example: {@code "com.example.Selector1:3", "com.example.Selector2:2"}</p>
     *
     * @param selector one or more selectors
     * @return this operation instance
     * @see #methodSelectors(Collection) #methodSelectors(Collection)
     */
    public TestNgOperation methodSelectors(String... selector) {
        return methodSelectors(List.of(selector));
    }

    /**
     * Specifies the list of {@code .class} files or class names implementing {@code IMethodSelector}.
     *
     * <p>For example: {@code "com.example.Selector1:3", "com.example.Selector2:2"}</p>
     *
     * @param selector the list of selectors
     * @return this operation instance
     * @see #methodSelectors(String...) #methodSelectors(String...)
     */
    public TestNgOperation methodSelectors(Collection<String> selector) {
        options_.put("-methodselectors",
                String.join(",", selector.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * Lets you specify individual methods to run.
     *
     * <p>For example: {@code "com.example.Foo.f1", "com.example.Bar.f2"}</p>
     *
     * @param method one or more methods
     * @return this operation instance
     * @see #methods(Collection) #methods(Collection)
     */
    public TestNgOperation methods(String... method) {
        return methods(List.of(method));
    }

    /**
     * Lets you specify individual methods to run.
     *
     * <p>For example: {@code "com.example.Foo.f1", "com.example.Bar.f2"}</p>
     *
     * @param method the list of methods
     * @return this operation instance
     * @see #methods(String...) #methods(String...)
     */
    public TestNgOperation methods(Collection<String> method) {
        methods_.addAll(method);
        return this;
    }

    /**
     * Returns the methods to run.
     *
     * @return the set of methods
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> methods() {
        return methods_;
    }

    /**
     * Mixed mode autodetects the type of current test and run it with appropriate runner.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isMixed {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation mixed(Boolean isMixed) {
        options_.put("-mixed", String.valueOf(isMixed));
        return this;
    }

    /**
     * The list of {@code .class} files or class names implementing {@code ITestRunnerFactory}.
     * <p>
     * A fully qualified class name that implements {@code org.testng.ITestObjectFactory} which can be used to create
     * test class and listener instances.
     *
     * @param factory one or more factory
     * @return this operation instance
     * @see #objectFactory(Collection) #objectFactory(Collection)
     */
    public TestNgOperation objectFactory(String... factory) {
        return objectFactory(List.of(factory));
    }

    /**
     * The list of {@code .class} files or class names implementing {@code ITestRunnerFactory}.
     * <p>
     * A fully qualified class name that implements {@code org.testng.ITestObjectFactory} which can be used to create
     * test class and listener instances.
     *
     * @param factory the list of factories
     * @return this operation instance
     * @see #objectFactory(String...) #objectFactory(String...)
     */
    public TestNgOperation objectFactory(Collection<String> factory) {
        options_.put("-objectfactory",
                String.join(",", factory.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * Returns the run options.
     *
     * @return the map of run options
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, String> options() {
        return options_;
    }

    /**
     * The list of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param method one or more methods
     * @return this operation instance
     * @see #overrideIncludedMethods(Collection) #overrideIncludedMethods(Collection)
     */
    public TestNgOperation overrideIncludedMethods(String... method) {
        return overrideIncludedMethods(List.of(method));
    }

    /**
     * The list of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param method the list of methods
     * @return this operation instance
     * @see #overrideIncludedMethods(String...) #overrideIncludedMethods(String...)
     */
    public TestNgOperation overrideIncludedMethods(Collection<String> method) {
        options_.put("-overrideincludedmethods",
                String.join(",", method.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * The list of packages to include in this test.
     * If the package name ends with .* then subpackages are included too.
     * Required if no {@link #suites(String... suites)} specified.
     *
     * <p>For example: {@code "com.example", "test.sample.*"}</p>
     *
     * @param name one or more names
     * @return this operation instance
     * @see #packages(Collection) #packages(Collection)
     */
    public TestNgOperation packages(String... name) {
        return packages(List.of(name));
    }

    /**
     * The list of packages to include in this test.
     * If the package name ends with .* then subpackages are included too.
     * Required if no {@link #suites(String... suites)} specified.
     *
     * <p>For example: {@code "com.example", "test.sample.*"}</p>
     *
     * @param name the list of names
     * @return this operation instance
     * @see #packages(String...) #packages(String...)
     */
    public TestNgOperation packages(Collection<String> name) {
        packages_.addAll(name.stream().filter(TextUtils::isNotBlank).toList());
        return this;
    }

    /**
     * Returns the suite packages to run.
     *
     * @return the set of packages
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> packages() {
        return packages_;
    }

    /**
     * If specified, sets the default mechanism used to determine how to use parallel threads when running tests.
     * If not set, default mechanism is not to use parallel threads at all.
     * This can be overridden in the suite definition.
     *
     * @param mechanism the mechanism
     * @return this operation instance
     * @see Parallel
     */
    public TestNgOperation parallel(Parallel mechanism) {
        options_.put("-parallel", mechanism.name().toLowerCase());
        return this;
    }

    /**
     * Specifies the port number.
     *
     * @param port the port
     * @return this operation instance
     */
    public TestNgOperation port(int port) {
        if (port >= 1) {
            options_.put("-port", String.valueOf(port));
        }
        return this;
    }

    /**
     * Should TestNG consider failures in Data Providers as test failures.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isPropagateDataProviderFailure {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation propagateDataProviderFailureAsTestFailure(Boolean isPropagateDataProviderFailure) {
        options_.put("-propagateDataProviderFailureAsTestFailure", String.valueOf(isPropagateDataProviderFailure));
        return this;
    }

    /**
     * Specifies the extended configuration for custom report listener.
     *
     * @param reporter the reporter
     * @return this operation instance
     */
    public TestNgOperation reporter(String reporter) {
        addOption("-reporter", reporter);
        return this;
    }

    /**
     * Should TestNG use a global Shared ThreadPool (At suite level) for running data providers.
     *
     * @param shareThreadPoolForDataProviders {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation shareThreadPoolForDataProviders(boolean shareThreadPoolForDataProviders) {
        if (shareThreadPoolForDataProviders) {
            options_.put("-shareThreadPoolForDataProviders", "true");
        }
        return this;
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory one or more directories
     * @return this operation instance
     * @see #sourceDir(Collection)
     */
    public TestNgOperation sourceDir(String... directory) {
        return sourceDir(List.of(directory));
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory one or more directories
     * @return this operation instance
     * @see #sourceDirFiles(Collection)
     */
    public TestNgOperation sourceDir(File... directory) {
        return sourceDirFiles(List.of(directory));
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory one or more directories
     * @return this operation instance
     * @see #sourceDirPaths(Collection)
     */
    public TestNgOperation sourceDir(Path... directory) {
        return sourceDirPaths(List.of(directory));
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory the list of directories
     * @return this operation instance
     * @see #sourceDir(String...)
     */
    public TestNgOperation sourceDir(Collection<String> directory) {
        options_.put("-sourcedir", String.join(";", directory.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory the list of directories
     * @return this operation instance
     * @see #sourceDir(File...)
     */
    public TestNgOperation sourceDirFiles(Collection<File> directory) {
        return sourceDir(directory.stream().map(File::getAbsolutePath).toList());
    }

    /**
     * The directories where your javadoc annotated test sources are. This option is only necessary
     * if you are using javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directory the list of directories
     * @return this operation instance
     * @see #sourceDir(Path...)
     */
    public TestNgOperation sourceDirPaths(Collection<Path> directory) {
        return sourceDirFiles(directory.stream().map(Path::toFile).toList());
    }

    /**
     * Specifies the List of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param listenerToSkip the listeners to skip
     * @return this operation instance
     * @see #spiListenersToSkip(Collection) #spiListenersToSkip(Collection)
     */
    public TestNgOperation spiListenersToSkip(String... listenerToSkip) {
        return spiListenersToSkip(List.of(listenerToSkip));
    }

    /**
     * Specifies the List of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param listenerToSkip the listeners to skip
     * @return this operation instance
     * @see #spiListenersToSkip(String...) #spiListenersToSkip(String...)
     */
    public TestNgOperation spiListenersToSkip(Collection<String> listenerToSkip) {
        options_.put("-spilistenerstoskip",
                String.join(",", listenerToSkip.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * This specifies the default name of the test suite, if not specified in the suite definition file or source code.
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different suite name.
     *
     * @param name the name
     * @return this operation instance
     */
    public TestNgOperation suiteName(String name) {
        addOption("-suitename", '"' + name + '"');
        return this;
    }

    /**
     * Specifies the size of the thread pool to use to run suites.
     * Required if no {@link #packages(String...)} specified.
     *
     * @param poolSize the pool size
     * @return this operation instance
     */
    public TestNgOperation suiteThreadPoolSize(int poolSize) {
        if (poolSize >= 0) {
            options_.put("-suitethreadpoolsize", String.valueOf(poolSize));
        }
        return this;
    }

    /**
     * Specifies the suites to run.
     *
     * <p>For example: {@code "testng.xml", "testng2.xml"}</p>
     *
     * @param suite one or more suites
     * @return this operation instance
     * @see #suites(Collection) #suites(Collection)
     */
    public TestNgOperation suites(String... suite) {
        return suites(List.of(suite));
    }

    /**
     * Specifies the suites to run.
     *
     * <p>For example: {@code "testng.xml", "testng2.xml"}</p>
     *
     * @param suite the list of suites
     * @return this operation instance
     * @see #suites(String...) #suites(String...)
     */
    public TestNgOperation suites(Collection<String> suite) {
        suites_.addAll(suite.stream().filter(TextUtils::isNotBlank).toList());
        return this;
    }

    /**
     * Returns the suites to run.
     *
     * @return the set of suites
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> suites() {
        return suites_;
    }

    /**
     * Specifies the list of class files.
     *
     * <p>For example: {@code "org.foo.Test1","org.foo.test2"}</p>
     *
     * @param aClass one or more classes
     * @return this operation instance
     * @see #testClass(Collection) #testClass(Collection)
     */
    public TestNgOperation testClass(String... aClass) {
        return testClass(List.of(aClass));
    }

    /**
     * Specifies the list of class files.
     *
     * <p>For example: {@code "org.foo.Test1","org.foo.test2"}</p>
     *
     * @param aClass the list of classes
     * @return this operation instance
     * @see #testClass(String...) #testClass(String...)
     */
    public TestNgOperation testClass(Collection<String> aClass) {
        options_.put("-testclass", String.join(",", aClass.stream().filter(TextUtils::isNotBlank).toList()));
        return this;
    }

    /**
     * Specifies the classpath entries used to run tests.
     *
     * @param entry one or more entries
     * @return this operation instance
     * @see #testClasspath(String...) #testClasspath(String...)
     */
    public TestNgOperation testClasspath(String... entry) {
        return testClasspath(List.of(entry));
    }

    /**
     * Specifies the classpath entries used to run tests.
     *
     * @param entry the list of entries
     * @return this operation instance
     * @see #testClasspath(String...) #testClasspath(String...)
     */
    public TestNgOperation testClasspath(Collection<String> entry) {
        testClasspath_.addAll(entry.stream().filter(TextUtils::isNotBlank).toList());
        return this;
    }

    /**
     * Returns the classpath entries used for running tests.
     *
     * @return the set of test classpath
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> testClasspath() {
        return testClasspath_;
    }

    /**
     * Specifies a jar file that contains test classes. If a {@code testng.xml} file is found at the root of that
     * jar file, it will be used, otherwise, all the test classes found in this jar file will be considered test
     * classes.
     *
     * @param jar the jar
     * @return this operation instance
     */
    public TestNgOperation testJar(String jar) {
        addOption("-testjar", jar);
        return this;
    }

    /**
     * This specifies the default name of test, if not specified in the suite definition file or source code.
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different test name.
     *
     * @param name the name
     * @return this operation instance
     */
    public TestNgOperation testName(String name) {
        addOption("-testname", '"' + name + '"');
        return this;
    }

    /**
     * Only tests defined in a {@code <test>} tag matching one of these names will be run.
     *
     * @param name one or more names
     * @return this operation instance
     * @see #testNames(Collection) #testNames(Collection)
     */
    public TestNgOperation testNames(String... name) {
        return testNames(List.of(name));
    }

    /**
     * Only tests defined in a {@code <test>} tag matching one of these names will be run.
     *
     * @param name the list of names
     * @return this operation instance
     * @see #testName(String) #testName(String)
     */
    public TestNgOperation testNames(Collection<String> name) {
        options_.put("-testnames",
                name.stream()
                        .filter(TextUtils::isNotBlank)
                        .map(s -> '"' + s + '"')
                        .collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Specifies the factory used to create tests.
     *
     * @param factory the factory
     * @return this operation instance
     */
    public TestNgOperation testRunFactory(String factory) {
        addOption("-testrunfactory", factory);
        return this;
    }

    /**
     * This sets the default maximum number of threads to use for running tests in parallel. It will only take effect
     * if the parallel mode has been selected (for example, with the {@link #parallel(Parallel) parallel} option).
     * This can be overridden in the suite definition.
     *
     * @param count the count
     * @return this operation instance
     */
    public TestNgOperation threadCount(int count) {
        if (count >= 0) {
            options_.put("-threadcount", String.valueOf(count));
        }
        return this;
    }

    /**
     * Specifies the thread pool executor factory implementation that TestNG should use.
     *
     * @param factoryClass the factory class
     * @return this operation instance
     */
    public TestNgOperation threadPoolFactoryClass(String factoryClass) {
        addOption("-threadpoolfactoryclass", factoryClass);
        return this;
    }

    /**
     * Whether to use the default listeners
     *
     * <p>Default is {@code true}</p>
     *
     * @param isDefaultListener {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation useDefaultListeners(Boolean isDefaultListener) {
        options_.put("-usedefaultlisteners", String.valueOf(isDefaultListener));
        return this;
    }

    /**
     * Should TestNG use a global Shared ThreadPool (At suite level) for running regular and data driven tests.
     *
     * @param useGlobalThreadPool {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation useGlobalThreadPool(boolean useGlobalThreadPool) {
        if (useGlobalThreadPool) {
            options_.put("-useGlobalThreadPool", "true");
        }
        return this;
    }

    /**
     * Set the Level of verbosity.
     *
     * @param level the level
     * @return this operation instance
     * @see #log(int) #log(int)
     */
    public TestNgOperation verbose(int level) {
        if (level >= 0) {
            options_.put("-verbose", String.valueOf(level));
        }
        return this;
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}). The default is {@code testng.xml}, which means a file called
     * {@code testng.xml} at the root of the jar file. This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(String path) {
        addOption("-xmlpathinjar", path);
        return this;
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}). The default is {@code testng.xml}, which means a file called
     * {@code testng.xml} at the root of the jar file. This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(File path) {
        return xmlPathInJar(path.getAbsolutePath());
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}). The default is {@code testng.xml}, which means a file called
     * {@code testng.xml} at the root of the jar file. This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(Path path) {
        return xmlPathInJar(path.toFile());
    }

    /**
     * Create a test file and delete it on exit.
     *
     * @return this operation instance
     */
    private File tempFile() throws IOException {
        var temp = File.createTempFile("testng", ".xml");
        temp.deleteOnExit();
        return temp;
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private File writeDefaultSuite() throws IOException {
        var temp = tempFile();
        try (var bufWriter = Files.newBufferedWriter(Paths.get(temp.getPath()))) {
            bufWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">" +
                    "<suite name=\"bld Default Suite\" verbose=\"2\">" +
                    "<test name=\"All Packages\">" +
                    "<packages>");
            for (var p : packages_) {
                bufWriter.write(String.format("<package name=\"%s\"/>", p));
            }
            bufWriter.write("</packages></test></suite>");
        }
        return temp;
    }

    /**
     * Parallel Mechanisms
     */
    public enum Parallel {
        /**
         * Methods mechanism.
         */
        METHODS,
        /**
         * Tests mechanism.
         */
        TESTS,
        /**
         * Classes mechanism.
         */
        CLASSES
    }

    /**
     * Failure Policies
     */
    public enum FailurePolicy {
        /**
         * Skip failure policy.
         */
        SKIP,
        /**
         * Continue failure policy.
         */
        CONTINUE
    }

    private void addOption(String key, String value) {
        if (TextUtils.isNotBlank(value)) {
            options_.put(key, value);
        }
    }
}
