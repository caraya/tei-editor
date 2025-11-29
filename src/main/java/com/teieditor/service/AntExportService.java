package com.teieditor.service;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class AntExportService {

    /**
     * Orchestrates complex exports (DOCX, EPUB) by running the official Ant build files.
     */
    public void exportComplexFormat(String xmlContent, File stylesheetsDir, File outputFile, String formatType) throws Exception {
        
        // 1. Locate the Build File
        File buildFile;
        if ("DOCX".equalsIgnoreCase(formatType)) {
            buildFile = new File(stylesheetsDir, "docx/build-to.xml");
        } else if ("EPUB3".equalsIgnoreCase(formatType)) {
            buildFile = new File(stylesheetsDir, "epub/build-to.xml");
        } else {
            throw new IllegalArgumentException("Unsupported complex format: " + formatType);
        }

        if (!buildFile.exists()) {
            throw new IOException("Could not find Ant build file: " + buildFile.getAbsolutePath());
        }

        // 2. Create Temp Input File
        File tempInput = File.createTempFile("tei_export_source", ".xml");
        Files.writeString(tempInput.toPath(), xmlContent);
        tempInput.deleteOnExit();

        // 3. SWITCH WORKING DIRECTORY (The Fix)
        // Saxon resolves relative paths (like ../word/styles.xml) against 'user.dir'.
        // We must trick it into thinking we are running inside the stylesheet folder.
        String originalUserDir = System.getProperty("user.dir");
        File targetWorkDir = buildFile.getParentFile(); // e.g. .../Stylesheets/docx/
        
        try {
            System.setProperty("user.dir", targetWorkDir.getAbsolutePath());

            // 4. Configure Ant Project
            Project project = new Project();
            project.setBaseDir(targetWorkDir);
            project.init();
            
            // Logger
            DefaultLogger consoleLogger = new DefaultLogger();
            consoleLogger.setErrorPrintStream(System.err);
            consoleLogger.setOutputPrintStream(System.out);
            consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
            project.addBuildListener(consoleLogger);

            // 5. Set Properties
            project.setUserProperty("ant.file", buildFile.getAbsolutePath());
            project.setUserProperty("inputFile", tempInput.getAbsolutePath());
            project.setUserProperty("outputFile", outputFile.getAbsolutePath());
            project.setUserProperty("profile", "default"); 

            // 6. Load and Execute
            ProjectHelper.configureProject(project, buildFile);
            project.executeTarget(project.getDefaultTarget());

        } catch (Exception e) {
            throw new IOException("Ant Build Failed: " + e.getMessage(), e);
        } finally {
            // CRITICAL: Always restore the original working directory
            System.setProperty("user.dir", originalUserDir);
            
            // Clean up
            // tempInput.delete();
        }
    }
}