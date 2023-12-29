package com.example.ftpclient.managers;

import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ListManager {
    private ListView<String> sessionList;
    private ObservableList<String> list;

    public ListManager(ListView<String> sessionList) {
        this.sessionList = sessionList;
        this.list = sessionList.getItems();
    }

    public void add(String name){
        list.add(name);
    }

    public void delete(String name){
        list.remove(name);
    }

    public String getCurrentSessionName(){
        return sessionList.getSelectionModel().getSelectedItem();
    }

    public void setSelectedPosition(int index) {
        sessionList.getSelectionModel().select(index);
    }

    public int getSelectedItemPosition() {
        return  sessionList.getSelectionModel().getSelectedIndex();
    }

    public int getListSize(){
        return list.size();
    }


    public void removeSelectedLayer() {
        int index = getSelectedItemPosition();
        list.remove(index);
        if (list.isEmpty()) {
            setSelectedPosition(-1);
        } else if (index < list.size()) {
            setSelectedPosition(index);
        } else {
            setSelectedPosition(index - 1);
        }
    }
}
