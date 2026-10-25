package com.nz.admin.framework.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

    @Test
    void shouldDeleteFilesInsideBaseAndRemainIdempotent() throws IOException {
        Path basePath = Files.createDirectory(tempDir.resolve("storage"));
        LocalFileStorageServiceImpl service = createService(basePath);
        Path filePath = Files.writeString(basePath.resolve("remove.txt"), "payload");
        FileStorageObject file = new FileStorageObject().setFilePath(filePath.toString());

        service.delete(file);
        service.delete(file);

        assertTrue(Files.notExists(filePath));
    }

    @Test
    void shouldRejectDeletingTheBaseDirectoryOrAnyDirectory() throws IOException {
        Path basePath = Files.createDirectory(tempDir.resolve("storage"));
        Path nestedDirectory = Files.createDirectory(basePath.resolve("nested"));
        Path sentinel = Files.writeString(nestedDirectory.resolve("keep.txt"), "keep");
        LocalFileStorageServiceImpl service = createService(basePath);

        assertThrows(IllegalArgumentException.class,
                () -> service.delete(new FileStorageObject().setFilePath(basePath.toString())));
        assertThrows(IllegalArgumentException.class,
                () -> service.delete(new FileStorageObject().setFilePath(nestedDirectory.toString())));

        assertTrue(Files.exists(sentinel));
        assertTrue(Files.isDirectory(nestedDirectory));
    }

    @Test
    void shouldRejectDeletingFilesAndDirectoriesOutsideBase() throws IOException {
        Path basePath = Files.createDirectory(tempDir.resolve("storage"));
        Path outsideFile = Files.writeString(tempDir.resolve("outside.txt"), "keep-file");
        Path outsideDirectory = Files.createDirectory(tempDir.resolve("outside-dir"));
        Path directorySentinel = Files.writeString(outsideDirectory.resolve("keep.txt"), "keep-dir");
        LocalFileStorageServiceImpl service = createService(basePath);

        assertThrows(IllegalArgumentException.class,
                () -> service.delete(new FileStorageObject().setFilePath(outsideFile.toString())));
        assertThrows(IllegalArgumentException.class,
                () -> service.delete(new FileStorageObject().setFilePath(outsideDirectory.toString())));

        assertEquals("keep-file", Files.readString(outsideFile));
        assertEquals("keep-dir", Files.readString(directorySentinel));
    }

    @Test
    void shouldRejectDeletingAnOutsideSymlinkTarget() throws IOException {
        Path basePath = Files.createDirectory(tempDir.resolve("storage"));
        Path outsideFile = Files.writeString(tempDir.resolve("outside.txt"), "keep");
        Path link = basePath.resolve("outside-link.txt");
        try {
            Files.createSymbolicLink(link, outsideFile);
        } catch (UnsupportedOperationException | SecurityException | IOException exception) {
            assumeTrue(false, "当前文件系统不支持创建符号链接");
        }
        LocalFileStorageServiceImpl service = createService(basePath);

        assertThrows(IllegalArgumentException.class,
                () -> service.delete(new FileStorageObject().setFilePath(link.toString())));

        assertTrue(Files.isSymbolicLink(link));
        assertEquals("keep", Files.readString(outsideFile));
    }

    @Test
    void shouldSurfacePhysicalDeleteFailureWithPathContext() throws IOException {
        Path basePath = Files.createDirectory(tempDir.resolve("storage"));
        Path readOnlyDirectory = Files.createDirectory(basePath.resolve("read-only"));
        Path filePath = Files.writeString(readOnlyDirectory.resolve("blocked.txt"), "keep");
        LocalFileStorageServiceImpl service = createService(basePath);
        Set<PosixFilePermission> originalPermissions;
        try {
            originalPermissions = Files.getPosixFilePermissions(readOnlyDirectory);
        } catch (UnsupportedOperationException exception) {
            assumeTrue(false, "当前文件系统不支持 POSIX 权限");
            return;
        }

        try {
            Files.setPosixFilePermissions(readOnlyDirectory,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            assumeTrue(!Files.isWritable(readOnlyDirectory), "当前进程可以绕过只读目录权限");

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.delete(new FileStorageObject().setFilePath(filePath.toString())));
            assertTrue(exception.getMessage().contains(filePath.toString()));
            assertTrue(exception.getCause() instanceof IOException);
            assertTrue(Files.exists(filePath));
        } finally {
            Files.setPosixFilePermissions(readOnlyDirectory, originalPermissions);
        }
    }

    private LocalFileStorageServiceImpl createService(Path basePath) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setBasePath(basePath.toString());
        return new LocalFileStorageServiceImpl(properties, new FileSecurityValidator(properties));
    }

}
