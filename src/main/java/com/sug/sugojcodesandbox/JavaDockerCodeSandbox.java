package com.sug.sugojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import com.sug.sugojcodesandbox.model.ExecuteMessage;
import com.sug.sugojcodesandbox.model.JudgeInfo;
import com.sug.sugojcodesandbox.security.MySecurityManager;
import com.sug.sugojcodesandbox.utils.ProcessUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaDockerCodeSandbox implements CodeSandbox {
    private static final String GROBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GROBAL_JAVA_CLASS_NAME = "Main.java";
    private static final Long TIME_OUT=10*1000L;
    private static final Boolean FIRST_INIT=false;

    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 7"));
        String code = ResourceUtil.readStr("TestCode/SimpleCompute/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/SleepError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/MemoryError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/WriteFileError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userCodeParentPath = null;
        File userCodeFile = null;
        try {

            //1.把code放到指定文件
            String userDir = System.getProperty("user.dir");
            String grobalCodePathName = userDir + File.separator + GROBAL_CODE_DIR_NAME;

            if (!FileUtil.exist(grobalCodePathName)) {
                FileUtil.mkdir(grobalCodePathName);
            }
            userCodeParentPath = grobalCodePathName + File.separator + UUID.randomUUID();
            String userCodePath = userCodeParentPath + File.separator + GROBAL_JAVA_CLASS_NAME;
            userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
            //2.把文件编译
            String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());

            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeCompileMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeCompileMessage);
            if (executeCompileMessage.getExitValue() != 0) {
                ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
                executeCodeResponse.setStatus(2);
                return executeCodeResponse;
            }

            //3.运行编译的.class
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();

            //拉取镜像
            String image="openjdk:17-alpine";
            if(FIRST_INIT)
            {
                PullImageCmd pullImageCmd =dockerClient.pullImageCmd(image);
                PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
                try {
                    pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
                } catch (InterruptedException e) {
                    System.out.println("拉取镜像异常");
                    throw new RuntimeException(e);
                }
            }
            //创建容器
            CreateContainerCmd createContainerCmd=dockerClient.createContainerCmd(image);
            HostConfig hostConfig = new HostConfig();
            hostConfig.withMemory(100*1000*1000L);
            hostConfig.withCpuCount(1L);
            hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
            CreateContainerResponse createContainerResponse= createContainerCmd
                    .withHostConfig(hostConfig)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withTty(true)
                    .withCmd("tail", "-f", "/dev/null")
                    .exec();
            System.out.println(createContainerResponse);
            String containerId=createContainerResponse.getId();
            //启动容器+执行代码
            dockerClient.startContainerCmd(containerId).exec();
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for(String input:inputList)
            {
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withAttachStdin(true)
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withCmd("java", "-cp", "/app", "Main")
                        .exec();
                String execId = execCreateCmdResponse.getId();
                String inputWithNewLine = input + "\n";

                try (
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputWithNewLine.getBytes(StandardCharsets.UTF_8));
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        ByteArrayOutputStream errorStream = new ByteArrayOutputStream()
                ) {
                    // 执行命令并阻塞等待其完成
                    ExecStartResultCallback resultCallback =new ExecStartResultCallback(outputStream, errorStream);

                    // 如果需要支持 stdin，有些版本是这样传的，或者直接通过 execStartCmd 关联：
                    dockerClient.execStartCmd(execId)
                            .withDetach(false)
                            .withTty(false)
                            .withStdIn(inputStream)
                            .exec(resultCallback)
                            .awaitCompletion();

                    // 收集当前用例的运行结果
                    ExecuteMessage executeMessage = new ExecuteMessage();
                    executeMessage.setMessage(outputStream.toString("UTF-8").trim());
                    executeMessage.setErrorMessage(errorStream.toString("UTF-8").trim());
                    // 注意：execStartCmd 本身不直接提供 exitCode，如果需要获取精确的退出状态，需要调用 dockerClient.inspectExecCmd(execId).exec().getExitCode()

                    System.out.println("测试用例 [" + input + "] 运行结果:\n" + executeMessage);
                    executeMessageList.add(executeMessage);

                } catch (Exception e) {
                    System.out.println("执行测试用例异常");
                    throw new RuntimeException(e);
                }

            }



            //4.整理输出信息

        } catch (IOException e) {
            //系统错误

        } finally {
            //5.删除多余的文件
//            if (userCodeFile != null && userCodeFile.getParentFile() != null) {
//                boolean delete = FileUtil.del(userCodeParentPath);
//                System.out.println("删除多余文件" + (delete ? "成功" : "失败"));
//            }
        }
        return new ExecuteCodeResponse();
    }
}
