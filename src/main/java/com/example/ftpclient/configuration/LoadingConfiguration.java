package com.example.ftpclient.configuration;

import com.example.ftpclient.utils.LoadDefaultSettings;
import com.example.ftpclient.utils.LoadDefaultSettingsImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config.properties")
public class LoadingConfiguration {

    @Bean
    public LoadDefaultSettings getSettings(@Value("${server.domain}") String serverDomain,
                                           @Value("${server.username}") String userName,
                                           @Value("${server.password}") String password){
        return new LoadDefaultSettingsImpl(serverDomain, userName, password);
    }

}
