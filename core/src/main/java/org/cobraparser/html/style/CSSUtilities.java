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

package org.cobraparser.html.style;

import cz.vutbr.web.css.*;
import cz.vutbr.web.csskit.RuleFactoryImpl;
import cz.vutbr.web.csskit.antlr4.CSSParserFactory;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.domimpl.HTMLElementImpl;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.UserAgentContext;
import org.cobraparser.ua.UserAgentContext.Request;
import org.cobraparser.ua.UserAgentContext.RequestKind;
import org.cobraparser.util.SecurityUtil;
import org.cobraparser.util.Strings;
import org.cobraparser.util.Urls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.stylesheets.MediaList;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.StringTokenizer;

public class CSSUtilities {
  private static final Logger logger = LoggerFactory.getLogger(CSSUtilities.class.getName());
  private static final RuleFactory rf = RuleFactoryImpl.getInstance();

  private CSSUtilities() {
  }

  public static String preProcessCss(final String text) {
    try {
      final BufferedReader reader = new BufferedReader(new StringReader(text));
      String line;
      final StringBuffer sb = new StringBuffer();
      String pendingLine = null;
      // Only last line should be trimmed.
      while ((line = reader.readLine()) != null) {
        final String tline = line.trim();
        if (tline.length() != 0) {
          if (pendingLine != null) {
            sb.append(pendingLine);
            sb.append("\r\n");
            pendingLine = null;
          }
          if (tline.startsWith("//")) {
            pendingLine = line;
            continue;
          }
          sb.append(line);
          sb.append("\r\n");
        }
      }
      return sb.toString();
    } catch (final IOException ioe) {
      // not possible
      throw new IllegalStateException(ioe.getMessage());
    }
  }

  public static InputSource getCssInputSourceForStyleSheet(final String text, final String scriptURI) {
    final java.io.Reader reader = new StringReader(text);
    final InputSource is = new InputSource(reader);
    is.setURI(scriptURI);
    return is;
  }

  public static StyleSheet jParseStyleSheet(final org.w3c.dom.Node ownerNode, final String baseURI, final String stylesheetStr, final UserAgentContext bcontext) {
    return jParseCSS2(ownerNode, baseURI, stylesheetStr, bcontext);
  }

  public static StyleSheet jParse(final org.w3c.dom.Node ownerNode, final String href, final HTMLDocumentImpl doc, final String baseUri,
      final boolean considerDoubleSlashComments) throws MalformedURLException {
    final UserAgentContext bcontext = doc.getUserAgentContext();
    final NetworkRequest request = bcontext.createHttpRequest();
    final URL baseURL = new URL(baseUri);
    final URL cssURL = Urls.createURL(baseURL, href);
    final String cssURI = cssURL.toExternalForm();
    // Perform a synchronous request
    SecurityUtil.doPrivileged(() -> {
      try {
        request.open("GET", cssURI, false);
        request.send(null, new Request(cssURL, RequestKind.CSS));
      } catch (final IOException thrown) {
        logger.warn("parse()", thrown);
      }
      return getEmptyStyleSheet();
    });
    final int status = request.getStatus();
    if ((status != 200) && (status != 0)) {
      logger.warn("Unable to parse CSS. URI=[" + cssURI + "]. Response status was " + status + ".");
      return getEmptyStyleSheet();
    }

    final String text = request.getResponseText();
    if ((text != null) && !"".equals(text)) {
      final String processedText = considerDoubleSlashComments ? preProcessCss(text) : text;
      return jParseCSS2(ownerNode, cssURI, processedText, bcontext);
    } else {
      return getEmptyStyleSheet();
    }
  }

  public static StyleSheet getEmptyStyleSheet() {
    final StyleSheet css = rf.createStyleSheet();
    css.unlock();
    return css;
  }

  private static StyleSheet jParseCSS2(final org.w3c.dom.Node ownerNode, final String cssURI, final String processedText,
      final UserAgentContext bcontext) {

    try {
      final URL base = new URL(cssURI);
      CSSFactory.setAutoImportMedia(new MediaSpec("screen"));
      return CSSParserFactory.getInstance().parse(processedText, new SafeNetworkProcessor(bcontext), "utf-8", CSSParserFactory.SourceType.EMBEDDED, base);
    } catch (IOException | CSSException e) {
      logger.debug( "Unable to parse CSS. URI=[" + cssURI + "].", e);
      return getEmptyStyleSheet();
    }
  }

  public static class SafeNetworkProcessor implements NetworkProcessor {
    final UserAgentContext bcontext;

    public SafeNetworkProcessor(final UserAgentContext bcontext) {
      this.bcontext = bcontext;
    }

    @Override
    public InputStream fetch(final URL url) throws IOException {
      try {
        return AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
          final NetworkRequest request = bcontext.createHttpRequest();
          request.open("GET", url, false);
          request.send(null, new Request(url, RequestKind.CSS));
          final byte[] responseBytes = request.getResponseBytes();
          if (responseBytes == null) {
            // This can happen when a request is denied by the request manager.
            throw new IOException("Empty response");
          } else {
            return new ByteArrayInputStream(responseBytes);
          }
        });
      } catch (final PrivilegedActionException e) {
        if (e.getException() instanceof IOException) {
          throw (IOException) e.getException();
        } else {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static StyleSheet jParseInlineStyle(final String style, final String encoding,
      final HTMLElementImpl element, final boolean inlinePriority) {
    try {
      return CSSParserFactory.getInstance().parse(style, new SafeNetworkProcessor(null), null, CSSParserFactory.SourceType.INLINE, element, inlinePriority, element.getDocumentURL());
    } catch (IOException | CSSException e) {
      logger.debug("Unable to parse CSS. CSS=[" + style + "].", e);
      return getEmptyStyleSheet();
    }
  }

  public static boolean matchesMedia(final String mediaValues, final UserAgentContext rcontext) {
    if ((mediaValues == null) || (mediaValues.length() == 0)) {
      return true;
    }
    if (rcontext == null) {
      return false;
    }
    final StringTokenizer tok = new StringTokenizer(mediaValues, ",");
    while (tok.hasMoreTokens()) {
      final String token = tok.nextToken().trim();
      final String mediaName = Strings.trimForAlphaNumDash(token);
      if (rcontext.isMedia(mediaName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean matchesMedia(final MediaList mediaList, final UserAgentContext rcontext) {
    if (mediaList == null) {
      return true;
    }
    final int length = mediaList.getLength();
    if (length == 0) {
      return true;
    }
    if (rcontext == null) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      final String mediaName = mediaList.item(i);
      if (rcontext.isMedia(mediaName)) {
        return true;
      }
    }
    return false;
  }

}
