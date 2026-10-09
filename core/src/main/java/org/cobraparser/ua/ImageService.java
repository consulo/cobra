package org.cobraparser.ua;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.ServiceLoader;

/**
 * Measures and paints the pictures of documents: images, image inputs and background images.
 *
 * @author VISTALL
 * @since 2026-10-09
 */
public abstract class ImageService {
  public static final ImageService INSTANCE = init();

  private static ImageService init() {
    ServiceLoader<ImageService> loader = ServiceLoader.load(ImageService.class, ImageService.class.getClassLoader());
    return loader.findFirst().orElseGet(DefaultImageService::new);
  }

  /**
   * @return the width of the picture in CSS pixels, or {@code -1} while it is not known
   */
  public abstract int getWidth(Image image, ImageObserver observer);

  /**
   * @return the height of the picture in CSS pixels, or {@code -1} while it is not known
   */
  public abstract int getHeight(Image image, ImageObserver observer);

  /**
   * Paints the picture scaled into the box, given in CSS pixels.
   */
  public abstract void paint(Graphics g, Image image, int x, int y, int width, int height, ImageObserver observer);
}
