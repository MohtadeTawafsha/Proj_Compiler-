module com.example.comp439_mohammad {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.comp439_mohammad to javafx.fxml;
    exports com.example.comp439_mohammad;
}