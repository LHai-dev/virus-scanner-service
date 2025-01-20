package kh.gov.gdc.virusscannerservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirusScannerService {
    private final S3Service s3Service;
    private final kh.gov.gdc.virusscannerservice.config.ClamAvScanner clamAvScanner;

    @Value("${aws.s3.buckets.quarantine}")
    public String quarantineBucket;

    @Value("${aws.s3.buckets.clean}")
    private String cleanBucket;

    public kh.gov.gdc.virusscannerservice.dto.ScanResponse scanFile(String key) {
        // Validate UUID format


        // Get file from quarantine bucket
        var s3Stream = s3Service.getFileStream(quarantineBucket, key);
        
        // Scan file
        var scanResult = clamAvScanner.scan(s3Stream.getInputStream());
        
        if (scanResult.isMalicious()) {
            log.warn("Malicious file detected: {}", key);
            s3Service.deleteFile(quarantineBucket, key);
            
            return new kh.gov.gdc.virusscannerservice.dto.ScanResponse(
                false,
                null,
                "Malicious file detected: " + String.join(", ", scanResult.getVirusNames())
            );
        }

        // Move to clean bucket with new UUID
        String cleanKey = UUID.randomUUID().toString();
        s3Service.moveFile(quarantineBucket, key, cleanBucket, cleanKey);
        
        return new kh.gov.gdc.virusscannerservice.dto.ScanResponse(true, cleanKey, "File scanned successfully");
    }
}