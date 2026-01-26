package com.example;

import org.testng.Assert;
import org.testng.annotations.Test;

class ExamplesTest {
    private final ExamplesLib example = new ExamplesLib();

    @Test(groups = {"foo", "foobar"})
    void foo() {
        Assert.assertNotEquals(example.getMessage(), "foo");
    }

    @Test(groups = {"bar", "foobar"})
    void bar() {
        Assert.assertNotEquals(example.getMessage(), "bar");
    }

    @Test(groups = {"hello"})
    void verifyHello() {
        Assert.assertEquals(example.getMessage(), "Hello World!");
    }
}