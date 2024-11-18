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
package org.cobraparser.util.gui;

import org.cobraparser.css.StandardColorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author J. H. S.
 */
public class ColorFactory {
    private static final Logger logger = LoggerFactory.getLogger(ColorFactory.class.getName());

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public static final ColorFactory INSTANCE = new ColorFactory();

    public static ColorFactory getInstance() {
        return INSTANCE;
    }

    private Map<String, Color> cachedColors = new ConcurrentHashMap<>();

    private ColorFactory() {
    }

    public StandardColorProvider getStandardColorProvider() {
        return StandardColorProvider.INSTANCE;
    }

    private static final String RGB_START = "rgb(";
    private static final String RGBA_START = "rgba(";

    public boolean isColor(final String colorSpec) {
        if (colorSpec.startsWith("#")) {
            return true;
        }

        final String normalSpec = colorSpec.toLowerCase();
        if (normalSpec.startsWith(RGB_START)) {
            return true;
        }

        Color color = cachedColors.get(normalSpec);
        if (color != null) {
            return true;
        }

        return getStandardColorProvider().getColor(normalSpec) != null;
    }

    public Color getColor(final String colorSpec) {
        final String normalSpec = colorSpec.toLowerCase();

        Color standardColor = getStandardColorProvider().getColor(normalSpec);
        if (standardColor != null) {
            return standardColor;
        }

        return cachedColors.computeIfAbsent(normalSpec, ColorFactory::parseColor);
    }

    private static Color parseColor(String normalSpec) {
        if (normalSpec.startsWith(RGB_START)) {
            // CssParser produces this format.
            final int endIdx = normalSpec.lastIndexOf(')');
            final String commaValues = endIdx == -1 ? normalSpec.substring(RGB_START.length()) : normalSpec.substring(RGB_START.length(),
                endIdx);
            final StringTokenizer tok = new StringTokenizer(commaValues, ",");
            int r = 0, g = 0, b = 0;
            if (tok.hasMoreTokens()) {
                final String rstr = tok.nextToken().trim();
                try {
                    r = Integer.parseInt(rstr);
                }
                catch (final NumberFormatException nfe) {
                    // ignore
                }
                if (tok.hasMoreTokens()) {
                    final String gstr = tok.nextToken().trim();
                    try {
                        g = Integer.parseInt(gstr);
                    }
                    catch (final NumberFormatException nfe) {
                        // ignore
                    }
                    if (tok.hasMoreTokens()) {
                        final String bstr = tok.nextToken().trim();
                        try {
                            b = Integer.parseInt(bstr);
                        }
                        catch (final NumberFormatException nfe) {
                            // ignore
                        }
                    }
                }
            }
            return new Color(r, g, b);
        }
        else if (normalSpec.startsWith("#")) {
            // TODO: OPTIMIZE: It would be more efficient to
            // create new Color(hex), but CssParser doesn't
            // give us values formatted with "#" either way.
            final int len = normalSpec.length();
            final int[] rgba = new int[4];
            rgba[3] = 255;
            if (len == 4) {
                for (int i = 1; i < 4; i++) {
                    final String hexText = normalSpec.substring(i, i + Math.min(1, len - i));
                    try {
                        final int singleDigitValue = Integer.parseInt(hexText, 16);
                        rgba[i - 1] = (singleDigitValue << 4) | singleDigitValue;
                    }
                    catch (final NumberFormatException nfe) {
                        // Ignore
                    }
                }

            }
            else {
                for (int i = 0; i < rgba.length; i++) {
                    final int idx = (2 * i) + 1;
                    if (idx < len) {
                        final String hexText = normalSpec.substring(idx, idx + Math.min(2, len - idx));
                        try {
                            rgba[i] = Integer.parseInt(hexText, 16);
                        }
                        catch (final NumberFormatException nfe) {
                            // Ignore
                        }
                    }
                }
            }
            return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
        else if (normalSpec.startsWith(RGBA_START)) {
            final int endIdx = normalSpec.lastIndexOf(')');
            final String commaValues = endIdx == -1 ? normalSpec.substring(RGBA_START.length()) : normalSpec.substring(RGBA_START.length(),
                endIdx);
            final StringTokenizer tok = new StringTokenizer(commaValues, ",");
            try {
                if (tok.hasMoreTokens()) {
                    final String rstr = tok.nextToken().trim();
                    final int r = Integer.parseInt(rstr);
                    if (tok.hasMoreTokens()) {
                        final String gstr = tok.nextToken().trim();
                        final int g = Integer.parseInt(gstr);
                        if (tok.hasMoreTokens()) {
                            final String bstr = tok.nextToken().trim();
                            final int b = Integer.parseInt(bstr);
                            if (tok.hasMoreTokens()) {
                                final String astr = tok.nextToken().trim();
                                final float a = Float.parseFloat(astr);
                                return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
                            }
                        }
                    }
                }
            }
            catch (final NumberFormatException nfe) {
                // ignore
            }
        }
        
        if (logger.isWarnEnabled()) {
            logger.warn("getColor(): Color spec [" + normalSpec + "] unknown.");
        }
        return Color.RED;
    }
}
