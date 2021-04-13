// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.subminutelambdaexecutor;

import software.amazon.awscdk.core.App;

public class SubMinuteLambdaExecutorApp {
    public static void main(final String[] args) {
        App app = new App();

        new SubMinuteLambdaExecutorStack(app, "SubMinuteLambdaExecutorStack");

        app.synth();
    }
}
