package kh.gov.gdc.virusscannerservice.controller;

import kh.gov.gdc.virusscannerservice.dto.ScanLog;
import kh.gov.gdc.virusscannerservice.dto.ScanResponse;
import kh.gov.gdc.virusscannerservice.service.S3Service;
import kh.gov.gdc.virusscannerservice.service.VirusScannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/scanner")
@RequiredArgsConstructor
@Slf4j
public class VirusScannerController {
    private final VirusScannerService scannerService;
    private final S3Service s3Service;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            String key = file.getOriginalFilename();
            // Upload file
            s3Service.uploadFile(file, key);

            // Scan the uploaded file
            ScanResponse scanResult = scannerService.scanFile(key);

            if (scanResult.isSuccess()) {
                redirectAttributes.addFlashAttribute("message", "File uploaded and scanned successfully: " + key);
            } else {
                redirectAttributes.addFlashAttribute("error", "File upload succeeded but scan failed: " + scanResult.getMessage());
            }
            redirectAttributes.addFlashAttribute("latestScanResult", scanResult);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload/scan failed: " + e.getMessage());
        }
        return "redirect:/scanner";
    }

    @GetMapping("/download/{key}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String key) {
        try {
            var s3Object = s3Service.getFileStream(scannerService.cleanBucket, key);

            org.springframework.http.HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", key);

            return ResponseEntity.ok().headers(headers).body(new InputStreamResource(s3Object.getInputStream()));
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/scan")
    public String scanFile(@RequestParam String key, Model model) {
        try {
            ScanResponse response = scannerService.scanFile(key);
            model.addAttribute("scanResult", response);
        } catch (Exception e) {
            model.addAttribute("error", "Scan failed: " + e.getMessage());
        }
        return "scanner";
    }

    @GetMapping("/logs")
    @ResponseBody
    public List<ScanLog> getLogs() {
        return scannerService.getScanLogs();
    }

    @GetMapping
    public String showScannerPage(Model model, @RequestParam(required = false) String bucket) {
        if (bucket != null) {
            List<String> files = s3Service.listFiles(bucket);
            model.addAttribute("files", files);
            model.addAttribute("currentBucket", bucket);
        }

        // Add clean files list
        List<String> cleanFiles = s3Service.listFiles(scannerService.cleanBucket);
        model.addAttribute("cleanFiles", cleanFiles);

        // Add scan logs
        List<ScanLog> scanLogs = scannerService.getScanLogs();
        model.addAttribute("scanLogs", scanLogs);

        return "scanner";
    }

    @ResponseBody
    @GetMapping("/api/files")
    public List<String> getFiles(@RequestParam String bucket) {
        return s3Service.listFiles(bucket);
    }
}