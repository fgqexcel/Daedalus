package daedalus;

import com.sun.deploy.util.SystemUtils;
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
    private Text filename;
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
    @FXML
    private TextField extraFiles;

    private boolean named = false, saved = false, canCompile = true, compiled = false, fresh = true;
    private File file = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        newfile.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        openfile.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        savefile.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        compilefile.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        runfile.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
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
        compiled = named = saved = false;
        file = null;
        codeInput.clear();
        if (canCompile) {
            codeOutput.clear();
        }
        filename.setText("New File");
        removeBold();
        fresh = true;
    }

    @FXML
    private void menuOpen(ActionEvent event) {
        if (!fresh && !saved) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Unsaved progress!");
            alert.setHeaderText("You're about to closethis file without saving!");
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Verilog File");
        if (file != null) {
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());
        }
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Verilog VHDL file", ".vl"));
        file = fileChooser.showOpenDialog(codeInput.getScene().getWindow());
        if (file == null) {
            return;
        }
        filename.setText(file.getName());
        saved = named = true;
        compiled = false;
        try (Scanner read = new Scanner(file)) {
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
        binaryName.setText(filename.getText());
    }

    @FXML
    private void menuSave(ActionEvent event) {
        if (!named) {
            menuSaveAs(event);
            return;
        }
        if (saved) {
            return;
        }
        try (PrintWriter write = new PrintWriter(file)) {
            write.write(codeInput.getText());
            saved = true;
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
        if (file != null) {
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());
        } else {
            fileChooser.setInitialFileName("new.vl");
        }
        file = fileChooser.showSaveDialog(codeInput.getScene().getWindow());
        try (PrintWriter write = new PrintWriter(file)) {
            write.write(codeInput.getText());
            named = saved = true;
            removeBold();
            filename.setText(file.getName().substring(0, file.getName().lastIndexOf('.')));
            binaryName.setText(filename.getText());
        } catch (Exception e) {
            System.out.println("Error occured saving file: " + e);
        }

    }

    @FXML
    private void menuCompile(ActionEvent event) {
        if (!canCompile) {
            return;
        }
        if (!saved) {
            menuSave(null);
        }
        codeOutput.clear();
        try {
            List<String> args = new ArrayList();
            String base = (file.getParentFile().getAbsolutePath() + "/").replace('/', file.getAbsolutePath().contains("\\") ? '\\' : '/');
            args.add("iverilog");
            args.add("-o");
            args.add(base + binaryName.getText());
            args.add(file.getAbsolutePath());
            for (String s : (extraFiles.getText().replace('/', file.getAbsolutePath().contains("\\") ? '\\' : '/').split(" "))) {
                args.add(base + s);
            }
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                codeOutput.appendText(line);
            }
            reader.close();
            if (p.exitValue() == 0) {
                codeOutput.appendText("Code compiled successfully!\n");
                compiled = true;
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
        if (!compiled) {
            menuCompile(null);
        }
        if (!compiled) {
            return;
        }
        codeOutput.clear();
        try {
            ProcessBuilder pb = new ProcessBuilder("vvp",
                    (file.getParentFile().getAbsolutePath() + "/").replace('/', file.getAbsolutePath().contains("\\") ? '\\' : '/') + binaryName.getText());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                codeOutput.appendText(line);
            }
            reader.close();
            if (p.exitValue() == 0) {
                codeOutput.appendText("\nCode ran successfully!\n");
                compiled = true;
            } else {
                codeOutput.appendText("\nCode was unable to run!\n");
            }

        } catch (Exception e) {
            codeOutput.appendText("\nError occured compiling: " + e);
        }
    }

    @FXML
    public void menuQuit(Event event) {
        if (saved || fresh) {
            ((Stage) codeInput.getScene().getWindow()).close();
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
            menuSave(null);
            ((Stage) codeInput.getScene().getWindow()).close();
        } else if (result.get() == nosave) {
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
        if (event.isControlDown()) {
            return;
        }
        fresh = false;
        if (saved || !named) {
            if ((int) event.getCharacter().charAt(0) == 13 || (int) event.getCharacter().charAt(0) == 10) {
                for (int i = 0; i < (codeInput.getParagraphs().get(codeInput.getParagraphs().size() - 2)).chars().filter(ch -> ch == 9).count(); i++) {
                    codeInput.appendText("\t");
                }
            }
            Font oldFont = filename.getFont();
            compiled = saved = false;
            filename.setFont(Font.font(oldFont.getFamily(), FontWeight.BOLD, oldFont.getSize()));
        }
    }

    private void removeBold() {
        Font oldFont = filename.getFont();
        filename.setFont(Font.font(oldFont.getFamily(), oldFont.getSize()));
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
    private void helpExtraBinary(MouseEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("How to use 'binary name' and 'extra files' field");
        alert.setHeaderText(null);
        alert.setContentText("The left box is the name used to compile the binary (AKA the value supplied to iverilog when using the '-o' argument. "
                + "The right box is a list of all other files to be compiled into the binary, all separated by spaces, and all written using a path RELATIVE to the "
                + "CURRENTLY open file (please include the file extention, Ex: .vl).\n\niverilog -o <binary name> <current file> <list of extra files>");

        alert.showAndWait();
    }

}
