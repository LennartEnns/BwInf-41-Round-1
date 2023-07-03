package application;

import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import java.util.regex.Pattern;
import java.util.Optional;
import java.util.ArrayList;

/** Controller-Klasse der JavaFX-Anwendung */
public class Controller {
    private BaseText baseText;
    private GapText gapText;

    private boolean btIsSaved, btIsCached, btLinesCounted, ignoreBtLineBreaks;
    private boolean gtIsSaved, gtIsCached;
    private Font gtLabelDefaultFont, btLabelDefaultFont;

    private boolean completionsSearched = false;

    private FileChooser fileChooser;
    private Path btDefaultPath, btPath;
    private Path gtDefaultPath, gtPath;

    private final ButtonType doSave = new ButtonType("Speichern");
    private final ButtonType doNotSave = new ButtonType("Nicht speichern");
    private final ButtonType cancel = ButtonType.CANCEL;

    private Stage stage;
    @FXML private MenuItem btSaveItem, btSaveAtItem, gtSaveItem, gtSaveAtItem;
    @FXML private MenuItem btCloseItem, gtCloseItem;

    @FXML private TextArea output, gapTextArea, baseTextArea;
    @FXML private Label gapTextLabel, baseTextLabel;
    @FXML private TextField lineField;
    private int nBaseTextLines;

    @FXML private Button bSearch, bNextIndex, bPreviousIndex;
    @FXML private CheckBox cbMatchCase;
    private boolean matchSearchCase;

    // Themes
    private final String darkTheme = getClass().getResource("dark_theme.css").toExternalForm();
    private final String lightTheme = getClass().getResource("light_theme.css").toExternalForm();

    private ArrayList<Completion> completions;
    private ArrayList<int[]> markedIndices;
    private int currentMarkedIndex;

    private enum saveOptions {
        ASK, ALWAYS, NEVER
    }
    private saveOptions saveOption;

