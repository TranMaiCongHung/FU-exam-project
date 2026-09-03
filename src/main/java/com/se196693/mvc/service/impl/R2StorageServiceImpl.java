package com.se196693.mvc.service.impl;

import com.se196693.mvc.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class R2StorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file, String objectKey) {
        try {
            /*
            * đóng gói metadata gửi lên R2 gồm: tên thùng chứa (bucket), đường dẫn file (key)
            * và loại định danh MIME (contentType ví dụ image/png, image/jpeg).
            * Nếu thiếu contentType, trình duyệt khi click vào link ảnh c thể bị hiểu nhầm thành
            * lệnh tải file tahy vì hiển thị trực tiếp ảnh*/
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    /*đọc dữ liệu nguồn nhị phân trực tiếp từ bộ đệm của request mà không cần
                    * phải ghi file tạm ra đĩa cứng server, giúp tiết kiệm RAM và tốc độ upload nhanh*/
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );

            return objectKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        s3Client.deleteObject(request);
    }
}
