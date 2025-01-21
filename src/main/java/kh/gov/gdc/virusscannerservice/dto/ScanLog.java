package kh.gov.gdc.virusscannerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ScanLog {
    private String fileName;
    private String fileType;
    private long fileSize;
    private LocalDateTime scanTime;
    private boolean isMalicious;
    private List<String> virusNames;
    private String status; // "CLEAN" or "INFECTED"
    private String message;
}