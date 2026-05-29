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
        //String code = ResourceUtil.readStr("TestCode/SimpleCompute/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/SleepError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("TestCode/UnSafeCode/MemoryError.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("TestCode/UnSafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String userCodeParentPath = null;
        File userCodeFile = null;
        try {

            //1.把code放到指定文件
            List<String> inputList = executeCodeRequest.getInputList();
            String code = executeCodeRequest.getCode();
            String language = executeCodeRequest.getLanguage();

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
                String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);

                Process runProcess = Runtime.getRuntime().exec(runCmd);
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
            if (userCodeFile.getParentFile() != null) {
                boolean delete = FileUtil.del(userCodeParentPath);
                System.out.println("删除多余文件" + (delete ? "成功" : "失败"));
            }
        }
    }
}
