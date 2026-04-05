package org.cobraparser.css;

import cz.vutbr.web.css.StyleSheet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public class DefaultCssFactoryImpl extends DefaultCssFactory {
    // Raw CSS text — var() references are kept as-is and resolved at style-access time
    private final String rawStandardCSS;
    private final String rawUserCSS;

    public DefaultCssFactoryImpl() {
        // Load theme variables so --cobra-* aliases are available for resolution
        loadThemeVariables();
        rawStandardCSS = readResource("/org/cobraparser/css/standard.css");
        rawUserCSS = readResource("/org/cobraparser/css/user.css");

        // Reload theme variables on L&F change (theme.css may use --laf-dark etc.)
        CSSVariableResolver.INSTANCE.addChangeListener(this::onLafChange);
    }

    private void loadThemeVariables() {
        String themeText = readResource("/org/cobraparser/css/theme.css");
        CSSVariableResolver.INSTANCE.loadFromCssText(themeText);
    }

    private void onLafChange() {
        loadThemeVariables();
    }

    private String readResource(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(path, e);
        }
    }

    @Override
    public StyleSheet getStandardCSS(boolean xhtml) {
        // Parse raw CSS — var() references are resolved at style-access time in JStyleProperties
        return parseStyle(rawStandardCSS);
    }

    @Override
    public StyleSheet getUserCSS(boolean xhtml) {
        return parseStyle(rawUserCSS);
    }
}
