package com.example;

import rife.bld.BaseProject;
import rife.bld.BuildCommand;
import rife.bld.extension.JacocoReportOperation;
import rife.bld.extension.TestNgOperation;
import rife.bld.operations.TestOperation;

import java.io.IOException;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.test;

/**
 * Example build.
 *
 * <ul style="list-style-type:none">
 * <li>{@code ./bld compile test}</li>
 * <li>{@code ./bld compile jacoco}</li>
 * </ul>
 */
public class ExamplesBuild extends BaseProject {
    @Override
    public TestOperation<?, ?> testOperation() {
        return new TestNgOperation()
                .fromProject(this)
                .packages("com.example");
    }

    public ExamplesBuild() {
        pkg = "com.example";
        name = "Examples";
        version = version(0, 1, 0);

        javaRelease = 17;
        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL);

        scope(test).include(dependency("org.testng", "testng", version(7, 9, 0)));
    }

    public static void main(String[] args) {
        new ExamplesBuild().start(args);
    }

    @BuildCommand(summary = "Generates Jacoco Reports")
    public void jacoco() throws IOException {
        new JacocoReportOperation()
                .fromProject(this)
                .execute();
    }
}
