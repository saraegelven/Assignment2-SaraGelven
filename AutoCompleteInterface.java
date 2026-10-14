import java.util.List;

/**
 * A DLB-trie-backed autocomplete interface for path-like input.
 *
 * Implementations must expose a public constructor:
 * public AutoComplete(char separator)
 * where `separator` is the character (e.g. '/', '.', '-') that complete()
 * treats as a natural stopping point, in addition to the other stopping points.
 */
public interface AutoCompleteInterface {
    /**
     * Adds a path to the trie.
     * Running time: O(length of path * alphabet size).
     * 
     * @param path the full path/word to add (must not be empty)
     * @return true if this path was not already present, false if it was
     * @throws IllegalArgumentException if path is empty ("") or null
     */
    boolean add(String path);

    /**
     * Advances the running prefix by one character, moving currentNode along
     * with it if a matching child exists.
     * Running time: O(alphabet size).
     * 
     * @param c the next character
     * @return true if a matching child was found (prefix stays "in sync"),
     *         false otherwise
     */
    boolean advance(char c);

    /**
     * Retreats the running prefix by one character, moving currentNode back
     * up if it had moved down for the character being removed.
     * Running time: O(1).
     */
    void retreat();

    /**
     * Resets the running prefix back to empty and currentNode back to its
     * starting position, without altering the trie's contents. Equivalent
     * to (but cheaper than) calling retreat() until the prefix is empty.
     * Running time: O(1).
     */
    void reset();

    /**
     * Extends the running prefix as far as possible while the path through
     * the trie remains unambiguous, advancing currentNode and the running
     * prefix as it goes. Stops upon reaching: a node with no children; a node
     * with more than one child; a node that is a complete path but also has
     * a child; or immediately after appending this instance's separator
     * character.
     * Running time: O(length of extension).
     * 
     * @return the characters appended during this call; "" if blocked
     *         immediately (call nextPossibleChars() to see why)
     */
    String complete();

    /**
     * Reports the characters that could follow the current running prefix,
     * i.e. the distinct characters labeling children of currentNode (or the
     * top-level entries of the trie, if the running prefix is empty). Does
     * not change any state; safe to call at any time.
     * Running time: O(alphabet size).
     * 
     * @return the list of possible next characters, in the implementation's
     *         natural order; empty if there are no completions from here
     */
    List<Character> nextPossibleChars();
}
