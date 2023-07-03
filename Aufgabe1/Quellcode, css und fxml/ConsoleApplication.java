import application.BaseText;
import application.Completion;
import application.GapText;

import java.io.IOException;
import java.util.ArrayList;

/** Eine Konsolen-Anwendung */
public class ConsoleApplication {

    public static void main(String[] args) throws IOException {
        BaseText baseText = new BaseText();
        GapText gapText = GapText.fromFilenameInput();

        ArrayList<Completion> completions = gapText.findCompletions(baseText, false);
        System.out.println(gapText.completionsInfo(completions));

        System.out.print("\nZum Schließen die Enter-Taste drücken. ");
        System.in.read();
    }
}