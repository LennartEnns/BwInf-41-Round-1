package application;

import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

/**
 * Klasse für die Verarbeitung eines Lückentextes
 */
public class GapText {
    private String[] gapUnits;
    private int nUnits;
    private String textString;

    public GapText(String path, String filename) throws IOException {
        Scanner scan = new Scanner(new File(path + "\\" + filename + ".txt"),
                StandardCharsets.UTF_8);
        createUnits(scan);
    }
    public GapText(String gapTextString) {
        Scanner scan = new Scanner(gapTextString);
        createUnits(scan);
    }
    public GapText(File file) throws IOException {
        Scanner scan = new Scanner(file);
        createUnits(scan);
    }

    private void createUnits(Scanner scan) {
        StringBuilder string = new StringBuilder();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (! line.isBlank()) {
                string.append(line.strip()).append(" ");
            }
        }
        String res = string.toString();
        if (! res.isEmpty()) {
            if (res.charAt(0) == '\ufeff') {
                res = res.substring(1);
            }
        }
        textString = res.strip();
        gapUnits = textString.split(" ");
        nUnits = gapUnits.length;

        for (int i = 0; i < nUnits; i++) {
            if (gapUnits[i].equals("_")) {
                gapUnits[i] = null;
            }
        }
    }

    /** Gibt ein GapText-Objekt mit der Konsoleneingabe
     * als Datei zurück, falls vorhanden.
     */
    public static GapText fromFilenameInput() {
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("Name der Lückentext-Datei (ohne \".txt\"): ");
            String input = scan.nextLine();
            try {
                return new GapText("Beispieleingaben", input);
            } catch (IOException e) {
                System.out.println("Die Datei \"" + input +
                        ".txt\" existiert nicht unter \"Beispieleingaben\".\n");
            }
        }
    }

    /** Gibt true zurück, wenn die Einheit unit an der Stelle
     * index in den Lückentext passt.
     */
    public boolean match(String unit, int index, boolean matchCase) {
        if (gapUnits[index] == null) {
            for (char c: unit.toCharArray()) {
                if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                    return true;
                }
            }
        } else if (matchCase) return unit.equals(gapUnits[index]);
        else return unit.equalsIgnoreCase(gapUnits[index]);

        return false;
    }

    /** Findet alle Lösungen für den Lückentext im Suchtext baseText
     * und gibt sie in einer ArrayList zurück.
     */
    public ArrayList<Completion> findCompletions(BaseText baseText, boolean matchCase) {
        ArrayList<String> textUnits = baseText.getTextUnits();
        int nSearchUnits = textUnits.size();
        ArrayList<Completion> searches  = new ArrayList<Completion>();
        ArrayList<Completion> complete  = new ArrayList<Completion>();
        ArrayList<Completion> remove  = new ArrayList<Completion>();

        for (int i = 0; i < nSearchUnits; i++) {
            String unit = textUnits.get(i);

            for (Completion c: searches) {
                if (c.isComplete()) {
                    complete.add(c);
                    remove.add(c);
                }
                else if (match(unit, c.getnUnits(), matchCase)) {
                    c.append(unit);
                } else {
                    remove.add(c);
                }
            }

            for (Completion r: remove) {
                searches.remove(r);
            }
            remove.clear();

            if (nSearchUnits - i >= nUnits) {
                if (match(unit, 0, matchCase)) {
                    Completion c = new Completion(i, nUnits);
                    c.append(unit);
                    searches.add(c);
                }
            } else if (searches.isEmpty()) {
                break;
            }
        }
        for (Completion c: searches) {
            if (c.isComplete()) {
                complete.add(c);
            }
        }
        for (Completion c: complete) {
            c.setCharIndices(baseText.charByUnitIndices
                    (c.getStartUnitIndex(), c.getEndUnitIndex()));
            c.setLineIndices(baseText.linesByCharIndices
                    (c.getStartIndex(), c.getEndIndex()));
        }
        return complete;
    }

    /** Gibt die Informationen der Vervollständigungen in completions
     * mit jeweils einer Leerzeile Abstand als String zurück.
     */
    public String completionsInfo(ArrayList<Completion> completions) {
        String info = "Lückentext: " + textString + "\n";
        if (completions.isEmpty()) {
            return info + "Für diesen Lückensatz gibt es im Text keine Vervollständigungen.";
        }
        info += "Für diesen Lückentext ";
        if (completions.size() > 1) {
            info += "wurden im Text " + completions.size() + " Vervollständigungen gefunden:\n\n";
        } else {
            info += "wurde im Text eine Vervollständigung gefunden:\n\n";
        }

        int nComp = completions.size();
        String[] cInfos = new String[nComp];
        for (int i = 0; i < nComp; i++) {
            cInfos[i] = completions.get(i).info();
        }
        return info + String.join("\n\n", cInfos);
    }

    public String getTextString() {
        return textString;
    }
}