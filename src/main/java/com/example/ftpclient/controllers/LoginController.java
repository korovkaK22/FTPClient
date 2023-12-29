package com.example.ftpclient.controllers;

import com.example.ftpclient.ClientApplication;
import com.example.ftpclient.exceptions.LoginFailedException;
import com.example.ftpclient.utils.LoadDefaultSettings;
import com.example.ftpclient.utils.TriConsumer;
import javafx.scene.control.Label;
import javafx.stage.Stage;
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
    private TriConsumer<String, String, String> action;

    @FXML
    private TextField loginLabel;

    @FXML
    private TextField passwordLabel;

    @FXML
    private TextField serverLabel;


    @FXML
    private Label errorLabel;

    public void initLabels(String username, String password, String serverDomain){
        loginLabel.setText(username);
        passwordLabel.setText(password);
        serverLabel.setText(serverDomain);
    }

    @FXML
    void loginAction(ActionEvent event) throws Exception {
        try {
            action.accept(loginLabel.getText(), passwordLabel.getText(), serverLabel.getText());
            closeWindow();
        } catch (Exception e){
            String ex = "Can't login: "+ e.getMessage();
            errorLabel.setText(ex);
            logger.error(ex);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) loginLabel.getScene().getWindow();
        stage.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}
