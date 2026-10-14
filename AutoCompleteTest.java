import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/**
 * Sample tests - not exhaustive, but larger than a quick smoke test. These
 * only call the public interface, the same way Main.java does, so they
 * can't tell how your trie is built internally, only whether it behaves
 * correctly. The autograder will run a separate, more complete hidden
 * suite; use these to sanity-check your implementation as you build it,
 * and write your own additional tests as you go.
 *
 * Most tests share one fixture trie (see setUp()), built from:
 *   /api/users/create
 *   /api/users/delete
 *   /api/users/list
 *   /api/posts/create
 *   /api/posts/delete
 *   /settings/privacy/blocked-users
 *   /settings/privacy/data-export
 *   /settings/notifications/email
 *   /settings/notifications/push
 *   /help/faq
 *   /help/contact
 *
 * A few tests build their own small, separate trie when the fixture isn't
 * the right shape to exercise what's being tested (e.g. a path that is
 * itself a prefix of a longer path, or a non-'/' separator).
 */
public class AutoCompleteTest {

    private AutoCompleteInterface ac;

    @Before
    public void setUp() {
        ac = new AutoComplete('/');
        ac.add("/api/users/create");
        ac.add("/api/users/delete");
        ac.add("/api/users/list");
        ac.add("/api/posts/create");
        ac.add("/api/posts/delete");
        ac.add("/settings/privacy/blocked-users");
        ac.add("/settings/privacy/data-export");
        ac.add("/settings/notifications/email");
        ac.add("/settings/notifications/push");
        ac.add("/help/faq");
        ac.add("/help/contact");
    }

    /** Asserts that actual contains exactly the given characters, any order. */
    private void assertSameChars(List<Character> actual, Character... expected) {
        assertEquals("Wrong number of possible next characters: " + actual,
                expected.length, actual.size());
        for (Character c : expected) {
            assertTrue("Expected " + c + " to be a possible next character, got " + actual,
                    actual.contains(c));
        }
    }

    // ---------------------------------------------------------------
    // add()
    // ---------------------------------------------------------------

    @Test
    public void testAddReturnsTrueForNewPath() {
        AutoCompleteInterface fresh = new AutoComplete('/');
        assertTrue(fresh.add("/brand/new/path"));
    }

    @Test
    public void testAddReturnsFalseForDuplicatePath() {
        assertFalse(ac.add("/api/users/create")); // already added in setUp()
    }

