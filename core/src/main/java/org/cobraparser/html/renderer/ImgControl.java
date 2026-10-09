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

import cz.vutbr.web.css.CSSProperty.VerticalAlign;
import org.cobraparser.html.domimpl.HTMLElementImpl;
import org.cobraparser.html.domimpl.HTMLImageElementImpl;
import org.cobraparser.html.domimpl.ImageEvent;
import org.cobraparser.html.domimpl.ImageListener;
import org.cobraparser.ua.ImageResponse;
import org.cobraparser.ua.ImageService;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;

class ImgControl extends BaseControl implements ImageListener {
  private static final long serialVersionUID = -1510794248068777990L;
  private volatile ImageResponse imageResponse = new ImageResponse();
  // private final UserAgentContext browserContext;
  private String lastSrc;

  public ImgControl(final HTMLImageElementImpl modelNode) {
    super(modelNode);
    // this.browserContext = pcontext;
    modelNode.addImageListener(this);
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);
    final ImageResponse imageResponse = this.imageResponse;
    if (imageResponse.isDecoded()) {
      assert(imageResponse.img != null);
      final Dimension size = this.getSize();
      final Insets insets = this.getInsets();
      final int width = size.width - insets.left - insets.right;
      final int height = size.height - insets.top - insets.bottom;
      ImageService.INSTANCE.paint(g, imageResponse.img, insets.left, insets.top, width, height, this);
    } else {
      // TODO: show alt text
    }
  }

  private Dimension preferredSize;
  private int declaredWidth;
  private int declaredHeight;

  @Override
  public void reset(final int availWidth, final int availHeight) {
    // Expected in the GUI thread.
    // final HTMLElementImpl element = this.controlElement;

    // TODO: Remove the parameters dw, dh, and members declaredWith, declaredHeight completely.
    //       They seem to be used only for old style syntax.

    // final int dw = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("width"), availWidth, -1);
    // final int dh = HtmlValues.getOldSyntaxPixelSize(element.getAttribute("height"), availHeight, -1);
    final int dw = -1;
    final int dh = -1;
    this.declaredWidth = dw;
    this.declaredHeight = dh;
    this.preferredSize = this.createPreferredSize(dw, dh);
  }

  @Override
  public VerticalAlign getVAlign() {
    final HTMLElementImpl element = this.controlElement;
    final VerticalAlign verticalAlign = element.getRenderState().getVerticalAlign();
    return verticalAlign == null ? VerticalAlign.BASELINE : verticalAlign;
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension ps = this.preferredSize;
    return ps == null ? new Dimension(0, 0) : ps;
  }

  public Dimension createPreferredSize(int dw, int dh) {
    final ImageResponse imageResponseLocal = this.imageResponse;
    if (!imageResponseLocal.isDecoded()) {
      return new Dimension(dw == -1 ? 0 : dw, dh == -1 ? 0 : dh);
    }

    assert(imageResponseLocal.img != null);
    final Image img = imageResponseLocal.img;

    if (dw == -1) {
      if (dh != -1) {
        final int iw = ImageService.INSTANCE.getWidth(img, this);
        final int ih = ImageService.INSTANCE.getHeight(img, this);
        if (ih == 0) {
          dw = iw;
        } else {
          dw = (dh * iw) / ih;
        }
      } else {
        dw = ImageService.INSTANCE.getWidth(img, this);
      }
    }
    if (dh == -1) {
      if (dw != -1) {
        final int iw = ImageService.INSTANCE.getWidth(img, this);
        final int ih = ImageService.INSTANCE.getHeight(img, this);
        if (iw == 0) {
          dh = ih == -1 ? 0 : ih;
        } else {
          dh = (dw * ih) / iw;
        }
      } else {
        dh = ImageService.INSTANCE.getHeight(img, this);
      }
    }
    return new Dimension(dw, dh);
  }

  private final boolean checkPreferredSizeChange() {
    final Dimension newPs = this.createPreferredSize(this.declaredWidth, this.declaredHeight);
    final Dimension ps = this.preferredSize;
    if (ps == null) {
      return true;
    }
    if ((ps.width != newPs.width) || (ps.height != newPs.height)) {
      this.preferredSize = newPs;
      return true;
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int,
   * int)
   */
  @Override
  public boolean imageUpdate(final Image img, final int infoflags, final int x, final int y, final int w, final int h) {
    if (((infoflags & ImageObserver.ALLBITS) != 0) || ((infoflags & ImageObserver.FRAMEBITS) != 0)) {
      SwingUtilities.invokeLater(() -> {
        if (!checkPreferredSizeChange()) {
          repaint();
        } else {
          ruicontrol.preferredSizeInvalidated();
        }
      });
    }
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.awt.Component#imageUpdate(java.awt.Image, int, int, int, int,
   * int)
   */
  public void imageUpdate(final Image img, final int w, final int h) {
    SwingUtilities.invokeLater(() -> {
      if (!checkPreferredSizeChange()) {
        repaint();
      } else {
        ruicontrol.preferredSizeInvalidated();
      }
    });
  }

  public boolean paintSelection(final Graphics g, final boolean inSelection, final RenderableSpot startPoint, final RenderableSpot endPoint) {
    return inSelection;
  }

  public void imageLoaded(final ImageEvent event) {
    // Implementation of ImageListener. Invoked in a request thread most likely.
    final ImageResponse imageResponseLocal = event.imageResponse;
    this.imageResponse = imageResponseLocal;
    if (imageResponseLocal.isDecoded()) {
      assert(imageResponseLocal.img != null);
      final Image image = imageResponseLocal.img;
      final int width = image.getWidth(this);
      final int height = image.getHeight(this);
      this.imageUpdate(image, width, height);
    }
  }

  public void imageAborted() {
    // do nothing
  }

  @Override
  public String toString() {
    return "ImgControl[src=" + this.lastSrc + "]";
  }

  @Override
  public boolean isReadyToPaint() {
    return imageResponse.isReadyToPaint();
  }
}
