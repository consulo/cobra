package org.cobraparser.html.domimpl;

import cz.vutbr.web.css.*;
import org.cobraparser.html.style.ComputedJStyleProperties;
import org.cobraparser.html.style.JStyleProperties;
import org.cobraparser.js.HideFromJS;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

// TODO: Extend a common interface or a minimal class instead of HTMLElementImpl
public class GeneratedElement extends HTMLElementImpl {

  private final NodeData nodeData;
  private JStyleProperties currentStyle;
  private final TermList content;

  public GeneratedElement(final HTMLElementImpl parent, final NodeData nodeData, final TermList content) {
    super("");
    setParentImpl(parent);
    setOwnerDocument(parent.getOwnerDocument());
    this.nodeData = nodeData;
    this.content = content;
  }

  @HideFromJS
  public JStyleProperties getCurrentStyle() {
    synchronized (this) {
      if (currentStyle != null) {
        return currentStyle;
      }
      currentStyle = new ComputedJStyleProperties(this, nodeData, true);
      return currentStyle;
    }
  }

  @Override
  public NodeImpl[] getChildrenArray() {
    final ArrayList<NodeImpl> nodeList = new ArrayList<>();
    for (final Term<?> c : content) {
      if (c instanceof TermIdent) {
        // TODO
      } else if (c instanceof TermString) {
        final String value = ((TermString) c).getValue();
        final Text txt = getOwnerDocument().createTextNode(value);
        nodeList.add((NodeImpl) txt);
      } else if (c instanceof TermURI) {
        // TODO
      } else if (c instanceof TermFunction) {
        final TermFunction f = (TermFunction) c;
        if (f.getFunctionName().equals("attr")) {
          final List<Term<?>> params = f.getValue();
          if (params.size() > 0) {
            final String val = ((ElementImpl) getParentNode()).getAttribute(params.get(0).toString());
            if (val != null) {
              final Text txt = getOwnerDocument().createTextNode(val);
              nodeList.add((NodeImpl) txt);
            }
          }
        } else {
          // TODO
        }
      }
    }
    return nodeList.toArray(new NodeImpl[nodeList.size()]);
  }
}
