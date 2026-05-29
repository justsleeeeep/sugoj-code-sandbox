package com.sug.sugojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RunFileError {
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String path=userDir+ File.separator+"src/main/resources/dangerous.bat";
        Runtime.getRuntime().exec(path);
        System.out.println("run bat");
    }
}
