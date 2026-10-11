package org.cobraparser.html.renderer;

import org.w3c.dom.Node;

/**
 * A boundary point of a text selection, defined like a DOM Range boundary point:
 * for a text node the offset is a character offset into its data, for any other node it is a child index.
 * <p>
 * Selection points refer to the DOM, not to renderables, so they stay valid when the document is laid out again.
 */
public final class SelectionPoint {
    private final Node myNode;
    private final int myOffset;

    public SelectionPoint(Node node, int offset) {
        myNode = node;
        myOffset = offset;
    }

    public Node getNode() {
        return myNode;
    }

    public int getOffset() {
        return myOffset;
    }

    /**
     * Compares two points in document order.
     *
     * @return a negative number, zero or a positive number if {@code a} is before, at or after {@code b}
     */
    public static int compare(SelectionPoint a, SelectionPoint b) {
        return new DomOrder().compare(a, b);
    }

    /**
     * Returns true if the node of this point is attached to the given document.
     */
    public boolean isIn(Node document) {
        Node node = myNode;
        while (node.getParentNode() != null) {
            node = node.getParentNode();
        }
        return node == document;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SelectionPoint that && myNode == that.myNode && myOffset == that.myOffset;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(myNode) * 31 + myOffset;
    }

    @Override
    public String toString() {
        return "SelectionPoint[" + myNode.getNodeName() + ":" + myOffset + "]";
    }
}
