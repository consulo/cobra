package org.cobraparser.ua;

import org.cobraparser.html.style.HtmlValues;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
class DefaultImageService extends ImageService {
  @Override
  public int getWidth(final Image image, final ImageObserver observer) {
    final int width = image.getWidth(observer);
    return width < 0 ? width : HtmlValues.scaleToDevicePixels(width);
  }

  @Override
  public int getHeight(final Image image, final ImageObserver observer) {
    final int height = image.getHeight(observer);
    return height < 0 ? height : HtmlValues.scaleToDevicePixels(height);
  }

  @Override
  public void paint(final Graphics g, final Image image, final int x, final int y, final int width, final int height, final ImageObserver observer) {
    if ((width <= 0) || (height <= 0)) {
      return;
    }

    final int imageWidth = image.getWidth(observer);
    final int imageHeight = image.getHeight(observer);
    if ((width < imageWidth) || (height < imageHeight)) {
      g.drawImage(getScaledInstance(image, width, height, observer), x, y, width, height, observer);
    } else {
      final Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(image, x, y, width, height, observer);
      } finally {
        g2.dispose();
      }
    }
  }

  // Adapted from: https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
  private static Image getScaledInstance(final Image img, final int targetWidth, final int targetHeight, final ImageObserver observer) {
    Image ret = img;
    int w = img.getWidth(observer);
    int h = img.getHeight(observer);

    while (w != targetWidth || h != targetHeight) {
      if (w > targetWidth) {
        w /= 2;
      }
      if (w < targetWidth) {
        w = targetWidth;
      }

      if (h > targetHeight) {
        h /= 2;
      }
      if (h < targetHeight) {
        h = targetHeight;
      }

      final BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    }

    return ret;
  }
}
