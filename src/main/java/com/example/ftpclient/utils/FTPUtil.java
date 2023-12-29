package com.example.ftpclient.utils;

import com.example.ftpclient.exceptions.LoginFailedException;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

public class FTPUtil {

    public static FTPClient createNewSession(String name, String password, String server) throws IOException {
        FTPClient result = new FTPClient();
        result.setControlEncoding("UTF-8");
        result.connect(server);
        if (!result.login(name, password)){
            throw new LoginFailedException("Username or password are incorrect");
        }
        return result;
    }
}
