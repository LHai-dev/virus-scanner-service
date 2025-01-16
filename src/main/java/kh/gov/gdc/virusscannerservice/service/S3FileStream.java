package kh.gov.gdc.virusscannerservice.service;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class S3FileStream {
    @Getter
    private S3ObjectInputStream inputStream;
    private String versionId;

}
