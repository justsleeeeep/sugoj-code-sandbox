

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String  path=userDir+ File.separator+"src/main/resources/application.yml";
        List<String> allLines = Files.readAllLines(Path.of(path));
        System.out.println(allLines);
    }
}
