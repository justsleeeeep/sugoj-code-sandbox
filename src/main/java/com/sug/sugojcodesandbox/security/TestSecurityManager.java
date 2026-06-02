package com.sug.sugojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.Charset;

public class TestSecurityManager {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
        FileUtil.writeString("aaa","aa",Charset.defaultCharset());
        //FileUtil.readLines("D:\\LearningProject\\sugoj-code-sandbox\\src\\main\\resources\\application.yml", Charset.defaultCharset());
    }
}
