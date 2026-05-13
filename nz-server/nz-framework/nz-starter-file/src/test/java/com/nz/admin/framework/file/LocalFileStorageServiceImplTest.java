package com.nz.admin.framework.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadFileIntoBasePath() throws IOException {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBasePath(tempDir.toString());
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(properties, new FileSecurityValidator(properties));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "hello".getBytes()
        );

        FileStorageObject storageObject = service.upload(file, 100L);

        assertEquals("avatar.png", storageObject.getOriginalName());
        assertEquals("png", storageObject.getFileExt());
        assertEquals(5L, storageObject.getFileSize());
        assertEquals(100L, storageObject.getUploaderId());
        assertTrue(storageObject.getFilePath().startsWith(tempDir.toString()));
        assertTrue(Files.exists(Path.of(storageObject.getFilePath())));
    }

    @Test
    void shouldDownloadFileWhenPathIsInsideBasePath() throws IOException {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBasePath(tempDir.toString());
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(properties, new FileSecurityValidator(properties));

        Path file = Files.writeString(tempDir.resolve("demo.txt"), "demo");
        FileStorageObject storageObject = new FileStorageObject()
                .setFileName("demo.txt")
                .setOriginalName("报告.txt")
                .setFilePath(file.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();
        service.download(storageObject, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("%E6%8A%A5%E5%91%8A.txt"));
        assertArrayEquals("demo".getBytes(), response.getContentAsByteArray());
    }

    @Test
    void shouldRejectDownloadWhenPathEscapesBasePath() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBasePath(tempDir.toString());
        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(properties, new FileSecurityValidator(properties));

        FileStorageObject storageObject = new FileStorageObject()
                .setFileName("evil.txt")
                .setFilePath(tempDir.getParent().resolve("evil.txt").toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> service.download(storageObject, response));
        assertEquals("非法文件路径", exception.getMessage());
    }
}
