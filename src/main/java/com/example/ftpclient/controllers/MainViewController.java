package com.example.ftpclient.controllers;

import com.example.ftpclient.ClientApplication;
import com.example.ftpclient.exceptions.CreateItemException;
import com.example.ftpclient.files.DummyFile;
import com.example.ftpclient.managers.ListManager;
import com.example.ftpclient.managers.SessionManager;
import com.example.ftpclient.memento.ConnectionMemento;
import com.example.ftpclient.utils.FTPUtil;
import com.example.ftpclient.utils.FtpFilesUtil;
import com.example.ftpclient.utils.TreeViewUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MainViewController implements Initializable {
    private static final Logger logger = LogManager.getRootLogger();

    @FXML
    @Getter
    private ListView<String> sessionList;

    @FXML
    private Label errorLabel;

    @FXML
    private Button uploadFolderButton;

    @FXML
    private Button downloadButton;

    @Setter
    private Consumer<Parent> openWindow;

    @FXML
    private Button uploadFileButton;

    @FXML
    private Button deleteButton;

    @Setter
    private FTPClient ftpClient;

    @FXML
    private TreeView<FTPFile> treeViewPanel;

    private ListManager listManager;
    private SessionManager sessionManager;

    private String currentSessionName;


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

        listManager = new ListManager(sessionList);
        sessionManager = new SessionManager();

        sessionList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && (int) newValue >= 0) {
                changeConnection();
            }
        });

        disableAllButtons();
        treeViewPanel.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateButtonsBasedOnSelection(newValue);
            resetErrorLabel();
        });
    }

    /**
     * Вивкнути всі кнопки управління, що справа
     */
    private void disableAllButtons() {
        uploadFolderButton.setDisable(true);
        downloadButton.setDisable(true);
        uploadFileButton.setDisable(true);
        deleteButton.setDisable(true);

    }


    /**
     * Ініціалізує TreeView на початку
     */
    public void initPanel() {
        logger.debug("Panel init");
        TreeItem<FTPFile> rootItem = getRootItem(ftpClient);
        treeViewPanel.setRoot(rootItem);

    }

    private TreeItem<FTPFile> getRootItem(FTPClient client) {
        try {
            FTPFile rootFile = new DummyFile();
            rootFile.setType(FTPFile.DIRECTORY_TYPE);
            rootFile.setName(client.printWorkingDirectory());
            TreeItem<FTPFile> result = new TreeItem<>(rootFile);
            TreeViewUtils.loadDirectoryInsideItem(client.printWorkingDirectory(), result, client);
            return result;
        } catch (IOException e) {
            String message = "Can't make rootItem: " + e.getMessage();
            logger.error(message, e);
            setErrorText(message);
            throw new CreateItemException(message);
        }
    }


    public void addSession(String name, FTPClient client) {
        listManager.add(name);
        ConnectionMemento memento = new ConnectionMemento(client, getRootItem(client));
        sessionManager.save(name, memento);
        if (listManager.getListSize() == 1){
            listManager.setSelectedPosition(0);
        }
    }

    private void changeConnection() {
        if (ftpClient != null) {
            sessionManager.save(currentSessionName, getMemento());
        }
        currentSessionName = listManager.getCurrentSessionName();
        ConnectionMemento memento = sessionManager.get(currentSessionName);
        setMemento(memento);
    }


    public ConnectionMemento getMemento() {
        return new ConnectionMemento(ftpClient, treeViewPanel.getRoot());
    }

    public void setMemento(ConnectionMemento memento) {
        this.ftpClient = memento.getFtpClient();
        treeViewPanel.setRoot(memento.getRootItem());
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
        if (ftpClient != null) {
            initPanel();
        }
    }

    /**
     * Повертає директорію, у якій знахходитсья файл; якщо файл є папкою,
     * то повертає директорію самої папки
     *
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

    @FXML
    void addSessionButtonAction(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(ClientApplication.class.getResource("/fxml/login-view.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setAction((a, b, c) -> {
            FTPClient client = FTPUtil.createNewSession(a, b, c);
            addSession(a, client);
        });
        openWindow.accept(root);
    }

    @FXML
    void removeSessionButtonAction(ActionEvent event) throws IOException {
        sessionManager.remove(currentSessionName);
        listManager.removeSelectedLayer();
        treeViewPanel.setRoot(null);
        currentSessionName = null;
        ftpClient.disconnect();
        ftpClient = null;
    }


    /**
     * Вивести повідомелення про помилку на лейбл
     *
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
     *
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