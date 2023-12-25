package com.example.ftpclient.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getRootLogger();

    @FXML
    private Label errorLabel;

    @FXML
    private Button uploadFolderButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button uploadFileButton;

    @FXML
    private Button deleteButton;

    @Setter
    private FTPClient ftpClient;



    @FXML
    private TreeView<FTPFile> treeViewPanel;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treeViewPanel.setCellFactory(tv -> new TreeCell<FTPFile>() {
            @Override
            protected void updateItem(FTPFile file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });

        treeViewPanel.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateButtonsBasedOnSelection(newValue);
        });

        // Початкове вимкнення кнопок
        uploadFolderButton.setDisable(true);
        downloadButton.setDisable(true);
        uploadFileButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void updateButtonsBasedOnSelection(TreeItem<FTPFile> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) {
            // Нічого не вибрано
            uploadFolderButton.setDisable(true);
            downloadButton.setDisable(true);
            uploadFileButton.setDisable(true);
            deleteButton.setDisable(true);
        } else {
            // Перевірка, чи вибраний елемент є папкою
            boolean isDirectory = selectedItem.getValue().isDirectory();
            boolean isDummy = "(Empty)".equals(selectedItem.getValue().getName());
            uploadFolderButton.setDisable(!isDirectory || isDummy);
            uploadFileButton.setDisable(!isDirectory || isDummy);
            downloadButton.setDisable(isDummy);
            deleteButton.setDisable(isDummy);
        }
    }


    public void initPanel() {
        try {
            logger.debug("Ініт панельки");
            FTPFile rootFile = new FTPFile();
            rootFile.setType(FTPFile.DIRECTORY_TYPE);
            rootFile.setName(ftpClient.printWorkingDirectory());
            TreeItem<FTPFile> rootItem = new TreeItem<>(rootFile);
            treeViewPanel.setRoot(rootItem);
            loadDirectory(ftpClient.printWorkingDirectory(), rootItem);
        } catch (IOException e) {
            logger.error("Can't load treeView Panel: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private String getSelectedPath() {
        TreeItem<FTPFile> selectedItem = treeViewPanel.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return null;
        }

        StringBuilder path = new StringBuilder(selectedItem.getValue().getName());
        TreeItem<FTPFile> parent = selectedItem.getParent();

        while (parent != null && parent.getValue() != null) {
            path.insert(0, parent.getValue().getName() + "/");
            parent = parent.getParent();
        }

        return path.substring(1,path.length());
    }

    public void loadDirectory(String path, TreeItem<FTPFile> parentItem) throws IOException {
        FTPFile[] files = ftpClient.listFiles(path);
        Platform.runLater(() -> {
            parentItem.getChildren().clear();
            for (FTPFile file : files) {
                TreeItem<FTPFile> treeItem = new TreeItem<>(file);
                parentItem.getChildren().add(treeItem);

                if (file.isDirectory()) {
                    FTPFile dummyFile = new FTPFile();
                    dummyFile.setName("(Empty)");
                    dummyFile.setType(FTPFile.DIRECTORY_TYPE);
                    TreeItem<FTPFile> dummyItem = new TreeItem<>(dummyFile);
                    treeItem.getChildren().add(dummyItem);

                    treeItem.addEventHandler(TreeItem.branchExpandedEvent(), event -> {
                        try {
                            if (treeItem.getChildren().contains(dummyItem)) {
                                String childPath = path + "/" + file.getName();
                                if (ftpClient.listFiles(childPath).length != 0) {
                                    treeItem.getChildren().remove(dummyItem);
                                    loadDirectory(childPath, treeItem);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }


    @FXML
    void uploadFileAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Виберіть файл або папку для завантаження");
        File selectedFile = fileChooser.showOpenDialog(null); // Використовуйте ваш Stage, якщо є

        if (selectedFile != null) {
            new Thread(() -> {
                try {
                    String selectedPath = getSelectedPath(); // Отримати шлях на FTP сервері для завантаження
                    if (selectedFile.isDirectory()) {
                        // Якщо вибрана папка, викликаємо метод uploadDirectory
                        uploadDirectory(selectedFile, selectedPath);
                    } else {
                        // Якщо вибраний файл, завантажуємо його безпосередньо
                        try (FileInputStream fileInputStream = new FileInputStream(selectedFile)) {
                            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                            String remoteFilePath = selectedPath + "/" + selectedFile.getName();
                            ftpClient.storeFile(remoteFilePath, fileInputStream);
                        }
                    }
                    Platform.runLater(() -> refreshAndExpandTreeView(Objects.requireNonNull(getSelectedPath())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            System.out.println("Файл або папку не вибрано.");
        }
    }

    private void uploadDirectory(File localDirectory, String remoteDirectoryPath) throws IOException {
        for (File file : Objects.requireNonNull(localDirectory.listFiles())) {
            if (file.isFile()) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    String remoteFilePath = remoteDirectoryPath + "/" + file.getName();
                    logger.debug("Завантажую файл: "+remoteFilePath);//=========
                    ftpClient.storeFile(remoteFilePath, fileInputStream);
                }
            } else if (file.isDirectory()) {
                String newRemoteDirectoryPath = remoteDirectoryPath + "/" + file.getName();
                logger.debug("Завантажую папку: "+newRemoteDirectoryPath);//=========
                ftpClient.makeDirectory(newRemoteDirectoryPath);
                uploadDirectory(file, newRemoteDirectoryPath);
            }
        }
    }

    @FXML
    void deleteAction(ActionEvent event) {
        String selectedPath = getSelectedPath();
        if (selectedPath != null) {
            try {
                if (deleteRemoteFileOrDirectory(selectedPath)) {
                    System.out.println("Елемент видалено успішно.");
                    // Тут можна додати логіку для оновлення TreeView, щоб видалити елемент
                } else {
                    System.out.println("Не вдалося видалити елемент.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Елемент не вибрано.");
        }
    }

    private boolean deleteRemoteFileOrDirectory(String path) throws IOException {
        FTPFile[] files = ftpClient.listFiles(path);
        if (files != null && files.length > 0) {
            // Якщо це директорія з файлами/піддиректоріями, видалити їх спочатку
            for (FTPFile file : files) {
                String childPath = path + "/" + file.getName();
                if (file.isDirectory()) {
                    deleteRemoteFileOrDirectory(childPath);
                } else {
                    ftpClient.deleteFile(childPath);
                }
            }
        }
        refreshAndExpandTreeView(path);
        return ftpClient.removeDirectory(path) || ftpClient.deleteFile(path);
    }

    /**
     * Работає
     *
     * @param event
     */
    @FXML
    void uploadFolderAction(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Виберіть папку для завантаження");
        File selectedDirectory = directoryChooser.showDialog(null); // Використовуйте ваш Stage, якщо є

        if (selectedDirectory != null) {
            new Thread(() -> {
                try {
                    String remoteParentDirectory = getSelectedPath();
                    if (remoteParentDirectory != null) {
                        String remoteDirectoryPath = remoteParentDirectory + "/" + selectedDirectory.getName();
                        ftpClient.makeDirectory(remoteDirectoryPath);
                        uploadDirectory(selectedDirectory, remoteDirectoryPath);

                    }
                } catch (IOException e) {
                    Platform.runLater(e::printStackTrace);
                }
            }).start();
            refreshAndExpandTreeView(Objects.requireNonNull(getSelectedPath()));
        } else {
            System.out.println("Папку не вибрано.");
        }
    }

    /**
     * Работає
     *
     * @param event
     */
    @FXML
    void downloadAction(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Виберіть директорію для збереження");
        File selectedLocalDirectory = directoryChooser.showDialog(null);

        if (selectedLocalDirectory != null) {
            String selectedRemotePath = getSelectedPath();
            if (selectedRemotePath != null && !selectedRemotePath.isEmpty()) {
                new Thread(() -> {
                    try {
                        FTPFile selectedFile = ftpClient.mlistFile(selectedRemotePath);
                        if (selectedFile != null && selectedFile.isDirectory()) {
                            File newLocalDirectory = new File(selectedLocalDirectory, new File(selectedRemotePath).getName());
                            if (!newLocalDirectory.exists()) {
                                newLocalDirectory.mkdir();
                            }
                            downloadDirectory(selectedRemotePath, newLocalDirectory);
                        } else if (selectedFile != null && selectedFile.isFile()) {
                            downloadFile(selectedRemotePath, new File(selectedLocalDirectory, selectedFile.getName()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } else {
            System.out.println("Директорію не вибрано.");
        }
    }
    private void downloadDirectory(String remotePath, File localDirectory) throws IOException {
        FTPFile[] files = ftpClient.listFiles(remotePath);
        for (FTPFile file : files) {
            String remoteFilePath = remotePath + "/" + file.getName();
            File localFile = new File(localDirectory, file.getName());
            if (file.isDirectory()) {
                localFile.mkdir();
                downloadDirectory(remoteFilePath, localFile);
            } else {
                downloadFile(remoteFilePath, localFile);
            }
        }
    }

    private void downloadFile(String remoteFilePath, File localFile) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(localFile)) {
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.retrieveFile(remoteFilePath, outputStream);
        }
    }

    @FXML
    void reloadAction() {
        initPanel();
    }


    public void refreshAndExpandTreeView(String pathToExpand) {
        logger.info("Розширяю дерево до шляху: "+ pathToExpand);
        initPanel(); // Перезавантаження TreeView

        String[] pathParts = pathToExpand.split("/");
        expandToPath(treeViewPanel.getRoot(), pathParts, 0);
    }


    private void expandToPath(TreeItem<FTPFile> item, String[] pathParts, int index) {
        if (item == null || index >= pathParts.length) {
            logger.warn("немає такого об'єкта: "+ index);
            return;
        }

        item.setExpanded(true);
        if (item.getValue().getName().equals(pathParts[index])) {
            for (TreeItem<FTPFile> child : item.getChildren()) {
                expandToPath(child, pathParts, index + 1);
            }
        }
    }

}