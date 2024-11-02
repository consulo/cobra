/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2014 Uproot Labs India Pvt Ltd

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

package org.cobraparser.html.style;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.Term;
import cz.vutbr.web.csskit.TermURIImpl;
import org.cobraparser.js.AbstractScriptableDelegate;
import org.cobraparser.js.HideFromJS;
import org.cobraparser.util.Urls;
import org.w3c.dom.css.CSS2Properties;

import java.net.MalformedURLException;
import java.net.URL;

abstract public class JStyleProperties extends AbstractScriptableDelegate implements CSS2Properties {
    private String overlayColor;
    final private CSS2PropertiesContext context;
    // TODO: this flag can be removed when the layout can handle empty strings
    // currently there is only a check for null and not for empty string
    final protected boolean nullIfAbsent;

    public JStyleProperties(final CSS2PropertiesContext context, final boolean nullIfAbsent) {
        this.context = context;
        this.nullIfAbsent = nullIfAbsent;
    }

    //TODO All the methods that are not implemented need more detailed understanding.
    // most of them are short hand properties and they need to be constructed from the long
    // forms of the respective properties
    @Override
    public String getAzimuth() {
        return helperTryBoth("azimuth");
    }

    @Override
    public String getBackground() {
        // TODO need to implement this method. GH #143
        return "";
    }

    @Override
    public String getBackgroundAttachment() {
        return helperGetProperty("background-attachment");
    }

    @Override
    public String getBackgroundColor() {
        return helperTryBoth("background-color");
    }

    @Override
    public String getBackgroundImage() {
        // TODO
        // need to check if upstream can provide the absolute url of
        //  the image so that it can directly be passed.
        String quotedUri = null;
        final TermURIImpl t = (TermURIImpl) getNodeData().getValue("background-image", false);
        if (t != null) {
            URL finalUrl = null;
            try {
                finalUrl = Urls.createURL(t.getBase(), t.getValue());
            }
            catch (final MalformedURLException e) {
                e.printStackTrace();
            }
            quotedUri = finalUrl == null ? null : finalUrl.toString();
        }
        return quotedUri == null ? null : "url(" + quotedUri + ")";
    }

    @Override
    public String getBackgroundPosition() {
        return helperGetValue("background-position");
    }

    @Override
    public String getBackgroundRepeat() {
        return helperGetProperty("background-repeat");
    }

    @Override
    public String getBorder() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderCollapse() {
        return helperGetProperty("border-collapse");
    }

    @Override
    public String getBorderColor() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderSpacing() {
        return helperGetValue("border-spacing");
    }

    @Override
    public String getBorderStyle() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderTop() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderRight() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderBottom() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderLeft() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBorderTopColor() {
        return helperTryBoth("border-top-color");
    }

    @Override
    public String getBorderRightColor() {
        return helperTryBoth("border-right-color");
    }

    @Override
    public String getBorderBottomColor() {
        return helperTryBoth("border-bottom-color");
    }

    @Override
    public String getBorderLeftColor() {
        return helperTryBoth("border-left-color");
    }

    @Override
    public String getBorderTopStyle() {
        return helperGetProperty("border-top-style");
    }

    @Override
    public String getBorderRightStyle() {
        return helperGetProperty("border-right-style");
    }

    @Override
    public String getBorderBottomStyle() {
        return helperGetProperty("border-bottom-style");
    }

    @Override
    public String getBorderLeftStyle() {
        return helperGetProperty("border-left-style");
    }

    @Override
    public String getBorderTopWidth() {
        final String width = helperTryBoth("border-top-width");
        // TODO
        // temp hack to support border thin/medium/thick
        // need to implement it at the place where it is actually being processed
        return border2Pixel(width);
    }

    @Override
    public String getBorderRightWidth() {
        final String width = helperTryBoth("border-right-width");
        // TODO
        // temp hack to support border thin/medium/thick
        // need to implement it at the place where it is actually being processed
        return border2Pixel(width);
    }

