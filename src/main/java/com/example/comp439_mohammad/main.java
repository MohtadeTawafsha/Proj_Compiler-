package com.example.comp439_mohammad;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Set up the FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a File to Parse");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            // Open the FileChooser dialog
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());

                // Process the file using the Scanner and Parser
                List<ScannerLexer.Token> tokens = ScannerLexer.scanFile(selectedFile.getAbsolutePath());
                ScannerLexer.Parser parser = new ScannerLexer.Parser(tokens);
                try {
                    parser.parse();
                    System.out.println("Parsing completed successfully.");
                } catch (ScannerLexer.Parser.MaxDepthExceededException e) {
                    System.err.println("Error: " + e.getMessage());
                } catch (ScannerLexer.SyntaxError e) {
                    System.err.println("Syntax Error: " + e.getMessage());
                }
            } else {
                System.out.println("No file selected.");
            }
        } catch (ScannerLexer.SyntaxError e) {
            System.err.println("Syntax error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Launch the JavaFX application
        launch(args);
    }
}
