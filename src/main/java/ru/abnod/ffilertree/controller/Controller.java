package ru.abnod.ffilertree.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.effect.Bloom;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import org.codehaus.plexus.util.FileUtils;
import ru.abnod.ffilertree.model.TreeItemNode;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private final static ContextMenu rowMenu = new ContextMenu();
    public static String hostname = "Unknown";
    private static SimpleDateFormat dtf = new SimpleDateFormat("dd MMM yy,   HH:mm:ss");
    private static TreeItem<File> copyLink;
    private static TreeItem<File> cutLink;
    private static boolean override = false;
    private static ImageView ledInactiveCopy = new ImageView("/icons/Ledgrey.svg-10.png");
    private static ImageView ledInactiveCut = new ImageView("/icons/Ledgrey.svg-10.png");
    private static ImageView ledActiveCopy = new ImageView("/icons/Ledgreen.svg-10.png");
    private static ImageView ledActiveCut = new ImageView("/icons/Ledgreen.svg-10.png");
    private static AtomicInteger counter = new AtomicInteger(0);


    @FXML
    private TreeTableView<File> treeTableView;
    @FXML
    private TreeTableColumn<File, String> columnName;
    @FXML
    private TreeTableColumn<File, String> columnDate;
    @FXML
    private TreeTableColumn<File, String> columnSize;
    @FXML
    private TextField newItemInput;
    @FXML
    private Button buttonCopy;
    @FXML
    private Button buttonCut;
    @FXML
    private Label copyIndicatorLabel;
    @FXML
    private Label copyCountLabel;

    public void initialize() {

        createContextMenu();

        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("host unknown");
        }

        TreeItemNode root = new TreeItemNode(new File(hostname), new ImageView("/icons/icoPC.png"));
        root.setExpanded(true);

        addChildrens(root, File.listRoots());

        columnName.setCellValueFactory(p -> {
            if (p.getValue().getValue().getParent() == null) {
                return new ReadOnlyStringWrapper(p.getValue().getValue().toString());
            }
            return new ReadOnlyObjectWrapper<>(p.getValue().getValue().getName());
        });
        columnDate.setCellValueFactory(p -> {
            if (p.getValue().getValue().exists()) {
                return new ReadOnlyStringWrapper(dtf.format(new Date(p.getValue().getValue().lastModified())));
            } else return new ReadOnlyStringWrapper(null);
        });
        columnSize.setCellValueFactory(p -> {
            if (p.getValue().getValue().isFile()) {
                double sizeKb = p.getValue().getValue().length() / 1024;
                if (sizeKb > 1023) {
                    if (sizeKb > 1048575) {
                        return new ReadOnlyStringWrapper(String.format("%.2f %s", sizeKb / 1048576, "Gb"));
                    }
                    return new ReadOnlyStringWrapper(String.format("%.2f %s", sizeKb / 1024, " Mb"));
                }
                return new ReadOnlyStringWrapper(String.format("%.2f %s", sizeKb, " Kb"));
            } else return new ReadOnlyStringWrapper("");
        });

        treeTableView.setRowFactory((TreeTableView<File> treeTableView) -> {
            final TreeTableRow<File> row = new TreeTableRow<>();
            row.contextMenuProperty().bind(Bindings.when(Bindings.isNotNull(row.itemProperty()))
                    .then(rowMenu)
                    .otherwise((ContextMenu) null));
            return row;
        });

        treeTableView.setRoot(root);
        Bloom bloom = new Bloom(0.1);
        ledActiveCopy.setEffect(bloom);
        ledActiveCut.setEffect(bloom);
        buttonCopy.setGraphic(ledInactiveCopy);
        buttonCut.setGraphic(ledInactiveCut);
    }

    private void addChildrens(TreeItemNode treeItemNode, File[] filesToAdd) {
        for (File file : filesToAdd) {
            TreeItemNode temp = new TreeItemNode(file);
            treeItemNode.getChildren().add(temp);
        }
    }

    public void launchFile(MouseEvent mouseEvent) {
        if (mouseEvent.isPrimaryButtonDown() && (mouseEvent.getClickCount() == 2)) {
            try {
                if (treeTableView.getSelectionModel().getSelectedItem() != null) {
                    File selectedFile = treeTableView.getSelectionModel().getSelectedItem().getValue();

                    if (selectedFile.isFile()) {
                        Desktop.getDesktop().open(selectedFile);
                    }
                }
            } catch (IOException ex) {
                alertShow("Access Error", null, "Error getting access to file", Alert.AlertType.INFORMATION);
            }
        }
    }


    public void itemCreate() {
        TreeItem<File> temp = treeTableView.getSelectionModel().getSelectedItem();
        if (temp != null) {
            File parentPrefix = temp.getValue();
            if (!treeTableView.getSelectionModel().getSelectedItem().getValue().getName().equals(hostname)) {
                String newName = newItemInput.getText();
                Pattern pattern = Pattern.compile("[\\?\\\"\\\\\\/:\\*<>\\|]");
                Matcher matcher = pattern.matcher(newName);
                if ((!newName.equals("")) && !(matcher.find())) {
                    String newItemName = parentPrefix + File.separator + newName;
                    File itemToCreate = new File(newItemName);
                    if (!itemToCreate.exists()) {
                        try {
                            if (itemToCreate.getName().contains(".")) {
                                itemToCreate.createNewFile();
                            } else {
                                itemToCreate.mkdir();
                            }
                            temp.getChildren().add(new TreeItemNode(itemToCreate));
                        } catch (IOException e) {
                            alertShow("Access Error", null, "Creation failed", Alert.AlertType.INFORMATION);
                        }
                    }
                }
            }
        }
    }

    public void itemCopy() {
        if (treeTableView.getSelectionModel().getSelectedItem() != null && !treeTableView.getSelectionModel().getSelectedItem().getValue().getName().equals(hostname)) {
            cutLink = null;
            buttonCopy.setGraphic(ledActiveCopy);
            copyLink = treeTableView.getSelectionModel().getSelectedItem();
            buttonCut.setGraphic(ledInactiveCut);
        }
    }

    public void itemCut() {
        if (treeTableView.getSelectionModel().getSelectedItem() != null && !treeTableView.getSelectionModel().getSelectedItem().getValue().getName().equals(hostname)) {
            copyLink = null;
            buttonCut.setGraphic(ledActiveCut);
            cutLink = treeTableView.getSelectionModel().getSelectedItem();
            buttonCopy.setGraphic(ledInactiveCopy);
        }
    }

    public void itemPaste() {
        TreeItem<File> destination = treeTableView.getSelectionModel().getSelectedItem();
        if (destination != null && copyLink != null) {
            if (!destination.getValue().getAbsolutePath().equals(copyLink.getParent().getValue().getAbsolutePath())) {
                promptShow("Confirm Copying", "Prepare to copy from " + copyLink.getValue() + " to " + destination.getValue(),
                        "Choose copy mode.");

                Task task = new Task() {
                    @Override
                    protected Object call() throws Exception {
                        paste(copyLink, destination, override, false);
                        return null;
                    }
                };
                new Thread(task).start();
            }
        } else if (destination != null && cutLink != null) {
            if (!destination.getValue().getAbsolutePath().equals(cutLink.getParent().getValue().getAbsolutePath())) {
                promptShow("Confirm Move", "Prepare to move from " + cutLink.getValue() + " to " + destination.getValue(),
                        "Choose move mode.");
                setCopyProcessLabel(1);
                Task task = new Task() {
                    @Override
                    protected Object call() throws Exception {
                        paste(cutLink, destination, override, true);
                        return null;
                    }
                };
                new Thread(task).start();
                buttonCut.setGraphic(ledInactiveCut);
            }
        }
    }

    private void paste(TreeItem<File> source, TreeItem<File> destination, boolean override, boolean move) {
        Platform.runLater(() -> setCopyProcessLabel(1));
        try {
            File sourceFile = source.getValue();
            File checkFile = new File(destination.getValue().toString() + File.separator + sourceFile.getName());
            if (override || !checkFile.exists()) {
                Files.walkFileTree(sourceFile.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                Path target = checkFile.toPath().resolve(sourceFile.toPath().relativize(dir));
                                try {
                                    Files.copy(dir, target);
                                } catch (FileAlreadyExistsException e) {
                                    if (!Files.isDirectory(target))
                                        throw e;
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.copy(file, checkFile.toPath().resolve(sourceFile.toPath().relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                if (move) {
                    if (sourceFile.isDirectory()) {
                        FileUtils.deleteDirectory(sourceFile);
                        source.getParent().getChildren().remove(source);
                    } else if (sourceFile.isFile()) {
                        FileUtils.fileDelete(sourceFile.toString());
                        source.getParent().getChildren().remove(source);
                    }
                    cutLink = null;
                } else update(destination);
            }
        } catch (IOException e) {
            Platform.runLater(() -> alertShow("Copy Error", null, "Some files were not copied...", Alert.AlertType.ERROR));
        }
        Platform.runLater(() -> setCopyProcessLabel(-1));
    }

    public void itemDelete() {
        TreeItem<File> temp = treeTableView.getSelectionModel().getSelectedItem();
        if (temp != null) {
            if (!treeTableView.getSelectionModel().getSelectedItem().getValue().getName().equals(hostname)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Remove files");
                alert.setHeaderText("Are you sure?");
                alert.setContentText("Remove " + temp.getValue().getName() + "?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    try {
                        deletion(temp);
                    } catch (IOException e) {
                        alertShow("Access Error", null, "Some objects can not be deleted", Alert.AlertType.INFORMATION);
                    }
                }
            }
        }
    }

    private void deletion(TreeItem<File> treeItem) throws IOException {
        if (!treeItem.getChildren().isEmpty()) {
            ObservableList<TreeItem<File>> listToDelete = treeItem.getChildren();
            TreeItem<File>[] arrayDel = new TreeItem[listToDelete.size()];
            listToDelete.toArray(arrayDel);
            for (TreeItem<File> trf : arrayDel) {
                deletion(trf);
            }
        }
        if (treeItem.getValue().delete()) {
            treeItem.getParent().getChildren().remove(treeItem);
        } else throw new IOException();
    }

    private void alertShow(String title, String header, String text, Alert.AlertType alertType) {
        Alert alert2 = new Alert(alertType);
        alert2.setTitle(title);
        alert2.setHeaderText(header);
        alert2.setContentText(text);
        alert2.showAndWait();
    }

    private void promptShow(String title, String header, String context) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);

        ButtonType buttonTypeOne = new ButtonType("Keep existing files");
        ButtonType buttonTypeTwo = new ButtonType("Override existing files");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne || result.get() == buttonTypeTwo) {
            override = result.get() == buttonTypeTwo;
        } else {
            alert.close();
        }
    }

    public void update() {
        TreeItemNode node = (TreeItemNode) treeTableView.getSelectionModel().getSelectedItem();
        node.update();
    }

    private void update(TreeItem<File> treeItem) {
        TreeItemNode node = (TreeItemNode) treeItem;
        node.update();
    }

    public void closeProgram() {
        Platform.exit();
    }

    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("FFileR Tree v0.1a" + System.lineSeparator() + "mail to: ganeroth@gmail.com");
        alert.showAndWait();
    }

    private void createContextMenu() {
        MenuItem cutMenu = new MenuItem("Cut");
        MenuItem copyMenu = new MenuItem("Copy");
        MenuItem pasteMenu = new MenuItem("Paste", new ImageView("/icons/paste.png"));
        MenuItem deleteMenu = new MenuItem("Delete", new ImageView("/icons/trash.png"));
        MenuItem updateMenu = new MenuItem("Update", new ImageView("/icons/update.png"));

        cutMenu.setOnAction(t -> itemCut());
        copyMenu.setOnAction(t -> itemCopy());
        pasteMenu.setOnAction(t -> itemPaste());
        deleteMenu.setOnAction(t -> itemDelete());
        updateMenu.setOnAction(t -> update());

        rowMenu.getItems().addAll(copyMenu, cutMenu, pasteMenu, deleteMenu, updateMenu);
    }

    private synchronized void setCopyProcessLabel(int value) {
        counter.addAndGet(value);
        if (counter.get() > 0) {
            copyCountLabel.setText(String.valueOf(counter.get()));
            copyIndicatorLabel.setVisible(true);
            copyCountLabel.setVisible(true);
        } else {
            copyIndicatorLabel.setVisible(false);
            copyCountLabel.setVisible(false);
        }
    }
}
