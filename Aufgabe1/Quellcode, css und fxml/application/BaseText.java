package application;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.lang.Character;

import java.util.ArrayList;

/**
 * Klasse für die Verarbeitung des zu durchsuchenden Textes
 */
public class BaseText {
    private enum C_TYPES {
        LETTER, WHITESPACE, NEWLINE, BOM, MISC
    }
    private final ArrayList<String> textUnits;
    private final ArrayList<int[]> unitStartEndIndices;
    private String textString;

    private final ArrayList<Integer> newLineIndices;
    private final int nLines;

    public BaseText(String text) throws IOException {
        textString = text;
        textUnits = new ArrayList<String>();
        unitStartEndIndices = new ArrayList<int[]>();
        newLineIndices = new ArrayList<Integer>();
        assembleTextUnitsFromString(text);
        nLines = newLineIndices.size() + 1;
    }
    public BaseText(File file) throws IOException {
        textUnits = new ArrayList<String>();
        unitStartEndIndices = new ArrayList<int[]>();
        newLineIndices = new ArrayList<Integer>();
        assembleTextUnitsFromFile(file);
        nLines = newLineIndices.size() + 1;
    }
    public BaseText() throws IOException {
        this(new File("Alice_im_Wunderland.txt"));
    }
    public BaseText fromPath (String path) throws IOException {
        return new BaseText(new File(path));
    }

    private void assembleTextUnitsFromString(String text) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(text));
        String unit = "";
        int charIndex = -1;
        int[] currentUnitIndices = new int[2];
        int cInt;
        while ((cInt = reader.read()) != -1) {
            char c = (char) cInt;
            C_TYPES type = charType(c);
            if (type == C_TYPES.BOM) {
                continue;
            }
            charIndex ++;
            switch (type) {
                case MISC, LETTER:
                    if (! unit.isEmpty()) {
                        boolean b;
                        if (type == C_TYPES.LETTER) {
                            b = charType(unit.charAt(0)) == C_TYPES.LETTER;
                        } else {
                            b = unit.charAt(0) == c;
                        }
                        if (b) {
                            unit += c;
                            continue;
                        }
                        textUnits.add(unit);
                        currentUnitIndices[1] = charIndex - 1;
                        unitStartEndIndices.add(currentUnitIndices);
                        currentUnitIndices = new int[2];
                    }
                    currentUnitIndices[0] = charIndex;
                    unit = String.valueOf(c);
                    break;
                case WHITESPACE, NEWLINE:
                    if (! unit.isEmpty()) {
                        if (! (type == C_TYPES.NEWLINE && charType(unit.charAt(0)) == C_TYPES.LETTER
                                && unit.endsWith("-"))) {
                            textUnits.add(unit);
                            currentUnitIndices[1] = charIndex - 1;
                            unitStartEndIndices.add(currentUnitIndices);
                            currentUnitIndices = new int[2];
                            unit = "";
                        }
                    }
                    if (type == C_TYPES.NEWLINE) {
                        newLineIndices.add(charIndex);
                    }
            }
        }
        textUnits.add(unit);
        currentUnitIndices[1] = charIndex;
        unitStartEndIndices.add(currentUnitIndices);
    }
    private void assembleTextUnitsFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
        String unit = "";
        StringBuilder textStringBuilder = new StringBuilder();
        int charIndex = -1;
        int[] currentUnitIndices = new int[2];
        int cInt;
        while ((cInt = reader.read()) != -1) {
            char c = (char) cInt;
            C_TYPES type = charType(c);
            if (type == C_TYPES.BOM) {
                continue;
            }
            textStringBuilder.append(c);
            charIndex ++;
            switch (type) {
                case MISC, LETTER:
                    if (! unit.isEmpty()) {
                        boolean b;
                        if (type == C_TYPES.LETTER) {
                            b = charType(unit.charAt(0)) == C_TYPES.LETTER;
                        } else {
                            b = unit.charAt(0) == c;
                        }
                        if (b) {
                            unit += c;
                            continue;
                        }
                        textUnits.add(unit);
                        currentUnitIndices[1] = charIndex - 1;
                        unitStartEndIndices.add(currentUnitIndices);
                        currentUnitIndices = new int[2];
                    }
                    currentUnitIndices[0] = charIndex;
                    unit = String.valueOf(c);
                    break;
                case WHITESPACE, NEWLINE:
                    if (! unit.isEmpty()) {
                        if (! (type == C_TYPES.NEWLINE && charType(unit.charAt(0)) == C_TYPES.LETTER
                                && unit.endsWith("-"))) {
                            textUnits.add(unit);
                            currentUnitIndices[1] = charIndex - 1;
                            unitStartEndIndices.add(currentUnitIndices);
                            currentUnitIndices = new int[2];
                            unit = "";
                        }
                    }
                    if (type == C_TYPES.NEWLINE) {
                        newLineIndices.add(charIndex);
                    }
            }
        }
        textString = textStringBuilder.toString();
        textUnits.add(unit);
        currentUnitIndices[1] = charIndex;
        unitStartEndIndices.add(currentUnitIndices);
    }

    private static C_TYPES charType(char c) {
        if (Character.isAlphabetic(c) || Character.isDigit(c) ||
                c == '\'' || c == '-') {
            return C_TYPES.LETTER;
        }
        if (Character.isWhitespace(c)) {
            if (c == '\n') {
                return C_TYPES.NEWLINE;
            }
            return C_TYPES.WHITESPACE;
        }
        if (c == '\ufeff') {
            return C_TYPES.BOM;
        }
        return C_TYPES.MISC;
    }

    public int[] linesByCharIndices(int index0, int index1) {
        if (index0 > index1) {
            int t = index0;
            index0 = index1;
            index1 = t;
        }
        int[] lines = {-1, -1};
        for (int i = 1; i < nLines + 1; i++) {
            int nlIndex;
            if (i < nLines) {
                nlIndex = newLineIndices.get(i - 1);
            } else {
                nlIndex = textString.length();
            }

            if (nlIndex >= index0 && lines[0] == -1) {
                lines[0] = i - 1;
            }
            if (nlIndex >= index1) {
                lines[1] = i - 1;
                return lines;
            }
        }
        if (lines[0] > -1) {
            lines[1] = nLines - 1;
            return lines;
        }
        return null;
    }
    public int lineByCharIndex(int index, boolean newlinesIgnored) {
        for (int i = 1; i < nLines + 1; i++) {
            int nlIndex;
            if (i < nLines) {
                nlIndex = newLineIndices.get(i - 1);
            } else {
                nlIndex = textString.length();
            }
            if (newlinesIgnored) {
                index += 1;
            }

            if (nlIndex >= index) {
                return i - 1;
            }
        }
        return nLines - 1;
    }

    public int[] charByUnitIndices(int index0, int index1) {
        int[] indices = new int[2];
        indices[0] = unitStartIndex(index0);
        indices[1] = unitEndIndex(index1);
        return indices;
    }

    public ArrayList<String> getTextUnits() {
        return textUnits;
    }
    public int unitStartIndex(int unitIndex) {
        return unitStartEndIndices.get(unitIndex)[0];
    }
    public int unitEndIndex(int unitIndex) {
        return unitStartEndIndices.get(unitIndex)[1];
    }

    public String getTextString() {
        return textString;
    }
    public int getNLines() {
        return nLines;
    }
    public int getLineStartIndex(int lineIndex) {
        if (lineIndex > 0) {
            return newLineIndices.get(lineIndex - 1);
        }
        return 0;
    }
}