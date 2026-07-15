package com.unitbv.myquiz.app.services;

import com.unitbv.myquiz.app.util.FileValidator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class FileService {

    public static final String UPLOAD_FOLDER = "files";

    private final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Value("${upload.dir:}")
    private String configuredUploadDir;

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    private String rootDir;

    @PostConstruct
    public void initFileServiceImpl() {
        Path uploadPath = resolveUploadPath();
        setRootDir(uploadPath.toString());
        try {
            Files.createDirectories(uploadPath);
            logger.atInfo().addArgument(getRootDir()).log("Root folder for upload '{}' created");
        } catch (IOException e) {
            logger.atError()
                  .addArgument(getRootDir())
                  .setCause(e)
                  .log("Could not create the root folder for upload: {}");
            throw new IllegalStateException("Could not initialize the root folder for upload", e);
        }
    }

    /**
     * Saves a multipart file to the upload root directory and returns the absolute path.
     * <p>
     * The original filename may contain directory components when the file originates from a
     * {@code webkitdirectory} (folder) browser upload (e.g. {@code archives/test.zip}).  Only the
     * base filename is used for the target path; a UUID prefix is added to avoid name collisions
     * between concurrent or sequential uploads.
     *
     * @param file the multipart file to save
     * @return absolute path of the saved file
     * @throws IOException if the file cannot be written to disk
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = createUniqueFilename(originalFilename);
        Path targetPath = Path.of(getRootDir()).resolve(uniqueFilename);

        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.atInfo().addArgument(targetPath).log("File uploaded to: {}");
        return targetPath.toString();
    }

    public void removeFile(String filepath) {
        try {
            Path path = Paths.get(filepath);
            if (FileValidator.exists(path)) {
                Files.deleteIfExists(path);
                logger.atInfo().addArgument(filepath).log("File removed from: {}");
            } else {
                logger.atWarn().addArgument(filepath).log("File does not exist: {}");
            }
        } catch (IOException e) {
            logger.atError()
                  .addArgument(filepath)
                  .setCause(e)
                  .log("Error removing file: {}");
        }
    }

    public void removeAllFiles(String dirName) {
        Path directory = Path.of(dirName);
        if (!FileValidator.isDirectory(directory)) {
            logger.atWarn().addArgument(dirName).log("Not a directory: {}");
            return;
        }
        try (var stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                  .filter(path -> !path.equals(directory))
                  .forEach(this::deletePath);
            logger.atInfo().addArgument(dirName).log("All files removed from: {}");
        } catch (IOException e) {
            logger.atError()
                  .addArgument(dirName)
                  .setCause(e)
                  .log("Error removing all files: {}");
        }
    }

    public void removeUploadRootDir() {
        removeDir(getRootDir());
    }

    public String getFilename(String filepath) {
        return Path.of(filepath).getFileName().toString();
    }

    public void removeDir(String dirName) {
        Path directory = Path.of(dirName);
        if (FileValidator.exists(directory)) {
            try {
                FileSystemUtils.deleteRecursively(directory);
                logger.atInfo().addArgument(dirName).log("Directory removed: {}");
            } catch (IOException e) {
                logger.atError().addArgument(dirName).setCause(e).log("Failed to remove directory: {}");
            }
        } else {
            logger.atWarn().addArgument(dirName).log("Directory does not exist: {}");
        }
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
        validateZipInput(zipFilePath);

        Path inpArchive = prepareExtractionDirectory(zipFilePath);
        try {
            extractArchive(zipFilePath, inpArchive);
            ensureDirectoryExists(destFolder);
            int count = moveExcelFiles(inpArchive, destFolder);
            logger.atInfo().addArgument(count).log("Extracted and renamed {} Excel files from archive");
            return count;
        } finally {
            if (FileValidator.exists(inpArchive)) {
                FileSystemUtils.deleteRecursively(inpArchive);
            }
        }
    }

    private Path resolveUploadPath() {
        String uploadDir = configuredUploadDir;
        if (uploadDir == null || uploadDir.isBlank()) {
            logger.atDebug().log("Upload directory is not configured - default temp will be used");
            uploadDir = System.getProperty("java.io.tmpdir");
        }
        return Path.of(uploadDir).resolve(UPLOAD_FOLDER);
    }

    private String createUniqueFilename(String originalFilename) {
        String baseName = "";
        if (originalFilename != null && !originalFilename.isBlank()) {
            baseName = Path.of(originalFilename).getFileName().toString();
        }
        if (baseName.isBlank()) {
            baseName = "upload";
        }
        return UUID.randomUUID() + "_" + baseName;
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.atError()
                  .addArgument(path)
                  .setCause(e)
                  .log("Error removing file: {}");
        }
    }

    private void validateZipInput(Path zipFilePath) throws IOException {
        if (!FileValidator.exists(zipFilePath)) {
            throw new IOException("Zip file does not exist: " + zipFilePath);
        }
        if (!FileValidator.isReadable(zipFilePath)) {
            throw new IOException("Zip file is not readable: " + zipFilePath);
        }
        if (zipFilePath.getParent() == null) {
            throw new IOException("Zip file must have a parent directory: " + zipFilePath);
        }
    }

    private Path prepareExtractionDirectory(Path zipFilePath) throws IOException {
        Path inpArchive = zipFilePath.getParent().resolve("inpArchive");
        if (FileValidator.exists(inpArchive)) {
            FileSystemUtils.deleteRecursively(inpArchive);
        }
        Files.createDirectories(inpArchive);
        return inpArchive;
    }

    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!FileValidator.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private void extractArchive(Path zipFilePath, Path destinationDir) throws IOException {
        if (isSystemUnzipAvailable()) {
            extractWithSystemUnzip(zipFilePath, destinationDir);
            return;
        }

        logger.atWarn().addArgument(zipFilePath.toString())
              .log("System 'unzip' is not available. Falling back to Java ZIP extraction for: {}");
        extractWithJavaZip(zipFilePath, destinationDir);
    }

    private int moveExcelFiles(Path sourceDir, Path destFolder) throws IOException {
        AtomicInteger count = new AtomicInteger();
        try (var excelStream = Files.walk(sourceDir)) {
            excelStream.filter(FileValidator::isRegularFile)
                    .filter(FileValidator::isExcelFile)
                    .forEach(excelFile -> moveExcelFile(excelFile, destFolder, count));
        }
        return count.get();
    }

    private void moveExcelFile(Path excelFile, Path destFolder, AtomicInteger count) {
        try {
            Path parent = excelFile.getParent();
            String parentName = parent != null && parent.getFileName() != null ? parent.getFileName().toString() : "";
            String partialParent = parentName;
            int underscoreIdx = parentName.indexOf('_');
            if (underscoreIdx > 0) {
                partialParent = parentName.substring(0, underscoreIdx);
            }

            String originalFileName = excelFile.getFileName().toString();
            String targetName = partialParent.isEmpty() ? originalFileName : partialParent + "_" + originalFileName;
            Path targetFile = uniqueTargetPath(destFolder, targetName);
            Files.move(excelFile, targetFile);
            logger.atInfo().addArgument(targetFile.toString()).log("Excel file moved and renamed to: {}");
            count.incrementAndGet();
        } catch (IOException e) {
            logger.atError().addArgument(excelFile.toString()).setCause(e).log("Error moving Excel file: {}");
        }
    }

    private Path uniqueTargetPath(Path destFolder, String fileName) {
        Path targetFile = destFolder.resolve(fileName);
        int suffix = 1;
        while (Files.exists(targetFile)) {
            int dotIdx = fileName.lastIndexOf('.');
            String baseName = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
            String ext = dotIdx > 0 ? fileName.substring(dotIdx) : "";
            fileName = baseName + "_" + suffix + ext;
            targetFile = destFolder.resolve(fileName);
            suffix++;
        }
        return targetFile;
    }

    private boolean isSystemUnzipAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("unzip", "-v");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void extractWithSystemUnzip(Path zipFilePath, Path destinationDir) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "unzip",
                zipFilePath.toAbsolutePath().toString(),
                "-d",
                destinationDir.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.atError().addArgument(zipFilePath.toString())
                      .log("Unzip command failed for: {} (exit code: " + exitCode + ")");
                throw new IOException("Unzip command failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unzip interrupted", e);
        }
    }

    private void extractWithJavaZip(Path zipFilePath, Path destinationDir) throws IOException {
        Path normalizedDestination = destinationDir.toAbsolutePath().normalize();

        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path targetPath = resolveZipEntryPath(normalizedDestination, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                    continue;
                }

                Path parent = targetPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (var entryInputStream = zipFile.getInputStream(entry)) {
                    Files.copy(entryInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private Path resolveZipEntryPath(Path destinationRoot, String entryName) throws IOException {
        if (destinationRoot == null) {
            throw new IOException("ZIP extraction destination is not configured");
        }

        String normalizedEntryName = entryName == null ? "" : entryName.replace('\\', '/').trim();
        if (normalizedEntryName.isEmpty()) {
            throw new IOException("ZIP entry name is empty");
        }
        if (normalizedEntryName.startsWith("/")
                || normalizedEntryName.startsWith("../")
                || normalizedEntryName.contains("/../")
                || normalizedEntryName.matches("^[A-Za-z]:.*")) {
            throw new IOException("Invalid ZIP entry path: " + entryName);
        }

        Path resolvedPath = destinationRoot.resolve(normalizedEntryName).normalize();
        if (!resolvedPath.startsWith(destinationRoot)) {
            throw new IOException("Invalid ZIP entry path: " + entryName);
        }
        return resolvedPath;
    }

}
