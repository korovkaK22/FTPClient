package com.example.ftpclient;

import com.example.ftpclient.controllers.LoginController;
import com.example.ftpclient.controllers.MainViewController;
import com.example.ftpclient.exceptions.LoginFailedException;
import com.example.ftpclient.utils.FTPUtil;
import com.example.ftpclient.utils.LoadDefaultSettings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class ClientApplication extends Application {
    private static final Logger logger = LogManager.getRootLogger();
    @Setter
    private static LoadDefaultSettings settings;

    private Stage stage;


    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        loadLoginView();
    }

    private void loadLoginView() {
        try {
            logger.debug("Починаю запуск вікна авторизації");
            FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            stage.setTitle("FTP Client - Login");
            controller.initLabels(settings.getUsername(), settings.getPassword(), settings.getServerDomain());
            controller.setAction((a,b,c) -> {FTPClient client = FTPUtil.createNewSession(a,b,c);
                loadFirstTimeMainView(a, client);});
            openDialogWindowOnStage(root);

        } catch (Exception e) {
            logger.error("Can't load login view: " + e.getMessage());
        }
    }



    private void loadFirstTimeMainView(String str, FTPClient client){
        try {
            logger.debug("Починаю запуск самого клієнту");
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            MainViewController controller = fxmlLoader.getController();
            controller.addSession(str, client);
            controller.getSessionList().getSelectionModel().select(0);
            controller.setOpenWindow(this::openDialogWindowOnStage);
            stage.setTitle("FTP Client - Main View");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            logger.error("Can't load main view: " + e.getMessage(), e);
        }
    }



    private void openDialogWindowOnStage(Parent root) {
        Scene scene = new Scene(root);
        Stage dialogStage = new Stage();
        dialogStage.setScene(scene);

        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(stage);

        dialogStage.showAndWait();
    }


}
