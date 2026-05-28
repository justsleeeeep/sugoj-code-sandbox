package com.sug.sugojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import com.sug.sugojcodesandbox.model.ExecuteMessage;
import com.sug.sugojcodesandbox.model.JudgeInfo;
import com.sug.sugojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.max;

public class JavaNativeCodeSandbox implements CodeSandbox {
    private static final String GROBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GROBAL_JAVA_CLASS_NAME = "Main.java";

    public static void main(String[] args) {
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 7"));
        String code = ResourceUtil.readStr("TestCode/SimpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        //1.把code放到指定文件
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        String grobalCodePathName = userDir + File.separator + GROBAL_CODE_DIR_NAME;

        if (!FileUtil.exist(grobalCodePathName)) {
            FileUtil.mkdir(grobalCodePathName);
        }
        String userCodeParentPath = grobalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GROBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //2.把文件编译
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //3.运行编译的.class
        List<ExecuteMessage>executeMessageList =new ArrayList<>();
        for (String input : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, "运行", input);
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        //4.整理输出信息
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        //执行没错误初始为1
        executeCodeResponse.setStatus(1);
        List<String>outList=new ArrayList<>();
        Long executeTime=0L;
        for(ExecuteMessage executeMessage:executeMessageList)
        {
            String errorMessage=executeMessage.getErrorMessage();
            if(!StrUtil.isBlank(errorMessage))
            {
                executeCodeResponse.setMessage(errorMessage);
                //执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outList.add(executeMessage.getMessage());
            executeTime=Math.max(executeMessage.getTime(),executeTime);
        }
        executeCodeResponse.setOutputList(outList);
        JudgeInfo judgeInfo =new JudgeInfo();
        judgeInfo.setTime(executeTime);
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        System.out.println(judgeInfo);
        return null;
    }
}
