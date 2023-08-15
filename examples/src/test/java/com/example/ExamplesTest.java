package com.example;

import org.testng.Assert;
import org.testng.annotations.Test;

class ExampleTest {
    public static void main(String[] args) {
        new ExampleTest().verifyHello();
    }

    @Test
    void testFail() {
        Assert.fail("failed");
    }

    @Test
    void verifyHello() {
        Assert.assertTrue(true);
    }
}