package com.example.ftpclient.controllers;

import com.example.ftpclient.ClientApplication;
import com.example.ftpclient.exceptions.LoginFailedException;
import javafx.scene.control.Label;
import lombok.extern.java.Log;
import org.apache.commons.net.ftp.FTPClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class LoginController implements Initializable {
    private static final Logger logger = LogManager.getRootLogger();


    @Setter
    private FTPClient ftpClient;
    @Setter
    private Runnable action;

    @FXML
    private Button loginButton;

    @FXML
    private TextField loginLabel;

    @FXML
    private TextField passwordLabel;

    @FXML
    private TextField serverLabel;

    @FXML
    private TextField serverPortLabel;

    @FXML
    private Label errorLabel;

    @FXML
    void loginAction(ActionEvent event) throws Exception {
        try {
            ftpClient.connect(serverLabel.getText());
            if (!ftpClient.login(loginLabel.getText(), passwordLabel.getText())){
                throw new LoginFailedException("Username or password are incorrect");
            }
            action.run();
        } catch (Exception e){
            errorLabel.setText(e.getMessage());
            logger.error(e.getMessage());
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
