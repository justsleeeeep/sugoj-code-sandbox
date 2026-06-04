package com.sug.sugojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import com.sug.sugojcodesandbox.model.ExecuteMessage;
import com.sug.sugojcodesandbox.model.JudgeInfo;
import com.sug.sugojcodesandbox.security.MySecurityManager;
import com.sug.sugojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandboxOld implements CodeSandbox {
    private static final String GROBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GROBAL_JAVA_CLASS_NAME = "Main.java";
    private static final Long TIME_OUT=10*1000L;
    private static final List<String>blackList=Arrays.asList("Files","exec");
    private static final WordTree wordTree= new WordTree();
    static{
        wordTree.addWords(blackList);
    }
    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 7"));
        //String code = ResourceUtil.readStr("TestCode/SimpleCompute/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/SleepError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/MemoryError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/WriteFileError.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("TestCode/UnSafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.setSecurityManager(new MySecurityManager());
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userCodeParentPath = null;
        File userCodeFile = null;
//        FoundWord foundWord=wordTree.matchWord(code);
//        if(foundWord!=null)
//        {
//            System.out.println(foundWord);
//            return null;
//        }
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
            List<ExecuteMessage> executeRunMessageList = new ArrayList<>();
            for (String input : inputList) {
                String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(()-> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeRunMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, "运行", input);
                System.out.println(executeRunMessage);
                executeRunMessageList.add(executeRunMessage);

            }


            //4.整理输出信息
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            //执行没错误初始为1
            executeCodeResponse.setStatus(1);
            List<String> outList = new ArrayList<>();
            Long executeTime = 0L;
            for (ExecuteMessage executeMessage : executeRunMessageList) {
                String errorMessage = executeMessage.getErrorMessage();
                if (!StrUtil.isBlank(errorMessage)) {
                    executeCodeResponse.setMessage(errorMessage);
                    //执行中存在错误
                    executeCodeResponse.setStatus(3);
                    break;
                }
                outList.add(executeMessage.getMessage());
                executeTime = Math.max(executeMessage.getTime(), executeTime);
            }
            executeCodeResponse.setOutputList(outList);
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setTime(executeTime);
//        judgeInfo.setMemory();
            executeCodeResponse.setJudgeInfo(judgeInfo);
            System.out.println(judgeInfo);
            return executeCodeResponse;
        } catch (IOException e) {
            //系统错误
            //            throw new RuntimeException(e);
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setStatus(4);
            executeCodeResponse.setMessage(e.getMessage());
            return executeCodeResponse;
        } finally {
            //删除多余的文件
            if (userCodeFile != null && userCodeFile.getParentFile() != null) {
                boolean delete = FileUtil.del(userCodeParentPath);
                System.out.println("删除多余文件" + (delete ? "成功" : "失败"));
            }
        }
    }
}
