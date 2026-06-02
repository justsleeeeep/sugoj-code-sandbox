package com.sug.sugojcodesandbox.security;

import java.security.Permission;

public class MySecurityManager extends SecurityManager{
    @Override
    public void checkPermission(Permission perm) {
        //super.checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec权限不足"+cmd);
        //super.checkExec(cmd);
    }

    @Override
    public void checkRead(String file) {
        //throw new SecurityException("checkRead权限不足"+file);
        super.checkRead(file);
    }

    @Override
    public void checkWrite(String file) {
        throw new SecurityException("checkWrite权限不足"+file);
        //super.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        throw new SecurityException("checkDelete权限不足"+file);
        //super.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("checkConnect权限不足"+host+":"+port);
        //super.checkConnect(host, port);
    }
}