    @Override
    public String getBorderBottomWidth() {
        final String width = helperTryBoth("border-bottom-width");
        // TODO
        // temp hack to support border thin/medium/thick
        // need to implement it at the place where it is actually being processed
        return border2Pixel(width);
    }

    @Override
    public String getBorderLeftWidth() {
        final String width = helperTryBoth("border-left-width");
        // TODO
        // temp hack to support border thin/medium/thick
        // need to implement it at the place where it is actually being processed
        return border2Pixel(width);
    }

    // TODO
    // temp hack to support border thin/medium/thick
    // this method should be removed once it is implemented where border is actually processed
    private static String border2Pixel(final String width) {
        if (width != null) {
            if ("thin".equalsIgnoreCase(width)) {
                return HtmlValues.BORDER_THIN_SIZE;
            }
            if ("medium".equalsIgnoreCase(width)) {
                return HtmlValues.BORDER_MEDIUM_SIZE;
            }
            if ("thick".equalsIgnoreCase(width)) {
                return HtmlValues.BORDER_THICK_SIZE;
            }
        }
        return width;
    }

    @Override
    public String getBorderWidth() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBottom() {
        return helperTryBoth("bottom");
    }

    @Override
    public String getCaptionSide() {
        return helperGetProperty("caption-side");
    }

    @Override
    public String getClear() {
        return helperGetProperty("clear");
    }

    @Override
    public String getClip() {
        return helperTryBoth("clip");
    }

    @Override
    public String getColor() {
        return helperTryBoth("color");
    }

    @Override
    public String getContent() {
        return helperTryBoth("content");
    }

    @Override
    public String getCounterIncrement() {
        return helperTryBoth("couter-increment");
    }

    @Override
    public String getCounterReset() {
        return helperTryBoth("couter-reset");
    }

    @Override
    public String getCue() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCueAfter() {
        return helperTryBoth("cue-after");
    }

    @Override
    public String getCueBefore() {
        return helperTryBoth("cue-before");
    }

    @Override
    public String getCursor() {
        return helperGetProperty("cursor");
    }

    @Override
    public String getDirection() {
        return helperGetProperty("direction");
    }

    @Override
    public String getDisplay() {
        return helperGetProperty("display");
    }

    @Override
    public String getElevation() {
        return helperTryBoth("elevation");
    }

    @Override
    public String getEmptyCells() {
        return helperGetProperty("empty-cells");
    }

    @Override
    public String getCssFloat() {
        return this.getFloat();
    }

    @Override
    public String getFont() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFontFamily() {
        return helperTryBoth("font-family");
    }

    @Override
    public String getFontSize() {
        return helperTryBoth("font-size");
    }

    @Override
    public String getFontSizeAdjust() {
        return helperTryBoth("font-adjust");
    }

    @Override
    public String getFontStretch() {
        return helperGetProperty("font-stretch");
    }

    @Override
    public String getFontStyle() {
        return helperGetProperty("font-style");
    }

    @Override
    public String getFontVariant() {
        return helperGetProperty("font-variant");
    }

    @Override
    public String getFontWeight() {
        return helperGetProperty("font-weight");
    }

    @Override
    public String getHeight() {
        return helperGetValue("height");
    }

    @Override
    public String getLeft() {
        return helperTryBoth("left");
    }

    @Override
    public String getLetterSpacing() {
        return helperTryBoth("letter-spacing");
    }

    @Override
    public String getLineHeight() {
        return helperTryBoth("line-height");
    }

    @Override
    public String getListStyle() {
        final String listStyleType = getListStyleType();
        final String listStylePosition = getListStylePosition();
        final StringBuilder listStyle = new StringBuilder();

        if ((listStyleType != null) && !("null".equals(listStyleType))) {
            listStyle.append(listStyleType);
        }

        if ((listStylePosition != null) && !("null".equals(listStylePosition))) {
            listStyle.append(" " + listStylePosition);
        }

        final String listStyleText = listStyle.toString().trim();
        return listStyleText.length() == 0 ? null : listStyleText;
    }

