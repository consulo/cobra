package org.cobraparser.html.renderer;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Compares DOM boundary points in document order.
 * <p>
 * A point {@code (node, offset)} is keyed by the child-index path from the root to {@code node}, followed by {@code offset}.
 * Keys compare lexicographically, a key that is a prefix of another being smaller. This gives document order for any mix of
 * text offsets and child indexes, since a text offset is only ever compared against offsets of the same text node.
 * <p>
 * Paths are cached, so an instance must not outlive a DOM modification.
 */
final class DomOrder {
    private final Map<Node, int[]> myPaths = new IdentityHashMap<>();
    private final Map<Node, Map<Node, Integer>> myChildIndexes = new IdentityHashMap<>();

    int compare(SelectionPoint a, SelectionPoint b) {
        return compare(a.getNode(), a.getOffset(), b.getNode(), b.getOffset());
    }

    int compare(Node nodeA, int offsetA, Node nodeB, int offsetB) {
        if (nodeA == nodeB) {
            return Integer.compare(offsetA, offsetB);
        }
        return compareKeys(key(nodeA, offsetA), key(nodeB, offsetB));
    }

    SelectionPoint min(SelectionPoint a, SelectionPoint b) {
        return compare(a, b) <= 0 ? a : b;
    }

    SelectionPoint max(SelectionPoint a, SelectionPoint b) {
        return compare(a, b) >= 0 ? a : b;
    }

    /**
     * The boundary point just before {@code node}, in its parent.
     */
    SelectionPoint before(Node node) {
        Node parent = node.getParentNode();
        return parent == null ? new SelectionPoint(node, 0) : new SelectionPoint(parent, indexOf(node));
    }

    /**
     * The boundary point just after {@code node}, in its parent.
     */
    SelectionPoint after(Node node) {
        Node parent = node.getParentNode();
        return parent == null ? new SelectionPoint(node, childCount(node)) : new SelectionPoint(parent, indexOf(node) + 1);
    }

    int indexOf(Node node) {
        Node parent = node.getParentNode();
        if (parent == null) {
            return 0;
        }
        Map<Node, Integer> indexes = myChildIndexes.get(parent);
        if (indexes == null) {
            indexes = new IdentityHashMap<>();
            NodeList children = parent.getChildNodes();
            int length = children.getLength();
            for (int i = 0; i < length; i++) {
                indexes.put(children.item(i), i);
            }
            myChildIndexes.put(parent, indexes);
        }
        Integer index = indexes.get(node);
        return index == null ? 0 : index;
    }

    static int childCount(Node node) {
        NodeList children = node.getChildNodes();
        return children == null ? 0 : children.getLength();
    }

    private int[] key(Node node, int offset) {
        int[] path = path(node);
        int[] key = Arrays.copyOf(path, path.length + 1);
        key[path.length] = offset;
        return key;
    }

    private int[] path(Node node) {
        int[] path = myPaths.get(node);
        if (path == null) {
            Node parent = node.getParentNode();
            if (parent == null) {
                path = new int[0];
            }
            else {
                int[] parentPath = path(parent);
                path = Arrays.copyOf(parentPath, parentPath.length + 1);
                path[parentPath.length] = indexOf(node);
            }
            myPaths.put(node, path);
        }
        return path;
    }

    private static int compareKeys(int[] a, int[] b) {
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) {
                return Integer.compare(a[i], b[i]);
            }
        }
        return Integer.compare(a.length, b.length);
    }
}
