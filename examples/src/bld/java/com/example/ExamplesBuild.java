package com.example;

import rife.bld.BaseProject;
import rife.bld.BuildCommand;
import rife.bld.extension.JacocoReportOperation;
import rife.bld.extension.TestNgOperation;
import rife.bld.operations.TestOperation;

import java.io.IOException;
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
public class ExamplesBuild extends BaseProject {
    public ExamplesBuild() {
        pkg = "com.example";
        name = "Examples";
        version = version(0, 1, 0);

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL);

        scope(test).include(dependency("org.testng", "testng", version(7, 10, 2)));
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

        new ExamplesBuild().start(args);
    }

    @BuildCommand(summary = "Generates Jacoco Reports")
    public void jacoco() throws Exception {
        new JacocoReportOperation()
                .fromProject(this)
                .execute();
    }

    @Override
    public TestOperation<?, ?> testOperation() {
        return new TestNgOperation()
                .fromProject(this)
                .packages("com.example");
    }
}
