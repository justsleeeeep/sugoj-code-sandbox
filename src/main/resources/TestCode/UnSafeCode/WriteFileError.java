

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main
{
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String path=userDir+ File.separator+"src/main/resources/dangerous.bat";
        String errorProgram="java -version 2>&1";
        Files.write(Path.of(path), errorProgram.getBytes());
        System.out.println("dangerous");
    }
}
