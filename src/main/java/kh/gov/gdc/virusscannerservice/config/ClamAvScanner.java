package kh.gov.gdc.virusscannerservice.config;

import kh.gov.gdc.virusscannerservice.dto.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClamAvScanner {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Value("${clamav.port:3310}")
    private int port;

    @Value("${clamav.host:127.0.0.1}")
    private String host;

    @Value("${clamav.timeout:60000}")
    private int timeout;

    public ScanResult scan(InputStream fileStream) {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                log.debug("Attempting to connect to ClamAV at {}:{} (attempt {}/{})",
                        host, port, attempt + 1, MAX_RETRIES);

                try (Socket socket = new Socket(host, port)) {
                    socket.setSoTimeout(timeout);
                    return scanWithSocket(socket, fileStream);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to scan on attempt {}/{}. Error: {}",
                        attempt + 1, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry", ie);
                    }
                }
            }
        }

        log.error("Failed to scan file after {} attempts", MAX_RETRIES, lastException);
        throw new RuntimeException("Failed to scan file after " + MAX_RETRIES + " attempts", lastException);
    }

    private ScanResult scanWithSocket(Socket socket, InputStream fileStream) throws IOException {
        try (OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            log.debug("Connected to ClamAV, sending INSTREAM command");
            out.write("zINSTREAM\0".getBytes());
            out.flush();

            byte[] buffer = new byte[2048];
            int read;
            long totalBytesScanned = 0;

            while ((read = fileStream.read(buffer)) != -1) {
                byte[] sizeHeader = ByteBuffer.allocate(4).putInt(read).array();
                out.write(sizeHeader);
                out.write(buffer, 0, read);
                out.flush();
                totalBytesScanned += read;
                log.trace("Sent {} bytes to ClamAV (total: {})", read, totalBytesScanned);
            }

            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            log.debug("Finished sending file data, reading response");

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            while ((read = in.read(buffer)) != -1) {
                responseBuffer.write(buffer, 0, read);
                if (read < buffer.length) break;
            }

            String result = responseBuffer.toString().trim();
            log.debug("ClamAV scan result: {}", result);

            if (result.contains("FOUND")) {
                List<String> viruses = new ArrayList<>();
                viruses.add(result.split(" FOUND ")[1].trim());
                return new ScanResult(true, viruses);
            }

            return new ScanResult(false, null);
        }
    }
}