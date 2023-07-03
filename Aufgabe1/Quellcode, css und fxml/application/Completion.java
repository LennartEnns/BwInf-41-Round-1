package application;

/**
 * Klasse zur Speicherung und Verwaltung einer
 * Lückentext-Vervollständigung
 */
public class Completion {
    private String[] units;
    private int length;
    private int nUnits;

    // Einheiten-Index im Suchtext, an dem die Vervollständigung startet
    private int startUnitIndex;
    private int[] charIndices;
    private int[] lineIndices;

    public Completion(int startUnitIndex, int length) {
        this.startUnitIndex = startUnitIndex;
        this.length = length;
        charIndices = new int[2];
        lineIndices = new int[2];
        units = new String[length];
        nUnits = 0;
    }

    public void append(String unit) {
        units[nUnits] = unit;
        nUnits ++;
    }

    public boolean isComplete() {
        return nUnits >= length;
    }

    public String info() {
        String info = String.join(" ", units);
        char[] punctuation = {'.', ',', ':', ';'};
        for (char c: punctuation) {
            info = info.replace(" " + c, String.valueOf(c));
        }

        info += "\n-> Zeile " + (lineIndices[0] + 1);
        if (lineIndices[1] > lineIndices[0]) {
            info += " - " + (lineIndices[1] + 1);
        }
        return info;
    }

    public void setLineIndices(int[] indices) {
        lineIndices = indices;
    }
    public void setCharIndices(int[] charIndices) {
        this.charIndices = charIndices;
    }

    public int getnUnits() {
        return nUnits;
    }
    public int getStartUnitIndex() {
        return startUnitIndex;
    }
    public int getEndUnitIndex() {
        return startUnitIndex + length - 1;
    }

    public int getStartIndex() {
        return charIndices[0];
    }
    public int getEndIndex() {
        return charIndices[1];
    }

    public int getStartLineIndex() {
        return lineIndices[0];
    }
    public int getEndLineIndex() {
        return lineIndices[1];
    }
}