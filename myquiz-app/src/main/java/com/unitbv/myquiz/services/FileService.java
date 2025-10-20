package com.unitbv.myquiz.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileService {
    String uploadFile(MultipartFile file);
    void removeFile(String filepath);
    public void removeDir(String dirName);
    void removeAllFiles(String dirName);
    void removeUploadRootDir();
    String getFilename(String filepath);

    int unzipAndRenameExcelFiles(Path archive, Path tempDir) throws IOException;
}
