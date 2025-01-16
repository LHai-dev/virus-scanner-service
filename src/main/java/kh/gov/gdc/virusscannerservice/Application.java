package kh.gov.gdc.virusscannerservice;

import kh.gov.gdc.virusscannerservice.controller.VirusScannerController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;



@SpringBootApplication
// We use direct @Import instead of @ComponentScan to speed up cold starts
// @ComponentScan(basePackages = "kh.gov.gdc.virusscannerservice.controller")
//@Import({ VirusScannerController.class })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}