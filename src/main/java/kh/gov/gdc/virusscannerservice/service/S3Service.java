package kh.gov.gdc.virusscannerservice.service;

import com.amazonaws.util.IOUtils;
import kh.gov.gdc.virusscannerservice.FileNotFoundException;
import kh.gov.gdc.virusscannerservice.dto.S3FileStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3 amazonS3;
    @Value("${aws.s3.buckets.quarantine}")
    private String quarantineBucket;

    public ObjectMetadata getObjectMetadata(String bucket, String key) {
        try {
            // Get metadata of the object from S3
            GetObjectMetadataRequest request = new GetObjectMetadataRequest(bucket, key);
            ObjectMetadata metadata = amazonS3.getObjectMetadata(request);

            return metadata;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new FileNotFoundException("File not found: " + key);
            }
            throw e;
        }
    }

    public S3FileStream getFileStream(String bucket, String key) {
        try {
            GetObjectRequest request = new GetObjectRequest(bucket, key);
            S3Object s3Object = amazonS3.getObject(request);

            return new S3FileStream(
                    s3Object.getObjectContent(),
                    s3Object.getObjectMetadata().getVersionId()
            );
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new FileNotFoundException("File not found: " + key);
            }
            throw e;
        }
    }
    public List<String> listFiles(String bucket) {
        try {
            // Create a request to list objects in the bucket
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
                    .withBucketName(bucket);

            ListObjectsV2Result result = amazonS3.listObjectsV2(listObjectsV2Request);
            List<String> fileKeys = new ArrayList<>();

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                fileKeys.add(objectSummary.getKey());
            }

            return fileKeys;
        } catch (AmazonS3Exception e) {
            log.error("Error listing files in S3 bucket: {}", bucket, e);
            throw e;
        }
    }
    public void uploadFile(MultipartFile file, String key) {
        try {
            // Read the file content into a byte array
            byte[] fileBytes = IOUtils.toByteArray(file.getInputStream());

            // Create a ByteArrayInputStream for uploading
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);

            // Create a PutObjectRequest to upload the file
            PutObjectRequest putObjectRequest = new PutObjectRequest(quarantineBucket, key, byteArrayInputStream,
                    new ObjectMetadata());

            // Optionally, you can set the stream buffer size (e.g., 10 MB) to configure a larger buffer
            putObjectRequest.getRequestClientOptions().setReadLimit(10 * 1024 * 1024); // 10 MB read limit

            // Upload the file to S3
            amazonS3.putObject(putObjectRequest);

            log.info("File uploaded successfully to S3 with key: {}", key);

        } catch (IOException e) {
            log.error("Error reading file input stream", e);
            throw new RuntimeException("Error reading file input stream", e);
        } catch (AmazonS3Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        }
    }
    public void deleteFile(String bucket, String key) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));
        } catch (AmazonS3Exception e) {
            log.error("Error deleting file from S3", e);
            throw e;
        }
    }

    public void moveFile(String sourceBucket, String sourceKey,
                         String destBucket, String destKey) {
        try {
            // Copy to new location
            CopyObjectRequest copyRequest = new CopyObjectRequest(
                    sourceBucket, sourceKey, destBucket, destKey
            );

            amazonS3.copyObject(copyRequest);

            // Delete from old location
            deleteFile(sourceBucket, sourceKey);

        } catch (AmazonS3Exception e) {
            log.error("Error moving file in S3", e);
            throw e;
        }
    }
}