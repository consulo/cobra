/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Apr 17, 2005
 */
package org.cobraparser.html.renderer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import org.cobraparser.html.domimpl.ModelNode;
import org.cobraparser.html.style.RenderState;

class RWord extends BaseBoundableRenderable {
  final String shownWord;
  /** Offset of the first source character of this word in the text node. */
  final int textStart;
  /** Offset just past the last source character of this word in the text node. */
  final int textEnd;
  public final FontMetrics fontMetrics;
  public final int descent;
  public final int ascentPlusLeading;

  public RWord(final ModelNode me, final String word, final int textStart, final RenderableContainer container, final FontMetrics fontMetrics,
      final int descent, final int ascentPlusLeading,
      final int height, final int textTransform) {
    super(container, me);
    final String renderedWord = textTransform == RenderState.TEXTTRANSFORM_NONE ? word : transformText(word, textTransform);
    this.shownWord = renderedWord;
    this.textStart = textStart;
    this.textEnd = textStart + word.length();
    this.fontMetrics = fontMetrics;
    this.descent = descent;
    this.ascentPlusLeading = ascentPlusLeading;
    this.height = height;
    // TODO: In anti-aliasing, stringWidth is said not to be reliable.
    // Dimensions set when constructed.
    this.width = fontMetrics.stringWidth(renderedWord);
  }

  private static String transformText(final String word, final int textTransform) {
    String string;
    switch (textTransform) {
    case RenderState.TEXTTRANSFORM_CAPITALIZE:
      string = Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase();
      break;
    case RenderState.TEXTTRANSFORM_LOWERCASE:
      string = word.toLowerCase();
      break;
    case RenderState.TEXTTRANSFORM_UPPERCASE:
      string = word.toUpperCase();
      break;
    default:
      string = word;
    }
    return string;
  }

  @Override
  protected void invalidateLayoutLocal() {
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * net.sourceforge.xamj.domimpl.markup.Renderable#paint(java.awt.Graphics)
   */
  public void paint(final Graphics g) {
    final RenderState rs = this.modelNode.getRenderState();

    if (rs.getVisibility() != RenderState.VISIBILITY_VISIBLE) {
      // Just don't paint it.
      return;
    }

    final String word = this.shownWord;
    final int width = this.width;
    final int ascentPlusLeading = this.ascentPlusLeading;
    final int height = this.height;
    final int textDecoration = rs.getTextDecorationMask();
    final Color bkg = rs.getTextBackgroundColor();
    if (bkg != null) {
      final Color oldColor = g.getColor();
      try {
        g.setColor(bkg);
        g.fillRect(0, 0, width, height);
      } finally {
        g.setColor(oldColor);
      }
    }
    g.drawString(word, 0, ascentPlusLeading);
    final int td = textDecoration;
    if (td != 0) {
      if ((td & RenderState.MASK_TEXTDECORATION_UNDERLINE) != 0) {
        final int lineOffset = ascentPlusLeading + 2;
        g.drawLine(0, lineOffset, width, lineOffset);
      }
      if ((td & RenderState.MASK_TEXTDECORATION_LINE_THROUGH) != 0) {
        final FontMetrics fm = this.fontMetrics;
        final int lineOffset = fm.getLeading() + ((fm.getAscent() + fm.getDescent()) / 2);
        g.drawLine(0, lineOffset, width, lineOffset);
      }
      if ((td & RenderState.MASK_TEXTDECORATION_OVERLINE) != 0) {
        final FontMetrics fm = this.fontMetrics;
        final int lineOffset = fm.getLeading();
        g.drawLine(0, lineOffset, width, lineOffset);
      }
      if ((td & RenderState.MASK_TEXTDECORATION_BLINK) != 0) {
        // TODO
      }
    }
    final Color over = rs.getOverlayColor();
    if (over != null) {
      final Color oldColor = g.getColor();
      try {
        g.setColor(over);
        g.fillRect(0, 0, width, height);
      } finally {
        g.setColor(oldColor);
      }
    }
  }

  public boolean onMouseClick(final MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseClick(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onDoubleClick(final MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onDoubleClick(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMousePressed(final MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseDown(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMouseReleased(final MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseUp(me, event, x, y);
    } else {
      return true;
    }
  }

  public boolean onMouseDisarmed(final MouseEvent event) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onMouseDisarmed(me, event);
    } else {
      return true;
    }
  }

  public RenderableSpot getLowestRenderableSpot(final int x, final int y) {
    return new RenderableSpot(this, x, y);
  }

  public boolean isContainedByNode() {
    return true;
  }

  public boolean onRightClick(final MouseEvent event, final int x, final int y) {
    final ModelNode me = this.modelNode;
    if (me != null) {
      return HtmlController.getInstance().onContextMenu(me, event, x, y);
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    return "RWord[word=" + this.shownWord + "]";
  }
}
