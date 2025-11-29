package com.teieditor.service;

import javafx.application.Platform;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StylesheetManager {

    private static final String DOWNLOAD_URL = "https://github.com/TEIC/Stylesheets/releases/download/v7.56.0/tei-xsl-7.56.0.zip";
    
    // Default location (Fall back to this if user doesn't specify one)
    public File getDefaultDir() {
        return new File(System.getProperty("user.home"), ".tei-editor/stylesheets");
    }

    // UPDATED: Now accepts a specific target directory
    public void downloadAndInstall(File targetDir, Runnable onSuccess, Runnable onError) {
        Thread thread = new Thread(() -> {
            try {
                if (!targetDir.exists()) targetDir.mkdirs();

                System.out.println("Downloading stylesheets to: " + targetDir.getAbsolutePath());
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(DOWNLOAD_URL)).build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) throw new IOException("Download failed: HTTP " + response.statusCode());

                System.out.println("Extracting...");
                unzip(response.body(), targetDir.toPath());

                System.out.println("Installation complete.");
                Platform.runLater(onSuccess);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(onError);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void unzip(InputStream source, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(source)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = targetDir.resolve(zipEntry.getName());
                if (!newPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Bad zip entry: " + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}