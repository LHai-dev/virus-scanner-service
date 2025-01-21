package kh.gov.gdc.virusscannerservice.service;

import kh.gov.gdc.virusscannerservice.dto.ScanLog;
import kh.gov.gdc.virusscannerservice.dto.ScanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class VirusScannerService {
    private final S3Service s3Service;
    private final kh.gov.gdc.virusscannerservice.config.ClamAvScanner clamAvScanner;

    // Store recent scan logs in memory (you might want to use a database in production)
    private final Queue<ScanLog> scanLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_SIZE = 100; // Keep last 100 logs

    @Value("${aws.s3.buckets.quarantine}")
    public String quarantineBucket;

    @Value("${aws.s3.buckets.clean}")
    public String cleanBucket;

    public List<ScanLog> getScanLogs() {
        List<ScanLog> logs = new ArrayList<>(scanLogs);
        Collections.reverse(logs); // Most recent first
        return logs;
    }

    public void addScanLog(ScanLog log) {
        scanLogs.add(log);
        // Keep only recent logs
        while (scanLogs.size() > MAX_LOG_SIZE) {
            scanLogs.poll();
        }
    }

    public ScanLog createScanLog(String fileName, Long fileSize, String fileType, ScanResponse scanResult) {
        return new ScanLog(
                fileName,
                fileType,
                fileSize,
                LocalDateTime.now(),
                scanResult.isMalicious(),
                scanResult.getVirusNames(),
                scanResult.isMalicious() ? "INFECTED" : "CLEAN",
                scanResult.getMessage()
        );
    }

    public kh.gov.gdc.virusscannerservice.dto.ScanResponse scanFile(String key) {
        try {
            // Get file from quarantine bucket (you might need the full key path)
            var s3ObjectMetadata = s3Service.getObjectMetadata(quarantineBucket, key); // Fetch the metadata first

            // Get file stream from S3 (This returns the stream itself, without metadata)
            var s3Stream = s3Service.getFileStream(quarantineBucket, key);

            // Scan file
            var scanResult = clamAvScanner.scan(s3Stream.getInputStream());

            // Create scan response
            ScanResponse response;
            if (scanResult.isMalicious()) {
                log.warn("Malicious file detected: {}", key);
                s3Service.deleteFile(quarantineBucket, key);

                response = new ScanResponse(
                        false,
                        null,
                        "Malicious file detected: " + String.join(", ", scanResult.getVirusNames()),
                        true,
                        scanResult.getVirusNames()
                );
            } else {
                String cleanKey = UUID.randomUUID().toString();
                s3Service.moveFile(quarantineBucket, key, cleanBucket, cleanKey);

                response = new ScanResponse(
                        true,
                        cleanKey,
                        "File scanned successfully",
                        false,
                        Collections.emptyList()
                );
            }

            // Add scan log using metadata information
            ScanLog scanLog = createScanLog(
                    key,
                    s3ObjectMetadata.getContentLength(),   // Access content length from metadata
                    s3ObjectMetadata.getContentType(),     // Access content type from metadata
                    response
            );
            addScanLog(scanLog);

            return response;

        } catch (Exception e) {
            log.error("Error scanning file: {}", e.getMessage());
            throw new RuntimeException("Error scanning file: " + e.getMessage());
        }
    }

}