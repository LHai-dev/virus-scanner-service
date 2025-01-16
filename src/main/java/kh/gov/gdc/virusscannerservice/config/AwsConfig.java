package kh.gov.gdc.virusscannerservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AwsConfig {

    @Value("${aws.s3.access-key-id}")
    private String accessKeyId;
    @Value("${aws.s3.access-key-secret}")
    private String accessKeSecret;
    @Value("${aws.s3.region-name}")
    private String bucketRegion;
    @Value("${aws.s3.endpoint}")
    private String bucketEndpoint;
    @Value("${aws.s3.development}")
    private boolean localS3;

    @Bean
    public AmazonS3 getAwsS3Client() {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKeyId, accessKeSecret);
        if (localS3) {
            return AmazonS3ClientBuilder
                    .standard()
                    .enablePathStyleAccess()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(bucketEndpoint, bucketRegion))
                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                    .build();
        } else {
            return AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                    .withRegion(bucketRegion)
                    .build();
        }
    }

}
