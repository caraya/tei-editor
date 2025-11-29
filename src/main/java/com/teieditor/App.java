package com.teieditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Load the FXML Layout
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/view/editor.fxml"));
        Parent root = fxmlLoader.load();
        
        // Setup the Scene
        Scene scene = new Scene(root, 1200, 800);
        
        stage.setTitle("TEI Visual Editor");
        
        // --- SET APPLICATION ICON ---
        // We use App.class to locate resources relative to the classpath.
        // The image must be located at src/main/resources/images/app-icon.png
        try {
            Image icon = new Image(Objects.requireNonNull(App.class.getResourceAsStream("/images/app-icon.png")));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Warning: Could not load application icon. " + e.getMessage());
        }
        
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}