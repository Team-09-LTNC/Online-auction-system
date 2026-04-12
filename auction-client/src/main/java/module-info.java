module auction.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.videcodeauctionclient to javafx.fxml;
    exports com.example.videcodeauctionclient;
}