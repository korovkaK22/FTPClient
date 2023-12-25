package com.example.ftpclient.controllers;

import com.example.ftpclient.exceptions.CommandExecuteException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainViewController {
    private static final Logger logger = LogManager.getRootLogger();
    @FXML
    private TextField commandLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Button createButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button sendButton;

    @FXML
    private TreeView<?> treViewPanel;

    @Setter
    FTPClient ftpClient;



    @FXML
    void createAction(ActionEvent event) {

    }

    @FXML
    void downloadAction(ActionEvent event) {

    }

    @FXML
    void sendCommandAction(ActionEvent event) {
        try {
            errorLabel.setText("");
            String command = commandLabel.getText();
            String[] parts = command.split(" ", 2);
            if (parts.length ==2) {
                ftpClient.doCommand(parts[0], parts[1]);
            } else {
                ftpClient.doCommand(command, "");
            }


        } catch (Exception e){
            logger.error("Can't execute user command: "+ e.getMessage(), e);
            errorLabel.setText(e.getMessage());
        }
    }

}