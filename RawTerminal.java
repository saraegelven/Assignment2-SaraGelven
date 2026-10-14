import java.io.IOException;
import java.io.InputStream;

/**
 * PROVIDED SCAFFOLDING - you do not need to modify this class.
 *
 * By default, a terminal buffers input line-by-line: it holds every keystroke
 * (Tab included) until the user presses Enter, and it echoes characters back
 * on its own. To detect a Tab press the instant it happens, the terminal has
 * to be put into "raw" mode, which turns off that buffering and echoing.
 * This class handles that so you can focus on the trie logic.
 *
 * Works on Linux and macOS terminals (it shells out to `stty`). If you're on
 * Windows, run your program inside WSL or Git Bash so a real terminal is
 * available. When input is piped in from a file instead of typed live (e.g.
 * for automated testing), raw mode is skipped automatically since piped input
 * isn't line-buffered in the first place - readKey() will work either way.
 */
public class RawTerminal {

    /** The kinds of keystrokes this reader distinguishes. */
    public enum KeyType { CHAR, TAB, ENTER, BACKSPACE, EOF }

    /** A single keystroke: its type, and (for CHAR) the character itself. */
    public static class Key {
        public final KeyType type;
        public final char ch;
        Key(KeyType type, char ch) { this.type = type; this.ch = ch; }
        @Override public String toString() {
            return type == KeyType.CHAR ? ("CHAR('" + ch + "')") : type.toString();
        }
    }

    private final InputStream in = System.in;
    private boolean rawModeActive = false;

    /**
     * Switches the terminal into raw mode. Call this once, before your input
     * loop starts. Safe to call even when there's no real terminal attached
     * (e.g. piped input) - it will just silently do nothing in that case.
     */
    public void enterRawMode() {
        if (System.console() == null) return; // no real terminal (e.g. piped input); nothing to do
        try {
            runStty("-icanon", "-echo");
            rawModeActive = true;
        } catch (IOException | InterruptedException e) {
            System.err.println("Warning: could not enter raw terminal mode (" + e.getMessage()
                    + "). Tab may not behave as expected; try running in a Linux/macOS terminal.");
        }
    }

    /**
     * Restores normal terminal behavior. ALWAYS call this before your program
     * exits - put it in a finally block, so the user's terminal isn't left in
     * a broken state if your program crashes.
     */
    public void exitRawMode() {
        if (!rawModeActive) return;
        try {
            runStty("icanon", "echo");
        } catch (IOException | InterruptedException e) {
            System.err.println("Warning: could not restore terminal mode. Run `stty sane` if your terminal looks off.");
        }
    }

    private void runStty(String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "stty";
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        pb.start().waitFor();
    }

    /**
     * Blocks until the next keystroke is available and returns it.
     *
     * Returns EOF either when the input stream has genuinely ended (read()
     * returns -1 - the case when input is piped rather than typed live), or
     * when the user presses Ctrl-D (byte 4) while the terminal is in raw
     * mode. Normally the OS's canonical line mode is what turns Ctrl-D into
     * a real end-of-stream signal; raw mode disables that, so Ctrl-D would
     * otherwise just arrive as an ordinary (invisible) character instead of
     * stopping the program.
     *
     * @return the next Key, or a Key of type EOF if input has ended or Ctrl-D was pressed.
     */
    public Key readKey() throws IOException {
        int b = in.read();
        if (b == -1 || b == 4) return new Key(KeyType.EOF, '\0');
        if (b == 9)  return new Key(KeyType.TAB, '\t');
        if (b == 10 || b == 13) return new Key(KeyType.ENTER, '\n');
        if (b == 127 || b == 8) return new Key(KeyType.BACKSPACE, '\0');
        return new Key(KeyType.CHAR, (char) b);
    }
}