package com.nz.admin.framework.file;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OssFileStorageServiceImplTest {
    private OSS client;
    private OssFileStorageServiceImpl storage;

    @BeforeEach
    void setup() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getOss().setBucketName("test-bucket");
        properties.getOss().setPathPrefix("files");
        client = mock(OSS.class);
        storage = new OssFileStorageServiceImpl(properties, new FileSecurityValidator(properties), () -> client);
    }

    @Test
    void reservesDeterministicTargetBeforeAnyExternalWrite() {
        var file = new MockMultipartFile("file", "sample.txt", "text/plain", "content".getBytes());
        FileStorageObject target = storage.prepareUpload(file, 1L);
        assertThat(target.getFilePath()).startsWith("oss://test-bucket/files/");
        verifyNoInteractions(client);
        assertThatCode(() -> storage.writeUpload(file, target)).doesNotThrowAnyException();
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(request.capture());
        assertThat(request.getValue().getKey()).isEqualTo(target.getFileName());
        verify(client).shutdown();
    }

    @Test
    void downloadStreamsBytesAndClosesObjectAndClient() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        OSSObject object = new OSSObject();
        object.setObjectContent(new ByteArrayInputStream("content".getBytes()) {
            @Override public void close() throws IOException { closed.set(true); super.close(); }
        });
        when(client.getObject("test-bucket", "files/sample.txt")).thenReturn(object);
        MockHttpServletResponse response = new MockHttpServletResponse();
        storage.download(new FileStorageObject().setFileName("files/sample.txt").setOriginalName("sample.txt"), response);
        assertThat(response.getContentAsByteArray()).isEqualTo("content".getBytes());
        assertThat(response.getHeader("Content-Disposition")).contains("attachment;");
        assertThat(closed.get()).isTrue();
        verify(client).shutdown();
    }

    @Test
    void downloadFailureStillClosesClient() {
        when(client.getObject("test-bucket", "missing.txt")).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> storage.download(new FileStorageObject().setFileName("missing.txt"),
                new MockHttpServletResponse())).isInstanceOf(IllegalStateException.class);
        verify(client).shutdown();
    }

    @Test
    void uploadFailureStillClosesClient() {
        when(client.putObject(any(PutObjectRequest.class))).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> storage.upload(new MockMultipartFile("file", "sample.txt", "text/plain", "x".getBytes()), 1L))
                .isInstanceOf(IllegalStateException.class);
        verify(client).shutdown();
    }
}
