package com.sug.sugojcodesandbox.utils;

import com.sug.sugojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ProcessUtils {

    /**
     * 执行交互式进程获取信息
     *
     * @param process
     * @param optName
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process process, String optName, String input) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        InputStream inputStream = process.getInputStream();
        OutputStream outputStream = process.getOutputStream();
        StopWatch stopWatch=new StopWatch();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        try {
            stopWatch.start();
            outputStreamWriter.write(input);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            int exitValue = process.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(optName + "成功");
                InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String OutLine;
                StringBuilder Out = new StringBuilder();
                while ((OutLine = bufferedReader.readLine()) != null) {
                    Out.append(OutLine);
                }
                executeMessage.setMessage(Out.toString());
            } else {
                System.out.println(optName + "失败 错误码:" + exitValue);
                InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String OutLine;
                StringBuilder Out = new StringBuilder();
                while ((OutLine = bufferedReader.readLine()) != null) {
                    Out.append(OutLine);
                }
                executeMessage.setErrorMessage(Out.toString());
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;
    }

    /**
     * 执行进程获取信息
     *
     * @param process
     * @param optName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process process, String optName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch=new StopWatch();
        try {
            stopWatch.start();
            int exitValue = process.waitFor();
            executeMessage.setExitValue(exitValue);

            if (exitValue == 0) {
                System.out.println(optName + "成功");
                InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String OutLine;
                StringBuilder Out = new StringBuilder();
                while ((OutLine = bufferedReader.readLine()) != null) {
                    Out.append(OutLine);
                }
                executeMessage.setMessage(Out.toString());
            } else {
                System.out.println(optName + "失败 错误码:" + exitValue);
                InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String OutLine;
                StringBuilder Out = new StringBuilder();
                while ((OutLine = bufferedReader.readLine()) != null) {
                    Out.append(OutLine);
                }
                executeMessage.setErrorMessage(Out.toString());
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return executeMessage;
    }
}