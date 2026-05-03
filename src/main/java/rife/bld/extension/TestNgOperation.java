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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import rife.bld.BaseProject;
import rife.bld.extension.tools.ClasspathTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.operations.TestOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import javax.xml.stream.XMLOutputFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Run tests with <a href="https://testng.org/">TestNG</a>.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Builder pattern intentionally exposes mutable collections"
)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestNgOperation extends TestOperation<TestNgOperation, List<String>> {

    private static final String LOG_ARG = "-log";
    private static final XMLOutputFactory XML_FACTORY = XMLOutputFactory.newInstance();
    private static final Logger logger = Logger.getLogger(TestNgOperation.class.getName());
    private final Set<String> excludeGroups_ = new HashSet<>();
    private final Set<String> groups_ = new HashSet<>();
    private final Set<String> methods_ = new HashSet<>();
    private final Map<String, String> options_ = new LinkedHashMap<>();
    private final Set<String> packages_ = new HashSet<>();
    private final Set<String> suites_ = new HashSet<>();
    private final Set<String> testClasses_ = new HashSet<>();
    private final Set<String> testClasspath_ = new HashSet<>();
    private final Set<String> testNames_ = new HashSet<>();
    private BaseProject project_;

    @Override
    public void execute() throws IOException, InterruptedException, ExitStatusException {
        if (project_ == null) {
            if (!silent() && logger.isLoggable(Level.SEVERE)) {
                logger.severe("A project must be specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (packages_.isEmpty() && suites_.isEmpty() && methods_.isEmpty()) {
            if (!silent() && logger.isLoggable(Level.SEVERE)) {
                logger.severe("At least an XML suite, package or method is required.");
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
            if (ObjectTools.isEmpty(testClasspath_)) {
                args.add(
                        ClasspathTools.joinClasspath(
                                ClasspathTools.joinClasspath(
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

            while (!project_.arguments().isEmpty()) {
                var arg = project_.arguments().get(0);

                if (arg.startsWith(LOG_ARG + '=')) {
                    var level = arg.substring(LOG_ARG.length() + 1);
                    try {
                        verbose(Integer.parseInt(level));
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException("Invalid log level: " + level, nfe);
                    }
                } else {
                    var target = targetCollectionForArg(arg);
                    if (target.isEmpty()) {
                        break; // unknown flag, stop parsing
                    }

                    var prefixEnd = arg.indexOf('=') + 1;
                    var value = arg.substring(prefixEnd);
                    target.get().addAll(Arrays.asList(value.split(",")));
                }
                project_.arguments().remove(0);
            }

            options_.forEach((k, v) -> {
                args.add(k);
                if (!v.isEmpty()) {
                    args.add(v);
                }
            });

            boolean hasClasses = ObjectTools.isNotEmpty(testClasses_);
            boolean hasMethods = ObjectTools.isNotEmpty(methods_);
            if (ObjectTools.isNotEmpty(suites_)) {
                args.addAll(suites_);
            } else if (!hasClasses && !hasMethods) {
                try {
                    args.add(writeDefaultSuite().toString());
                } catch (IOException ioe) {
                    if (!silent() && logger.isLoggable(Level.SEVERE)) {
                        logger.severe("An IO error occurred while accessing the default testng.xml file: "
                                + ioe.getMessage());
                    }
                    throw new RuntimeException(ioe);
                }
            }

            if (hasClasses) {
                args.add("-testclass");
                args.add(String.join(",", testClasses_));
            }

            if (hasMethods) {
                args.add("-methods");
                args.add(String.join(",", methods_));
            }

            if (ObjectTools.isNotEmpty(testNames_)) {
                args.add("-testnames");
                args.add(String.join(",", testNames_));
            }

            if (ObjectTools.isNotEmpty(groups_)) {
                args.add("-groups");
                args.add(String.join(",", groups_));
            }

            if (ObjectTools.isNotEmpty(excludeGroups_)) {
                args.add("-excludegroups");
                args.add(String.join(",", excludeGroups_));
            }

            if (!silent() && logger.isLoggable(Level.FINE)) {
                logger.fine(String.join(" ", args));
            }

            if (!silent() && logger.isLoggable(Level.INFO)) {
                logger.info(String.format("Report will be saved in file://%s",
                        new File(options_.get("-d")).toURI().getPath()));
            }
        }

        return args;
    }

    /**
     * Configures the {@link BaseProject}.
     * <p>
     * Sets the {@link #directory(File) report directory} to the project
     * {@link rife.bld.BaseProject#buildDirectory() build directory} if not already set.
     *
     * @param project the project
     * @return this operation instance
     */
    @Override
    public TestNgOperation fromProject(@NonNull BaseProject project) {
        project_ = Objects.requireNonNull(project, "The project must not be null");
        if (!options_.containsKey("-d")) {
            directory(new File(project.buildDirectory(), "test-output"));
        }
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
    public TestNgOperation alwaysRunListeners(boolean isAlwaysRunListeners) {
        return optBool("-alwaysrunlisteners", isAlwaysRunListeners);
    }

    /**
     * This sets the default maximum number of threads to use for data providers when running tests in parallel.
     * <p>
     * It will only take effect if the parallel mode has been selected (for example, with the
     * {@link #parallel(Parallel) parallel} option). This can be overridden in the suite definition.
     *
     * @param count the count
     * @return this operation instance
     */
    public TestNgOperation dataProviderThreadCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("'dataProviderThreadCount' count must be >= 0");
        }
        options_.put("-dataproviderthreadcount", String.valueOf(count));
        return this;
    }

    /**
     * The dependency injector factory implementation that TestNG should use.
     *
     * @param injectorFactory the injector factory
     * @return this operation instance
     */
    public TestNgOperation dependencyInjectorFactory(@NonNull String injectorFactory) {
        return opt("-dependencyinjectorfactory", injectorFactory);
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
     *
     * @param directoryPath the directory path
     * @return this operation instance
     */
    public TestNgOperation directory(@NonNull String directoryPath) {
        return opt("-d", directoryPath);
    }

    /**
     * The directory where the reports will be generated
     *
     * <p>Default is {@code build/test-output})</p>
     *
     * @param directoryPath the directory path
     * @return this operation instance
     */
    public TestNgOperation directory(@NonNull File directoryPath) {
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
    public TestNgOperation directory(@NonNull Path directoryPath) {
        return directory(directoryPath.toFile());
    }

    /**
     * Returns the list of groups to exclude from this run.
     *
     * @return the set of groups
     */
    public Set<String> excludeGroups() {
        return excludeGroups_;
    }

    /**
     * The list of groups you want to be excluded from this run.
     *
     * @param groups one or more groups
     * @return this operation instance
     * @see #excludeGroups(Collection) #excludeGroups(Collection)
     */
    public TestNgOperation excludeGroups(@NonNull String... groups) {
        Objects.requireNonNull(groups, "'excludeGroups' must not be null");
        return excludeGroups(List.of(groups));
    }

    /**
     * The list of groups you want to be excluded from this run.
     *
     * @param groups the list of groups
     * @return this operation instance
     * @see #excludeGroups(String...) #excludeGroups(String...)
     */
    public TestNgOperation excludeGroups(@NonNull Collection<String> groups) {
        ObjectTools.requireAllNotEmpty(groups, "'excludeGroups' and its elements must not be null or empty");
        excludeGroups_.clear();
        excludeGroups_.addAll(groups);
        return this;
    }

    /**
     * Should TestNG fail execution if all tests were skipped and nothing was run.
     *
     * @param isFailAllSkipped {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation failWhenEverythingSkipped(boolean isFailAllSkipped) {
        return optBool("-failwheneverythingskipped", isFailAllSkipped);
    }

    /**
     * Whether TestNG should continue to execute the remaining tests in the suite or skip them if in a {@code @Before*}
     * method.
     *
     * @param policy the policy
     * @return this operation instance
     */
    public TestNgOperation failurePolicy(@NonNull FailurePolicy policy) {
        Objects.requireNonNull(policy, "'failurePolicy' must not be null");
        options_.put("-configfailurepolicy", policy.asArgument());
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
    public TestNgOperation generateResultsPerSuite(boolean resultsPerSuite) {
        return optBool("-generateResultsPerSuite", resultsPerSuite);
    }

    /**
     * Returns the list of groups to run.
     *
     * @return the set of groups
     */
    public Set<String> groups() {
        return groups_;
    }

    /**
     * The list of groups you want to run.
     *
     * <p>For example: {@code "windows", "linux", "regression}</p>
     *
     * @param groups one or more groups
     * @return this operation instance
     * @see #groups(Collection) #groups(Collection)
     */
    public TestNgOperation groups(@NonNull String... groups) {
        ObjectTools.requireAllNotEmpty(groups, "'groups' and its elements must not be null or empty");
        return groups(List.of(groups));
    }

    /**
     * The list of groups you want to run.
     *
     * <p>For example: {@code "windows", "linux", "regression}</p>
     *
     * @param groups the list of groups
     * @return this operation instance
     * @see #groups(String...)
     */
    public TestNgOperation groups(@NonNull Collection<String> groups) {
        ObjectTools.requireAllNotEmpty(groups, "'groups' and its elements must not be null or empty");
        groups_.clear();
        groups_.addAll(groups);
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
    public TestNgOperation ignoreMissedTestName(boolean isIgnoreMissedTestNames) {
        return optBool("-ignoreMissedTestNames", isIgnoreMissedTestNames);
    }

    /**
     * Should TestNG report all iterations of a data driven test as individual skips, in-case of upstream failures.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isIncludeDrivenTestsWhenSkipping {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation includeAllDataDrivenTestsWhenSkipping(boolean isIncludeDrivenTestsWhenSkipping) {
        return optBool("-includeAllDataDrivenTestsWhenSkipping", isIncludeDrivenTestsWhenSkipping);
    }

    /**
     * Enables or disables the JUnit mode.
     *
     * <p>Default is {@code false}</p>
     *
     * @param isJunit {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation jUnit(boolean isJunit) {
        return optBool("-junit", isJunit);
    }

    /**
     * The list of {@code .class} files or list of class names implementing {@code ITestListener} or
     * {@code ISuiteListener}
     *
     * @param listeners one or more listeners
     * @return this operation instance
     * @see #listener(Collection)
     */
    public TestNgOperation listener(@NonNull String... listeners) {
        Objects.requireNonNull(listeners, "'listener' must not be null");
        return listener(List.of(listeners));
    }

    /**
     * The list of {@code .class} files or list of class names implementing {@code ITestListener} or
     * {@code ISuiteListener}
     *
     * @param listeners the list of listeners
     * @return this operation instance
     * @see #listener(String...)
     */
    public TestNgOperation listener(@NonNull Collection<String> listeners) {
        return optJoin("-listener", listeners);
    }

    /**
     * An implementation of {@code ListenerComparator} that will be used by TestNG to determine order of execution for
     * listeners.
     *
     * @param listenerComparator the listener comparator
     * @return this operation instance
     */
    public TestNgOperation listenerComparator(@NonNull String listenerComparator) {
        return opt("-listenercomparator", listenerComparator);
    }

    /**
     * The factory used to create TestNG listeners.
     *
     * @param listenerFactory the listener factory
     * @return this operation instance
     */
    public TestNgOperation listenerFactory(@NonNull String listenerFactory) {
        return opt("-listenerfactory", listenerFactory);
    }

    /**
     * Set the Level of verbosity.
     *
     * @param level the level
     * @return this operation instance
     * @see #verbose(int) #verbose(int)
     */
    public TestNgOperation log(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Log level must be >= 0");
        }
        options_.put(LOG_ARG, String.valueOf(level));
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
    public TestNgOperation methodSelectors(@NonNull String... selector) {
        Objects.requireNonNull(selector, "'methodSelectors' must not be null");
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
    public TestNgOperation methodSelectors(@NonNull Collection<String> selector) {
        return optJoin("-methodselectors", selector);
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
    public TestNgOperation methods(@NonNull String... method) {
        Objects.requireNonNull(method, "'methods' must not be null");
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
    public TestNgOperation methods(@NonNull Collection<String> method) {
        ObjectTools.requireAllNotEmpty(method, "'methods' must not be empty or null");
        methods_.clear();
        methods_.addAll(method);
        return this;
    }

    /**
     * Returns the methods to run.
     *
     * @return the set of methods
     */
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
    public TestNgOperation mixed(boolean isMixed) {
        return optBool("-mixed", isMixed);
    }

    /**
     * The list of {@code .class} files or class names implementing {@code ITestRunnerFactory}.
     * <p>
     * A fully qualified class name that implements {@code org.testng.ITestObjectFactory} which can be used to create
     * test class and listener instances.
     *
     * @param factories one or more factory
     * @return this operation instance
     * @see #objectFactory(Collection)
     */
    public TestNgOperation objectFactory(@NonNull String... factories) {
        Objects.requireNonNull(factories, "'objectFactory' must not be null");
        return objectFactory(List.of(factories));
    }

    /**
     * The list of {@code .class} files or class names implementing {@code ITestRunnerFactory}.
     * <p>
     * A fully qualified class name that implements {@code org.testng.ITestObjectFactory} which can be used to create
     * test class and listener instances.
     *
     * @param factories the list of factories
     * @return this operation instance
     * @see #objectFactory(String...)
     */
    public TestNgOperation objectFactory(@NonNull Collection<String> factories) {
        return optJoin("-objectfactory", factories);
    }

    /**
     * Returns the run options.
     *
     * @return the map of run options
     */
    public Map<String, String> options() {
        return options_;
    }

    /**
     * The list of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param methods one or more methods
     * @return this operation instance
     * @see #overrideIncludedMethods(Collection)
     */
    public TestNgOperation overrideIncludedMethods(@NonNull String... methods) {
        Objects.requireNonNull(methods, "'overrideIncludedMethods' must not be null");
        return overrideIncludedMethods(List.of(methods));
    }

    /**
     * The list of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param methods the list of methods
     * @return this operation instance
     * @see #overrideIncludedMethods(String...)
     */
    public TestNgOperation overrideIncludedMethods(@NonNull Collection<String> methods) {
        return optJoin("-overrideincludedmethods", methods);
    }

    /**
     * The list of packages to include in this test.
     * If the package name ends with .* then subpackages are included too.
     * Required if no {@link #suites(String... suites)} specified.
     *
     * <p>For example: {@code "com.example", "test.sample.*"}</p>
     *
     * @param names one or more names
     * @return this operation instance
     * @see #packages(Collection)
     */
    public TestNgOperation packages(@NonNull String... names) {
        Objects.requireNonNull(names, "'packages' must not be null");
        return packages(List.of(names));
    }

    /**
     * The list of packages to include in this test.
     * If the package name ends with .* then subpackages are included too.
     * Required if no {@link #suites(String... suites)} specified.
     *
     * <p>For example: {@code "com.example", "test.sample.*"}</p>
     *
     * @param names the list of names
     * @return this operation instance
     * @see #packages(String...)
     */
    public TestNgOperation packages(@NonNull Collection<String> names) {
        ObjectTools.requireAllNotEmpty(names, "'packages' and its elements must not be null or empty");
        packages_.clear();
        packages_.addAll(names);
        return this;
    }

    /**
     * Returns the suite packages to run.
     *
     * @return the set of packages
     */
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
    public TestNgOperation parallel(@NonNull Parallel mechanism) {
        Objects.requireNonNull(mechanism, "'parallel' must not be null");
        options_.put("-parallel", mechanism.asArgument());
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
    public TestNgOperation propagateDataProviderFailureAsTestFailure(boolean isPropagateDataProviderFailure) {
        return optBool("-propagateDataProviderFailureAsTestFailure", isPropagateDataProviderFailure);
    }

    /**
     * Specifies the extended configuration for custom report listener.
     *
     * @param reporter the reporter
     * @return this operation instance
     */
    public TestNgOperation reporter(String reporter) {
        return opt("-reporter", reporter);
    }

    /**
     * Should TestNG use a global Shared ThreadPool (At suite level) for running data providers.
     *
     * @param shareThreadPoolForDataProviders {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation shareThreadPoolForDataProviders(boolean shareThreadPoolForDataProviders) {
        return optBool("-shareThreadPoolForDataProviders", shareThreadPoolForDataProviders);
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories one or more directories
     * @return this operation instance
     * @see #sourceDir(Collection)
     */
    public TestNgOperation sourceDir(@NonNull String... directories) {
        Objects.requireNonNull(directories, "'sourceDir' must not be null");
        return sourceDir(List.of(directories));
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories one or more directories
     * @return this operation instance
     * @see #sourceDirFiles(Collection)
     */
    public TestNgOperation sourceDir(@NonNull File... directories) {
        Objects.requireNonNull(directories, "'sourceDir' must not be null");
        return sourceDirFiles(List.of(directories));
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories one or more directories
     * @return this operation instance
     * @see #sourceDirPaths(Collection)
     */
    public TestNgOperation sourceDir(@NonNull Path... directories) {
        Objects.requireNonNull(directories, "'sourceDir' must not be null");
        return sourceDirPaths(List.of(directories));
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories the list of directories
     * @return this operation instance
     * @see #sourceDir(String...)
     */
    public TestNgOperation sourceDir(@NonNull Collection<String> directories) {
        ObjectTools.requireAllNotEmpty(directories, "'sourceDir' and its elements must not be null or empty");
        options_.put("-sourcedir", String.join(";", directories));
        return this;
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories the list of directories
     * @return this operation instance
     * @see #sourceDir(File...)
     */
    public TestNgOperation sourceDirFiles(@NonNull Collection<File> directories) {
        ObjectTools.requireAllNotEmpty(directories,
                "'sourceDirFiles' and its elements must not be null or empty");
        options_.put("-sourcedir",
                directories.stream().map(File::getAbsolutePath).collect(Collectors.joining(";")));
        return this;
    }

    /**
     * The directories where your Javadoc annotated test sources are. This option is only necessary
     * if you are using Javadoc type annotations. (e.g. {@code "src/test"} or
     * {@code "src/test/org/testng/eclipse-plugin", "src/test/org/testng/testng"}).
     *
     * @param directories the list of directories
     * @return this operation instance
     * @see #sourceDir(Path...)
     */
    public TestNgOperation sourceDirPaths(@NonNull Collection<Path> directories) {
        ObjectTools.requireAllNotEmpty(directories,
                "'sourceDirPaths' and its elements must not be null or empty");
        options_.put("-sourcedir",
                directories.stream()
                        .map(Path::toAbsolutePath)
                        .map(Object::toString)
                        .collect(Collectors.joining(";")));
        return this;
    }

    /**
     * Specifies the List of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param listeners the listeners to skip
     * @return this operation instance
     * @see #spiListenersToSkip(Collection) #spiListenersToSkip(Collection)
     */
    public TestNgOperation spiListenersToSkip(@NonNull String... listeners) {
        Objects.requireNonNull(listeners, "'spiListenersToSkip' must not be null");
        return spiListenersToSkip(List.of(listeners));
    }

    /**
     * Specifies the List of fully qualified class names of listeners that should be skipped from being wired in via
     * Service Loaders.
     *
     * @param listeners the listeners to skip
     * @return this operation instance
     * @see #spiListenersToSkip(String...) #spiListenersToSkip(String...)
     */
    public TestNgOperation spiListenersToSkip(@NonNull Collection<String> listeners) {
        Objects.requireNonNull(listeners, "'spiListenersToSkip' must not be null");
        return optJoin("-spilistenerstoskip", listeners);
    }

    /**
     * This specifies the default name of the test suite, if not specified in the suite definition file or source code.
     * <p>
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different suite name.
     *
     * @param name the name
     * @return this operation instance
     */
    public TestNgOperation suiteName(String name) {
        ObjectTools.requireNotEmpty(name, "'suiteName' must not be null or empty");
        return opt("-suitename", '"' + name + '"');
    }

    /**
     * Specifies the size of the thread pool to use to run suites.
     * <p>
     * Required if no {@link #packages(String...)} specified.
     *
     * @param poolSize the pool size
     * @return this operation instance
     */
    public TestNgOperation suiteThreadPoolSize(int poolSize) {
        if (poolSize < 0) {
            throw new IllegalArgumentException("'suiteThreadPoolSize' must be >= 0");
        }
        options_.put("-suitethreadpoolsize", String.valueOf(poolSize));
        return this;
    }

    /**
     * Specifies the suites to run.
     *
     * <p>For example: {@code "testng.xml", "testng2.xml"}</p>
     *
     * @param suites one or more suites
     * @return this operation instance
     * @see #suites(Collection)
     */
    public TestNgOperation suites(@NonNull String... suites) {
        ObjectTools.requireAllNotEmpty(suites, "'suites' and its elements must not be null or empty");
        return suites(List.of(suites));
    }

    /**
     * Specifies the suites to run.
     *
     * <p>For example: {@code "testng.xml", "testng2.xml"}</p>
     *
     * @param suites the list of suites
     * @return this operation instance
     * @see #suites(String...)
     */
    public TestNgOperation suites(@NonNull Collection<String> suites) {
        ObjectTools.requireAllNotEmpty(suites, "'suites' and its elements must not be null or empty");
        suites_.clear();
        suites_.addAll(suites);
        return this;
    }

    /**
     * Returns the suites to run.
     *
     * @return the set of suites
     */
    public Set<String> suites() {
        return suites_;
    }

    /**
     * Specifies the list of class files.
     *
     * <p>For example: {@code "org.foo.Test1","org.foo.test2"}</p>
     *
     * @param classes one or more classes
     * @return this operation instance
     * @see #testClass(Collection)
     */
    public TestNgOperation testClass(@NonNull String... classes) {
        Objects.requireNonNull(classes, "'testClass' must not be null");
        return testClass(List.of(classes));
    }

    /**
     * Specifies the list of class files.
     *
     * <p>For example: {@code "org.foo.Test1","org.foo.test2"}</p>
     *
     * @param classes the list of classes
     * @return this operation instance
     * @see #testClass(String...)
     */
    public TestNgOperation testClass(@NonNull Collection<String> classes) {
        ObjectTools.requireAllNotEmpty(classes, "`testClass` and its elements must not be null or empty");
        testClasses_.clear();
        testClasses_.addAll(classes);
        return this;
    }

    /**
     * Returns the test classes to run.
     *
     * @return the set of test classes
     */
    public Set<String> testClasses() {
        return testClasses_;
    }

    /**
     * Specifies the classpath entries used to run tests.
     *
     * <p>Replaces default classpath. Include the build {@link rife.bld.BaseProject#buildMainDirectory() main} and
     * {@link rife.bld.BaseProject#buildTestDirectory() test} directories compiled classes are needed.</p>
     *
     * @param entries one or more entries
     * @return this operation instance
     * @see #testClasspath(String...)
     */
    public TestNgOperation testClasspath(@NonNull String... entries) {
        Objects.requireNonNull(entries, "`testClasspath` must not be null");
        return testClasspath(List.of(entries));
    }

    /**
     * Specifies the classpath entries used to run tests.
     *
     * <p>Replaces default classpath. Include the build {@link rife.bld.BaseProject#buildMainDirectory() main} and
     * {@link rife.bld.BaseProject#buildTestDirectory() test} directories compiled classes are needed.</p>
     *
     * @param entries the list of entries
     * @return this operation instance
     * @see #testClasspath(String...)
     */
    public TestNgOperation testClasspath(@NonNull Collection<String> entries) {
        ObjectTools.requireAllNotEmpty(entries, "`testClasspath` and its elements must not be null or empty");
        testClasspath_.clear();
        testClasspath_.addAll(entries);
        return this;
    }

    /**
     * Returns the classpath entries used for running tests.
     *
     * @return the set of test classpath
     */
    public Set<String> testClasspath() {
        return testClasspath_;
    }

    /**
     * Specifies a jar file that contains test classes.
     * <p>
     * If a {@code testng.xml} file is found at the root of that jar file, it will be used, otherwise, all the test
     * classes found in this jar file will be considered test classes.
     *
     * @param jar the jar
     * @return this operation instance
     */
    public TestNgOperation testJar(@NonNull String jar) {
        return opt("-testjar", jar);
    }

    /**
     * This specifies the default name of test, if not specified in the suite definition file or source code.
     * <p>
     * This option is ignored if the {@code suite.xml} file or the source code specifies a different test name.
     *
     * @param name the name
     * @return this operation instance
     */
    public TestNgOperation testName(@NonNull String name) {
        return opt("-testname", name);
    }

    /**
     * Returns the list of test names to run.
     *
     * @return the set of test names
     */
    public Set<String> testNames() {
        return testNames_;
    }

    /**
     * Only tests defined in a {@code <test>} tag matching one of these names will be run.
     *
     * @param names one or more names
     * @return this operation instance
     * @see #testNames(Collection) #testNames(Collection)
     */
    public TestNgOperation testNames(@NonNull String... names) {
        Objects.requireNonNull(names, "`testNames` must not be null");
        return testNames(List.of(names));
    }

    /**
     * Only tests defined in a {@code <test>} tag matching one of these names will be run.
     *
     * @param names the list of names
     * @return this operation instance
     * @see #testName(String) #testName(String)
     */
    public TestNgOperation testNames(@NonNull Collection<String> names) {
        ObjectTools.requireAllNotEmpty(names, "`testNames` and its elements must not be null or empty");
        testNames_.clear();
        testNames_.addAll(names);
        return this;
    }

    /**
     * Specifies the factory used to create tests.
     *
     * @param factory the factory
     * @return this operation instance
     */
    public TestNgOperation testRunFactory(@NonNull String factory) {
        return opt("-testrunfactory", factory);
    }

    /**
     * This sets the default maximum number of threads to use for running tests in parallel.
     * <p>
     * It will only take effect if the parallel mode has been selected (for example, with the
     * {@link #parallel(Parallel) parallel} option).
     * <p>
     * This can be overridden in the suite definition.
     *
     * @param count the count
     * @return this operation instance
     */
    public TestNgOperation threadCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("threadCount must be >= 0");
        }
        options_.put("-threadcount", String.valueOf(count));
        return this;
    }

    /**
     * Specifies the thread pool executor factory implementation that TestNG should use.
     *
     * @param factoryClass the factory class
     * @return this operation instance
     */
    public TestNgOperation threadPoolFactoryClass(@NonNull String factoryClass) {
        return opt("-threadpoolfactoryclass", factoryClass);
    }

    /**
     * Whether to use the default listeners
     *
     * <p>Default is {@code true}</p>
     *
     * @param isDefaultListener {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation useDefaultListeners(boolean isDefaultListener) {
        return optBool("-usedefaultlisteners", isDefaultListener);
    }

    /**
     * Should TestNG use a global Shared ThreadPool (At suite level) for running regular and data driven tests.
     *
     * @param useGlobalThreadPool {@code true} or {@code false}
     * @return this operation instance
     */
    public TestNgOperation useGlobalThreadPool(boolean useGlobalThreadPool) {
        return optBool("-useGlobalThreadPool", useGlobalThreadPool);
    }

    /**
     * Set the Level of verbosity.
     *
     * @param level the level
     * @return this operation instance
     * @see #log(int) #log(int)
     */
    public TestNgOperation verbose(int level) {
        return log(level);
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}).
     * <p>
     * The default is {@code testng.xml}, which means a file called {@code testng.xml} at the root of the jar file.
     * <p>
     * This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(@NonNull String path) {
        return opt("-xmlpathinjar", path);
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}).
     * <p>
     * The default is {@code testng.xml}, which means a file called {@code testng.xml} at the root of the jar file.
     * <p>
     * This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(@NonNull File path) {
        Objects.requireNonNull(path, "The XML path in jar must not be null");
        return xmlPathInJar(path.getAbsolutePath());
    }

    /**
     * This attribute should contain the path to a valid XML file inside the test jar
     * (e.g. {@code "resources/testng.xml"}).
     * <p>
     * The default is {@code testng.xml}, which means a file called {@code testng.xml} at the root of the jar file.
     * <p>
     * This option will be ignored unless a test jar is specified.
     *
     * @param path the path
     * @return this operation instance
     */
    public TestNgOperation xmlPathInJar(@NonNull Path path) {
        Objects.requireNonNull(path, "The XML path in jar must not be null");
        return xmlPathInJar(path.toFile());
    }

    // -key -> key
    private String normalizeKey(@NonNull String key) {
        return key.startsWith("-") ? key.substring(1) : key;
    }

    // Stores a non-blank string option.
    private TestNgOperation opt(String key, String value) {
        ObjectTools.requireNotEmpty(key, "'%s' must not be null or empty", normalizeKey(key));
        options_.put(key, value);
        return this;
    }

    // Stores a boolean option unconditionally.
    private TestNgOperation optBool(String key, boolean value) {
        options_.put(key, Boolean.toString(value));
        return this;
    }

    // Joins a non-empty collection into a comma-separated option.
    private TestNgOperation optJoin(String key, Collection<String> values) {
        ObjectTools.requireAllNotEmpty(values,
                "'%s' and its elements must not be null or empty", normalizeKey(key));
        options_.put(key, String.join(",", values));
        return this;
    }

    /**
     * Returns the target collection for a given bld command-line argument prefix, or
     * {@link Optional#empty()} if the argument is not a recognized flag.
     */
    private Optional<Set<String>> targetCollectionForArg(String arg) {
        if (arg.startsWith("-suites=")) {
            return Optional.of(suites_);
        }
        if (arg.startsWith("-testclass=")) {
            return Optional.of(testClasses_);
        }
        if (arg.startsWith("-testnames=")) {
            return Optional.of(testNames_);
        }
        if (arg.startsWith("-methods=")) {
            return Optional.of(methods_);
        }
        if (arg.startsWith("-groups=")) {
            return Optional.of(groups_);
        }
        if (arg.startsWith("-excludegroups=")) {
            return Optional.of(excludeGroups_);
        }
        return Optional.empty();
    }

    /**
     * Writes a default testng.xml to build directory when no suites/methods/classes specified.
     *
     * @return the generated file
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private Path writeDefaultSuite() throws IOException {
        var temp = Path.of(project_.buildDirectory().getAbsolutePath(), "testng-generated.xml");
        try (var fos = Files.newOutputStream(temp)) {
            var writer = XML_FACTORY.createXMLStreamWriter(fos, "UTF-8");
            try (var ignored = (AutoCloseable) writer::close) {
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeDTD("<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">");
                writer.writeStartElement("suite");
                writer.writeAttribute("name", "bld Default Suite");
                writer.writeAttribute("verbose", "2");
                writer.writeStartElement("test");
                writer.writeAttribute("name", "All Packages");
                writer.writeStartElement("packages");
                for (var p : packages_) {
                    writer.writeEmptyElement("package");
                    writer.writeAttribute("name", p);
                }
                writer.writeEndElement(); // packages
                writer.writeEndElement(); // test
                writer.writeEndElement(); // suite
                writer.writeEndDocument();
            }
        } catch (Exception e) {
            throw new IOException("Failed to write default testng suite file", e);
        }
        return temp;
    }

    /**
     * Failure Policies
     */
    public enum FailurePolicy {
        SKIP,
        CONTINUE;

        /**
         * Returns the TestNG command line argument value.
         *
         * @return the argument value
         */
        public String asArgument() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Parallel {
        METHODS,
        TESTS,
        CLASSES;

        /**
         * Returns the TestNG command line argument value.
         *
         * @return the argument value
         */
        public String asArgument() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}