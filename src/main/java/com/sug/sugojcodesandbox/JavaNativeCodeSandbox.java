package com.sug.sugojcodesandbox;

import com.sug.sugojcodesandbox.model.ExecuteCodeRequest;
import com.sug.sugojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
