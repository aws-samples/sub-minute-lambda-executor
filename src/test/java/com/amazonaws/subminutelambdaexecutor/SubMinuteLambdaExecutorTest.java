// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.subminutelambdaexecutor;

import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class SubMinuteLambdaExecutorTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() throws IOException {
        java.util.Map<String,String> context = new HashMap<>();
        App app = App.Builder.create().context(context).build();

        SubMinuteLambdaExecutorStack stack = new SubMinuteLambdaExecutorStack(app, "test");
    }
}
