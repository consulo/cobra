/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2015 Uproot Labs India Pvt Ltd

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

 */

package org.cobraparser.html.renderer;

import java.awt.FontMetrics;

import org.cobraparser.html.domimpl.ModelNode;

public class RTab extends RWord {

  public RTab(final ModelNode me, final int textStart, final RenderableContainer container, final FontMetrics fontMetrics, final int descent,
      final int ascentPlusLeading, final int height, final int numSpaces) {
    super(me, "\t", textStart, container, fontMetrics, descent, ascentPlusLeading, height, 0);
    this.width = fontMetrics.charWidth(' ') * numSpaces;
  }

}
