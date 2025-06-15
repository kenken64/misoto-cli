package sg.edu.nus.iss.misoto.cli.fileops;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import sg.edu.nus.iss.misoto.cli.errors.UserError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for FileOperations
 */
@DisplayName("FileOperations Tests")
class FileOperationsTest {

    private FileOperations fileOperations;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileOperations = new FileOperations();
    }

    @Test
    @DisplayName("fileExists should return true for existing files")
    void testFileExistsTrue() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
        
        assertTrue(fileOperations.fileExists(testFile.toString()));
    }

    @Test
    @DisplayName("fileExists should return false for non-existing files")
    void testFileExistsFalse() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        
        assertFalse(fileOperations.fileExists(nonExistentFile.toString()));
    }

    @Test
    @DisplayName("fileExists should return false for directories")
    void testFileExistsForDirectory() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        assertFalse(fileOperations.fileExists(testDir.toString()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("fileExists should return false for invalid paths")
    void testFileExistsInvalidPaths(String path) {
        assertFalse(fileOperations.fileExists(path));
    }

    @Test
    @DisplayName("directoryExists should return true for existing directories")
    void testDirectoryExistsTrue() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        assertTrue(fileOperations.directoryExists(testDir.toString()));
    }

    @Test
    @DisplayName("directoryExists should return false for non-existing directories")
    void testDirectoryExistsFalse() {
        Path nonExistentDir = tempDir.resolve("nonexistent");
        
        assertFalse(fileOperations.directoryExists(nonExistentDir.toString()));
    }

    @Test
    @DisplayName("directoryExists should return false for files")
    void testDirectoryExistsForFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");
        
        assertFalse(fileOperations.directoryExists(testFile.toString()));
    }

    @Test
    @DisplayName("ensureDirectory should create directory if it doesn't exist")
    void testEnsureDirectoryCreate() {
        Path newDir = tempDir.resolve("newdir");
        
        assertFalse(Files.exists(newDir));
        
        fileOperations.ensureDirectory(newDir.toString());
        
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));
    }

    @Test
    @DisplayName("ensureDirectory should create nested directories")
    void testEnsureDirectoryNested() {
        Path nestedDir = tempDir.resolve("level1").resolve("level2").resolve("level3");
        
        assertFalse(Files.exists(nestedDir));
        
        fileOperations.ensureDirectory(nestedDir.toString());
        
        assertTrue(Files.exists(nestedDir));
        assertTrue(Files.isDirectory(nestedDir));
    }

    @Test
    @DisplayName("ensureDirectory should not throw if directory already exists")
    void testEnsureDirectoryExists() throws IOException {
        Path existingDir = tempDir.resolve("existing");
        Files.createDirectory(existingDir);
        
        assertDoesNotThrow(() -> fileOperations.ensureDirectory(existingDir.toString()));
        assertTrue(Files.exists(existingDir));
    }

    @Test
    @DisplayName("ensureDirectory should throw for empty path")
    void testEnsureDirectoryEmptyPath() {
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.ensureDirectory("")
        );
        assertEquals("Directory path cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("readTextFile should read file content with UTF-8")
    void testReadTextFileUtf8() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World! 🌍";
        Files.writeString(testFile, content, StandardCharsets.UTF_8);
        
        String result = fileOperations.readTextFile(testFile.toString());
        
        assertEquals(content, result);
    }

    @Test
    @DisplayName("readTextFile should read file with specified encoding")
    void testReadTextFileWithEncoding() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content, StandardCharsets.ISO_8859_1);
        
        String result = fileOperations.readTextFile(testFile.toString(), StandardCharsets.ISO_8859_1);
        
        assertEquals(content, result);
    }

    @Test
    @DisplayName("readTextFile should throw for non-existent file")
    void testReadTextFileNotFound() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.readTextFile(nonExistentFile.toString())
        );
        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    @DisplayName("readTextFile should throw for directory")
    void testReadTextFileDirectory() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.readTextFile(testDir.toString())
        );
        assertTrue(exception.getMessage().contains("not a regular file"));
    }

    @Test
    @DisplayName("readTextFile should throw for empty path")
    void testReadTextFileEmptyPath() {
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.readTextFile("")
        );
        assertEquals("File path cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("readFileLines should read specific lines from file")
    void testReadFileLines() throws IOException {
        Path testFile = tempDir.resolve("multiline.txt");
        String content = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";
        Files.writeString(testFile, content);
        
        List<String> lines = fileOperations.readFileLines(testFile.toString(), 2, 4);
        
        assertEquals(List.of("Line 2", "Line 3", "Line 4"), lines);
    }

    @Test
    @DisplayName("readFileLines should handle out-of-bounds line numbers")
    void testReadFileLinesOutOfBounds() throws IOException {
        Path testFile = tempDir.resolve("short.txt");
        String content = "Line 1\nLine 2";
        Files.writeString(testFile, content);
        
        List<String> lines = fileOperations.readFileLines(testFile.toString(), 1, 10);
        
        assertEquals(List.of("Line 1", "Line 2"), lines);
    }

    @Test
    @DisplayName("readFileLines should return empty list for invalid range")
    void testReadFileLinesInvalidRange() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Line 1\nLine 2");
        
        List<String> lines = fileOperations.readFileLines(testFile.toString(), 5, 3);
        
        assertTrue(lines.isEmpty());
    }

    @Test
    @DisplayName("writeTextFile should create new file with content")
    void testWriteTextFile() throws IOException {
        Path testFile = tempDir.resolve("new.txt");
        String content = "Test content";
        
        fileOperations.writeTextFile(testFile.toString(), content);
        
        assertTrue(Files.exists(testFile));
        assertEquals(content, Files.readString(testFile));
    }

    @Test
    @DisplayName("writeTextFile should create directories if needed")
    void testWriteTextFileCreateDir() throws IOException {
        Path nestedFile = tempDir.resolve("subdir").resolve("test.txt");
        String content = "Test content";
        
        fileOperations.writeTextFile(nestedFile.toString(), content);
        
        assertTrue(Files.exists(nestedFile));
        assertEquals(content, Files.readString(nestedFile));
    }

    @Test
    @DisplayName("writeTextFile should overwrite existing file by default")
    void testWriteTextFileOverwrite() throws IOException {
        Path testFile = tempDir.resolve("existing.txt");
        Files.writeString(testFile, "Original content");
        
        String newContent = "New content";
        fileOperations.writeTextFile(testFile.toString(), newContent);
        
        assertEquals(newContent, Files.readString(testFile));
    }

    @Test
    @DisplayName("writeTextFile with custom options should respect settings")
    void testWriteTextFileWithOptions() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Original");
        
        WriteOptions options = WriteOptions.builder()
            .overwrite(false)
            .build();
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.writeTextFile(testFile.toString(), "New content", options)
        );
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("writeTextFile should handle null content")
    void testWriteTextFileNullContent() throws IOException {
        Path testFile = tempDir.resolve("null.txt");
        
        fileOperations.writeTextFile(testFile.toString(), null);
        
        assertTrue(Files.exists(testFile));
        assertEquals("", Files.readString(testFile));
    }

    @Test
    @DisplayName("appendTextFile should add content to existing file")
    void testAppendTextFile() throws IOException {
        Path testFile = tempDir.resolve("append.txt");
        Files.writeString(testFile, "Original");
        
        fileOperations.appendTextFile(testFile.toString(), " Appended");
        
        assertEquals("Original Appended", Files.readString(testFile));
    }

    @Test
    @DisplayName("appendTextFile should create file if it doesn't exist")
    void testAppendTextFileNewFile() throws IOException {
        Path testFile = tempDir.resolve("new.txt");
        String content = "New content";
        
        fileOperations.appendTextFile(testFile.toString(), content);
        
        assertTrue(Files.exists(testFile));
        assertEquals(content, Files.readString(testFile));
    }

    @Test
    @DisplayName("deleteFile should remove existing file")
    void testDeleteFile() throws IOException {
        Path testFile = tempDir.resolve("delete.txt");
        Files.writeString(testFile, "content");
        
        assertTrue(Files.exists(testFile));
        
        fileOperations.deleteFile(testFile.toString());
        
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("deleteFile should not throw if file doesn't exist")
    void testDeleteFileNotExists() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        
        assertDoesNotThrow(() -> fileOperations.deleteFile(nonExistentFile.toString()));
    }

    @Test
    @DisplayName("deleteFile should throw for directory")
    void testDeleteFileDirectory() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.deleteFile(testDir.toString())
        );
        assertTrue(exception.getMessage().contains("not a regular file"));
    }

    @Test
    @DisplayName("rename should move file successfully")
    void testRename() throws IOException {
        Path oldFile = tempDir.resolve("old.txt");
        Path newFile = tempDir.resolve("new.txt");
        String content = "Test content";
        Files.writeString(oldFile, content);
        
        fileOperations.rename(oldFile.toString(), newFile.toString());
        
        assertFalse(Files.exists(oldFile));
        assertTrue(Files.exists(newFile));
        assertEquals(content, Files.readString(newFile));
    }

    @Test
    @DisplayName("rename should throw for non-existent source")
    void testRenameNonExistentSource() {
        Path oldFile = tempDir.resolve("nonexistent.txt");
        Path newFile = tempDir.resolve("new.txt");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.rename(oldFile.toString(), newFile.toString())
        );
        assertTrue(exception.getMessage().contains("Source path not found"));
    }

    @Test
    @DisplayName("copyFile should duplicate file successfully")
    void testCopyFile() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Path destFile = tempDir.resolve("dest.txt");
        String content = "Test content";
        Files.writeString(sourceFile, content);
        
        fileOperations.copyFile(sourceFile.toString(), destFile.toString());
        
        assertTrue(Files.exists(sourceFile));
        assertTrue(Files.exists(destFile));
        assertEquals(content, Files.readString(destFile));
    }

    @Test
    @DisplayName("copyFile should create destination directory if needed")
    void testCopyFileCreateDir() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Path destFile = tempDir.resolve("subdir").resolve("dest.txt");
        String content = "Test content";
        Files.writeString(sourceFile, content);
        
        fileOperations.copyFile(sourceFile.toString(), destFile.toString());
        
        assertTrue(Files.exists(destFile));
        assertEquals(content, Files.readString(destFile));
    }

    @Test
    @DisplayName("copyFile should throw for non-existent source")
    void testCopyFileNonExistentSource() {
        Path sourceFile = tempDir.resolve("nonexistent.txt");
        Path destFile = tempDir.resolve("dest.txt");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.copyFile(sourceFile.toString(), destFile.toString())
        );
        assertTrue(exception.getMessage().contains("Source file not found"));
    }

    @Test
    @DisplayName("copyFile should throw for directory source")
    void testCopyFileDirectorySource() throws IOException {
        Path sourceDir = tempDir.resolve("sourcedir");
        Path destFile = tempDir.resolve("dest.txt");
        Files.createDirectory(sourceDir);
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.copyFile(sourceDir.toString(), destFile.toString())
        );
        assertTrue(exception.getMessage().contains("not a regular file"));
    }

    @Test
    @DisplayName("listDirectory should return sorted file list")
    void testListDirectory() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        Files.writeString(testDir.resolve("file2.txt"), "content");
        Files.writeString(testDir.resolve("file1.txt"), "content");
        Files.createDirectory(testDir.resolve("subdir"));
        
        List<String> files = fileOperations.listDirectory(testDir.toString());
        
        assertEquals(List.of("file1.txt", "file2.txt", "subdir"), files);
    }

    @Test
    @DisplayName("listDirectory should throw for non-existent directory")
    void testListDirectoryNotFound() {
        Path nonExistentDir = tempDir.resolve("nonexistent");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.listDirectory(nonExistentDir.toString())
        );
        assertTrue(exception.getMessage().contains("Directory not found"));
    }

    @Test
    @DisplayName("listDirectory should throw for file path")
    void testListDirectoryForFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.listDirectory(testFile.toString())
        );
        assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    @DisplayName("findFiles should find files recursively")
    void testFindFilesRecursive() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        Path subDir = testDir.resolve("subdir");
        Files.createDirectory(subDir);
        
        Files.writeString(testDir.resolve("file1.txt"), "content");
        Files.writeString(subDir.resolve("file2.txt"), "content");
        
        FindOptions options = FindOptions.builder()
            .recursive(true)
            .includeDirectories(false)
            .build();
        
        List<String> files = fileOperations.findFiles(testDir.toString(), options);
        
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(f -> f.endsWith("file1.txt")));
        assertTrue(files.stream().anyMatch(f -> f.endsWith("file2.txt")));
    }

    @Test
    @DisplayName("findFiles should include directories when requested")
    void testFindFilesIncludeDirectories() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        Path subDir = testDir.resolve("subdir");
        Files.createDirectory(subDir);
        
        Files.writeString(testDir.resolve("file.txt"), "content");
        
        FindOptions options = FindOptions.builder()
            .recursive(true)
            .includeDirectories(true)
            .build();
        
        List<String> files = fileOperations.findFiles(testDir.toString(), options);
        
        assertTrue(files.size() >= 2); // At least file and subdir
        assertTrue(files.stream().anyMatch(f -> f.endsWith("file.txt")));
        assertTrue(files.stream().anyMatch(f -> f.endsWith("subdir")));
    }

    @Test
    @DisplayName("findFiles should respect non-recursive option")
    void testFindFilesNonRecursive() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        Path subDir = testDir.resolve("subdir");
        Files.createDirectory(subDir);
        
        Files.writeString(testDir.resolve("file1.txt"), "content");
        Files.writeString(subDir.resolve("file2.txt"), "content");
        
        FindOptions options = FindOptions.builder()
            .recursive(false)
            .includeDirectories(false)
            .build();
        
        List<String> files = fileOperations.findFiles(testDir.toString(), options);
        
        assertEquals(1, files.size());
        assertTrue(files.get(0).endsWith("file1.txt"));
    }

    @Test
    @DisplayName("getFileSize should return correct file size")
    void testGetFileSize() throws IOException {
        Path testFile = tempDir.resolve("size.txt");
        String content = "Hello, World!";
        Files.writeString(testFile, content);
        
        long size = fileOperations.getFileSize(testFile.toString());
        
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, size);
    }

    @Test
    @DisplayName("getFileSize should throw for non-existent file")
    void testGetFileSizeNotFound() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.getFileSize(nonExistentFile.toString())
        );
        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    @DisplayName("getFileSize should throw for directory")
    void testGetFileSizeDirectory() throws IOException {
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        
        UserError exception = assertThrows(
            UserError.class,
            () -> fileOperations.getFileSize(testDir.toString())
        );
        assertTrue(exception.getMessage().contains("not a regular file"));
    }

    @Test
    @DisplayName("operations should handle Windows-style paths")
    void testWindowsPaths() throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            // Skip this test on non-Windows systems
            return;
        }
        
        Path testFile = tempDir.resolve("windows.txt");
        Files.writeString(testFile, "content");
        
        // Test with backslashes (Windows style)
        String windowsPath = testFile.toString().replace('/', '\\');
        
        assertTrue(fileOperations.fileExists(windowsPath));
        assertEquals("content", fileOperations.readTextFile(windowsPath));
    }

    @Test
    @DisplayName("operations should handle special characters in paths")
    void testSpecialCharactersInPaths() throws IOException {
        Path testFile = tempDir.resolve("test file with spaces & symbols.txt");
        String content = "Test content";
        Files.writeString(testFile, content);
        
        assertTrue(fileOperations.fileExists(testFile.toString()));
        assertEquals(content, fileOperations.readTextFile(testFile.toString()));
    }

    @Test
    @DisplayName("operations should handle very long file paths")
    void testLongFilePaths() throws IOException {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longName.append("verylongfilename");
        }
        
        Path testFile = tempDir.resolve(longName.toString().substring(0, 200) + ".txt");
        String content = "Test content";
        
        // This might fail on some filesystems due to path length limits
        try {
            Files.writeString(testFile, content);
            assertTrue(fileOperations.fileExists(testFile.toString()));
            assertEquals(content, fileOperations.readTextFile(testFile.toString()));
        } catch (Exception e) {
            // Expected on some filesystems - this is actually correct behavior
            assertTrue(e instanceof IOException || e instanceof UserError);
        }
    }
}
