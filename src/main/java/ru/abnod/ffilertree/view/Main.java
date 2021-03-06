package ru.abnod.ffilertree.view;

        import javafx.application.Application;
        import javafx.fxml.FXMLLoader;
        import javafx.scene.Parent;
        import javafx.scene.Scene;
        import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/mainFrame.fxml"));
        primaryStage.setTitle("FFilerr");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(480);
        primaryStage.show();
    }
}
