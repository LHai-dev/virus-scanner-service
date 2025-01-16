package kh.gov.gdc.virusscannerservice.controller;

import kh.gov.gdc.virusscannerservice.dto.ScanRequest;
import kh.gov.gdc.virusscannerservice.dto.ScanResponse;
import kh.gov.gdc.virusscannerservice.service.S3Service;
import kh.gov.gdc.virusscannerservice.service.VirusScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
//@EnableWebMvc
public class VirusScannerController {
    private final VirusScannerService scannerService;
    private final S3Service s3Service;

    @PostMapping
    public ResponseEntity<ScanResponse> scanFile(@RequestBody ScanRequest request) {
        try {
            return ResponseEntity.ok(scannerService.scanFile(request.getKey()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new ScanResponse(false, null, "Invalid key format: " + e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ScanResponse(false, null, "Error processing file: " + e.getMessage())
            );
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFileToS3(@RequestParam("file") MultipartFile file) {
        try {
            // Define the S3 key (path) where the file will be stored
            String key = file.getOriginalFilename();

            // Call the S3Service to upload the file
            s3Service.uploadFile(file, key);

            return ResponseEntity.ok("File uploaded successfully to S3 with key: " + key);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading file: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(@RequestParam String bucket) {
        try {


            List<String> fileKeys = s3Service.listFiles(bucket);
            return ResponseEntity.ok(fileKeys);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Error listing files: " + e.getMessage()));
        }
    }
}