package com.unitbv.myquiz.app.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileService {

    public static final String UPLOAD_FOLDER = "files";

    Logger logger = LoggerFactory.getLogger(FileService.class);

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    private String rootDir;

    @PostConstruct
    public void initFileServiceImpl() {
        String uploadDir = System.getProperty("upload.dir");
        if (uploadDir == null) {
            logger.atDebug().log("Upload directory is not set - default temp will be used");
            uploadDir = System.getProperty("java.io.tmpdir");
        }
        setRootDir(uploadDir + File.separator + UPLOAD_FOLDER);
        try {
            Files.createDirectories(Path.of(rootDir));
            logger.atInfo().addArgument(getRootDir()).log("Root folder for upload '{}' created");
        } catch (IOException e) {
            logger.atError().addArgument(getRootDir()).log("Could not create the root folder for upload: {}", e);
            throw new IllegalStateException("Could not initialize the root folder for upload", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        initFileServiceImpl();
        String filepath = "not found";
        try {
            filepath = getRootDir() + File.separator + file.getOriginalFilename();
            Files.copy(file.getInputStream(), Paths.get(filepath));
            logger.atInfo().addArgument(filepath).log("File uploaded to: {}");
        } catch (IOException e) {
            logger.atError().addArgument(filepath).log("Error uploading file: {}", e);
        }
        return filepath;
    }

    public void removeFile(String filepath) {
        try {
            Files.deleteIfExists(Paths.get(filepath));
            logger.atInfo().addArgument(filepath).log("File removed from: {}");
        } catch (IOException e) {
            logger.atError().addArgument(filepath).log("Error removing file: {}", e);
        }
    }

    public void removeAllFiles(String dirName) {
        try (var stream = Files.walk(Path.of(dirName))) {
            stream.filter(Files::isRegularFile)
                  .map(Path::toFile)
                  .forEach(File::delete);
            logger.atInfo().addArgument(getRootDir()).log("All files removed from: {}");
        } catch (IOException e) {
            logger.atError().addArgument(getRootDir()).log("Error removing all files: {}", e);
        }
    }

    public void removeUploadRootDir() {
        removeDir(getRootDir());
    }

    public String getFilename(String filepath) {
        int pos = filepath.lastIndexOf(File.separator);
        return filepath.substring(pos + 1);
    }

    public void removeDir(String dirName) {
        FileSystemUtils.deleteRecursively(new File(dirName));
        logger.atInfo().addArgument(getRootDir()).log("Root directory removed: {}");
    }

    @PreDestroy
    public void destroyFileServiceImpl() {
        removeUploadRootDir();
        logger.atInfo().addArgument(getRootDir()).log("Root folder for upload '{}' removed");
    }

    /**
     * Extracts all Excel files from a zip archive, renames them to start with the string before the first underscore of their parent folder,
     * and places them in the destination folder (flat structure).
     * @param zipFilePath Path to the zip archive
     * @param destFolder Destination folder for extracted and renamed Excel files
     * @return number of files extracted
     */
    public int unzipAndRenameExcelFiles(Path zipFilePath, Path destFolder) throws IOException {
        // Prepare inpArchive folder in the same parent as zipFilePath
        Path inpArchive = zipFilePath.getParent().resolve("inpArchive");
        if (Files.exists(inpArchive)) {
            FileSystemUtils.deleteRecursively(inpArchive.toFile());
        }
        Files.createDirectories(inpArchive);
        // Run system unzip command
        try {
            ProcessBuilder pb = new ProcessBuilder("unzip", zipFilePath.toAbsolutePath().toString(), "-d", inpArchive.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.atError().addArgument(zipFilePath.toString()).log("Unzip command failed for: {} (exit code: " + exitCode + ")");
                throw new IOException("Unzip command failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unzip interrupted", e);
        }
        // Ensure destFolder exists
        if (!Files.exists(destFolder)) {
            Files.createDirectories(destFolder);
        }
        AtomicInteger count = new AtomicInteger();
        // Walk inpArchive for Excel files
        try (var excelStream = Files.walk(inpArchive)) {
            excelStream.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".xls") || p.toString().endsWith(".xlsx"))
                .forEach(excelFile -> {
                    try {
                        Path parent = excelFile.getParent();
                        String parentName = parent != null ? parent.getFileName().toString() : "";
                        String partialParent = parentName;
                        int underscoreIdx = parentName.indexOf('_');
                        if (underscoreIdx > 0) {
                            partialParent = parentName.substring(0, underscoreIdx);
                        }
                        String originalFileName = excelFile.getFileName().toString();
                        String newFileName = partialParent.isEmpty() ? originalFileName : partialParent + "_" + originalFileName;
                        Path targetFile = destFolder.resolve(newFileName);
                        int suffix = 1;
                        while (Files.exists(targetFile)) {
                            int dotIdx = newFileName.lastIndexOf('.');
                            String baseName = dotIdx > 0 ? newFileName.substring(0, dotIdx) : newFileName;
                            String ext = dotIdx > 0 ? newFileName.substring(dotIdx) : "";
                            newFileName = baseName + "_" + suffix + ext;
                            targetFile = destFolder.resolve(newFileName);
                            suffix++;
                        }
                        Files.move(excelFile, targetFile);
                        logger.atInfo().addArgument(targetFile.toString()).log("Excel file moved and renamed to: {}");
                        count.getAndIncrement();
                    } catch (IOException e) {
                        logger.atError().addArgument(excelFile.toString()).log("Error moving Excel file: {}", e);
                    }
                });
        } finally {
            // Clean up inpArchive
            FileSystemUtils.deleteRecursively(inpArchive.toFile());
        }
        logger.atInfo().addArgument(count).log("Extracted and renamed {} Excel files from archive");
        return count.get();
    }

}
