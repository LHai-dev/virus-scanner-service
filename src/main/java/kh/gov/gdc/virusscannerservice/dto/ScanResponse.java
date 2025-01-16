package kh.gov.gdc.virusscannerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScanResponse {
    private boolean success;
    private String cleanFileKey;
    private String message;
}
