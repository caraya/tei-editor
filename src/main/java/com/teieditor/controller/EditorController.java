package com.teieditor.controller;

import com.teieditor.service.AntExportService;
import com.teieditor.service.ExportService;
import com.teieditor.service.StylesheetManager;
import com.teieditor.service.ValidationService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class EditorController {

    // --- XSLT MAPPING REGISTRY ---
    private static final Map<String, String> XSLT_MAPPINGS = Map.of(
        "HTML",     "html/html.xsl",
        "PDF",      "fo/fo.xsl",
        "LaTeX",    "latex/latex.xsl",
        "Markdown", "profiles/default/markdown/to.xsl",
        "BibTeX",   "profiles/default/bibtex/to.xsl",
        "EPUB3",    "epub/build-to.xml",
        "Docx",     "docx/build-to.xml"
    );

    // --- UI Components ---
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox visualContainer;
    @FXML private VBox codeContainer;
    @FXML private WebView visualWebView;
    @FXML private TextArea codeEditor;
    @FXML private Label statusLabel;
    @FXML private Button viewToggleBtn;
    @FXML private VBox sidebar;
    @FXML private Accordion tagAccordion;
    @FXML private ToggleButton sidebarToggleBtn;

    // --- Services ---
    private WebEngine webEngine;
    private final ValidationService validationService = new ValidationService();
    private final ExportService exportService = new ExportService();
    private final StylesheetManager stylesheetManager = new StylesheetManager();
    private final AntExportService antExportService = new AntExportService(); 
    
    // --- State ---
    private File currentFile = null; 
    private File teiStylesheetsDir = null; 
    
    private enum ViewMode { VISUAL_ONLY, SPLIT, CODE_ONLY }
    private ViewMode currentViewMode = ViewMode.VISUAL_ONLY;
    private boolean isVisualFocused = true;

    // Sync Flags
    private boolean isUpdatingFromVisual = false;
    private boolean isUpdatingFromCode = false;

    // Undo/Redo
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isUndoingRedoing = false;
    private PauseTransition typingTimer;

    // Default Template
    private final String DEFAULT_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
                <teiHeader>
                    <fileDesc>
                        <titleStmt>
                            <title>Comprehensive TEI Manuscript Example</title>
                            <author>Jane Doe</author>
                            <editor>John Smith</editor>
                        </titleStmt>
                        <publicationStmt>
                            <publisher>Open Humanities Press</publisher>
                            <pubPlace>London</pubPlace>
                            <date when="2025-11-24"/>
                            <availability>
                                <licence target="https://creativecommons.org/licenses/by/4.0/">CC BY 4.0</licence>
                            </availability>
                        </publicationStmt>
                        <sourceDesc>
                            <bibl>
                                <title>Imaginary Source Book</title>
                                <author>Jane Doe</author>
                                <date when="2025"/>
                            </bibl>
                        </sourceDesc>
                    </fileDesc>
                    <encodingDesc>
                        <projectDesc>
                            <p>This TEI file demonstrates a wide range of TEI tags for educational purposes.</p>
                        </projectDesc>
                    </encodingDesc>
                    <profileDesc>
                        <langUsage>
                            <language ident="en">English</language>
                            <language ident="la">Latin</language>
                        </langUsage>
                        <textClass>
                            <keywords>
                                <term>TEI</term>
                                <term>manuscript</term>
                                <term>example</term>
                            </keywords>
                        </textClass>
                    </profileDesc>
                    <revisionDesc>
                        <change when="2025-11-24">Initial comprehensive example created.</change>
                    </revisionDesc>
                </teiHeader>
                <text>
                    <front>
                        <titlePage>
                            <docTitle>
                                <titlePart type="main">Comprehensive TEI Manuscript Example</titlePart>
                                <titlePart type="sub">A Demonstration of TEI Markup</titlePart>
                            </docTitle>
                            <docAuthor>Jane Doe</docAuthor>
                            <docDate when="2025-11-24">November 24, 2025</docDate>
                        </titlePage>
                        <div type="dedication">
                            <p>To all scholars of digital humanities.</p>
                        </div>
                        <div type="preface">
                            <head>Preface</head>
                            <p>This document illustrates the use of various <hi rend="italic">TEI</hi> elements in encoding a manuscript.</p>
                        </div>
                    </front>
                    <body>
                        <div type="chapter" n="1">
                            <head>Chapter 1: Beginnings</head>
                            <p>In the <date when="2025-11-24">year 2025</date>, <persName ref="#jane">Jane Doe</persName> began her journey in <placeName ref="#london">London</placeName>.<note type="editorial">This is a fictional account.</note></p>
                            <p>She wrote, <q>"The <hi rend="bold">future</hi> of digital texts is bright."</q></p>
                            <list type="bulleted">
                                <item>TEI encoding</item>
                                <item>Digital preservation</item>
                                <item>Open access</item>
                            </list>
                            <table>
                                <row>
                                    <cell>Year</cell>
                                    <cell>Event</cell>
                                </row>
                                <row>
                                    <cell>2025</cell>
                                    <cell>Started project</cell>
                                </row>
                                <row>
                                    <cell>2026</cell>
                                    <cell>Published results</cell>
                                </row>
                            </table>
                        </div>
                        <div type="chapter" n="2">
                            <head>Chapter 2: Exploration</head>
                            <p>Jane met <persName ref="#john">John Smith</persName>, a renowned <roleName>editor</roleName>.<note type="footnote">John Smith is also fictional.</note></p>
                            <p>Together, they visited <placeName ref="#paris">Paris</placeName> and discussed <term>textual criticism</term> and <term>markup languages</term>.</p>
                            <p>Jane quoted a Latin phrase: <foreign xml:lang="la">Veni, vidi, vici</foreign>.<note type="translation">I came, I saw, I conquered.</note></p>
                            <p>They referenced <bibl><title>TEI Guidelines</title> by <author>TEI Consortium</author> (<date when="2022"/>)</bibl> for their work.</p>
                        </div>
                    </body>
                    <back>
                        <div type="appendix">
                            <head>Appendix: Glossary</head>
                            <list type="gloss">
                                <label>TEI</label>
                                <item>Text Encoding Initiative</item>
                                <label>XML</label>
                                <item>eXtensible Markup Language</item>
                            </list>
                        </div>
                        <div type="notes">
                            <head>Notes</head>
                            <note type="general">This manuscript is for demonstration only.</note>
                        </div>
                        <div type="references">
                            <head>References</head>
                            <listBibl>
                                <bibl xml:id="tei-guidelines">
                                    <title>TEI P5: Guidelines for Electronic Text Encoding and Interchange</title>
                                    <author>TEI Consortium</author>
                                    <date when="2022"/>
                                    <ref target="https://tei-c.org/guidelines/">Online</ref>
                                </bibl>
                            </listBibl>
                        </div>
                    </back>
                </text>
                <standOff>
                    <listPerson>
                        <person xml:id="jane">
                            <persName>Jane Doe</persName>
                            <sex value="f"/>
                            <birth when="1990"/>
                        </person>
                        <person xml:id="john">
                            <persName>John Smith</persName>
                            <sex value="m"/>
                            <birth when="1985"/>
                        </person>
                    </listPerson>
                    <listPlace>
                        <place xml:id="london">
                            <placeName>London</placeName>
                            <location>
                                <geo>51.5074,-0.1278</geo>
                            </location>
                        </place>
                        <place xml:id="paris">
                            <placeName>Paris</placeName>
                            <location>
                                <geo>48.8566,2.3522</geo>
                            </location>
                        </place>
                    </listPlace>
                </standOff>
            </TEI>
            """;

    @FXML
    public void initialize() {
        webEngine = visualWebView.getEngine();
        codeEditor.setText(DEFAULT_TEMPLATE);
        
        typingTimer = new PauseTransition(Duration.millis(1000));
        typingTimer.setOnFinished(e -> {});

        URL url = getClass().getResource("/view/visual_editor.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            System.err.println("CRITICAL ERROR: Could not find /view/visual_editor.html");
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", this);
                syncCodeToVisual();
            }
        });

        codeEditor.focusedProperty().addListener((obs, old, newVal) -> { if(newVal) isVisualFocused = false; });
        visualWebView.focusedProperty().addListener((obs, old, newVal) -> { if(newVal) isVisualFocused = true; });

        codeEditor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUndoingRedoing) return; 

            if (!isUpdatingFromVisual) {
                if (typingTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                    commitToUndoHistory(oldVal);
                }
                typingTimer.playFromStart();

                isUpdatingFromCode = true;
                syncCodeToVisual();
                isUpdatingFromCode = false;
            }
        });

        visualWebView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                    Object isTextEditing = webEngine.executeScript("isEditingText()");
                    if (isTextEditing instanceof Boolean && !(Boolean) isTextEditing) {
                        handleDelete();
                        event.consume();
                    }
                }
            }
            if (event.isShortcutDown()) {
                if (event.getCode() == KeyCode.Z) {
                    if (event.isShiftDown()) handleRedo();
                    else handleUndo();
                    event.consume();
                }
            }
        });
        
        updateLayout();
        populateTagSidebar();
        handleToggleSidebar(); 
    }

    // ==========================================
    // EXPORT WORKFLOW
    // ==========================================

    private void performExport(String formatName, String extension, String relativeXsltKey) {
        if (teiStylesheetsDir == null || !teiStylesheetsDir.exists()) {
            configureStylesheetsFlow(() -> performExport(formatName, extension, relativeXsltKey));
            return;
        }

        String xsltPath = XSLT_MAPPINGS.get(formatName);
        if (xsltPath == null) { showError("Error", "Unknown format: " + formatName); return; }
        
        File xsltFile = resolveSmartPath(teiStylesheetsDir, xsltPath);
        
        if (xsltFile == null || !xsltFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Configuration Error");
            alert.setHeaderText("Stylesheet Not Found");
            alert.setContentText("Could not find '" + xsltPath + "' in:\n" + teiStylesheetsDir.getAbsolutePath() + 
                                 "\n\nPlease ensure you selected the correct TEI Stylesheets root folder.");
            alert.showAndWait();
            teiStylesheetsDir = null;
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to " + formatName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(formatName, "*." + extension));
        fileChooser.setInitialFileName("export." + extension);
        
        File outputFile = fileChooser.showSaveDialog(null);
        if (outputFile == null) return;

        statusLabel.setText("Exporting to " + formatName + "...");
        new Thread(() -> {
            try {
                if (formatName.equals("Docx") || formatName.equals("EPUB3")) {
                    antExportService.exportComplexFormat(codeEditor.getText(), teiStylesheetsDir, outputFile, formatName);
                } else if (formatName.equals("PDF")) {
                    exportService.transformToPdf(codeEditor.getText(), xsltFile, outputFile);
                } else {
                    exportService.transform(codeEditor.getText(), xsltFile, outputFile);
                }
                Platform.runLater(() -> statusLabel.setText("Export Successful: " + outputFile.getName()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Export Failed", e.getMessage()));
            }
        }).start();
    }

    private void configureStylesheetsFlow(Runnable onSuccess) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("TEI Stylesheets Configuration");
        alert.setHeaderText("External Stylesheets Required");
        alert.setContentText("To export documents, the TEI XSLT Stylesheets are required.\n\n" +
                             "Do you already have the stylesheets downloaded/cloned on your computer?");

        ButtonType btnYesSelect = new ButtonType("Yes, Select Folder");
        ButtonType btnNoDownload = new ButtonType("No, Download Them");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnYesSelect, btnNoDownload, btnCancel);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnYesSelect) {
                handleSelectExistingFolder(onSuccess);
            } else if (type == btnNoDownload) {
                handleDownloadFlow(onSuccess);
            }
        });
    }

    private void handleSelectExistingFolder(Runnable onSuccess) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select TEI Stylesheets Directory");
        File selectedDir = dirChooser.showDialog(null);

        if (selectedDir != null) {
            teiStylesheetsDir = selectedDir;
            onSuccess.run();
        }
    }

    private void handleDownloadFlow(Runnable onSuccess) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Download Stylesheets");
        alert.setHeaderText("Download Location");
        alert.setContentText("Where would you like to save the stylesheets (~50MB)?");

        ButtonType btnCustom = new ButtonType("Choose Folder...");
        ButtonType btnDefault = new ButtonType("Use Default");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnCustom, btnDefault, btnCancel);

        alert.showAndWait().ifPresent(type -> {
            File targetDir = null;
            if (type == btnCustom) {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Select Download Destination");
                targetDir = dirChooser.showDialog(null);
            } else if (type == btnDefault) {
                targetDir = stylesheetManager.getDefaultDir();
            }

            if (targetDir != null) {
                statusLabel.setText("Downloading Stylesheets...");
                File finalDir = targetDir;
                stylesheetManager.downloadAndInstall(targetDir,
                    () -> { 
                        statusLabel.setText("Download Complete.");
                        teiStylesheetsDir = finalDir;
                        onSuccess.run();
                    },
                    () -> { 
                        statusLabel.setText("Download Failed.");
                        showError("Network Error", "Could not download stylesheets.");
                    }
                );
            }
        });
    }

    private File resolveSmartPath(File rootDir, String relativePath) {
        File direct = new File(rootDir, relativePath);
        if (direct.exists()) return direct;

        File nested = new File(new File(rootDir, "xml/tei/stylesheet"), relativePath);
        if (nested.exists()) return nested;
        
        File[] subs = rootDir.listFiles(File::isDirectory);
        if (subs != null) {
            for (File sub : subs) {
                File subCheck = new File(new File(sub, "xml/tei/stylesheet"), relativePath);
                if (subCheck.exists()) return subCheck;
            }
        }
        return null;
    }

    @FXML public void exportHtml() { performExport("HTML", "html", null); }
    @FXML public void exportPdf() { performExport("PDF", "pdf", null); }
    @FXML public void exportMarkdown() { performExport("Markdown", "md", null); }
    @FXML public void exportLatex() { performExport("LaTeX", "tex", null); }
    @FXML public void exportEpub() { performExport("EPUB3", "epub", null); }
    @FXML public void exportDocx() { performExport("Docx", "docx", null); }
    @FXML public void exportBibtex() { performExport("BibTeX", "bib", null); }

    @FXML public void validateXml() {
        statusLabel.setText("Validating...");
        statusLabel.setStyle("-fx-text-fill: black;");
        new Thread(() -> {
            boolean isValid = validationService.validateTei(codeEditor.getText());
            Platform.runLater(() -> {
                if (isValid) { statusLabel.setText("✔ Valid TEI XML"); statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;"); }
                else { statusLabel.setText("✘ Invalid TEI XML"); statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); }
            });
        }).start();
    }

    // --- CLIPBOARD ---
    @FXML public void handleCopy() {
        if (!isVisualFocused) { codeEditor.copy(); return; }
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            Object result = webEngine.executeScript("getSelectedXml()");
            if (result instanceof String && !((String) result).isEmpty()) {
                ClipboardContent content = new ClipboardContent();
                content.putString((String) result);
                Clipboard.getSystemClipboard().setContent(content);
                statusLabel.setText("Copied TEI element.");
            }
        }
    }

    @FXML public void handleCut() {
        if (!isVisualFocused) { codeEditor.cut(); return; }
        handleCopy(); handleDelete(); statusLabel.setText("Cut TEI element.");
    }

    @FXML public void handlePaste() {
        if (!isVisualFocused) { codeEditor.paste(); return; }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            snapshotBeforeAction();
            String content = clipboard.getString();
            String safeContent = content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
            webEngine.executeScript("pasteXml('" + safeContent + "')");
            statusLabel.setText("Pasted from clipboard.");
        }
    }

    // --- UNDO/REDO ---
    private void commitToUndoHistory(String state) {
        if (state == null || state.isEmpty()) return;
        if (!undoStack.isEmpty() && undoStack.peek().equals(state)) return;
        undoStack.push(state);
        redoStack.clear(); 
        statusLabel.setText("State saved.");
    }

    @FXML public void handleUndo() {
        if (undoStack.isEmpty()) { statusLabel.setText("Nothing to Undo."); return; }
        isUndoingRedoing = true; 
        redoStack.push(codeEditor.getText());
        codeEditor.setText(undoStack.pop());
        syncCodeToVisual();
        isUndoingRedoing = false; 
        statusLabel.setText("Undid action.");
    }

    @FXML public void handleRedo() {
        if (redoStack.isEmpty()) { statusLabel.setText("Nothing to Redo."); return; }
        isUndoingRedoing = true; 
        undoStack.push(codeEditor.getText());
        codeEditor.setText(redoStack.pop());
        syncCodeToVisual();
        isUndoingRedoing = false; 
        statusLabel.setText("Redid action.");
    }

    // --- SYNC ---
    public void updateFromVisual(String xmlContent) {
        if (isUpdatingFromCode) return;
        if (isUndoingRedoing) return;
        isUpdatingFromVisual = true;
        Platform.runLater(() -> {
            if (typingTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) commitToUndoHistory(codeEditor.getText());
            typingTimer.playFromStart();
            String formattedXml = formatXml(xmlContent);
            codeEditor.setText(formattedXml);
            isUpdatingFromVisual = false;
        });
    }
    
    private void snapshotBeforeAction() { commitToUndoHistory(codeEditor.getText()); }

    private void syncCodeToVisual() {
        if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            String cleanXml = codeEditor.getText().replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
            webEngine.executeScript("renderFromXml('" + cleanXml + "')");
        }
    }
    
    private String formatXml(String xml) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            StreamResult result = new StreamResult(new StringWriter());
            StreamSource source = new StreamSource(new StringReader(xml));
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Exception e) { return xml; }
    }

    // --- DOM OPS ---
    private void insertElement(String tagName) {
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            snapshotBeforeAction();
            visualWebView.requestFocus(); 
            webEngine.executeScript("insertTeiElement('" + tagName + "')");
        }
    }
    
    @FXML public void handleDelete() {
        if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            snapshotBeforeAction();
            webEngine.executeScript("deleteSelectedElement()");
        }
    }

    // --- SIDEBAR ---
    private void populateTagSidebar() {
        Map<String, List<String>> tagCategories = Map.of(
            "Structure", List.of("div", "head", "body", "front", "back", "titlePage"),
            "Paragraphs & Lines", List.of("p", "lg", "l", "sp", "speaker", "ab"),
            "Lists", List.of("list", "item", "label", "listBibl", "bibl"),
            "Tables", List.of("table", "row", "cell"),
            "Inline Elements", List.of("hi", "q", "quote", "foreign", "emph", "term", "gloss", "mentioned"),
            "Names & Dates", List.of("name", "persName", "placeName", "orgName", "date", "time", "num"),
            "Notes & Refs", List.of("note", "ref", "ptr", "anchor")
        );
        tagAccordion.getPanes().clear();
        tagCategories.forEach((category, tags) -> {
            FlowPane content = new FlowPane();
            content.setVgap(5); content.setHgap(5); content.setPadding(new Insets(10));
            content.setPrefWrapLength(230); 
            for (String tag : tags) {
                Button tagBtn = new Button("<" + tag + ">");
                tagBtn.setStyle("-fx-background-radius: 15; -fx-min-width: 60;");
                tagBtn.setTooltip(new Tooltip("Insert <" + tag + "> element"));
                tagBtn.setOnAction(e -> insertElement(tag));
                content.getChildren().add(tagBtn);
            }
            tagAccordion.getPanes().add(new TitledPane(category, content));
        });
        if (!tagAccordion.getPanes().isEmpty()) tagAccordion.setExpandedPane(tagAccordion.getPanes().get(0));
    }

    @FXML public void handleToggleSidebar() {
        boolean isVisible = sidebarToggleBtn.isSelected();
        sidebar.setVisible(isVisible);
        sidebar.setManaged(isVisible);
    }

    @FXML public void handleToggleView() {
        switch (currentViewMode) {
            case VISUAL_ONLY -> currentViewMode = ViewMode.SPLIT;
            case SPLIT -> currentViewMode = ViewMode.CODE_ONLY;
            case CODE_ONLY -> currentViewMode = ViewMode.VISUAL_ONLY;
        }
        updateLayout();
    }

    private void updateLayout() {
        mainSplitPane.getItems().clear();
        switch (currentViewMode) {
            case VISUAL_ONLY: mainSplitPane.getItems().add(visualContainer); viewToggleBtn.setText("View: Visual Only"); syncCodeToVisual(); break;
            case CODE_ONLY: mainSplitPane.getItems().add(codeContainer); viewToggleBtn.setText("View: Code Only"); break;
            case SPLIT: mainSplitPane.getItems().addAll(visualContainer, codeContainer); mainSplitPane.setDividerPositions(0.5); viewToggleBtn.setText("View: Split"); syncCodeToVisual(); break;
        }
    }

    // --- FILE OPS ---
    @FXML public void handleNew() {
        if (!codeEditor.getText().equals(DEFAULT_TEMPLATE) && currentFile == null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("New Document"); alert.setHeaderText("Discard changes?"); alert.setContentText("Start new document?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        }
        currentFile = null;
        codeEditor.setText(DEFAULT_TEMPLATE);
        undoStack.clear(); redoStack.clear();
        statusLabel.setText("New document created.");
    }

    @FXML public void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open TEI XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TEI XML", "*.xml", "*.tei"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                currentFile = file;
                codeEditor.setText(content);
                undoStack.clear(); redoStack.clear();
                statusLabel.setText("Opened: " + file.getName());
            } catch (IOException e) { showError("Read Error", e.getMessage()); }
        }
    }

    @FXML public void handleSave() { if (currentFile != null) saveToFile(currentFile); else handleSaveAs(); }

    @FXML public void handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save TEI XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TEI XML", "*.xml"));
        if (currentFile != null) { fileChooser.setInitialFileName(currentFile.getName()); fileChooser.setInitialDirectory(currentFile.getParentFile()); }
        else { fileChooser.setInitialFileName("document.xml"); }
        File file = fileChooser.showSaveDialog(null);
        if (file != null) saveToFile(file);
    }

    private void saveToFile(File file) {
        try { Files.writeString(file.toPath(), codeEditor.getText()); currentFile = file; statusLabel.setText("Saved: " + file.getName()); }
        catch (IOException e) { showError("Save Error", e.getMessage()); }
    }

    @FXML public void handleExit() { Platform.exit(); }

    // --- Menu Helpers ---
    @FXML public void insertDiv() { insertElement("div"); }
    @FXML public void insertHead() { insertElement("head"); }
    @FXML public void insertParagraph() { insertElement("p"); }
    @FXML public void insertLg() { insertElement("lg"); }
    @FXML public void insertSp() { insertElement("sp"); }
    @FXML public void insertList() { insertElement("list"); }
    @FXML public void insertItem() { insertElement("item"); }
    @FXML public void insertTable() { insertElement("table"); }
    @FXML public void insertName() { insertElement("name"); }
    @FXML public void insertDate() { insertElement("date"); }
    @FXML public void insertHi() { insertElement("hi"); }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error"); alert.setHeaderText(header); alert.setContentText(content);
        alert.showAndWait();
    }
}