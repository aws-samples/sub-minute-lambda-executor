// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

const AWS = require('aws-sdk');
const ddb = new AWS.DynamoDB({ apiVersion: '2012-08-10' });
const response = require('cfn-response');

// This method is run during the initial creation of the DynamoDB table and will
// populate the table with the required row and field values
exports.handler = (event, context) => {
    console.info("Event: " + JSON.stringify(event));
    console.info("Context: " + JSON.stringify(context));

    if (event.RequestType == "Create") {
        try {
            let params = {
                TableName: event.ResourceProperties.TableName,
                Item: {
                    id: {
                        N: event.ResourceProperties.Id
                    },
                    running: {
                        BOOL: event.ResourceProperties.Running == 'true'
                    },
                    waitseconds: {
                        N: event.ResourceProperties.Waitseconds
                    }
                }
            };
            console.info("Executing: " + JSON.stringify(params));
            ddb.putItem(params, function(err, res) {
                if (err) {
                    console.error("Error: " + JSON.stringify(err));
                    response.send(event, context, response.FAILED, err);
                } else {
                    console.info("Success: " + JSON.stringify(res));
                    response.send(event, context, response.SUCCESS, res);
                }           // successful response
            });
        } catch(err) {
            console.error("Error: " + JSON.stringify(err));
            response.send(event, context, response.FAILED, err);
        }
    } else {
        console.info("Not a create event");
        response.send(event, context, response.SUCCESS, {});
    }
};
