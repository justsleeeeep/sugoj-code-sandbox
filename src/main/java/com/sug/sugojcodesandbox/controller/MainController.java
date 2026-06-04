package com.sug.sugojcodesandbox.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import com.sug.sugojcodesandbox.JavaDockerCodeSandbox;
import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController("/")
public class MainController
{
    @Resource
    JavaDockerCodeSandbox javaDockerCodeSandbox;
    @GetMapping("/health")
    String health()
    {
        return "ok";
    }
    @PostMapping("/executeCode")
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest)
    {
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.executeCode(executeCodeRequest);
        return executeCodeResponse;
    }

}
