package com.example.ftpclient.utils;

import com.example.ftpclient.files.DummyFile;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class TreeViewUtils {
    private static final Logger logger = LogManager.getRootLogger();

    /**
     * Завантажити в Item всі файли, що в ньому повинні бути.
     * @param path шлях, по якому перебуває Item
     * @param parentItem Item
     * @param ftpClient клієнт з підключеною сессією
     */
    public static void loadDirectoryInsideItem(String path, TreeItem<FTPFile> parentItem, FTPClient ftpClient) {
        try{
        FTPFile[] files = ftpClient.listFiles(path);
        Platform.runLater(() -> {
            parentItem.getChildren().clear();
            for (FTPFile file : files) {
                TreeItem<FTPFile> treeItem = getTreeItem(file, path, ftpClient);
                parentItem.getChildren().add(treeItem);
            }
            if (files.length == 0 && parentItem.getValue().isDirectory()){
                FTPFile dummyFile = new DummyFile();
                dummyFile.setName("(Empty)");
                TreeItem<FTPFile> dummyItem = new TreeItem<>(dummyFile);
                parentItem.getChildren().add(dummyItem);
            }
        });} catch (Exception e){
            logger.error("Can't make TreeItem "+ path, e);
        }
    }

    /**
     * @param file Ініціалізація файлу/директорії у TreeItem
     * @param path адреса, за якою знаходиться файл/Директорія
     * @param ftpClient клієнт з підключеною сессією
     * @return TreeItem
     */
    public static TreeItem<FTPFile> getTreeItem(FTPFile file, String path, FTPClient ftpClient){
        TreeItem<FTPFile> treeItem = new TreeItem<>(file);
        if (file.isDirectory()) {
            FTPFile dummyFile = new DummyFile();
            dummyFile.setName("(Empty)");
            TreeItem<FTPFile> dummyItem = new TreeItem<>(dummyFile);
            treeItem.getChildren().add(dummyItem);
            treeItem.addEventHandler(TreeItem.branchExpandedEvent(), event -> {
                try {
                    if (treeItem.getChildren().contains(dummyItem)) {
                        String childPath = path + "/" + file.getName();
                        if (ftpClient.listFiles(childPath).length != 0) {
                            treeItem.getChildren().remove(dummyItem);
                            loadDirectoryInsideItem(childPath, treeItem, ftpClient);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Can't make TreeItem "+ path, e);
                    e.printStackTrace();
                }
            });
        }
        return treeItem;
    }


}
