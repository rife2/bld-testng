package com.example;

import org.testng.Assert;
import org.testng.annotations.Test;

class SampleTest {

    private final ExampleLib example = new ExampleLib();

    @Test(groups = {"bonjour"})
    void verifyBonjour() {
        Assert.assertEquals(example.getMessage("Bonjour!"), "Bonjour!");
    }
}