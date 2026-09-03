package com.se196693.mvc.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String objectKey);

    void delete(String objectKey);
}
