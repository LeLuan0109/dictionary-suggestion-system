package org.example.dictionarysuggestionsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DictionaryApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(DictionaryApp.class.getResource("dictionary.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        stage.setTitle("Dictionary Suggestion System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}


