package daedalus;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author Pixel
 */
public class UIController implements Initializable {

    @FXML
    private TextArea codeInput;
    @FXML
    private TextArea codeOutput;
    @FXML
    private MenuItem newfile;
    @FXML
    private MenuItem openfile;
    @FXML
    private MenuItem savefile;
    @FXML
    private MenuItem compilefile;
    @FXML
    private MenuItem runfile;
    @FXML
    private TextField binaryName;
    // The two text objects for each open tab will be at indexes currentTab*2 (file name) and currentTab*2-1 (the 'X')
    // index is referring to the index the the tablist's list of children (tablist.getChildren())
    @FXML
    private HBox tablist;

    private boolean canCompile = true;
    private List<Tab> openTabs = new ArrayList();
    private int currentTab;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set keyboard shortcuts and make the first tab
        newfile.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        openfile.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        savefile.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        compilefile.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        runfile.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
        Text name = new Text("New File");
        name.setFont(Font.font(14));
        name.setUnderline(true);
        name.setOnMouseClicked(e -> switchToTab(e));
        tablist.getChildren().add(name);
        name = new Text("X");
        name.setFont(Font.font(14));
        name.setFont(Font.font(name.getFont().getFamily(), FontWeight.BOLD, name.getFont().getSize()));
        name.setOnMouseClicked(e -> closeTab(e));
        tablist.setSpacing(5);
        tablist.getChildren().add(name);
        openTabs.add(new Tab());
        currentTab = 0;
        // if the icarus binaries aren't found, don't allow compilation
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("iverilog");
            proc.waitFor();
            proc = rt.exec("vvp");
            proc.waitFor();
        } catch (Exception e) {
            System.out.println("Error occured checking for icarus binaries: " + e);
            canCompile = false;
            codeOutput.setText("\"iverilog\" and \"vvp\" could not be located, "
                    + "please insure that icarus is installed on your system and the binaries have been added to the system path.");
        }
    }

    @FXML
    private void menuNew(ActionEvent event) {
        // Save the current tab's text and binaryName before changing it to the new tab's
        openTabs.get(currentTab).text = codeInput.getText();
        openTabs.get(currentTab).binaryName = binaryName.getText();
        binaryName.clear();
        ((Text) tablist.getChildren().get(2 * currentTab)).setUnderline(false);
        Text name = new Text("New File");
        name.setFont(Font.font(14));
        name.setUnderline(true);
        name.setOnMouseClicked(e -> switchToTab(e));
        tablist.getChildren().add(name);
        name = new Text("X");
        name.setFont(Font.font(14));
        name.setFont(Font.font(name.getFont().getFamily(), FontWeight.BOLD, name.getFont().getSize()));
        name.setOnMouseClicked(e -> closeTab(e));
        tablist.setSpacing(5);
        tablist.getChildren().add(name);
        openTabs.add(new Tab());
        currentTab = openTabs.size() - 1;
        codeInput.clear();
        if (canCompile) {
            codeOutput.clear();
        }
    }

    @FXML
    private void menuOpen(ActionEvent event) {
        if (!openTabs.get(currentTab).fresh && !openTabs.get(currentTab).saved) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Unsaved progress!");
            alert.setHeaderText("You're about to close this file without saving!");
            alert.setContentText("I'm sure you were so busy realizing your logic that you didn't realize you haven't saved!");

            ButtonType save = new ButtonType("Save");
            ButtonType nosave = new ButtonType("Don't save");
            ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(save, nosave, cancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == save) {
                menuSave(null);
            } else if (result.get() == nosave) {
            } else {
                return;
            }
        }
        // The base directory is set to where the last open file was (in this tab only. I'll have it be global to all tabs later)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Verilog File");
        if (openTabs.get(currentTab).file != null) {
            fileChooser.setInitialDirectory(openTabs.get(currentTab).file.getParentFile());
            fileChooser.setInitialFileName(openTabs.get(currentTab).file.getName());
        }
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Verilog VHDL file", ".vl"));
        openTabs.get(currentTab).file = fileChooser.showOpenDialog(codeInput.getScene().getWindow());
        if (openTabs.get(currentTab).file == null) {
            return;
        }

        ((Text) tablist.getChildren().get(currentTab * 2)).setText(openTabs.get(currentTab).file.getName());
        openTabs.get(currentTab).saved = openTabs.get(currentTab).named = true;
        openTabs.get(currentTab).compiled = false;
        try (Scanner read = new Scanner(openTabs.get(currentTab).file)) {
            codeInput.clear();
            if (canCompile) {
                codeOutput.clear();
            }
            while (read.hasNext()) {
                codeInput.appendText(read.nextLine() + '\n');
            }
        } catch (Exception e) {
            System.out.println("Error occured opening file: " + e);
        }
        removeBold();
        binaryName.setText(((Text) tablist.getChildren().get(currentTab * 2)).getText().substring(0, openTabs.get(currentTab).file.getName().lastIndexOf('.')));
        clearFiles(null);
    }

    @FXML
    private void menuSave(ActionEvent event) {
        if (!openTabs.get(currentTab).named) {
            menuSaveAs(event);
            return;
        }
        if (openTabs.get(currentTab).saved) {
            return;
        }
        try (PrintWriter write = new PrintWriter(openTabs.get(currentTab).file)) {
            write.write(codeInput.getText());
            openTabs.get(currentTab).saved = true;
            removeBold();
        } catch (Exception e) {
            System.out.println("Error occured saving file: " + e);
        }
    }

    @FXML
    private void menuSaveAs(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a file to save to");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Verilog VHDL file", ".vl"));
        // The base directory is set to where the last open file was (in this tab only. I'll have it be global to all tabs later)
        if (openTabs.get(currentTab).file != null) {
            fileChooser.setInitialDirectory(openTabs.get(currentTab).file.getParentFile());
            fileChooser.setInitialFileName(openTabs.get(currentTab).file.getName());
        } else {
            fileChooser.setInitialFileName("new.vl");
        }
        openTabs.get(currentTab).file = fileChooser.showSaveDialog(codeInput.getScene().getWindow());
        try (PrintWriter write = new PrintWriter(openTabs.get(currentTab).file)) {
            write.write(codeInput.getText());
            openTabs.get(currentTab).named = openTabs.get(currentTab).saved = true;
            removeBold();
            ((Text) tablist.getChildren().get(currentTab * 2)).setText(openTabs.get(currentTab).file.getName().substring(0, openTabs.get(currentTab).file.getName().lastIndexOf('.')));
            binaryName.setText(((Text) tablist.getChildren().get(currentTab * 2)).getText());
        } catch (Exception e) {
            System.out.println("Error occured saving file: " + e);
        }

    }

    @FXML
    private void menuCompile(ActionEvent event) {
        if (!canCompile) {
            return;
        }
        if (!openTabs.get(currentTab).saved) {
            menuSave(null);
        }
        codeOutput.clear();
        try {
            // construct the command
            List<String> args = new ArrayList();
            String base = (openTabs.get(currentTab).file.getParentFile().getAbsolutePath() + "/").replace('/', openTabs.get(currentTab).file.getAbsolutePath().contains("\\") ? '\\' : '/');
            args.add("iverilog");
            args.add("-o");
            args.add(base + binaryName.getText());
            args.add(openTabs.get(currentTab).file.getAbsolutePath());
            for (String s : openTabs.get(currentTab).extraFiles) {
                args.add(s);
            }
            System.out.println(args);
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream();
            // run the command and direct the output to the codeOutput box
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                codeOutput.appendText(line);
            }
            reader.close();
            if (p.exitValue() == 0) {
                codeOutput.appendText("Code compiled successfully!\n");
                openTabs.get(currentTab).compiled = true;
            } else {
                codeOutput.appendText("\nCode was unable to compile!\n");
            }

        } catch (Exception e) {
            codeOutput.appendText("\nError occured compiling: " + e);
        }
    }

    @FXML
    private void menuRun(ActionEvent event) {
        if (!canCompile) {
            return;
        }
        if (!openTabs.get(currentTab).compiled) {
            menuCompile(null);
        }
        if (!openTabs.get(currentTab).compiled) {
            return;
        }
        codeOutput.clear();
        try {
            // make the command and direct the output to codeOutput
            ProcessBuilder pb = new ProcessBuilder("vvp",
                    (openTabs.get(currentTab).file.getParentFile().getAbsolutePath() + "/").replace('/', openTabs.get(currentTab).file.getAbsolutePath().contains("\\") ? '\\' : '/') + binaryName.getText());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                codeOutput.appendText(line + "\n");
            }
            reader.close();
            if (p.exitValue() == 0) {
                codeOutput.appendText("\nCode ran successfully!\n");
                openTabs.get(currentTab).compiled = true;
            } else {
                codeOutput.appendText("\nCode was unable to run!\n");
            }

        } catch (Exception e) {
            codeOutput.appendText("\nError occured compiling: " + e);
        }
    }

    @FXML
    public void menuQuit(Event event) {
        // for now you have to go back and save files individually. It'll be weird and cumbersome to have it cycle through a bunch
        // of unsaved files and prompting to name and save them
        boolean doSave = false;
        for (Tab t : openTabs) {
            if (t.saved == false && t.fresh == false) {
                doSave = true;
            }
        }
        if (!doSave) {
            ((Stage) codeInput.getScene().getWindow()).close();
            return;
        }
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Unsaved progress!");
        alert.setHeaderText("You're about to close without saving!");
        alert.setContentText("I'm sure you were so busy realizing your logic that you didn't realize you haven't saved!");

        ButtonType save = new ButtonType("Go back to save");
        ButtonType nosave = new ButtonType("Don't save");
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(save, nosave, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == nosave) {
            ((Stage) codeInput.getScene().getWindow()).close();
        } else {
            event.consume();
        }

    }

    @FXML
    private void menuAbout(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About Daedalus");
        alert.setHeaderText("Daedalus Verilog IDE for Icarus");
        alert.setContentText("Daedalus is a minimal IDE for being able to conveniently write, compile, and run verilog files. The word "
                + "'Daedalus' comes from greek mythology. Daedalus was a skillful craftsman, and was the father of Icarus. Seeing that "
                + "this IDE used Icarus to compile and run verilog files, I found this title to be fitting.\n\nIf you have any questions, "
                + "concerns, or recommendations, please feel free to email me at ajp6329@louisiana.edu.\n\n"
                + "The source for this project can be found at https://github.com/pixelrazor\n\nYours truly,\n\tAustin Pohlmann, the sole developer of this and\n\t"
                + "potentially a fellow peer.");

        alert.showAndWait();
    }

    @FXML
    private void keyTyped(KeyEvent event) {
        // I need to find a way to filter out cut/paste/undo/redo
        if (event.isShortcutDown()) {
            return;
        }
        openTabs.get(currentTab).fresh = false;
        if (openTabs.get(currentTab).saved || !openTabs.get(currentTab).named) {

            Font oldFont = ((Text) tablist.getChildren().get(currentTab * 2)).getFont();
            openTabs.get(currentTab).compiled = openTabs.get(currentTab).saved = false;
            ((Text) tablist.getChildren().get(currentTab * 2)).setFont(Font.font(oldFont.getFamily(), FontWeight.BOLD, oldFont.getSize()));
        }
        // This needs to be redone and needs to account for leading spaces for those select few people
        // I'm thinking of using a while loop to go through the previous line 1 char at a time, and stops when it hits a not tab/space
        //
        // this is broken. i need to switch over to using RichTextFX
        /*
        System.out.println(codeInput.getParagraphs().size());
        System.out.println(codeInput.getCaretPosition());
        if ((int) event.getCharacter().charAt(0) == 13 || (int) event.getCharacter().charAt(0) == 10) {
            for (int i = 0; i < (codeInput.getParagraphs().get(codeInput.getParagraphs().size() - 2)).chars().filter(ch -> ch == 9).count(); i++) {
                codeInput.appendText("\t");
            }
        }
         */
    }

    private void removeBold() {
        Font oldFont = ((Text) tablist.getChildren().get(currentTab * 2)).getFont();
        ((Text) tablist.getChildren().get(currentTab * 2)).setFont(Font.font(oldFont.getFamily(), oldFont.getSize()));
    }

    @FXML
    private void menuToggleWrap(ActionEvent event) {
        codeOutput.setWrapText(!codeOutput.isWrapText());
    }

    @FXML
    private void menuClear(ActionEvent event) {
        codeOutput.clear();
    }

    @FXML
    private void addFiles(MouseEvent event) {
        // this is for adding files that need to be compiled and linked to the currently open file
        FileChooser chooser = new FileChooser();
        if (openTabs.get(currentTab).file != null) {
            chooser.setInitialDirectory(openTabs.get(currentTab).file.getParentFile());
        }
        List<File> files = new ArrayList(chooser.showOpenMultipleDialog(codeInput.getScene().getWindow()));
        if (files.size() > 0) {
            for (File f : files) {
                String filepath = f.getAbsolutePath();
                if (!openTabs.get(currentTab).extraFiles.contains(filepath)) {
                    openTabs.get(currentTab).extraFiles.add(filepath);
                    codeOutput.appendText("Added " + filepath + "\n");
                }
            }
        }
    }

    @FXML
    private void viewFiles(MouseEvent event) {
        codeOutput.clear();
        for (String s : openTabs.get(currentTab).extraFiles) {
            codeOutput.appendText(s + "\n");
        }
    }

    @FXML
    private void clearFiles(MouseEvent event) {
        openTabs.get(currentTab).extraFiles.clear();
        codeOutput.clear();
    }

    private void closeTab(MouseEvent event) {
        // this is a mess, I know. I'll tidy this up a bunch at some point in the future
        int index = tablist.getChildren().indexOf(event.getSource()) - 1;
        if (openTabs.get(index / 2).saved || openTabs.get(index / 2).fresh) {
            openTabs.remove(index / 2);
            tablist.getChildren().remove(index);
            tablist.getChildren().remove(index);
            if (openTabs.size() == 0) {
                ((Stage) codeInput.getScene().getWindow()).close();
                return;
            }
            if (index / 2 <= currentTab) {
                currentTab--;
                System.out.println(currentTab);
                binaryName.setText(openTabs.get(currentTab).binaryName);
                codeInput.setText(openTabs.get(currentTab).text);
                ((Text) tablist.getChildren().get(currentTab * 2)).setUnderline(true);
            }
            return;
        }
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Unsaved progress!");
        alert.setHeaderText("You're about to close without saving!");
        alert.setContentText("I'm sure you were so busy realizing your logic that you didn't realize you haven't saved!");

        ButtonType save = new ButtonType("Save");
        ButtonType nosave = new ButtonType("Don't save");
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(save, nosave, cancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == save) {
            if (index / 2 == currentTab) {
                menuSave(null);
            } else {
                String currentCode = codeInput.getText();
                int currTab = currentTab;
                currentTab = index / 2;
                codeInput.setText(openTabs.get(currentTab).text);
                menuSave(null);
                currentTab = currTab;
                codeInput.setText(currentCode);
            }
            openTabs.remove(index / 2);
            tablist.getChildren().remove(index);
            tablist.getChildren().remove(index);
            if (openTabs.size() == 0) {
                ((Stage) codeInput.getScene().getWindow()).close();
                return;
            }
            if (index / 2 <= currentTab) {
                currentTab--;
                binaryName.setText(openTabs.get(currentTab).binaryName);
                codeInput.setText(openTabs.get(currentTab).text);
                ((Text) tablist.getChildren().get(currentTab * 2)).setUnderline(true);
            }
        } else if (result.get() == nosave) {

            if (openTabs.size() == 0) {
                ((Stage) codeInput.getScene().getWindow()).close();
                return;
            }
            openTabs.remove(index / 2);
            tablist.getChildren().remove(index);
            tablist.getChildren().remove(index);
            if (index / 2 <= currentTab) {
                currentTab--;
                binaryName.setText(openTabs.get(currentTab).binaryName);
                codeInput.setText(openTabs.get(currentTab).text);
                ((Text) tablist.getChildren().get(currentTab * 2)).setUnderline(true);
            }
        } else {
            event.consume();
        }
    }

    private void switchToTab(MouseEvent event) {
        // switching tabs = 8 lines, closing tabs = like 70 lines????
        // I honestly thought that this would be longer before I actually sat down to make it
        int index = tablist.getChildren().indexOf(event.getSource());
        openTabs.get(currentTab).text = codeInput.getText();
        openTabs.get(currentTab).binaryName = binaryName.getText();
        binaryName.setText(openTabs.get(index / 2).binaryName);
        codeInput.setText(openTabs.get(index / 2).text);
        ((Text) tablist.getChildren().get(index)).setUnderline(true);
        ((Text) tablist.getChildren().get(currentTab * 2)).setUnderline(false);
        currentTab = index / 2;
        codeOutput.clear();
    }

    // This stores relevant info for each open tab
    private class Tab {

        protected File file;
        protected String text, binaryName;
        protected List<String> extraFiles;
        protected boolean named = false, saved = false, compiled = false, fresh = true;

        public Tab() {
            file = null;
            extraFiles = new ArrayList();
        }

        public Tab(File file, List<String> extraFiles) {
            this.file = file;
            this.extraFiles = extraFiles;
        }

    }
}
