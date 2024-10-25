package com.unitbv.myquiz.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String uploadFile(MultipartFile file);
    void removeFile(String filepath);
    public void removeDir(String dirName);
    void removeAllFiles(String dirName);
    void removeUploadRootDir();
    String getFilename(String filepath);

}
