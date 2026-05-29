
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String userDir=System.getProperty("user.dir");
        String path=userDir+ File.separator+"src/main/resources/dangerous.bat";
        Process process = Runtime.getRuntime().exec(path);
        System.out.println("run bat");
    }
}
