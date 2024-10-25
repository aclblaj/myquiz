package com.unitbv.myquiz.services;

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

@Service
public class FileServiceImpl implements FileService{

    public static final String UPLOAD_FOLDER = "files";

    private String uploadDir;
    Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    private String rootDir;

    @PostConstruct
    public void initFileServiceImpl() {
        uploadDir = System.getProperty("upload.dir");
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
            throw new RuntimeException("Could not initialize the root folder for upload");
        }
    }

    @Override
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

    @Override
    public void removeFile(String filepath) {
        try {
            Files.deleteIfExists(Paths.get(filepath));
            logger.atInfo().addArgument(filepath).log("File removed from: {}");
        } catch (IOException e) {
            logger.atError().addArgument(filepath).log("Error removing file: {}", e);
        }
    }

    @Override
    public void removeAllFiles(String dirName) {
        try {
            Files.walk(Path.of(dirName))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(File::delete);
            logger.atInfo().addArgument(getRootDir()).log("All files removed from: {}");
        } catch (IOException e) {
            logger.atError().addArgument(getRootDir()).log("Error removing all files: {}", e);
        }
    }

    @Override
    public void removeUploadRootDir() {
        removeDir(getRootDir());
    }

    @Override
    public String getFilename(String filepath) {
        int pos = filepath.lastIndexOf(File.separator);
        String filename = filepath.substring(pos + 1);
        return filename;
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

}
