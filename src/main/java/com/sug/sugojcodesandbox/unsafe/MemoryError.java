package com.sug.sugojcodesandbox.unsafe;

import java.util.ArrayList;
import java.util.List;

public class MemoryError {
    public static void main(String[] args) {
        List<byte[]> bytes=new ArrayList<>();
        while(true)
        {
            bytes.add(new byte[10000]);
        }
    }

}
