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
 * Created on Oct 22, 2005
 */
package org.cobraparser.html.domimpl;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class LocalErrorHandler implements ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(LocalErrorHandler.class.getName());

  /**
   * @param context
   */
  public LocalErrorHandler() {
  }

  public void warning(final SAXParseException exception) throws SAXException {
    logger.warn(exception.getMessage(), exception.getCause());
  }

  public void error(final SAXParseException exception) throws SAXException {
    logger.error( exception.getMessage(), exception.getCause());
  }

  public void fatalError(final SAXParseException exception) throws SAXException {
    logger.error(exception.getMessage(), exception.getCause());
  }
}
