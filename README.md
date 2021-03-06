# Sub Minute Lambda Executor
When building applications in AWS, developers may be required to poll external systems such as HTTP, REST, or TCP endpoints. Polling the external system every few seconds may be required along with modifying the polling interval, such as polling faster during the day and slower at night. For example, tracking municipal bus predictions, vehicle positions, and alerts by querying restful endpoints. Typically, a developer would use a <a href="https://en.wikipedia.org/wiki/Cron">cron</a> style job scheduler to perform this type of scheduling task. Using AWS serverless services there is no way to create a scheduled task that is less than one minute. Using <a href="https://aws.amazon.com/cloudwatch/">Amazon CloudWatch</a>, you are able to produce events at intervals of one minute or above, but does not allow for scheduling less that a minute. This solution will demonstrate how to use a set of services to produce scheduled events at a rate of one second or more in a highly accurate manor. The solution will show how to invoke an AWS Lambda function to poll the external systems at scheduled intervals which can be adjusted over time.

## Architecture
<img alt="Architecture" src="./images/SubMinuteLambdaExecutor.jpg" />

* <a href="https://aws.amazon.com/step-functions/">AWS Step Functions</a> are used to invoke a Lambda function which controls the final Lambda execution. The AWS Step Function will restart itself once the Lambda completes.
* <a href="https://aws.amazon.com/lambda/">AWS Lambda</a> is used to control the timing and final asynchronous execution of the worker Lambda which will perform the operation required for the external system.
* <a href="https://aws.amazon.com/dynamodb/">Amazon DynamoDB</a> is used to store control flags. The "running" field determines if the system should continue to run. The "waitseconds" field is used to determine the period between worker Lambda invocations. 


## Setup
There are 2 methods to install the sample. Either via the pre-generated CloudFormation template with inline Lambda functions or via CDK.
### CloudFormation
1. A CloudFormation template (SubMinuteLambdaExecutor.yaml) has been pre-generated and can be used directly
1. Follow the <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-console-create-stack.html">Creating a stack on the AWS CloudFormation console</a> documentation
1. During the <b>Selecting a stack template</b> step upload the SubMinuteLambdaExecutor.yaml file provided 
1. During the <b>Specifying stack name and parameters</b> step please update the "waitseconds" to the timeout required
1. Complete the remaining steps from the documentation and wait for the stack to deploy
1. The outputs tab of the stack information will display name and arn for the DynamoDB table, Lambda functions, and Step Function.
### CDK
1. <a href="https://docs.aws.amazon.com/cdk/latest/guide/cli.html">AWS CDK Toolkit</a> is required
1. From a terminal window at the root directory of this project do ```cdk deploy```
1. When complete, a list of outputs will display name and arn for the DynamoDB table, Lambda functions, and Step Function.

## Running
1. To start the system, start execution of the Step Function that was created.
1. The Step Function does not require any inputs

## Stopping Execution
1. To stop execution, update the DynamoDB table column value "running" to false

## Resource Cleanup
### CloudFormation
1. Delete the CloudFormation stack

### CDK
1. From a terminal window at the root directory of this project do ```cdk destroy```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.