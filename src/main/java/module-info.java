module org.example.dictionarysuggestionsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;


    opens org.example.dictionarysuggestionsystem to javafx.fxml;
    opens org.example.dictionarysuggestionsystem.model to com.fasterxml.jackson.databind;
    exports org.example.dictionarysuggestionsystem;
}