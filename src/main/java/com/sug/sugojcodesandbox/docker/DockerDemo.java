package com.sug.sugojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
//        //测试连接
//        PingCmd pingCmd=dockerClient.pingCmd();
//        pingCmd.exec();
//        //拉取镜像
        String image="nginx:latest";
//        PullImageCmd pullImageCmd =dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback();
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        //创建容器
        CreateContainerCmd createContainerCmd=dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse= createContainerCmd
                .withCmd("echo","Hello Docker")
                .exec();
        System.out.println(createContainerResponse);
        String containerId=createContainerResponse.getId();
        //查看容器状态
        ListContainersCmd listContainersCmd= dockerClient.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        for(Container container:containerList)
        {
            System.out.println(container);
        }
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();
        //查看日志
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback(){
            @Override
            public void onNext(Frame item) {
                System.out.println("日志："+new String(item.getPayload()));
                super.onNext(item);
            }
        };
        dockerClient.logContainerCmd(containerId)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();
        //删除容器
        dockerClient.removeContainerCmd(containerId).exec();
    }
}
