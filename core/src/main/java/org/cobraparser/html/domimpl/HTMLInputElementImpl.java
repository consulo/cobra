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
 * Created on Jan 15, 2006
 */
package org.cobraparser.html.domimpl;

import org.cobraparser.html.FormInput;
import org.w3c.dom.html.HTMLInputElement;

public class HTMLInputElementImpl extends HTMLBaseInputElement implements HTMLInputElement {
  public HTMLInputElementImpl(final String name) {
    super(name);
  }

  private boolean defaultChecked;

  public boolean getDefaultChecked() {
    return this.defaultChecked;
  }

  public void setDefaultChecked(final boolean defaultChecked) {
    this.defaultChecked = defaultChecked;
  }

  @Override
  public boolean getChecked() {
    final InputContext ic = this.inputContext;
    if (ic == null) {
      return this.getAttributeAsBoolean("checked");
    } else {
      return ic.getChecked();
    }
  }

  @Override
  public void setChecked(final boolean checked) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setChecked(checked);
    }
  }

  public int getMaxLength() {
    final InputContext ic = this.inputContext;
    return ic == null ? 0 : ic.getMaxLength();
  }

  public void setMaxLength(final int maxLength) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setMaxLength(maxLength);
    }
  }

  /* public int getSize() {
    final InputContext ic = this.inputContext;
    return ic == null ? 0 : ic.getControlSize();

  public void setSize(final int size) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setControlSize(size);
    }
  }
  }*/

  public String getSize() {
    final InputContext ic = this.inputContext;
    final int size = ic == null ? 0 : ic.getControlSize();
    return String.valueOf(size);
  }

  public void setSize(final String size) {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.setControlSize(Integer.parseInt(size));
    }
  }

  public String getSrc() {
    return this.getAttribute("src");
  }

  public void setSrc(final String src) {
    this.setAttribute("src", src);
  }

  /**
   * Gets input type in lowercase.
   */
  public String getType() {
    final String type = this.getAttribute("type");
    return type == null ? null : type.toLowerCase();
  }

  public void setType(final String type) {
    this.setAttribute("type", type);
  }

  public String getUseMap() {
    return this.getAttribute("usemap");
  }

  public void setUseMap(final String useMap) {
    this.setAttribute("usemap", useMap);
  }

  public void click() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.click();
    }
  }

  public boolean isSubmittableWithEnterKey() {
    final String type = this.getType();
    return ((type == null) || "".equals(type) || "text".equals(type) || "password".equals(type));
  }

  public boolean isSubmittableWithPress() {
    final String type = this.getType();
    return "submit".equals(type) || "image".equals(type);
  }

  public boolean isSubmitInput() {
    final String type = this.getType();
    return "submit".equals(type);
  }

  public boolean isImageInput() {
    final String type = this.getType();
    return "image".equals(type);
  }

  public boolean isResetInput() {
    final String type = this.getType();
    return "reset".equals(type);
  }

  @Override
  void resetInput() {
    final InputContext ic = this.inputContext;
    if (ic != null) {
      ic.resetInput();
    }
  }

  @Override
  protected FormInput[] getFormInputs() {
    final String type = this.getType();
    final String name = this.getName();
    if (name == null) {
      return null;
    }
    if (type == null) {
      return new FormInput[] { new FormInput(name, this.getValue()) };
    } else {
      if ("text".equals(type) || "password".equals(type) || "hidden".equals(type) || "url".equals(type) || "number".equals(type) || "search".equals(type) || "".equals(type)) {
        return new FormInput[] { new FormInput(name, this.getValue()) };
      } else if ("submit".equals(type)) {
        // It's done as an "extra" form input
        return null;
      } else if ("radio".equals(type) || "checkbox".equals(type)) {
        if (this.getChecked()) {
          String value = this.getValue();
          if ((value == null) || (value.length() == 0)) {
            value = "on";
          }
          return new FormInput[] { new FormInput(name, value) };
        } else {
          return null;
        }
      } else if ("image".equals(type)) {
        // It's done as an "extra" form input
        return null;
      } else if ("file".equals(type)) {
        final java.io.File file = this.getFileValue();
        if (file == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("getFormInputs(): File input named " + name + " has null file.");
          }
          return null;
        } else {
          return new FormInput[] { new FormInput(name, file) };
        }
      } else {
        return null;
      }
    }
  }
}
