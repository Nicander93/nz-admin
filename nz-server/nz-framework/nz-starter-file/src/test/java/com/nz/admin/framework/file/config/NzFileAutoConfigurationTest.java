package com.nz.admin.framework.file.config;

import com.nz.admin.framework.file.FileStorageProperties;
import com.nz.admin.framework.file.FileSecurityValidator;
import com.nz.admin.framework.file.LocalFileStorageServiceImpl;
import com.nz.admin.framework.file.OssFileStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NzFileAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NzFileAutoConfiguration.class));

    @Test
    void shouldBindPropertiesAndRegisterStorageBeans() {
        contextRunner.withPropertyValues(
                        "nz.file.storage-type=oss",
                        "nz.file.base-path=/tmp/nz-upload",
                        "nz.file.max-file-size-bytes=2048",
                        "nz.file.oss.bucket-name=test-bucket"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorageProperties.class);
                    assertThat(context).hasSingleBean(FileSecurityValidator.class);
                    assertThat(context).hasBean("localFileStorageService");
                    assertThat(context).hasBean("ossFileStorageService");
                    assertThat(context.getBean("localFileStorageService")).isInstanceOf(LocalFileStorageServiceImpl.class);
                    assertThat(context.getBean("ossFileStorageService")).isInstanceOf(OssFileStorageServiceImpl.class);

                    FileStorageProperties properties = context.getBean(FileStorageProperties.class);
                    assertThat(properties.getStorageType()).isEqualTo("oss");
                    assertThat(properties.getBasePath()).isEqualTo("/tmp/nz-upload");
                    assertThat(properties.getMaxFileSizeBytes()).isEqualTo(2048L);
                    assertThat(properties.getOss().getBucketName()).isEqualTo("test-bucket");
                });
    }

    @Test
    void shouldBackOffWhenCustomLocalStorageBeanExists() {
        contextRunner.withBean("localFileStorageService", LocalFileStorageServiceImpl.class,
                        () -> new LocalFileStorageServiceImpl(new FileStorageProperties(),
                                new FileSecurityValidator(new FileStorageProperties())))
                .run(context -> assertThat(context.getBeansOfType(LocalFileStorageServiceImpl.class)).hasSize(1));
    }
}
