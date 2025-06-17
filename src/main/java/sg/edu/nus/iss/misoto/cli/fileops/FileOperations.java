package sg.edu.nus.iss.misoto.cli.fileops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.misoto.cli.errors.UserError;
import sg.edu.nus.iss.misoto.cli.utils.ValidationUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

/**
 * File Operations
 * 
 * Functions for interacting with the file system in a safe and consistent way.
 * Includes utilities for reading, writing, searching, and analyzing files.
 */
@Service
@Slf4j
public class FileOperations {
    
    /**
     * Check if a file exists
     */
    public boolean fileExists(String filePath) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(filePath);
            return Files.exists(path) && Files.isRegularFile(path);
        } catch (InvalidPathException e) {
            log.debug("Invalid path: {}", filePath);
            return false;
        }
    }
    
    /**
     * Check if a directory exists
     */
    public boolean directoryExists(String dirPath) {
        if (!ValidationUtil.isNonEmptyString(dirPath)) {
            return false;
        }
        
        try {
            Path path = Paths.get(dirPath);
            return Files.exists(path) && Files.isDirectory(path);
        } catch (InvalidPathException e) {
            log.debug("Invalid directory path: {}", dirPath);
            return false;
        }
    }
    
    /**
     * Create a directory if it doesn't exist
     */
    public void ensureDirectory(String dirPath) {
        if (!ValidationUtil.isNonEmptyString(dirPath)) {
            throw new UserError("Directory path cannot be empty");
        }
        
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.debug("Created directory: {}", dirPath);
            }
        } catch (InvalidPathException e) {
            throw new UserError("Invalid directory path: " + dirPath);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", dirPath, e);
            throw new UserError("Failed to create directory: " + dirPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Read a file as text with UTF-8 encoding
     */
    public String readTextFile(String filePath) {
        return readTextFile(filePath, StandardCharsets.UTF_8);
    }
    
    /**
     * Read a file as text with specified encoding
     */
    public String readTextFile(String filePath, Charset encoding) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new UserError("File not found: " + filePath);
            }
            
            if (!Files.isRegularFile(path)) {
                throw new UserError("Path is not a regular file: " + filePath);
            }
            
            return Files.readString(path, encoding);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + filePath);
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                throw new UserError("File not found: " + filePath);
            } else if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to file: " + filePath);
            }
            
            log.error("Failed to read file: {}", filePath, e);
            throw new UserError("Failed to read file: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * Read specific lines from a file (1-indexed)
     */
    public List<String> readFileLines(String filePath, int start, int end) {
        return readFileLines(filePath, start, end, StandardCharsets.UTF_8);
    }
    
    /**
     * Read specific lines from a file with specified encoding (1-indexed)
     */
    public List<String> readFileLines(String filePath, int start, int end, Charset encoding) {
        try {
            String content = readTextFile(filePath, encoding);
            List<String> lines = List.of(content.split("\\r?\\n"));
            
            // Convert from 1-indexed to 0-indexed
            int startIndex = Math.max(0, start - 1);
            int endIndex = Math.min(lines.size(), end);
            
            if (startIndex >= endIndex) {
                return List.of();
            }
            
            return lines.subList(startIndex, endIndex);
            
        } catch (Exception e) {
            throw new UserError("Failed to read lines " + start + "-" + end + " from file: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * Write text to a file with UTF-8 encoding
     */
    public void writeTextFile(String filePath, String content) {
        writeTextFile(filePath, content, new WriteOptions());
    }
    
    /**
     * Write text to a file with options
     */
    public void writeTextFile(String filePath, String content, WriteOptions options) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        if (content == null) content = "";
        
        try {
            Path path = Paths.get(filePath);
            
            // Ensure directory exists if createDir is true
            if (options.isCreateDir()) {
                Path parent = path.getParent();
                if (parent != null) {
                    ensureDirectory(parent.toString());
                }
            }
            
            // Check if file exists and overwrite is false
            if (Files.exists(path) && !options.isOverwrite()) {
                throw new UserError("File already exists: " + filePath);
            }
            
            // Write the file
            Files.writeString(path, content, options.getEncoding(),
                StandardOpenOption.CREATE,
                options.isOverwrite() ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW);
                
            log.debug("Wrote {} bytes to: {}", content.length(), filePath);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + filePath);
        } catch (FileAlreadyExistsException e) {
            throw new UserError("File already exists: " + filePath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to file: " + filePath);
            }
            
            log.error("Failed to write file: {}", filePath, e);
            throw new UserError("Failed to write file: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * Append text to a file with UTF-8 encoding
     */
    public void appendTextFile(String filePath, String content) {
        appendTextFile(filePath, content, new WriteOptions());
    }
    
    /**
     * Append text to a file with options
     */
    public void appendTextFile(String filePath, String content, WriteOptions options) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        if (content == null) content = "";
        
        try {
            Path path = Paths.get(filePath);
            
            // Ensure directory exists if createDir is true
            if (options.isCreateDir()) {
                Path parent = path.getParent();
                if (parent != null) {
                    ensureDirectory(parent.toString());
                }
            }
            
            // Append to the file
            Files.writeString(path, content, options.getEncoding(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            log.debug("Appended {} bytes to: {}", content.length(), filePath);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + filePath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to file: " + filePath);
            }
            
            log.error("Failed to append to file: {}", filePath, e);
            throw new UserError("Failed to append to file: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * Delete a file
     */
    public void deleteFile(String filePath) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                log.debug("File does not exist, nothing to delete: {}", filePath);
                return;
            }
            
            if (!Files.isRegularFile(path)) {
                throw new UserError("Path is not a regular file: " + filePath);
            }
            
            Files.delete(path);
            log.debug("Deleted file: {}", filePath);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + filePath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to file: " + filePath);
            }
            
            log.error("Failed to delete file: {}", filePath, e);
            throw new UserError("Failed to delete file: " + filePath + " - " + e.getMessage());
        }
    }
    
    /**
     * Rename a file or directory
     */
    public void rename(String oldPath, String newPath) {
        if (!ValidationUtil.isNonEmptyString(oldPath) || !ValidationUtil.isNonEmptyString(newPath)) {
            throw new UserError("File paths cannot be empty");
        }
        
        try {
            Path source = Paths.get(oldPath);
            Path target = Paths.get(newPath);
            
            if (!Files.exists(source)) {
                throw new UserError("Source path not found: " + oldPath);
            }
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Renamed: {} -> {}", oldPath, newPath);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid path: " + (!ValidationUtil.isNonEmptyString(oldPath) ? oldPath : newPath));
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied when renaming: " + oldPath + " -> " + newPath);
            }
            
            log.error("Failed to rename: {} -> {}", oldPath, newPath, e);
            throw new UserError("Failed to rename: " + oldPath + " -> " + newPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Copy a file
     */
    public void copyFile(String sourcePath, String destPath) {
        copyFile(sourcePath, destPath, new CopyOptions());
    }
    
    /**
     * Copy a file with options
     */
    public void copyFile(String sourcePath, String destPath, CopyOptions options) {
        if (!ValidationUtil.isNonEmptyString(sourcePath) || !ValidationUtil.isNonEmptyString(destPath)) {
            throw new UserError("File paths cannot be empty");
        }
        
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(destPath);
            
            if (!Files.exists(source)) {
                throw new UserError("Source file not found: " + sourcePath);
            }
            
            if (!Files.isRegularFile(source)) {
                throw new UserError("Source path is not a regular file: " + sourcePath);
            }
            
            // Ensure directory exists if createDir is true
            if (options.isCreateDir()) {
                Path parent = target.getParent();
                if (parent != null) {
                    ensureDirectory(parent.toString());
                }
            }
            
            // Set copy options
            StandardCopyOption[] copyOptions = options.isOverwrite() 
                ? new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                : new StandardCopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
            
            Files.copy(source, target, copyOptions);
            log.debug("Copied file: {} -> {}", sourcePath, destPath);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + (!ValidationUtil.isNonEmptyString(sourcePath) ? sourcePath : destPath));
        } catch (FileAlreadyExistsException e) {
            throw new UserError("Destination file already exists: " + destPath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied when copying: " + sourcePath + " -> " + destPath);
            }
            
            log.error("Failed to copy file: {} -> {}", sourcePath, destPath, e);
            throw new UserError("Failed to copy file: " + sourcePath + " -> " + destPath + " - " + e.getMessage());
        }
    }
    
    /**
     * List files and directories in a directory
     */
    public List<String> listDirectory(String dirPath) {
        if (!ValidationUtil.isNonEmptyString(dirPath)) {
            throw new UserError("Directory path cannot be empty");
        }
        
        try {
            Path path = Paths.get(dirPath);
            
            if (!Files.exists(path)) {
                throw new UserError("Directory not found: " + dirPath);
            }
            
            if (!Files.isDirectory(path)) {
                throw new UserError("Path is not a directory: " + dirPath);
            }
            
            try (Stream<Path> paths = Files.list(path)) {
                return paths.map(p -> p.getFileName().toString())
                           .sorted()
                           .toList();
            }
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid directory path: " + dirPath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to directory: " + dirPath);
            }
            
            log.error("Failed to list directory: {}", dirPath, e);
            throw new UserError("Failed to list directory: " + dirPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Find files recursively in a directory
     */
    public List<String> findFiles(String dirPath, FindOptions options) {
        if (!ValidationUtil.isNonEmptyString(dirPath)) {
            throw new UserError("Directory path cannot be empty");
        }
        
        try {
            Path startPath = Paths.get(dirPath);
            
            if (!Files.exists(startPath)) {
                throw new UserError("Directory not found: " + dirPath);
            }
            
            if (!Files.isDirectory(startPath)) {
                throw new UserError("Path is not a directory: " + dirPath);
            }
            
            int maxDepth = options.isRecursive() ? Integer.MAX_VALUE : 1;
            
            try (Stream<Path> paths = Files.walk(startPath, maxDepth)) {
                return paths.filter(path -> {
                        // Filter by type
                        if (options.isIncludeDirectories() && Files.isDirectory(path)) {
                            return true;
                        }
                        if (Files.isRegularFile(path)) {
                            return true;
                        }
                        return false;
                    })
                    .filter(path -> !path.equals(startPath)) // Exclude the root directory itself
                    .map(Path::toString)
                    .sorted()
                    .toList();
            }
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid directory path: " + dirPath);
        } catch (IOException e) {
            if (e instanceof AccessDeniedException) {
                throw new UserError("Access denied to directory: " + dirPath);
            }
            
            log.error("Failed to find files in directory: {}", dirPath, e);
            throw new UserError("Failed to find files in directory: " + dirPath + " - " + e.getMessage());
        }
    }
    
    /**
     * Get file size
     */
    public long getFileSize(String filePath) {
        if (!ValidationUtil.isNonEmptyString(filePath)) {
            throw new UserError("File path cannot be empty");
        }
        
        try {
            Path path = Paths.get(filePath);
            
            if (!Files.exists(path)) {
                throw new UserError("File not found: " + filePath);
            }
            
            if (!Files.isRegularFile(path)) {
                throw new UserError("Path is not a regular file: " + filePath);
            }
            
            return Files.size(path);
            
        } catch (InvalidPathException e) {
            throw new UserError("Invalid file path: " + filePath);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", filePath, e);
            throw new UserError("Failed to get file size: " + filePath + " - " + e.getMessage());
        }
    }
}
