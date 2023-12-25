package com.example.ftpclient;


import javafx.application.Application;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FtpClientApplication {
    private static final Logger logger = LogManager.getRootLogger();

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FtpClientApplication.class, args);
        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding("UTF-8");

        ClientApplication.setFtpClient(ftpClient);
        Application.launch(ClientApplication.class, args);
    }

}
