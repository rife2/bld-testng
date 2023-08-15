package com.example;

import org.testng.Assert;
import org.testng.annotations.Test;

class ExampleTest {
    @Test
    void verifyHello() {
        Assert.assertTrue(true);
    }

    @Test
    void testFail() {
        Assert.fail("failed");
    }

    public static void main(String[] args) {
        new ExampleTest().verifyHello();
    }
}