package com.example.ftpclient.managers;

import com.example.ftpclient.memento.ConnectionMemento;

import java.util.HashMap;

public class SessionManager {
    private final HashMap<String, ConnectionMemento> connections = new HashMap<>();

    public void save(String key, ConnectionMemento memento){
        connections.put(key, memento);
    }

    public void remove(String key){
        connections.remove(key);
    }

    public ConnectionMemento get(String key){
      return connections.get(key);
    }

}
