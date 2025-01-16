package kh.gov.gdc.virusscannerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ScanResult {
    private boolean isMalicious;
    private List<String> virusNames;
}
