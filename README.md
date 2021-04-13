# Sub Minute Lambda Executor

This project contains both the CDK and AWS CloudFormation template used to create the AWS Step Function and Amazon DynamoDB to control the execution cycle. 

### Architecture
<img alt="Architecture" src="./images/SubMinuteLambdaExecutor.jpg" />

### Requirements
* <a href="https://docs.aws.amazon.com/cdk/latest/guide/cli.html">AWS CDK Toolkit</a> (If rebuilding via CDK)

## Setup
There are 2 methods to install this sample as listed below either via the pre-generated CloudFormation template with inline Lambda functions or via CDK.
### CloudFormation
1. A CloudFormation template (SubMinuteLambdaExecutor.yaml) has been pre-generated and can be used directly
1. <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-console-create-stack.html">Follow the Creating a stack on the AWS CloudFormation console documentation</a>
1. During the <b>Selecting a stack template</b> step upload the SubMinuteLambdaExecutor.yaml file provided 
1. During the <b>Specifying stack name and parameters</b> step please update the "waitseconds" to the timeout required during setup
1. Complete the remaining steps from the documentation and wait for the stack to deploy
1. The outputs tab of the stack information will display the table name and arn, the Step Function name and arn, and the Lambda function names and arns
### CDK
1. From a terminal window at the root directory of this project do ```cdk deploy```
1. When complete, a list of outputs will display the table name and arn, the Step Function name and arn, and the Lambda function names and arns

## Running
1. To start the process either using the cli or console to start execution of the Step Function that was created.
1. The Step Function does not require any inputs

## Stopping Execution
1. To stop execution either using the cli or console update the DynamoDB table column value "running" to false

## Resource Cleanup
### CloudFormation
1. Delete the CloudFormation stack created

### CDK
1. From a terminal window at the root directory of this project do ```cdk destroy```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.