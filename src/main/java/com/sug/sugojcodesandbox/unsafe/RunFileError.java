package com.sug.sugojcodesandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class RunFileError {
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String path=userDir+ File.separator+"src/main/resources/dangerous.bat";
        Process process = Runtime.getRuntime().exec(path);
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String OutLine;
        StringBuilder Out = new StringBuilder();
        while ((OutLine = bufferedReader.readLine()) != null) {
            Out.append(OutLine+"\n");
        }
        System.out.println(Out);
        System.out.println("run bat");
    }
}
