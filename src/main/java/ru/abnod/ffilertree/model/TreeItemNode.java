package ru.abnod.ffilertree.model;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ru.abnod.ffilertree.controller.Controller.hostname;

public class TreeItemNode extends TreeItem<File> {
    private static FileSystemView view = FileSystemView.getFileSystemView();
    private static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();
    private boolean childrenLoaded = false;
    private boolean directory;

    public TreeItemNode(File value) {
        super(value);
        directory = value.isDirectory();
        if (!directory) {
            childrenLoaded = true;
        }
        setIcon(value);
    }

    public TreeItemNode(File value, Node graphic) {
        super(value, graphic);
        if (!value.isDirectory()) {
            childrenLoaded = true;
        }
    }

    private static Image getFileIcon(String fname) {
        final String ext = getFileExt(fname);

        Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
        if (fileIcon == null) {

            javax.swing.Icon jswingIcon = null;

            File file = new File(fname);
            if (file.exists()) {
                jswingIcon = view.getSystemIcon(file);
            } else {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("icon", ext);
                    jswingIcon = view.getSystemIcon(tempFile);
                } catch (IOException ignored) {
                    // Cannot create temporary file.
                } finally {
                    if (tempFile != null) tempFile.delete();
                }
            }

            if (jswingIcon != null) {
                fileIcon = jswingIconToImage(jswingIcon);
                mapOfFileExtToSmallIcon.put(ext, fileIcon);
            }
        }

        return fileIcon;
    }

    private static String getFileExt(String fname) {
        String ext = ".";
        int p = fname.lastIndexOf('.');
        if (p >= 0) {
            ext = fname.substring(p);
        }
        return ext.toLowerCase();
    }

    private static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
        BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    @Override
    public boolean isLeaf() {
        if (childrenLoaded) {
            return getChildren().isEmpty();
        }
        return false;
    }

    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (childrenLoaded) {
            return super.getChildren();
        }

        childrenLoaded = true;

        File[] list = getValue().listFiles();
        if (list != null) {
            List<TreeItem<File>> children = new ArrayList<>();
            for (File file : list) {
                children.add(new TreeItemNode(file));
            }
            super.getChildren().addAll(children);
        } else {
            super.getChildren().add(null);
            super.getChildren().clear();
        }
        return super.getChildren();
    }

    private void setIcon(File file) {
        if (file.getParent() == null && !file.getName().equals(hostname)) {
            this.setGraphic(new ImageView("/icoHDD.png"));
            return;
        } else if (!file.getName().equals(hostname)) {
            if (directory) {
                setGraphic(new ImageView("/icoFolderClosed.png"));
                test();
            } else {
                Image fxImage = getFileIcon(file.getName());
                this.setGraphic(new ImageView(fxImage));
            }
        }
    }

    private void test(){
        this.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue){setGraphic(new ImageView("/icoFolderOpen.png"));
            } else {setGraphic(new ImageView("/icoFolderClosed.png"));}
        });
    }

    public void update(){
        if (childrenLoaded) childrenLoaded = false;
        super.getChildren().clear();
        getChildren();
    }
}