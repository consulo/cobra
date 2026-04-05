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
 * Created on Nov 19, 2005
 */
package org.cobraparser.html.renderer;

import org.cobraparser.html.domimpl.HTMLElementImpl;
import org.cobraparser.html.style.RenderState;

import javax.swing.*;
import java.awt.*;

class HrControl extends BaseControl {
  private static final long serialVersionUID = 2138367420714598428L;

  public HrControl(final HTMLElementImpl modelNode) {
    super(modelNode);
    setOpaque(false);
  }

  @Override
  public void updateUI() {
    // Prevent L&F from installing any component UI — we paint ourselves
  }

  @Override
  protected void paintComponent(final Graphics g) {
    Color c = UIManager.getColor("Separator.foreground");
    if (c == null) {
      c = UIManager.getColor("Label.foreground");
    }
    if (c == null) {
      c = Color.GRAY;
    }
    g.setColor(c);
    final int y = getHeight() / 2;
    g.drawLine(0, y, getWidth() - 1, y);
  }

  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    return inSelection;
  }

  private int availWidth;

  @Override
  public void reset(final int availWidth, final int availHeight) {
    this.availWidth = availWidth;
  }

  @Override
  public Dimension getPreferredSize() {
    final RenderState rs = controlElement.getRenderState();
    final int vspace;
    if (rs != null) {
      final Font font = rs.getFont();
      vspace = font != null ? Math.round(font.getSize() * 0.8f) : 10;
    } else {
      vspace = 10;
    }
    return new Dimension(this.availWidth, 1 + 2 * vspace);
  }
}
