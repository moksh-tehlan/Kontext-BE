package com.moksh.kontext.aws.service;

import com.moksh.kontext.common.exception.S3DeleteException;
import com.moksh.kontext.common.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.cloudfront.url}")
    private String cloudFrontUrl;

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String key = folder + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String cloudFrontUrl = this.cloudFrontUrl + "/" + key;
            log.info("File uploaded successfully to S3, accessible via CloudFront: {}", cloudFrontUrl);
            
            return cloudFrontUrl;
            
        } catch (IOException e) {
            log.error("Error reading file content: {}", e.getMessage());
            throw new S3UploadException("Failed to read file content during S3 upload", e);
        } catch (S3Exception e) {
            log.error("S3 error during file upload: {}", e.getMessage());
            throw new S3UploadException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String s3Url) {
        try {
            String key = extractKeyFromUrl(s3Url);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: {}", s3Url);
            
        } catch (S3Exception e) {
            log.error("S3 error during file deletion: {}", e.getMessage());
            throw new S3DeleteException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private String extractKeyFromUrl(String url) {
        // Handle both CloudFront and S3 URLs
        if (url.startsWith(cloudFrontUrl)) {
            return url.replace(cloudFrontUrl + "/", "");
        }
        return url; // If it's already just the key
    }

}