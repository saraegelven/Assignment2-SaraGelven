import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PROVIDED CLI - you should not need to modify this file. Run it once your
 * AutoComplete class is implemented to play with tab-completion interactively.
 *
 * Usage (after `mvn compile`):
 *   java -cp target/classes Main dict-paths.txt /
 * or:
 *   mvn exec:java -Dexec.args="dict-paths.txt /"
 *
 * Controls once running:
 *   - type characters to extend the running prefix
 *   - Tab to auto-complete as far as unambiguous
 *   - Tab again (with nothing typed in between) to see the possible next
 *     characters, if you're stuck at a branch
 *   - Backspace to undo the last character
 *   - Enter to clear the current input and start over
 *   - Ctrl-D to quit
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 2 || args[1].length() != 1) {
            System.err.println("Usage: java Main <dictionary-file> <separator-char>");
            return;
        }

        char separator = args[1].charAt(0);
        AutoCompleteInterface ac = new AutoComplete(separator);

        List<String> lines = Files.readAllLines(Path.of(args[0]));
        int loaded = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                ac.add(trimmed);
                loaded++;
            }
        }

        System.out.println("Loaded " + loaded + " paths (separator = '" + separator + "').");
        System.out.println("Start typing. Tab to complete, Tab-Tab to see options, "
                + "Backspace to undo, Enter to clear.");
        System.out.println("Press Ctrl-D at any time to quit.");
        System.out.println("(If this program is ever killed unexpectedly - e.g. Ctrl-C or a crash - "
                + "before it can restore your terminal, just type `stty sane` afterward.)");
        System.out.println();

        RawTerminal term = new RawTerminal();
        term.enterRawMode();
        boolean justHitAmbiguousTab = false;
        StringBuilder typed = new StringBuilder(); // mirrors what's on screen

        try {
            while (true) {
                RawTerminal.Key key = term.readKey();
                switch (key.type) {

                    case CHAR:
                        ac.advance(key.ch);
                        typed.append(key.ch);
                        System.out.print(key.ch);
                        System.out.flush();
                        justHitAmbiguousTab = false;
                        break;

                    case TAB: {
                        String extension = ac.complete();
                        if (!extension.isEmpty()) {
                            typed.append(extension);
                            System.out.print(extension);
                            System.out.flush();
                        } else if (justHitAmbiguousTab) {
                            System.out.println();
                            System.out.println(ac.nextPossibleChars());
                            System.out.print(typed);
                            System.out.flush();
                        }
                        justHitAmbiguousTab = extension.isEmpty();
                        break;
                    }

                    case BACKSPACE:
                        if (typed.length() > 0) {
                            ac.retreat();
                            typed.deleteCharAt(typed.length() - 1);
                            System.out.print("\b \b");
                            System.out.flush();
                        }
                        justHitAmbiguousTab = false;
                        break;

                    case ENTER:
                        System.out.println();
                        ac.reset();
                        typed.setLength(0);
                        justHitAmbiguousTab = false;
                        break;

                    case EOF:
                        System.out.println();
                        System.out.println("Goodbye!");
                        return;
                }
            }
        } finally {
            term.exitRawMode();
        }
    }
}