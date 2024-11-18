package org.cobraparser.css;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.antlr4.CSSParserFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public abstract class DefaultCssFactory {
    public static final DefaultCssFactory INSTANCE = init();

    private static DefaultCssFactory init() {
        ServiceLoader<DefaultCssFactory> loader = ServiceLoader.load(DefaultCssFactory.class, DefaultCssFactory.class.getClassLoader());
        Optional<DefaultCssFactory> first = loader.findFirst();
        if (first.isPresent()) {
            return first.get();
        }

        return new DefaultCssFactoryImpl();
    }

    public abstract StyleSheet getStandardCSS(boolean xhtml);

    public abstract StyleSheet getUserCSS(boolean xhtml);

    protected static StyleSheet parseStyle(final String cssdata) {
        return parseStyle(cssdata, StyleSheet.Origin.AGENT);
    }

    protected static StyleSheet parseStyle(final String cssdata, final StyleSheet.Origin origin) {
        try {
            final StyleSheet newsheet = CSSParserFactory.getInstance().parse(cssdata, null, null, CSSParserFactory.SourceType.EMBEDDED, null);
            newsheet.setOrigin(origin);
            return newsheet;
        }
        catch (IOException | CSSException e) {
            throw new RuntimeException(e);
        }
    }
}