    @Test
    public void testAddingDuplicatePathDoesNotChangeBranchOptions() {
        ac.advance('/');
        List<Character> before = ac.nextPossibleChars();
        ac.add("/api/users/create"); // duplicate
        List<Character> after = ac.nextPossibleChars();
        assertEquals("Adding a duplicate path should not change branch structure",
                before.size(), after.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEmptyPathThrows() {
        ac.add("");
    }

    @Test
    public void testAddingNewTopLevelPathAddsToRootBranchOptions() {
        ac.add("/zebra");
        ac.advance('/');
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h', 'z');
    }

    // ---------------------------------------------------------------
    // advance()
    // ---------------------------------------------------------------

    @Test
    public void testAdvanceMatchingCharacterReturnsTrue() {
        assertTrue(ac.advance('/'));
    }

    @Test
    public void testAdvanceNonMatchingCharacterReturnsFalse() {
        ac.advance('/');
        assertFalse(ac.advance('z')); // no path has '/z...'
    }

    @Test
    public void testAdvanceCharacterByCharacterAlongAKnownPath() {
        AutoCompleteInterface fresh = new AutoComplete('/');
        fresh.add("/help/faq");
        String path = "/help/faq";
        for (char c : path.toCharArray()) {
            assertTrue("advance('" + c + "') should succeed along a known path",
                    fresh.advance(c));
        }
        assertTrue("should be at a leaf with no further completions",
                fresh.nextPossibleChars().isEmpty());
    }

    @Test
    public void testAdvanceOutOfSyncThenRetreatRecoversSync() {
        assertTrue(ac.advance('/'));
        assertFalse(ac.advance('z')); // falls out of sync; prefix grows, currentNode does not move
        ac.retreat(); // should undo exactly the 'z', landing back in sync after '/'
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h');
    }

    // ---------------------------------------------------------------
    // nextPossibleChars()
    // ---------------------------------------------------------------

    @Test
    public void testNextPossibleCharsAtEmptyPrefix() {
        // every path in the fixture starts with '/', so there's only one option
        assertSameChars(ac.nextPossibleChars(), '/');
    }

    @Test
    public void testNextPossibleCharsAtRootBranch() {
        ac.advance('/');
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h'); // api / settings / help
    }

    @Test
    public void testNextPossibleCharsEmptyOnEmptyTrie() {
        AutoCompleteInterface empty = new AutoComplete('/');
        assertTrue(empty.nextPossibleChars().isEmpty());
    }

    // ---------------------------------------------------------------
    // complete()
    // ---------------------------------------------------------------

    @Test
    public void testCompleteReturnsEmptyStringWhenImmediatelyBlocked() {
        ac.advance('/');
        assertEquals("", ac.complete()); // three-way branch: a / s / h
    }

    @Test
    public void testCompleteReturnsEmptyStringOnEmptyTrie() {
        AutoCompleteInterface empty = new AutoComplete('/');
        assertEquals("", empty.complete());
    }

    @Test
    public void testCompleteWalksOneSegmentAtATimeThroughBranches() {
        assertTrue(ac.advance('/'));
        assertEquals("", ac.complete()); // blocked: api / settings / help
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h');

        assertTrue(ac.advance('a'));
        // unambiguous through "pi", stops right after crossing the next '/'
        assertEquals("pi/", ac.complete());
        assertSameChars(ac.nextPossibleChars(), 'u', 'p'); // users vs posts

        assertTrue(ac.advance('u'));
        // unambiguous through "sers", stops right after crossing the next '/'
        assertEquals("sers/", ac.complete());
        assertSameChars(ac.nextPossibleChars(), 'c', 'd', 'l'); // create / delete / list
    }

    @Test
    public void testCompleteStopsAtFullLeaf() {
        assertTrue(ac.advance('/'));
        assertTrue(ac.advance('h'));
        assertEquals("elp/", ac.complete()); // unambiguous "help", stop after the '/'
        assertSameChars(ac.nextPossibleChars(), 'f', 'c'); // faq vs contact

        assertTrue(ac.advance('f'));
        assertEquals("aq", ac.complete()); // "faq" has no further children: a full leaf
        assertTrue(ac.nextPossibleChars().isEmpty());
        assertEquals("", ac.complete()); // already fully matched; nothing more to extend
    }

    @Test
    public void testCompleteStopsAtWordThatIsAlsoAPrefixOfALongerWord() {
        AutoCompleteInterface fresh = new AutoComplete('/');
        fresh.add("/api");
        fresh.add("/api/users");

        assertTrue(fresh.advance('/'));
        // "/api" is itself a complete path, even though "/api/users" also exists -
        // complete() must stop here rather than silently walking past it
        assertEquals("api", fresh.complete());
        assertEquals("", fresh.complete()); // still blocked for the same reason
        assertSameChars(fresh.nextPossibleChars(), '/');

        assertTrue(fresh.advance('/'));
        assertEquals("users", fresh.complete()); // now unambiguous all the way to the leaf
        assertTrue(fresh.nextPossibleChars().isEmpty());
    }

    @Test
    public void testRetreatUndoesOnlyOneCharacterEvenAfterABatchedComplete() {
        ac.advance('/');
        ac.advance('a');
        assertEquals("pi/", ac.complete()); // a 3-character batch in one call
        ac.retreat(); // undoes exactly one character: the trailing '/'
        // back at the node representing "/api" (no trailing slash): only one
        // way to continue, not the users/posts branch and not the root branch
        assertSameChars(ac.nextPossibleChars(), '/');
    }

    @Test
    public void testCompleteWithDifferentSeparator() {
        // guards against accidentally hardcoding '/' instead of using the
        // constructor's separator argument
        AutoCompleteInterface dotAc = new AutoComplete('.');
        dotAc.add("com.example.service.auth");
        dotAc.add("com.example.service.payments");
        dotAc.add("com.example.utils");

        assertTrue(dotAc.advance('c'));
        assertEquals("om.", dotAc.complete());
        assertEquals("example.", dotAc.complete());

        // "service" vs "utils" diverge on their very first letter: a branch
        assertEquals("", dotAc.complete());
        assertSameChars(dotAc.nextPossibleChars(), 's', 'u');

        assertTrue(dotAc.advance('s'));
        assertEquals("ervice.", dotAc.complete());
        assertSameChars(dotAc.nextPossibleChars(), 'a', 'p'); // auth vs payments
    }

    // ---------------------------------------------------------------
    // retreat() / reset()
    // ---------------------------------------------------------------

    @Test
    public void testRetreatMovesBackUpOneLevel() {
        ac.advance('/');
        ac.advance('a');
        ac.retreat(); // undo the 'a'
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h'); // back at the root branch
    }

    @Test
    public void testResetReturnsAllTheWayToTheStart() {
        ac.advance('/');
        ac.advance('a');
        ac.complete();
        ac.advance('u');
        ac.complete();

        ac.reset();

        assertSameChars(ac.nextPossibleChars(), '/'); // back to the very beginning
        ac.advance('/');
        assertSameChars(ac.nextPossibleChars(), 'a', 's', 'h'); // not left mid-prefix anywhere
    }
}