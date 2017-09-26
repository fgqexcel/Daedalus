package daedalus;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Daedalus extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
        Parent root = loader.load();
        UIController control = loader.getController();
        Scene scene = new Scene(root);
       
        stage.setScene(scene);
        stage.setTitle("Daedalus Verilog IDE for Icarus");
        stage.getIcons().add(new Image("/logo.jpg"));
        stage.setOnCloseRequest(e -> control.menuQuit(e));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}
