// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

const AWS = require('aws-sdk');
const ddb = new AWS.DynamoDB({ apiVersion: '2012-08-10' });
const lambda = new AWS.Lambda();

// This Lambda will control the asynchronous invocation of the given Lambda
// which will then perform the operation to call the external system.
// This Lambda is setup to have a 15 minute runtime. Internally we loop until 14 minutes
// to avoid going over the 15 minute max Lambda execution time.
exports.handler = async(event, context, callback) => {
    let processStartTime = process.uptime() * 1000;
    let loopCount = 60 * 14; // 14 mins
    let secondsLeftBeforeExecute = 0.0;
    let init = true;
    let timeCorrection = 0.0;
    let expectedExecuteTime = 0.0;
    let executeSkew = 0.0;
    let params = {
        TableName: event.tableName,
        Key: {
            "id": { N: "1" }
        }
    };

    let currentTS = new Date().getTime();
    let lastExecutionTS = event.lastExecutionTS;
    let lastExecutionDiff = lastExecutionTS > 0 ? currentTS - lastExecutionTS : 0;

    let lambdaParams = {
        FunctionName: event.functionName, // the lambda function we are going to invoke
        InvocationType: 'Event',
        Payload: '{}'
    };

    let startLoop = process.uptime() * 1000;
    for (let i = 0; i < loopCount; i++) {
        let running = false;
        let executeTimeout = 0;
        try {
            let data = await ddb.getItem(params).promise();
            running = data.Item.running.BOOL;
            executeTimeout = Number(data.Item.waitseconds.N);
            if (init) {
                secondsLeftBeforeExecute = executeTimeout;
                init = false;
                expectedExecuteTime = expectedExecuteTime + (executeTimeout * 1000);
            }
        }
        catch (err) {
            callback(null, {
                statusCode: 500,
                error: err,
                lastExecutionTS: new Date().getTime()
            })
            return;
        }
        if (running) {
            let dbComplete = process.uptime() * 1000;
            let dbExecuteTime = (dbComplete - startLoop);
            let timeout = 1000 - processStartTime - dbExecuteTime + timeCorrection - lastExecutionDiff;
            lastExecutionDiff = 0;
            if (timeout > 0) {
                await new Promise(resolve => setTimeout(resolve, timeout));
            }
            secondsLeftBeforeExecute--;
            if (secondsLeftBeforeExecute <= 0) {
                executeSkew = Number(process.uptime() * 1000) - Number(expectedExecuteTime);
                lambda.invoke(lambdaParams, function(err, data) {
                    if (err) {
                        callback(null, {
                            statusCode: 500,
                            error: err,
                            lastExecutionTS: new Date().getTime()
                        })
                    }
                });
                secondsLeftBeforeExecute = executeTimeout;
                expectedExecuteTime = expectedExecuteTime + (executeTimeout * 1000);
            }
        }
        else {
            break;
        }
        let endLoop = process.uptime() * 1000;
        let runtime = (endLoop - startLoop);
        timeCorrection = 1000 - runtime - processStartTime - executeSkew;
        if (timeCorrection > 0) {
            timeCorrection = 0;
        }
        processStartTime = 0.0;
        executeSkew = 0.0;
        startLoop = process.uptime() * 1000;
    }
    callback(null, {
        statusCode: 200,
        lastExecutionTS: new Date().getTime()
    });
};