    ////////////////////////////////////////////////////////////////////////////// Initialisierung, Stage-Setter und clearOutput
    public void initialize() {
        btLabelDefaultFont = baseTextLabel.getFont();
        gtLabelDefaultFont = gapTextLabel.getFont();
        resetBaseText();
        resetGapText();

        completions = new ArrayList<Completion>();
        markedIndices = new ArrayList<int[]>();
        saveOption = saveOptions.ASK;
        ignoreBtLineBreaks = false;
        matchSearchCase = false;

        output.setWrapText(true);

        fileChooser = new FileChooser();
        FileChooser.ExtensionFilter textFileFilter = new FileChooser.ExtensionFilter("Textdateien (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(textFileFilter);

        lineField.focusedProperty().addListener((obs, vOld, vNew) -> {
            if (! vNew) {
                onLineEnter();
            }
        });
        lineField.setTextFormatter(new TextFormatter<>(
                new IntegerStringConverter(),
                1,
                c -> Pattern.matches("\\d*", c.getText()) ? c : null )
        );

        baseTextArea.caretPositionProperty().addListener((obs, vOld, vNew) -> {
            currentMarkedIndex = -1;

            if (baseTextArea.isFocused()) {
                baseTextArea.getParent().requestFocus();
                renameLineFieldByPosition((int) vNew);
            }
        });
        baseTextArea.textProperty().addListener((obs, vOld, vNew) -> onBaseTextChanged(vNew));

        gapTextArea.textProperty().addListener((obs, vOld, vNew) -> onGapTextChanged(vNew));

        try {
            loadBaseTextFromFile(new File("Alice_Im_Wunderland.txt"));
        } catch (IOException ignored) {}
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public void clearOutput() {
        output.clear();
        completionsSearched = false;
    }

    ////////////////////////////////////////////////////////////////////////////// Methoden zur Theme-Änderung
    public void switchToDarkTheme() {
        stage.getScene().getRoot().getStylesheets().clear();
        stage.getScene().getRoot().getStylesheets().add(darkTheme);
    }
    public void switchToLightTheme() {
        stage.getScene().getRoot().getStylesheets().clear();
        stage.getScene().getRoot().getStylesheets().add(lightTheme);
    }

    ////////////////////////////////////////////////////////////////////////////// Methoden zum Dateien Auswählen und Speichern
    public String getCurrentDir() {
        try {
            return System.getProperty("user.dir");
        } catch (Exception ignored) {}
        return null;
    }
    public void setToCurrentDir(FileChooser fc) {
        String userDir = getCurrentDir();
        if (userDir != null) {
            File userDirFile = new File(userDir);
            if (userDirFile.canRead()) {
                fc.setInitialDirectory(userDirFile);
            }
        }
    }
    public File chooseFile(String dialogTitle, Path initialPath) {
        fileChooser.setTitle(dialogTitle);
        if (initialPath == null) {
            setToCurrentDir(fileChooser);
        } else {
            fileChooser.setInitialDirectory(initialPath.toFile());
        }

        return fileChooser.showOpenDialog(stage);
    }
    public File saveFile(String dialogTitle, String text) {
        fileChooser.setTitle(dialogTitle);
        setToCurrentDir(fileChooser);
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            if (saveFileAt(Path.of(file.getPath()), text)) {
                return file;
            }
        }
        return null;
    }
    public boolean saveFileAt(Path path, String text) {
        try {
            Files.writeString(path, text, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
    public Optional<ButtonType> requestFileSave(String title, String header) {
        switch(saveOption) {
            case ALWAYS:
                return Optional.of(doSave);
            case NEVER:
                return Optional.of(doNotSave);
        }
        Alert confirmSave = new Alert(Alert.AlertType.CONFIRMATION);
        confirmSave.getButtonTypes().setAll(doSave, doNotSave, cancel);

        confirmSave.setTitle(title);
        confirmSave.setHeaderText(header);
        return confirmSave.showAndWait();
    }

    ////////////////////////////////////////////////////////////////////////////// Speicher-Options-Setter
    public void onSaveAskSelected() {
        saveOption = saveOptions.ASK;
    }
    public void onSaveAlwaysSelected() {
        saveOption = saveOptions.ALWAYS;
    }
    public void onSaveNeverSelected() {
        saveOption = saveOptions.NEVER;
    }

    ////////////////////////////////////////////////////////////////////////////// GapText-Methoden
    public void resetGapText() {
        gtIsSaved = gtIsCached = true;
        gapTextLabel.setFont(gtLabelDefaultFont);
        gapTextLabel.setText("Unbenannt");
        gtCloseItem.setDisable(true);
        gtSaveItem.setDisable(true);
        gtSaveAtItem.setDisable(true);
    }
    public void loadGapText() throws IOException {
        if (! checkGtSave()) {
            return;
        }

        File file = chooseFile("Lückentext öffnen", gtDefaultPath);
        if (file == null) {return;}

        gapText = new GapText(file);

        gapTextArea.setText(gapText.getTextString());
        gtPath = Path.of(file.getPath());
        gtDefaultPath = gtPath.getParent();
        gapTextLabel.setText(file.getName());
        gapTextLabel.setFont(gtLabelDefaultFont);

        gtIsSaved = gtIsCached = true;
        gtSaveItem.setDisable(true);
        gtCloseItem.setDisable(false);

        gapTextArea.positionCaret(0);
        gapTextArea.requestFocus();
    }
    public boolean saveGapText() {
        if (! (gtIsSaved || saveOption == saveOptions.NEVER)) {
            if (gtPath == null) {
                return saveGapTextAt();
            } else if (saveFileAt(gtPath, gapTextArea.getText())){
                gtIsSaved = true; gtSaveItem.setDisable(true);
                gapTextLabel.setFont(gtLabelDefaultFont);
            }
        }
        return true;
    }
    public boolean saveGapTextAt() {
        File file = saveFile("Lückentext speichern", gapTextArea.getText());
        if (file != null) {
            gtPath = Path.of(file.getPath());
            gtDefaultPath = gtPath.getParent();
            gtIsSaved = true; gtSaveItem.setDisable(true);
            gapTextLabel.setFont(gtLabelDefaultFont);
            gapTextLabel.setText(file.getName());
            return true;
        }
        return false;
    }
    public boolean checkGtSave() {
        if (! gtIsSaved) {
            Optional<ButtonType> res = requestFileSave("Lückentext speichern",
                    "Soll der Lückentext gespeichert werden?");
            if (res.isEmpty() || res.get() == cancel) {return false;}
            if (res.get() == doSave) {
                return saveGapText();
            }
        }
        return true;
    }
    public void closeGapText() {
        if (! checkGtSave()) {
            return;
        }
        gapTextArea.clear();
        gapTextLabel.setText("");

        resetGapText();
    }

    ////////////////////////////////////////////////////////////////////////////// BaseText-Methoden
    public void resetBaseText() {
        btIsSaved = btIsCached = btLinesCounted = true;
        baseTextLabel.setFont(btLabelDefaultFont);
        baseTextLabel.setText("Unbenannt");
        btCloseItem.setDisable(true);
        btSaveItem.setDisable(true);
        btSaveAtItem.setDisable(true);
        nBaseTextLines = 1;
        lineField.setText("1");
    }
    public void loadBaseText() throws IOException {
        if (! checkBtSave()) {
            return;
        }
        File file = chooseFile("Suchtext öffnen", btDefaultPath);
        if (file == null) {return;}

        loadBaseTextFromFile(file);
    }
    public void loadBaseTextFromFile(File file) throws IOException {
        baseText = new BaseText(file);
        nBaseTextLines = baseText.getNLines();

        baseTextArea.setText(baseText.getTextString());
        if (file.isFile()) {
            btPath = Path.of(file.getPath());
            btDefaultPath = btPath.getParent();
            if (btDefaultPath == null) {
                btDefaultPath = Path.of(getCurrentDir());
            }
        } else {
            btDefaultPath = Path.of(file.getPath());
            loadBaseText();
            return;
        }
        baseTextLabel.setText(file.getName());
        baseTextLabel.setFont(btLabelDefaultFont);

        btIsSaved = btIsCached = btLinesCounted = ignoreBtLineBreaks = true;
        btSaveItem.setDisable(true);
        btCloseItem.setDisable(false);

        lineField.setText("1");
        baseTextArea.positionCaret(0);
        baseTextArea.requestFocus();
    }
    public boolean saveBaseText() {
        if (! (btIsSaved || saveOption == saveOptions.NEVER)) {
            if (btPath == null) {
                return saveBaseTextAt();
            } else if (saveFileAt(btPath, baseTextArea.getText())) {
                btIsSaved = true; btSaveItem.setDisable(true);
                baseTextLabel.setFont(btLabelDefaultFont);
            }
        }
        return true;
    }
    public boolean saveBaseTextAt(){
        File file = saveFile("Suchtext speichern", baseTextArea.getText());
        if (file != null) {
            btPath = Path.of(file.getPath());
            btDefaultPath = btPath.getParent();
            btIsSaved = true; btSaveItem.setDisable(true);
            baseTextLabel.setFont(btLabelDefaultFont);
            baseTextLabel.setText(file.getName());
            return true;
        }
        return false;
    }
    public boolean checkBtSave() {
        if (! btIsSaved) {
            Optional<ButtonType> res = requestFileSave("Suchtext speichern",
                    "Soll der Suchtext gespeichert werden?");
            if (res.isEmpty() || res.get() == cancel) {return false;}
            if (res.get() == doSave) {
                return saveBaseText();
            }
        }
        return true;
    }
    public void closeBaseText() {
        if (! checkBtSave()) {
            return;
        }
        baseTextArea.clear();
        baseTextLabel.setText("");

        resetBaseText();
    }

    ////////////////////////////////////////////////////////////////////////////// u.A. Reaktionsmethoden
    public void onGapTextChanged(String vNew) {
        gtIsSaved = gtIsCached = false;
        completionsSearched = false;
        gtSaveItem.setDisable(false);
        gtSaveAtItem.setDisable(false);
        gtCloseItem.setDisable(false);
        bSearch.setDisable(vNew.isBlank() || baseTextArea.getText().isBlank());

        gapTextLabel.setFont(Font.font(
                gtLabelDefaultFont.getName(),
                FontPosture.ITALIC,
                gtLabelDefaultFont.getSize()));
    }
    public void onBaseTextChanged(String vNew) {
        btIsSaved = btIsCached = btLinesCounted = ignoreBtLineBreaks = false;
        completionsSearched = false;
        lineField.setDisable(vNew.isEmpty());
        btSaveItem.setDisable(false);
        btSaveAtItem.setDisable(false);
        btCloseItem.setDisable(false);
        bSearch.setDisable(vNew.isBlank() || gapTextArea.getText().isBlank());

        bNextIndex.setDisable(true);
        bPreviousIndex.setDisable(true);

        baseTextLabel.setFont(Font.font(
                btLabelDefaultFont.getName(),
                FontPosture.ITALIC,
                btLabelDefaultFont.getSize()));
    }

    public int lineFieldValue() {
        return Integer.parseInt(lineField.getText());
    }
    public int btPosByLineIndex(int index) {
        String btString = baseTextArea.getText();
        int nBtChars = btString.length();
        int pos;

        if (index < nBaseTextLines - 1) {
            int newLines = 0;
            for (pos = 0; newLines < index + 1 && pos < nBtChars; pos++) {
                if (btString.charAt(pos) == '\n') {
                    newLines++;
                }
            }
            pos -= 1;
        } else {
            pos = nBtChars;
        }
        return pos;
    }
    public void onLineEnter() {
        if (! btLinesCounted) {
            nBaseTextLines = (int) baseTextArea.getText().chars().filter(c -> c == '\n').count() + 1;
            btLinesCounted = true;
        }
        if (lineField.getText().isEmpty()) {
            lineField.setText("1");
        } else {
            lineField.setText(String.valueOf(Math.min(Math.max(lineFieldValue(), 1), nBaseTextLines)));
        }

        int value = lineFieldValue() - 1;
        value = Math.min(Math.max(value, 0), nBaseTextLines - 1);
        int charPos;
        if (btIsCached) {
            if (value < nBaseTextLines - 1) {
                charPos = baseText.getLineStartIndex(value + 1) - value - 1;
            } else {
                charPos = baseText.getTextString().length() - 1;
            }
        } else {
            charPos = btPosByLineIndex(value);
        }
        baseTextArea.positionCaret(charPos);
        baseTextArea.requestFocus();
    }
    public void onLineEnterKey() {
        lineField.getParent().requestFocus();
    }
    public void renameLineFieldByPosition(int cPos) {
        if (! baseTextArea.isFocused()) {
            baseTextArea.requestFocus();
            int lineIndex;
            if(btIsCached) {
                lineIndex = baseText.lineByCharIndex(cPos, ignoreBtLineBreaks);
            } else {
                String subString = baseTextArea.getText(0, cPos);
                lineIndex = (int) subString.chars().filter(c -> c == '\n').count();
            }

            lineField.setText(String.valueOf(lineIndex + 1));
        }
    }
    public void onMatchCaseChange() {
        matchSearchCase = cbMatchCase.isSelected();
        completionsSearched = false;
    }

    ////////////////////////////////////////////////////////////////////////////// Suchmethode und Methoden zum Index Auswählen
    public void findGtCompletions() throws IOException {
        if (! btIsCached) {
            baseText = new BaseText(baseTextArea.getText());
            btIsCached = true;
        }
        if (! gtIsCached) {
            gapText = new GapText(gapTextArea.getText());
            gtIsCached = true;
        }
        if (! completionsSearched) {
            markedIndices.clear();
            currentMarkedIndex = -1;
            completions = gapText.findCompletions(baseText, matchSearchCase);
            completionsSearched = true;

            if (! completions.isEmpty()) {
                for (Completion c: completions) {
                    int[] indices = new int[2];
                    indices[0] = c.getStartIndex();
                    indices[1] = c.getEndIndex();
                    if (ignoreBtLineBreaks) {
                        indices[0] -= c.getStartLineIndex();
                        indices[1] -= c.getEndLineIndex();
                    }
                    markedIndices.add(indices);
                }
                markIndex(currentMarkedIndex = 0);
                bPreviousIndex.setDisable(false);
                bNextIndex.setDisable(false);
            } else {
                bPreviousIndex.setDisable(true);
                bNextIndex.setDisable(true);
            }
            output.appendText(gapText.completionsInfo(completions) + "\n\n");
        } else if (currentMarkedIndex == -1 && ! markedIndices.isEmpty()) {
            onNextIndex();
        }
    }
    public void onNextIndex() {
        if (currentMarkedIndex > -1) {
            currentMarkedIndex  = (currentMarkedIndex + 1) % markedIndices.size();
        } else {
            currentMarkedIndex = nextIndexFromPosition(baseTextArea.getCaretPosition());
        }
        markIndex(currentMarkedIndex);
    }
    public void onPreviousIndex() {
        if (currentMarkedIndex > 0) {
            currentMarkedIndex -= 1;
        } else if (currentMarkedIndex == 0) {
            currentMarkedIndex = markedIndices.size() - 1;
        } else {
            currentMarkedIndex = prevIndexFromPosition(baseTextArea.getCaretPosition());
        }
        markIndex(currentMarkedIndex);
    }
    public void markIndex(int index) {
        baseTextArea.requestFocus();
        baseTextArea.positionCaret(markedIndices.get(index)[0]);
        baseTextArea.selectPositionCaret(markedIndices.get(index)[1] + 1);
        currentMarkedIndex = index;
    }

    ////////////////////////////////////////////////////////////////////////////// Methoden zur Bestimmung des nächsten Index
    public int nextIndexFromPosition(int pos) {
        for (int i = 0; i < markedIndices.size(); i++) {
            if (markedIndices.get(i)[1] >= pos) {
                return i;
            }
        }
        return 0;
    }
    public int prevIndexFromPosition(int pos) {
        for (int i = markedIndices.size() - 1; i > -1 ; i--) {
            if (markedIndices.get(i)[0] < pos) {
                return i;
            }
        }
        return markedIndices.size() - 1;
    }
}