# TEI Visual Editor

Create a Java-based visual editor for TEI XML documents. WYSIWYG is the default mode but you can switch to raw XML editing as needed. The app has a menu bar with File options (New, Open, Save, Save As), an Edit options (Undo, Redo, Cut, Copy, Paste) and an Export menu (HTML, PDF, EPUB3, Others). The main area is a split view with the visual editor on the left and raw XML on the right. Changes in one view update the other in real-time.

## The application should

- support the full TEI XML schema
- provide a visual editing interface with drag-and-drop capabilities for common elements (sections, paragraphs, lists, tables, images)
- allow switching between WYSIWYG and raw XML editing modes
- validate the XML against the TEI schema in real-time
- support exporting the edited document as valid TEI XML
- include undo/redo functionality and version history for edits
- use JavaFX 21 for building the UI
- include unit and integration tests for core functionality
- export the XML document to PDF and HTML formats
- be documented with a README explaining setup, usage, and contribution guidelines
- be packaged as a standalone desktop application for Windows, macOS, and Linux
- Update the other view (WYSIWYG or raw XML) in real-time as the user makes changes in either view

## Constraints

- All code must be written in Java, not Kotlin
- If necessary, use Java 21 for library compatibility
- Do not use material design components
- Don't include material design dependencies

## Toolkits and Libraries

- JavaFX 21 for building the UI
- Jing for XML schema validation
- TEI schema definitions for schema validation
- TEI XSLT stylesheets for exporting to different formats
- Jing for Data validation
- Gradle for build automation and dependency management
- JUnit for unit testing
