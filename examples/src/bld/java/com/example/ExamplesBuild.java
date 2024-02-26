package com.example;

import rife.bld.BaseProject;
import rife.bld.BuildCommand;
import rife.bld.extension.TestNgOperation;

import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.test;

/**
 * Example build.
 *
 * <ul style="list-style-type:none">
 *     <li>./bld compile test</li>
 * </ul>
 */
public class ExamplesBuild extends BaseProject {
    public ExamplesBuild() {
        pkg = "com.example";
        name = "Examples";
        version = version(0, 1, 0);

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL);
        
        scope(test).include(dependency("org.testng", "testng", version(7, 9, 0)));
    }

    public static void main(String[] args) {
        new ExamplesBuild().start(args);
    }

    @BuildCommand(summary = "Tests the project with TestNG")
    public void test() throws Exception {
        new TestNgOperation()
                .fromProject(this)
                .packages("com.example")
                .execute();
    }
}
