// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

exports.handler = async(event, context, callback) => {
    console.info("Event: " + JSON.stringify(event));
    console.info("Context: " + JSON.stringify(context));
    await new Promise(resolve => setTimeout(resolve, 500));
    const response = {
        statusCode: 200,
        body: JSON.stringify('Hello from Lambda!'),
    };
    callback(null, response);
};