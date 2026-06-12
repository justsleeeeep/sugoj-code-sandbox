package com.sug.sugojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import com.sug.sugojcodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final Long TIME_OUT = 10 * 1000L;
    private static final Boolean FIRST_INIT = false;
    private DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    private String containerId = null;
    public static void main(String[] args) {
        JavaDockerCodeSandbox javaDockerCodeSandbox = new JavaDockerCodeSandbox();
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
        javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public List<ExecuteMessage> runCode(List<String> inputList, String userCodeParentPath) {
        //拉取镜像
        String image = "openjdk:17-alpine";
        if (FIRST_INIT)
        {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        //创建容器

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        //hostConfig.withSecurityOpts(Arrays.asList("seccomp=default"));
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = createContainerCmd
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .withCmd("tail", "-f", "/dev/null")
                .exec();
        System.out.println(createContainerResponse);
        containerId = createContainerResponse.getId();
        //启动容器+执行代码
        dockerClient.startContainerCmd(containerId).exec();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String input : inputList)
        {
            final long[] maxMemory = {0L};
            //获取程序内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                private Closeable closeable;
                @Override
                public void onNext(Statistics object) {
                    if (object != null && object.getMemoryStats() != null) {
                        //System.out.println("内存占用:"+object.getMemoryStats().getUsage());
                        Long usage = object.getMemoryStats().getUsage();
                        if (usage != null) {
                            // 动态更新峰值内存
                            maxMemory[0] = Math.max(maxMemory[0], usage);
                        }
                    }
                }
                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {
                    if (closeable != null) {
                        closeable.close();
                    }
                }
            });

            String shellCmd = String.format("echo '%s' | java -cp /app Main", input);

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdin(false)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("sh", "-c", shellCmd)
                    .exec();

            String execId = execCreateCmdResponse.getId();
            ExecuteMessage executeMessage = new ExecuteMessage();
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try (
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    ByteArrayOutputStream errorStream = new ByteArrayOutputStream()
            ) {
                // 执行命令并阻塞等待其完成
                boolean complete= dockerClient.execStartCmd(execId)
                        .withDetach(false)
                        .withTty(false)
                        .exec(new ExecStartResultCallback(outputStream, errorStream))
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                long totalTimeMillis = stopWatch.getTotalTimeMillis();
                statisticsResultCallback.close();
                // 收集当前用例的运行结果
                if(!complete)
                {
                    executeMessage.setExitValue(1); // 标记非正常退出
                    executeMessage.setMessage("");
                    executeMessage.setErrorMessage("Time Limit Exceeded"); // 抛出超时错误
                }
                else
                {
                    executeMessage.setExitValue(0);
                    executeMessage.setTime(totalTimeMillis);
                    executeMessage.setMemory(maxMemory[0]);
                    executeMessage.setMessage(outputStream.toString("UTF-8").trim());
                    executeMessage.setErrorMessage(errorStream.toString("UTF-8").trim());
                }
                executeMessageList.add(executeMessage);

            } catch (Exception e) {
                System.out.println("执行测试用例异常");
                throw new RuntimeException(e);
            }
        }
        for (ExecuteMessage executeMessage : executeMessageList) {
            System.out.println(executeMessage);
        }
        return executeMessageList;
    }

    @Override
    void deleteFile(File userCodeFile, String userCodeParentPath) {
        //5.删除多余的文件+刪除容器
        if (containerId != null) {
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                dockerClient.removeContainerCmd(containerId).exec();
                System.out.println("成功销毁并清理 Docker 容器: " + containerId);
            } catch (Exception e) {
                System.err.println("清理 Docker 容器失败: " + e.getMessage());
            }
            if (userCodeFile != null && userCodeFile.getParentFile() != null) {
                boolean delete = FileUtil.del(userCodeParentPath);
                System.out.println("删除多余文件" + (delete ? "成功" : "失败"));
            }
        }
    }
}
