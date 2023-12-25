package com.example.ftpclient.utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LoadDefaultSettingsImpl implements LoadDefaultSettings {
    private final String serverAddress;
    private final String username;
    private final String password;

    public String getServerDomain() {
        return serverAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