    @Override
    public String getListStyleImage() {
        return helperTryBoth("list-style-image");
    }

    @Override
    public String getListStylePosition() {
        return helperGetProperty("list-style-position");
    }

    @Override
    public String getListStyleType() {
        return helperGetProperty("list-style-type");
    }

    @Override
    public String getMargin() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMarginTop() {
        return helperTryBoth("margin-top");
    }

    @Override
    public String getMarginRight() {
        return helperTryBoth("margin-right");
    }

    @Override
    public String getMarginBottom() {
        return helperTryBoth("margin-bottom");
    }

    @Override
    public String getMarginLeft() {
        return helperTryBoth("margin-left");
    }

    @Override
    public String getMarkerOffset() {
        return helperTryBoth("marker-offset");
    }

    @Override
    public String getMarks() {
        return helperGetProperty("marks");
    }

    @Override
    public String getMaxHeight() {
        return helperTryBoth("max-height");
    }

    @Override
    public String getMaxWidth() {
        return helperTryBoth("max-width");
    }

    @Override
    public String getMinHeight() {
        return helperTryBoth("min-height");
    }

    @Override
    public String getMinWidth() {
        return helperTryBoth("min-width");
    }

    @Override
    public String getOrphans() {
        return helperGetValue("orphans");
    }

    @Override
    public String getOutline() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOutlineColor() {
        return helperTryBoth("outline-color");
    }

    @Override
    public String getOutlineStyle() {
        return helperGetProperty("outline-style");
    }

    //TODO add support for thick/think/medium
    @Override
    public String getOutlineWidth() {
        final String width = helperTryBoth("outline-border");
        return border2Pixel(width);
    }

    @Override
    public String getOverflow() {
        return helperGetProperty("overflow");
    }

    @Override
    public String getPadding() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPaddingTop() {
        return helperGetValue("padding-top");
    }

    @Override
    public String getPaddingRight() {
        return helperGetValue("padding-right");
    }

    @Override
    public String getPaddingBottom() {
        return helperGetValue("padding-bottom");
    }

    @Override
    public String getPaddingLeft() {
        return helperGetValue("padding-left");
    }

    @Override
    public String getPage() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPageBreakAfter() {
        return helperGetProperty("page-break-after");
    }

    @Override
    public String getPageBreakBefore() {
        return helperGetProperty("page-break-before");
    }

    @Override
    public String getPageBreakInside() {
        return helperGetProperty("page-break-inside");
    }

    @Override
    public String getPause() {
        return helperGetValue("pause");
    }

    @Override
    public String getPauseAfter() {
        return helperGetValue("pause-after");
    }

    @Override
    public String getPauseBefore() {
        return helperGetValue("pause-before");
    }

    @Override
    public String getPitch() {
        return helperTryBoth("pitch");
    }

    @Override
    public String getPitchRange() {
        return helperGetValue("pitchRange");
    }

    @Override
    public String getPlayDuring() {
        return helperTryBoth("play-during");
    }

    @Override
    public String getPosition() {
        return helperGetProperty("position");
    }

    @Override
    public String getQuotes() {
        return helperTryBoth("quotes");
    }

    @Override
    public String getRichness() {
        return helperGetValue("richness");
    }

    @Override
    public String getRight() {
        return helperTryBoth("right");
    }

    @Override
    public String getSize() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSpeak() {
        return helperGetProperty("speak");
    }

    @Override
    public String getSpeakHeader() {
        return helperGetProperty("speak-header");
    }

    @Override
    public String getSpeakNumeral() {
        return helperGetProperty("speak-numeral");
    }

    @Override
    public String getSpeakPunctuation() {
        return helperGetProperty("speak-punctuation");
    }

    @Override
    public String getSpeechRate() {
        return helperTryBoth("speech-rate");
    }

