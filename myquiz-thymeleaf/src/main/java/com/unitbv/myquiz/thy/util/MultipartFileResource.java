package com.unitbv.myquiz.thy.util;

import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public class MultipartFileResource extends InputStreamResource {
    private final String filename;
    private final long contentLength;

    public MultipartFileResource(MultipartFile multipartFile) throws IOException {
        super(multipartFile.getInputStream());
        this.filename = multipartFile.getOriginalFilename();
        this.contentLength = multipartFile.getSize();
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() {
        return this.contentLength;
    }
}

