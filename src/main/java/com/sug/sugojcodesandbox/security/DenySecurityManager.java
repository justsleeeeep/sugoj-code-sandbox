package com.sug.sugojcodesandbox.security;

import java.security.Permission;

/**
 * 禁用所有的安全管理器
 */
public class DenySecurityManager extends SecurityManager{
    /**
     * 禁用所有权限
     * @param perm   the requested permission.
     */
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限不足"+perm.getActions());
        //super.checkPermission(perm);
    }
}
