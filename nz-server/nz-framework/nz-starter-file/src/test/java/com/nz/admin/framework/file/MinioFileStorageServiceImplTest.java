package com.nz.admin.framework.file;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioFileStorageServiceImplTest {

    private FileStorageProperties properties;
    private MinioClient client;
    private MinioFileStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.getS3().setEndpoint("http://127.0.0.1:9000");
        properties.getS3().setAccessKeyId("access-key");
        properties.getS3().setAccessKeySecret("secret-key");
        properties.getS3().setBucketName("nz-admin");
        properties.getS3().setPathPrefix("uploads");
        client = mock(MinioClient.class);
        storageService = new MinioFileStorageServiceImpl(
                properties, new FileSecurityValidator(properties), () -> client);
    }

    @Test
    void uploadsObjectWithConfiguredPrefix() throws Exception {
        var file = new MockMultipartFile("file", "avatar.png", "image/png", "image".getBytes());

        FileStorageObject result = storageService.upload(file, 7L);

        assertThat(result.getFileName()).startsWith("uploads/").endsWith(".png");
        assertThat(result.getFilePath()).startsWith("s3://nz-admin/uploads/");
        assertThat(result.getUploaderId()).isEqualTo(7L);
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void checksConfiguredBucket() throws Exception {
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.checkAvailable();

        verify(client).bucketExists(any(BucketExistsArgs.class));
    }
}
