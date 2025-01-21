package kh.gov.gdc.virusscannerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ScanResponse {
    private boolean success;
    private String cleanFileKey;
    private String message;
    private boolean isMalicious;
    private List<String> virusNames;
}
