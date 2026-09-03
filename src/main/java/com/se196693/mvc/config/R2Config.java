package com.se196693.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {
    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        //tạo cặp khóa xác thực người dùng theo tiêu chuẩn bảo mật của AWS SDK
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                /*
                * mặc định S3Client sẽ trỏ tới các máy chủ của Amazon AWS(s2.amazone.com)
                * lệnh này ghi đề địa chỉ đihcs trỏ v endpoint của Cloudfalre R2*/
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        //Nhà cung cấp thông tin xác thực cố định dựa trên credentials vừa tạo
                        StaticCredentialsProvider.create(credentials)
                )
                //dùng định danh "auto" để Cloudflare tự định tuyến đến Data Center gần nhất.
                .region(Region.of("auto"))
                .build();
    }
}
