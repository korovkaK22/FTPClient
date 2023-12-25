package com.example.ftpclient.utils;

import javafx.scene.control.TreeItem;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Objects;

public class FtpFilesUtil {
    private static final Logger logger = LogManager.getRootLogger();

    /**
     * Повертає шлях вибраного елемента
     * @param selectedItem елемент
     * @return шлях
     * @throws NullPointerException якщо елемент null
     */
    public static String getSelectedPath(TreeItem<FTPFile> selectedItem) throws NullPointerException {
        if (selectedItem == null) {
            throw new NullPointerException("Item is null");
        }
        StringBuilder path = new StringBuilder(selectedItem.getValue().getName());
        TreeItem<FTPFile> parent = selectedItem.getParent();

        while (parent != null && parent.getValue() != null) {
            path.insert(0, parent.getValue().getName() + "/");
            parent = parent.getParent();
        }

        return path.toString();
    }


    /**
     * Вивантажити повну бібліотеку чи файл рекурсивно на сервер
     * @param localDirectory звідки вивантажити файли
     * @param remoteDirectoryPath куди завантажити дані
     * @param ftpClient клієнт з підключеною сесією
     * @throws IOException
     */
    public static void uploadFileOrDirectory(File localDirectory, String remoteDirectoryPath, FTPClient ftpClient) throws IOException {
        for (File file : Objects.requireNonNull(localDirectory.listFiles())) {
            if (file.isFile()) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    String remoteFilePath = remoteDirectoryPath + "/" + file.getName();
                    logger.debug("Upload file: "+remoteFilePath);
                    ftpClient.storeFile(remoteFilePath, fileInputStream);
                }
            } else if (file.isDirectory()) {
                String newRemoteDirectoryPath = remoteDirectoryPath + "/" + file.getName();
                logger.debug("Upload directory: "+newRemoteDirectoryPath);
                ftpClient.makeDirectory(newRemoteDirectoryPath);
                uploadFileOrDirectory(file, newRemoteDirectoryPath, ftpClient);
            }
        }
    }

    /**
     * Скачує директорію та всі файли, які є всередині на локальний комп'ютер
     * @param remotePath директорія на сервері
     * @param localDirectory директорія на локальному комп'ютері
     * @param ftpClient клієнт з підключеною сессією
     */
    public static void downloadDirectory(String remotePath, File localDirectory, FTPClient ftpClient) throws IOException {
        FTPFile[] files = ftpClient.listFiles(remotePath);
        for (FTPFile file : files) {
            String remoteFilePath = remotePath + "/" + file.getName();
            File localFile = new File(localDirectory, file.getName());
            if (file.isDirectory()) {
                localFile.mkdir();
                downloadDirectory(remoteFilePath, localFile, ftpClient);
            } else {
               FtpFilesUtil.downloadFile(remoteFilePath, localFile, ftpClient);
            }
        }
    }

    /**
     * Скачує файл на локальний комп'ютер
     * @param remoteFilePath шлях файлу на сервері
     * @param localFile файл на комп'ютері, куди потрібно записати
     * @param ftpClient клієнт з підключеною сессією
     */
    public static void downloadFile(String remoteFilePath, File localFile, FTPClient ftpClient)  {
        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.retrieveFile(remoteFilePath, outputStream);
        } catch (Exception e){
            logger.error(String.format("Can't download file %s : %s",remoteFilePath, e.getMessage()), e);
            e.printStackTrace();
        }
    }


    /**
     * Видаляє директорію або файл на сервері
     * @param remotePath адреса на сервері файлу
     * @param ftpClient фтп клієнт з підключеним з'єднанням
     * @return
     */
    public static boolean deleteRemoteFileOrDirectory(String remotePath, FTPClient ftpClient) throws IOException {
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files != null && files.length > 0) {
                // Якщо це директорія з файлами/піддиректоріями, видалити їх спочатку
                for (FTPFile file : files) {
                    String childPath = remotePath + "/" + file.getName();
                    if (file.isDirectory()) {
                        deleteRemoteFileOrDirectory(childPath, ftpClient);
                    } else {
                        ftpClient.deleteFile(childPath);
                    }
                }
            }
            return ftpClient.removeDirectory(remotePath) || ftpClient.deleteFile(remotePath);
    }


}
