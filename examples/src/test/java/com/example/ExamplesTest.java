package com.example;

import org.testng.Assert;
import org.testng.annotations.Test;

class ExamplesTest {
    private final ExamplesLib example = new ExamplesLib();

    @Test
    void foo() {
        Assert.assertNotEquals(example.getMessage(), "foo");
    }

    @Test
    void verifyHello() {
        Assert.assertEquals(example.getMessage(), "Hello World!");
    }
}