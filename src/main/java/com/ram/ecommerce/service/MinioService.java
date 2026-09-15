package com.ram.ecommerce.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName;
    private final String publicUrl;

    public MinioService(
            MinioClient minioClient,
            @Value("${minio.bucket-name}") String bucketName,
            @Value("${minio.public-url}") String publicUrl
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
    }
    @PostConstruct
    public void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            return publicUrl + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
}