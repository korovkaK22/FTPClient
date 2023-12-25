package com.example.ftpclient;

import com.example.ftpclient.controllers.LoginController;
import com.example.ftpclient.controllers.MainViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;


public class ClientApplication extends Application {
    private static final Logger logger = LogManager.getRootLogger();
    @Setter
    private static FTPClient ftpClient;

    private Stage stage;


    @Override
    public void start(Stage stage) throws IOException {

        this.stage = stage;
        loadLoginView();
    }

    private void loadLoginView() {
        try {
            logger.debug("Починаю запуск вікна авторизації");
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            LoginController controller = fxmlLoader.getController();
            controller.setAction(this::loadMainView);
            controller.setFtpClient(ftpClient);
            stage.setTitle("FTP Client - Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            logger.error("Can't load login view: " + e.getMessage());
        }
    }

    private void loadMainView() {
        try {
            logger.debug("Починаю запуск самого клієнту");
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApplication.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            MainViewController controller = fxmlLoader.getController();
            controller.setFtpClient(ftpClient);

            controller.initPanel();
            stage.setTitle("FTP Client - Main View");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            logger.error("Can't load main view: " + e.getMessage(), e);
        }
    }



}
