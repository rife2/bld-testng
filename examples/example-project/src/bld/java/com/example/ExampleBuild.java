package com.example;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.extension.JacocoReportOperation;
import rife.bld.extension.TestNgOperation;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.test;

/**
 * Example build.
 *
 * <pre>{@code
 * ./bld compile test
 * ./bld compile jacoco
 * }</pre>
 */
public class ExampleBuild extends Project {

    final String packageName = "com.example";
    final TestNgOperation testNgOperation = new TestNgOperation()
            .fromProject(this)
            .packages(packageName)
            .log(2);

    public ExampleBuild() {
        pkg = packageName;
        name = "Examples";
        version = version(0, 1, 0);

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL);

        scope(test)
                .include(dependency("org.slf4j", "slf4j-jdk14", "2.0.17"))
                .include(dependency("org.testng", "testng", version(7, 12, 0)));
    }

    public static void main(String[] args) {
        // Enable detailed logging for the JaCoCo extension
        var level = Level.ALL;
        var logger = Logger.getLogger("rife.bld.extension");
        var consoleHandler = new ConsoleHandler();

        consoleHandler.setLevel(level);
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
        logger.setUseParentHandlers(false);

        new ExampleBuild().start(args);
    }

    @BuildCommand(summary = "Generates Jacoco Reports")
    public void jacoco() throws Exception {
        new JacocoReportOperation()
                .fromProject(this)
                .testOperation(testNgOperation)
                .execute();
    }

    @Override
    public void test() throws Exception {
        testNgOperation.execute();
    }
}
