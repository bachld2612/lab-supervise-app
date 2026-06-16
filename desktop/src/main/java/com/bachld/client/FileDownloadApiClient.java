package com.bachld.client;

import com.bachld.config.RestClient;
import org.springframework.http.HttpMethod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDownloadApiClient {

    private final RestClient restClient;

    public FileDownloadApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Path downloadSharedFile(String fileToken, String fileName) {
        String url = restClient.getBaseUrl() + "/api/file-share/v1/" + fileToken + "/download";
        Path target = resolveDownloadPath(fileName);

        return restClient.getRestTemplate().execute(url, HttpMethod.GET, null, response -> {
            Files.createDirectories(target.getParent());
            try (InputStream input = response.getBody()) {
                Files.copy(input, target);
            }
            return target;
        });
    }

    private Path resolveDownloadPath(String fileName) {
        String downloadsPath = System.getProperty("user.home") + java.io.File.separator + "Downloads";
        Path downloadsDir = Path.of(downloadsPath);
        String safeFileName = sanitizeFileName(fileName);
        Path target = downloadsDir.resolve(safeFileName);

        if (!Files.exists(target)) {
            return target;
        }

        String baseName = safeFileName;
        String extension = "";
        int dot = safeFileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = safeFileName.substring(0, dot);
            extension = safeFileName.substring(dot);
        }

        int counter = 1;
        do {
            target = downloadsDir.resolve(baseName + " (" + counter + ")" + extension);
            counter++;
        } while (Files.exists(target));
        return target;
    }

    private String sanitizeFileName(String fileName) {
        String value = fileName == null ? "" : fileName.trim();
        if (value.isEmpty()) {
            value = "download";
        }
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        return value.isBlank() ? "download" : value;
    }
}
