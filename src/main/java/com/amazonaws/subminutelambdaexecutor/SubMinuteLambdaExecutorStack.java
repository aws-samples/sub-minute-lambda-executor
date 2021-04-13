// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.subminutelambdaexecutor;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.cloudformation.CustomResourceProvider;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SingletonFunction;
import software.amazon.awscdk.services.stepfunctions.*;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoAttributeValue;
import software.amazon.awscdk.services.stepfunctions.tasks.DynamoGetItem;
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SubMinuteLambdaExecutorStack extends Stack {
    public SubMinuteLambdaExecutorStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SubMinuteLambdaExecutorStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        Table table = createSubMinuteDynamoDBTable();
        Function subMinuteDemoLambda = createSubMinuteDemoLambda(table);
        Function subMinuteLambdaExecutorLambda = createSubMinuteLambdaExecutorLambda(table, subMinuteDemoLambda);
        createStepFunction(table, subMinuteLambdaExecutorLambda, subMinuteDemoLambda);
    }

    private static String readFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    private Table createSubMinuteDynamoDBTable() {
        CfnParameter runningParameter = CfnParameter.Builder.create(this, "running")
                .type("String")
                .allowedValues(Arrays.asList("true", "false"))
                .defaultValue("true")
                .description("Determines if the current state of the system is running. If this value is false any attempt to start the system will immediately fail out.")
                .build();

        CfnParameter waitSeconds = CfnParameter.Builder.create(this, "waitseconds")
                .type("Number")
                .minValue(1)
                .maxValue(59)
                .defaultValue("5")
                .description("The timeout value in seconds between executions")
                .build();

        Table table = Table.Builder.create(this, "SubMinuteLambdaExecutorDB")
                .removalPolicy(RemovalPolicy.DESTROY)
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.NUMBER)
                        .build())
                .build();

        CfnOutput.Builder.create(this, "TableName").exportName("TableName").value(table.getTableName()).build();
        CfnOutput.Builder.create(this, "TableArn").exportName("TableArn").value(table.getTableArn()).build();

        String functionCode = readFile("lambda/SubMinuteDBInit.js");
        final SingletonFunction lambdaFunction =
                SingletonFunction.Builder.create(this, "SubMinuteDBInit")
                        .description("Initialize the sub minute lambda execution tracking table")
                        .code(Code.fromInline(functionCode))
                        .handler("index.handler")
                        .timeout(Duration.seconds(10))
                        .runtime(Runtime.NODEJS_12_X)
                        .uuid(java.util.UUID.randomUUID().toString())
                        .build();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("TableName", table.getTableName());
        map.put("id", 1);
        map.put("running", runningParameter.getValueAsString());
        map.put("waitseconds", waitSeconds.getValueAsNumber());

        table.grantReadWriteData(lambdaFunction);

        software.amazon.awscdk.services.cloudformation.CustomResource.Builder.create(this, "SubMinuteDBInitResource")
                        .provider(CustomResourceProvider.fromLambda(lambdaFunction))
                        .properties(map)
                        .build();



        return table;
    }

    private Function createSubMinuteDemoLambda(Table table) {
        String functionCode = readFile("lambda/SubMinuteDemo.js");
        Function function = Function.Builder.create(this, "SubMinuteDemo")
                .runtime(Runtime.NODEJS_12_X)
                .code(Code.fromInline(functionCode))
                .handler("index.handler")
                .build();
        CfnOutput.Builder.create(this, "DemoLambda").exportName("DemoLambda").value(function.getFunctionName()).build();
        CfnOutput.Builder.create(this, "DemoLambdaArn").exportName("DemoLambdaArn").value(function.getFunctionArn()).build();
        return function;
    }

    private Function createSubMinuteLambdaExecutorLambda(Table table, Function subMinuteDemoLambda) {
        String functionCode = readFile("lambda/SubMinuteLambdaExecutor.js");
        Function function = Function.Builder.create(this, "SubMinuteLambdaExecutor")
                .runtime(Runtime.NODEJS_12_X)
                .code(Code.fromInline(functionCode))
                .handler("index.handler")
                .timeout(Duration.minutes(14))
                .build();
        table.grantReadData(function);
        subMinuteDemoLambda.grantInvoke(function);
        CfnOutput.Builder.create(this, "ExecutorLambda").exportName("ExecutorLambda").value(function.getFunctionName()).build();
        CfnOutput.Builder.create(this, "ExecutorLambdaArn").exportName("ExecutorLambdaArn").value(function.getFunctionArn()).build();
        return function;
    }

    private StateMachine createStepFunction(Table table, Function function, Function demoFunction) {

        Choice choiceCheckInput = Choice.Builder.create(this, "CheckInput").build();

        Pass passDefaultInput = Pass.Builder.create(this, "DefaultInput")
                .parameters(Map.ofEntries(
                        Map.entry("lastExecutionTS", "0")
                ))
                .build();

        DynamoGetItem dynamoDbGetRunningFlag = DynamoGetItem.Builder.create(this, "GetRunningFlag")
                .key(Map.ofEntries(Map.entry("id", DynamoAttributeValue.fromNumber(1))))
                .table(table)
                .resultPath("$.DynamoDB")
                .build();

        Pass passParse = Pass.Builder.create(this, "Parse")
                .parameters(Map.ofEntries(
                        Map.entry("waitseconds.$", "States.StringToJson($.DynamoDB.Item.waitseconds.N)"),
                        Map.entry("running.$", "$.DynamoDB.Item.running.BOOL"),
                        Map.entry("statemachineId.$", "$$.StateMachine.Id"),
                        Map.entry("lastExecutionTS.$", "$.lastExecutionTS")
                ))
                .build();

        Choice choiceIsRunning = Choice.Builder.create(this, "IsRunning").build();

        LambdaInvoke lambdaInvoke = LambdaInvoke.Builder.create(this, "InvokeLambda")
                .lambdaFunction(function)
                .timeout(Duration.minutes(15))
                .payload(TaskInput.fromObject(Map.ofEntries(
                        Map.entry("tableName", table.getTableName()),
                        Map.entry("functionName", demoFunction.getFunctionName()),
                        Map.entry("lastExecutionTS.$", "$.lastExecutionTS")
                )))
                .resultPath("$.LambdaExecution")
                .build();

        Pass passErrorRecovery = Pass.Builder.create(this, "ErrorRecovery")
                .parameters(Map.ofEntries(
                        Map.entry("lastExecutionTS", "0"),
                        Map.entry("statemachineId.$", "$$.StateMachine.Id"),
                        Map.entry("LambdaExecution", Map.ofEntries(
                                Map.entry("Payload", Map.ofEntries(
                                        Map.entry("lastExecutionTS", "0")
                                ))
                        ))
                ))
                .build();

        Wait waitErrorRecoveryWait = Wait.Builder.create(this, "ErrorRecoveryWait")
                .time(WaitTime.duration(Duration.seconds(5)))
                .build();

        CustomState restartStepFunction = CustomState.Builder.create(this, "RestartStepFunction")
                .stateJson(Map.ofEntries(
                        Map.entry("Type", "Task"),
                        Map.entry("Resource", "arn:aws:states:::states:startExecution"),
                        Map.entry("Parameters", Map.ofEntries(
                                Map.entry("StateMachineArn.$", "$.statemachineId"),
                                Map.entry("Input", Map.ofEntries(
                                        Map.entry("AWS_STEP_FUNCTIONS_STARTED_BY_EXECUTION_ID.$", "$$.Execution.Id"),
                                        Map.entry("lastExecutionTS.$", "$.LambdaExecution.Payload.lastExecutionTS")

                                ))
                        )),
                        Map.entry("End", true)
                ))
                .build();

        Pass passDone = Pass.Builder.create(this, "Done").build();

        choiceCheckInput.when(Condition.and(Condition.isPresent("$.lastExecutionTS"), Condition.isNotNull("$.lastExecutionTS")), dynamoDbGetRunningFlag);
        choiceCheckInput.otherwise(passDefaultInput);
        passDefaultInput.next(dynamoDbGetRunningFlag);
        dynamoDbGetRunningFlag.next(passParse);
        dynamoDbGetRunningFlag.addCatch(passErrorRecovery, CatchProps.builder()
                .errors(Arrays.asList("States.ALL"))
                .build());
        passParse.next(choiceIsRunning);
        choiceIsRunning.when(Condition.booleanEquals("$.running", true), lambdaInvoke);
        choiceIsRunning.otherwise(passDone);
        lambdaInvoke.next(restartStepFunction);
        lambdaInvoke.addCatch(passErrorRecovery, CatchProps.builder()
                .errors(Arrays.asList("States.ALL"))
                .build());
        passErrorRecovery.next(waitErrorRecoveryWait);
        waitErrorRecoveryWait.next(restartStepFunction);

        StateMachine stateMachine = StateMachine.Builder.create(this, "SubMinuteLambdaExecutorStateMachine")
                .timeout(Duration.minutes(15))
                .definition(choiceCheckInput)
                .stateMachineType(StateMachineType.STANDARD)
                .build();

        stateMachine.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("states:StartExecution"))
                .resources(List.of("*"))
                .build());

        CfnOutput.Builder.create(this, "StateMachineName").exportName("StateMachineName").value(stateMachine.getStateMachineName()).build();
        CfnOutput.Builder.create(this, "StateMachineArn").exportName("StateMachineArn").value(stateMachine.getStateMachineArn()).build();
        return stateMachine;
    }
}
