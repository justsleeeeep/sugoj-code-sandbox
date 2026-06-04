package com.sug.sugojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import com.sug.sugojcodesandbox.model.ExecuteMessage;
import com.sug.sugojcodesandbox.model.JudgeInfo;
import com.sug.sugojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    private static final String GROBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GROBAL_JAVA_CLASS_NAME = "Main.java";
    private static final Long TIME_OUT = 10 * 1000L;
    private static final Boolean FIRST_INIT = false;

    /**
     *
     * @param code 用户代码
     * @return 存放用户代码的文件
     */

    //1.把code放到指定文件
    public File saveCodeToFile(String code,String grobalCodePathName,String userCodePath) {
        String userDir = System.getProperty("user.dir");
        if (!FileUtil.exist(grobalCodePathName)) {
            FileUtil.mkdir(grobalCodePathName);
        }
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }


    /**
     *
      * @param userCodeFile
     * @return 执行消息
     */
    //2.把文件编译
    public ExecuteMessage compileFile(File userCodeFile)
    {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        Process compileProcess = null;
        try
        {
            compileProcess = Runtime.getRuntime().exec(compileCmd);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        ExecuteMessage executeCompileMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        System.out.println(executeCompileMessage);
        if(executeCompileMessage.getExitValue()!=0)
        {
            executeCompileMessage.setErrorMessage("compile error");
            throw new RuntimeException("compile error");
        }
        return executeCompileMessage;
    }


    /**
     *
     * @param inputList
     * @param userCodeParentPath
     * @return 执行代码返回的消息
     */
    //3.运行编译的.class
    public List<ExecuteMessage> runCode(List<String> inputList,String userCodeParentPath)
    {
        List<ExecuteMessage> executeRunMessageList = new ArrayList<>();
        for (String input : inputList)
        {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main", userCodeParentPath);
            Process runProcess = null;
            try
            {
                runProcess = Runtime.getRuntime().exec(runCmd);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            final Process finalProcess = runProcess;
            Thread timeoutThread = new Thread(() ->
            {
                try
                {
                    Thread.sleep(TIME_OUT);
                    finalProcess.destroy();
                }
                catch (InterruptedException e)
                {
                    return ;
                }
            });
            timeoutThread.setDaemon(true); // 设为守护线程
            timeoutThread.start();
            ExecuteMessage executeRunMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, "运行", input);
            timeoutThread.interrupted();
            executeRunMessageList.add(executeRunMessage);
        }
        return executeRunMessageList;
    }


    /**
     *
     * @param executeRunMessageList
     * @return
     */
    //4.整理输出信息
    ExecuteCodeResponse createOutputMessage(List<ExecuteMessage> executeRunMessageList)
    {
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
        //judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        System.out.println(judgeInfo);
        return executeCodeResponse;
    }

    /**
     *
     * @param userCodeFile
     * @param userCodeParentPath
     * @return
     */
    //5.删除多余的文件
    void deleteFile(File userCodeFile,String userCodeParentPath)
    {
        if (userCodeFile != null && userCodeFile.getParentFile() != null) {
            boolean delete = FileUtil.del(userCodeParentPath);
            System.out.println("删除多余文件" + (delete ? "成功" : "失败"));
        }
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest)
    {
        String userDir = System.getProperty("user.dir");
        String grobalCodePathName = userDir + File.separator + GROBAL_CODE_DIR_NAME;
        String userCodeParentPath = grobalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GROBAL_JAVA_CLASS_NAME;

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        //1.把code放到指定文件
        File userCodeFile= saveCodeToFile(code,grobalCodePathName,userCodePath);
        //2.把文件编译
        ExecuteMessage compileExecuteMessage = compileFile(userCodeFile);
        //3.运行编译的.class
        List<ExecuteMessage>executeRunMessageList = runCode(inputList,userCodeParentPath);
        //4.整理输出信息
        ExecuteCodeResponse executeCodeResponse= createOutputMessage(executeRunMessageList);
        //5.删除多余的文件
        deleteFile(userCodeFile,userCodeParentPath);
        return executeCodeResponse;
    }
}
