package com.example.comp439_mohammad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Button chooseFileButton = new Button("Choose File");
        TextArea errorTextArea = new TextArea();
        errorTextArea.setEditable(false);
        errorTextArea.setWrapText(true);

        VBox root = new VBox(10, chooseFileButton, errorTextArea);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a File to Parse");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        chooseFileButton.setOnAction(event -> {
            errorTextArea.clear();

            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());

                try {
                    List<ScannerLexer.Token> tokens = ScannerLexer.scanFile(selectedFile.getAbsolutePath());
                    ScannerLexer.Parser parser = new ScannerLexer.Parser(tokens);
                    try {
                        parser.parse();
                        System.out.println("Parsing completed successfully.");
                        errorTextArea.appendText("Parsing completed successfully.\n look in Console ");
                    } catch (ScannerLexer.Parser.MaxDepthExceededException e) {
                        System.err.println("Error: " + e.getMessage());
                        errorTextArea.appendText("Error: " + e.getMessage() + "\n");
                    } catch (ScannerLexer.SyntaxError e) {
                        System.err.println("Syntax Error: " + e.getMessage());
                        errorTextArea.appendText("Syntax Error: " + e.getMessage() + "\n");
                    }
                } catch (ScannerLexer.SyntaxError e) {
                    System.err.println("Syntax error: " + e.getMessage());
                    errorTextArea.appendText("Syntax error: " + e.getMessage() + "\n");
                } catch (Exception e) {
                    e.printStackTrace();
                    errorTextArea.appendText("An unexpected error occurred: " + e.getMessage() + "\n");
                }
            } else {
                System.out.println("No file selected.");
                errorTextArea.appendText("No file selected.\n");
            }
        });

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("File Parser");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
