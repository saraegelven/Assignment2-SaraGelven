import java.util.ArrayList;
import java.util.List;

public class AutoComplete implements AutoCompleteInterface {

    private final char separator;
    private DLBNode root;
    private StringBuilder prefix;
    private DLBNode currentNode;
    private DLBNode phantomTail;
    private int extraDepth;

    public AutoComplete(char separator) {
        this.separator = separator;
        root = null;
        prefix = new StringBuilder();
        currentNode = null;
        phantomTail = null;
        extraDepth = 0;
    }

    @Override
    public boolean add(String path) {
        if (path == null || path.equals("")) throw new IllegalArgumentException("calls add() with an empty string");
        if (wordExists(root, path, 0)) return false;

        root = put(root, path, 0);
        return true;
       // throw new UnsupportedOperationException("add() not implemented yet");
    }

    private boolean wordExists(DLBNode x, String key, int position) {
        if (x == null) return false;

        if (x.data == key.charAt(position)) {
            if (position < key.length() - 1) return wordExists(x.child, key, position + 1);
            else return x.isWord;
        } else return wordExists(x.sibling, key, position);
    }

    private DLBNode put(DLBNode x, String key, int position) {
        if (x == null) {
            DLBNode node = new DLBNode(key.charAt(position));
            if (position < key.length() - 1) {
                node.child = put(node.child, key, position + 1);
                node.child.parent = node;
            } else node.isWord = true;
            return node;
        }

        if (x.data == key.charAt(position)) {
            if (position < key.length() - 1) {
                x.child = put(x.child, key, position + 1);
                x.child.parent = x;
            } else x.isWord = true;
            return x;
        }
        
        x.sibling = put(x.sibling, key, position);
        return x;
    }

    @Override
    public boolean advance(char c) {
        prefix.append(c);

        if (extraDepth > 0) {
            DLBNode node = new DLBNode(c);
            node.parent = phantomTail;
            phantomTail = node;
            extraDepth++;
            return false;
        }

       DLBNode beginSearch = (currentNode == null) ? root : currentNode.child;
       DLBNode match = findSibling(beginSearch, c);
       if (match != null) {
            currentNode = match;
            return true;
       }

       DLBNode node = new DLBNode(c);
       node.parent = currentNode;
       phantomTail = node;
       extraDepth = 1;
       return false;
        // throw new UnsupportedOperationException("advance() not implemented yet");
    }

    private DLBNode findSibling(DLBNode x, char c) {
        DLBNode cur = x;
        while (cur != null) {
            if (cur.data == c) return cur;
            cur = cur.sibling;
        }
        return null;
    }

    @Override
    public void retreat() {
        if (prefix.length() == 0) throw new IllegalStateException();

        prefix.deleteCharAt(prefix.length() - 1);

        if (extraDepth > 0) {
            phantomTail = phantomTail.parent;
            extraDepth--;

            if (extraDepth == 0) {
                currentNode = phantomTail;
                phantomTail = null;
            }
            return;
        }
        currentNode = currentNode.parent;
        // throw new UnsupportedOperationException("retreat() not implemented yet");
    }

    @Override
    public void reset() {
        prefix = new StringBuilder();
        currentNode = null;
        phantomTail = null;
        extraDepth = 0;
        // throw new UnsupportedOperationException("reset() not implemented yet");
    }

    @Override
    public String complete() {
        if (extraDepth > 0) return prefix.toString();

        while(true) {
            if (currentNode != null && currentNode.isWord) break;

            DLBNode children = (currentNode == null) ? root : currentNode.child;
            int childCount = countSiblings(children);

            if (childCount == 0 || childCount > 1) break;

            currentNode = children;
            prefix.append(currentNode.data);
            if (currentNode.data == separator) break;
        }

        return prefix.toString();
        // throw new UnsupportedOperationException("complete() not implemented yet");
    }

    private int countSiblings(DLBNode x) {
        int count = 0;
        DLBNode cur = x;
        while (cur != null) {
            count++;
            cur = cur.sibling;
        }

        return count;
    }

    @Override
    public List<Character> nextPossibleChars() {
        List<Character> result = new ArrayList<>();
        if (extraDepth > 0) return result;

        DLBNode children = (currentNode == null) ? root : currentNode.child;
        DLBNode cur = children;
        while (cur != null) {
            result.add(cur.data);
            cur = cur.sibling;
        }

        return result;
        // throw new UnsupportedOperationException("nextPossibleChars() not implemented yet");
    }

    private class DLBNode {
        private char data;
        private boolean isWord;
        private DLBNode sibling;
        private DLBNode child;
        private DLBNode parent;

        private DLBNode(char data) {
            this.data = data;
            isWord = false;
            sibling = child = parent = null;
        }
    }
}
