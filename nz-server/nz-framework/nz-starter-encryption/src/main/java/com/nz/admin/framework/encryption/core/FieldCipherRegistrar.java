package com.nz.admin.framework.encryption.core;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 将 Spring 管理的加密器安装到 MyBatis TypeHandler 可访问的位置。
 */
public class FieldCipherRegistrar implements InitializingBean, DisposableBean {

    private final FieldCipher fieldCipher;

    public FieldCipherRegistrar(FieldCipher fieldCipher) {
        this.fieldCipher = fieldCipher;
    }

    @Override
    public void afterPropertiesSet() {
        FieldCipherHolder.install(fieldCipher);
    }

    @Override
    public void destroy() {
        FieldCipherHolder.clear(fieldCipher);
    }
}