    @Override
    public String getStress() {
        return helperGetValue("stress");
    }

    @Override
    public String getTableLayout() {
        return helperGetProperty("table-layout");
    }

    @Override
    public String getTextAlign() {
        return helperGetProperty("text-align");
    }

    @Override
    public String getTextDecoration() {
        return helperTryBoth("text-decoration");
    }

    @Override
    public String getTextIndent() {
        return helperGetValue("text-indent");
    }

    @Override
    public String getTextShadow() {
        // TODO need to implement this method
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTextTransform() {
        return helperGetProperty("text-transform");
    }

    @Override
    public String getTop() {
        return helperTryBoth("top");
    }

    @Override
    public String getUnicodeBidi() {
        return helperGetProperty("unicode-bidi");
    }

    @Override
    public String getVerticalAlign() {
        return helperGetProperty("vertical-align");
    }

    @Override
    public String getVisibility() {
        return helperGetProperty("visibility");
    }

    @Override
    public String getVoiceFamily() {
        return helperTryBoth("voice-family");
    }

    @Override
    public String getVolume() {
        return helperTryBoth("volume");
    }

    @Override
    public String getWhiteSpace() {
        return helperGetProperty("white-space");
    }

    @Override
    public String getWidows() {
        return helperGetValue("widows");
    }

    @Override
    public String getWidth() {
        return helperGetValue("width");
    }

    @Override
    public String getWordSpacing() {
        return helperTryBoth("word-spacing");
    }

    @Override
    public String getZIndex() {
        // TODO
        // refer to issue #77
        // According to the specs ZIndex value has to be integer but
        // jStyle Parser returns an float.
        // until then this is just a temp hack.
        final String zIndex = helperGetValue("z-index");
        float fZIndex = 0.0f;
        if (zIndex != null) {
            try {
                fZIndex = Float.parseFloat(zIndex);
            }
            catch (final NumberFormatException err) {
                err.printStackTrace();
            }
        }
        final int iZIndex = (int) fZIndex;
        return "" + iZIndex;
    }

    public String getOverlayColor() {
        return this.overlayColor;
    }

    public void setOverlayColor(final String value) {
        this.overlayColor = value;
        this.context.informLookInvalid();
    }

    // TODO references to this in internal code can use a more specific method.
    //      (we can implement specific methods like we have for other properties)
    public String getPropertyValue(final String string) {
        return helperGetProperty(string);
    }

    public String getFloat() {
        return helperGetProperty("float");
    }

    abstract protected NodeData getNodeData();

    private String helperGetValue(final String propertyName) {
        final NodeData nodeData = getNodeData();
        if (nodeData != null) {
            final Term<?> value = nodeData.getValue(propertyName, true);
            // The trim() is a temporary work around for #154
            return value == null ? null : value.toString().trim();
        }
        else {
            return nullIfAbsent ? null : "";
        }
    }

    private String helperGetProperty(final String propertyName) {
        final NodeData nodeData = getNodeData();
        if (nodeData != null) {
            final CSSProperty property = nodeData.getProperty(propertyName, true);
            // final CSSProperty property = nodeData.getProperty(propertyName);
            return property == null ? null : property.toString();
        }
        else {
            return nullIfAbsent ? null : "";
        }
    }

    @HideFromJS
    public String helperTryBoth(final String propertyName) {
        // These two implementations were deprecated after the changes in https://github.com/radkovo/jStyleParser/issues/50

    /* Original
    final String value = helperGetValue(propertyName);
    return value == null ? helperGetProperty(propertyName) : value;
    */

    /* Corrected (equivalent to below implementation, but less optimal)
    final String property = helperGetProperty(propertyName);
    return property == null || property.isEmpty() ? helperGetValue(propertyName) : property;
    */

        final NodeData nodeData = getNodeData();
        if (nodeData == null) {
            return null;
        }
        return nodeData.getAsString(propertyName, true);
    }
}
