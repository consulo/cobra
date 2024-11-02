package org.cobraparser.html.renderer;

import org.cobraparser.html.style.HtmlInsets;

import java.awt.*;

final class BorderOverrider {
  boolean leftOverridden = false;
  boolean rightOverridden = false;
  boolean bottomOverridden = false;
  boolean topOverridden = false;

  Insets get(final Insets borderInsets) {
    if (leftOverridden || rightOverridden || topOverridden || bottomOverridden) {
      final int topDash = topOverridden ? 0 : borderInsets.top;
      final int leftDash = leftOverridden ? 0 : borderInsets.left;
      final int bottomDash = bottomOverridden ? 0 : borderInsets.bottom;
      final int rightDash = rightOverridden ? 0 : borderInsets.right;
      return new Insets(topDash, leftDash, bottomDash, rightDash);
    }
    return borderInsets;
  }

  public void copyFrom(final BorderOverrider other) {
    this.topOverridden = other.topOverridden;
    this.leftOverridden = other.leftOverridden;
    this.rightOverridden = other.rightOverridden;
    this.bottomOverridden = other.bottomOverridden;
  }

  public HtmlInsets get(final HtmlInsets borderInsets) {
    if ((borderInsets != null) && (leftOverridden || rightOverridden || topOverridden || bottomOverridden)) {
      final int topDash = topOverridden ? 0 : borderInsets.top;
      final int leftDash = leftOverridden ? 0 : borderInsets.left;
      final int bottomDash = bottomOverridden ? 0 : borderInsets.bottom;
      final int rightDash = rightOverridden ? 0 : borderInsets.right;
      return new HtmlInsets(topDash, leftDash, bottomDash, rightDash, HtmlInsets.TYPE_PIXELS);
    }
    return borderInsets;
  }
}