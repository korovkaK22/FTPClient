package com.example.ftpclient.controllers;

import com.example.ftpclient.files.DummyFile;
import com.example.ftpclient.utils.FtpFilesUtil;
import com.example.ftpclient.utils.TreeViewUtils;
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
        uploadFolderButton.setDisable(true);
        downloadButton.setDisable(true);
        uploadFileButton.setDisable(true);
        deleteButton.setDisable(true);

        treeViewPanel.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateButtonsBasedOnSelection(newValue);
            resetErrorLabel();
        });
    }


    /**
     * Ініціалізує TreeView на початку
     */
    public void initPanel() {
        try {
            logger.debug("Panel init");
            FTPFile rootFile = new DummyFile();
            rootFile.setType(FTPFile.DIRECTORY_TYPE);
            rootFile.setName(ftpClient.printWorkingDirectory());
            TreeItem<FTPFile> rootItem = new TreeItem<>(rootFile);
            treeViewPanel.setRoot(rootItem);
            TreeViewUtils.loadDirectoryInsideItem(ftpClient.printWorkingDirectory(), rootItem, ftpClient);
        } catch (IOException e) {
            logger.error("Can't load treeView Panel: " + e.getMessage(), e);
            setErrorText("Can't upload file: " + e.getMessage());
        }
    }



    /**
     * Викликає меню для вивантаження файлу та зберігає його на сервер
     * Метод викликається кнопкою
     */
    @FXML
    void uploadFileAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Виберіть файл або папку для завантаження");
        File selectedFile = fileChooser.showOpenDialog(null);
        TreeItem<FTPFile> folderItem = getItemFileDirectory(treeViewPanel.getSelectionModel().getSelectedItem());
        String selectedPath = FtpFilesUtil.getSelectedPath(folderItem);
        if (selectedFile != null) {
            new Thread(() -> {
                logger.info(String.format("%s start upload", selectedFile.getName()));
                try {
                    if (selectedFile.isDirectory()) {
                        // Якщо вибрана папка, викликаємо метод uploadDirectory
                        FtpFilesUtil.uploadFileOrDirectory(selectedFile, selectedPath, ftpClient);
                    } else {
                        // Якщо вибраний файл, завантажуємо його безпосередньо
                        try (FileInputStream fileInputStream = new FileInputStream(selectedFile)) {
                            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                            String remoteFilePath = selectedPath + "/" + selectedFile.getName();
                            ftpClient.storeFile(remoteFilePath, fileInputStream);
                        }
                    }
                    Platform.runLater(() -> TreeViewUtils.loadDirectoryInsideItem(selectedPath, folderItem, ftpClient));
                    logger.info(String.format("%s was uploaded", selectedFile.getName()));

                } catch (IOException e) {
                    Platform.runLater(() -> setErrorText("Can't upload file: " + e.getMessage()));
                    logger.error("Can't upload file: " + e.getMessage(), e);
                }
            }).start();
        }
    }

    /**
     * Видаляє обраний файл. Якщо вибрана директорія, видаляє директорію і все, що в ній знаходиться
     * Метод викликається кнопкою
     */
    @FXML
    void deleteAction() {
        TreeItem<FTPFile> itemToDelete = treeViewPanel.getSelectionModel().getSelectedItem();
        TreeItem<FTPFile> itemToReload;
        if (itemToDelete.getValue().isDirectory()) {
            itemToReload = itemToDelete.getParent();
        } else {
            itemToReload = getItemFileDirectory(itemToDelete);
        }
        new Thread(() -> {
            String selectedPath = FtpFilesUtil.getSelectedPath(itemToDelete);
            try {
                if (FtpFilesUtil.deleteRemoteFileOrDirectory(selectedPath, ftpClient)) {
                    Platform.runLater(() -> TreeViewUtils.loadDirectoryInsideItem(
                            FtpFilesUtil.getSelectedPath(itemToReload), itemToReload, ftpClient));
                    logger.info(String.format("%s was deleted", itemToDelete.getValue().getName()));
                } else {
                    String name = itemToDelete.getValue().getName();
                    Platform.runLater(() -> setErrorText(String.format("%s wasn't deleted", name)));
                    logger.error(String.format("%s wasn't deleted: ", name));
                }
            } catch (Exception e) {
                String name = itemToDelete.getValue().getName();
                Platform.runLater(() -> setErrorText(String.format("%s wasn't deleted: %s", name, e.getMessage())));
                logger.error(String.format("%s wasn't deleted: %s", name, e.getMessage()), e);
            }
        }).start();
    }

    /**
     * Викликає меню для вивантаження папки та зберігає її на сервер
     * Метод викликається кнопкою
     */
    @FXML
    void uploadFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose folder to upload");
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            new Thread(() -> {
                TreeItem<FTPFile> selectedFile = treeViewPanel.getSelectionModel().getSelectedItem();
                TreeItem<FTPFile> directoryFile = getItemFileDirectory(selectedFile);
                String remoteParentDirectory = FtpFilesUtil.getSelectedPath(directoryFile);
                logger.info(String.format("Directory %s started upload", selectedDirectory.getName()));
                try {
                    String remoteDirectoryPath = remoteParentDirectory + "/" + selectedDirectory.getName();
                    ftpClient.makeDirectory(remoteDirectoryPath);
                    FtpFilesUtil.uploadFileOrDirectory(selectedDirectory, remoteDirectoryPath, ftpClient);
                } catch (IOException e) {
                    Platform.runLater(() -> setErrorText("Can't upload directory: " + e.getMessage()));
                    logger.error("Can't upload directory: " + e.getMessage(), e);
                }
                Platform.runLater(() -> {
                    TreeViewUtils.loadDirectoryInsideItem(remoteParentDirectory, directoryFile, ftpClient);
                    logger.info(String.format("Directory %s was uploaded", selectedDirectory.getName()));
                });
            }).start();
        }
    }

    /**
     * Викликає меню для скачування та скачує файл
     * Метод викликається кнопкою
     */
    @FXML
    void downloadAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose directory fow download");
        File selectedLocalDirectory = directoryChooser.showDialog(null);

        if (selectedLocalDirectory != null) {
            String selectedRemotePath = FtpFilesUtil.getSelectedPath(treeViewPanel.getSelectionModel().getSelectedItem());
            if (!selectedRemotePath.isEmpty()) {
                new Thread(() -> {
                    try {
                        FTPFile selectedFile = ftpClient.mlistFile(selectedRemotePath);
                        if (selectedFile != null && selectedFile.isDirectory()) {
                            File newLocalDirectory = new File(selectedLocalDirectory, new File(selectedRemotePath).getName());
                            if (!newLocalDirectory.exists()) {
                                newLocalDirectory.mkdir();
                            }
                            FtpFilesUtil.downloadDirectory(selectedRemotePath, newLocalDirectory, ftpClient);
                        } else if (selectedFile != null && selectedFile.isFile()) {
                            FtpFilesUtil.downloadFile(selectedRemotePath, new File(selectedLocalDirectory, selectedFile.getName()), ftpClient);
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> setErrorText("Can't download File(s): " + e.getMessage()));
                        logger.error("Can't download fFile(s): " + e.getMessage(), e);
                    }
                }).start();
            }
            logger.info(String.format("File(s) was downloaded to %s", selectedLocalDirectory.getName()));
        }
    }

    /**
     * Повністю обновляє каталог файлів
     * Метод викликається кнопкою
     */
    @FXML
    void reloadAction() {
        initPanel();
    }

    /**
     * Повертає директорію, у якій знахходитсья файл; якщо файл є папкою,
     * то повертає директорію самої папки
     * @param selectedItem файл
     * @return файл-директорія
     */
    private TreeItem<FTPFile> getItemFileDirectory(TreeItem<FTPFile> selectedItem) {
        FTPFile file = selectedItem.getValue();
        if (file instanceof DummyFile || file.isFile()) {
            return selectedItem.getParent();
        } else {
            return selectedItem;
        }
    }

    /**
     * Вивести повідомелення про помилку на лейбл
     * @param text надпис
     */
    private void setErrorText(String text) {
        errorLabel.setText("ERROR: " + text);
    }

    /**
     * Стерти повідомлення про помилку
     */
    private void resetErrorLabel() {
        errorLabel.setText("");
    }


    /**
     * Обновляє статус кнопок в залежності від вибраного елементу у TreeView
     * @param selectedItem вибрана кнопка
     */
    private void updateButtonsBasedOnSelection(TreeItem<FTPFile> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) {
            uploadFolderButton.setDisable(true);
            downloadButton.setDisable(true);
            uploadFileButton.setDisable(true);
            deleteButton.setDisable(true);
        } else {
            boolean isDummy = selectedItem.getValue() instanceof DummyFile;
            uploadFolderButton.setDisable(false);
            uploadFileButton.setDisable(false);
            downloadButton.setDisable(isDummy);
            deleteButton.setDisable(isDummy);
        }
    }


}