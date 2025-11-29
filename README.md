# TEI Visual Editor v1.0

A modern, cross-platform desktop application for editing **TEI (Text Encoding Initiative) XML** documents. It features a unique split-view interface that synchronizes a WYSIWYG visual editor with the raw XML code in real-time.

## Features

### 🖥️ Dual-Mode Editing

* **Visual Editor:** A user-friendly, drag-and-drop interface for structuring documents without touching code.
* **Code Editor:** A raw XML text area for precise control.
* **Split View:** Both editors are visible side-by-side for seamless switching.
* **Two-Way Sync:** Changes in the visual view update the code instantly, and typing in the code view updates the visual layout.

### 🛠️ Robust Tools

* **Drag-and-Drop:** Reorder elements or nest them (e.g., placing a `<list>` inside a `<div>`) using intuitive mouse actions.
* **Smart Clipboard:** Copy/Cut/Paste works contextually—copying visual blocks extracts their valid XML structure.
* **Undo/Redo:** Global history stack that tracks changes across both visual and code views.
* **Validation:** Real-time validation against the TEI Schema using **Jing**.

### 📄 Advanced Exporting

The application integrates the official **TEI XSLT Stylesheets** to convert documents into high-quality publication formats:

* **PDF:** Rendered via **Apache FOP** (XSL-FO).
* **Word (DOCX):** Generated via **Apache Ant** build pipelines.
* **eBook (EPUB3):** Generated via **Apache Ant** build pipelines.
* **Web (HTML), Markdown, LaTeX, BibTeX:** Generated via **Saxon-HE** (XSLT 3.0).

## Requirements

### System Requirements

* **OS:** Windows 10+, macOS 11+, or Modern Linux.
* **Java Runtime:** JDK 21 is required to build. (The final installer bundles the runtime).
* **Disk Space:** ~200MB (Application + External Stylesheets).

### Runtime Dependencies

* **TEI Stylesheets:** The "Export" functionality requires the official TEI XSLT Stylesheets (~50MB).
  * ***Note:*** The application automatically handles the downloading and configuration of these stylesheets upon first use. You do not need to install them manually.

## Getting Started

### 1. Clone & Run

This project uses Gradle. You do not need to install Gradle manually; use the included wrapper.

```bash
git clone https://github.com/caraya/tei-editor.git
cd tei-editor
```

To run the application directly from source:

```bash
# Linux / macOS
./gradlew run

# Windows
gradlew.bat run
```

### Building an Installer

To create a standalone native installer (DMG for Mac, EXE for Windows, DEB for Linux) that users can run without installing Java:

```bash
./gradlew jpackage
```

The output installer will be located in build/jpackage.

## Usage Guide

### Shortcuts

| Action | Windows / Linux | macOS |
| --- | --- | --- |
| New File | Ctrl + N | Cmd + N |
| Open File | Ctrl + O | Cmd + O |
| Save | Ctrl + S | Cmd + S |
| Save As | Ctrl + Shift + S | Cmd + Shift + S |
| Undo | Ctrl + Z | Cmd + Z |
| Redo | Ctrl + Shift + Z | Cmd + Shift + Z |
| Delete Element | Delete | Delete / Backspace |

## Visual Editing Tips

* **Selection**: Click any box (Paragraph, Div, Header) to select it. It will highlight orange.
* **Deletion**: Press Delete or Backspace to remove the selected element. (Note: If you are editing text inside a box, the key will delete text characters instead).
* **Drag & Drop**: Drag a box to the Top/Bottom edge of another box to reorder.Drag a box to the Middle of a container (like a `<div>`) to nest it inside
* **Exporting**: Go to the Export menu and select your desired format (e.g., PDF).
  * **First Run Only**: The app will check if the TEI Stylesheets are installed.If missing, it will prompt you to download them automatically to `~/.tei-editor/` stylesheets.
  * Alternatively, if you already have the stylesheets on your disk, you can point the app to that folder.

## Architecture & Libraries

* **UI**: JavaFX 21 (WebView for visual rendering).
* **Build System**: Gradle.
* **XML Validation**: Jing (RelaxNG).
* **Transformation**: Saxon-HE (XSLT 3.0 Processor).
* **PDF Rendering**: Apache FOP.
* **Complex Builds (Docx, EPUB3)**: Apache Ant (Embedded).

## License

MIT (See LICENSE file for details)
