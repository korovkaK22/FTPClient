package com.example.ftpclient.memento;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

@AllArgsConstructor
@Getter
public class ConnectionMemento {
    private FTPClient ftpClient;
    private TreeItem<FTPFile> rootItem;



}
