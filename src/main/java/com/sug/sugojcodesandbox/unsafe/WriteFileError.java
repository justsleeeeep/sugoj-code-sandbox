package com.sug.sugojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WriteFileError
{
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String path=userDir+ File.separator+"src/main/resources/dangerous.bat";
        String errorProgram="java -version 2>&1";
        Files.write(Paths.get(path), errorProgram.getBytes());
        System.out.println("dangerous");
    }
}
