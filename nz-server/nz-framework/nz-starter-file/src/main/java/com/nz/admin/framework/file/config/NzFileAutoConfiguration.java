package com.nz.admin.framework.file.config;

import com.nz.admin.framework.file.FileConfigSecretCodec;
import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileSecurityValidator;
import com.nz.admin.framework.file.LocalFileStorageServiceImpl;
import com.nz.admin.framework.file.MinioFileStorageServiceImpl;
import com.nz.admin.framework.file.OssFileStorageServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 文件存储自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class NzFileAutoConfiguration {

    @Bean("localFileStorageService")
    @ConditionalOnMissingBean(name = "localFileStorageService")
    public LocalFileStorageServiceImpl localFileStorageService(FileStorageProperties properties,
                                                               FileSecurityValidator fileSecurityValidator) {
        return new LocalFileStorageServiceImpl(properties, fileSecurityValidator);
    }

    @Bean("ossFileStorageService")
    @ConditionalOnMissingBean(name = "ossFileStorageService")
    public OssFileStorageServiceImpl ossFileStorageService(FileStorageProperties properties,
                                                           FileSecurityValidator fileSecurityValidator) {
        return new OssFileStorageServiceImpl(properties, fileSecurityValidator);
    }
    @Bean("s3FileStorageService")
    @ConditionalOnMissingBean(name = "s3FileStorageService")
    public MinioFileStorageServiceImpl s3FileStorageService(FileStorageProperties properties,
                                                            FileSecurityValidator fileSecurityValidator) {
        return new MinioFileStorageServiceImpl(properties, fileSecurityValidator);
    }


    @Bean
    @ConditionalOnMissingBean
    public FileConfigSecretCodec fileConfigSecretCodec(FileStorageProperties properties) {
        return new FileConfigSecretCodec(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileSecurityValidator fileSecurityValidator(FileStorageProperties properties) {
        return new FileSecurityValidator(properties);
    }
}
