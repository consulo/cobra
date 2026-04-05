package org.cobraparser.css;

import cz.vutbr.web.css.StyleSheet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2024-11-18
 */
public class DefaultCssFactoryImpl extends DefaultCssFactory {
    private volatile StyleSheet standardCSS;
    private volatile StyleSheet userCSS;

    public DefaultCssFactoryImpl() {
        // Load theme variables first so they are available when processing other CSS
        loadThemeVariables();
        buildStyleSheets();

        // Rebuild when L&F changes so --swing-* variables resolve to new UIManager colors
        CSSVariableResolver.INSTANCE.addChangeListener(this::onLafChange);
    }

    private void loadThemeVariables() {
        String themeText = readResource("/org/cobraparser/css/theme.css");
        CSSVariableResolver.INSTANCE.loadFromCssText(themeText);
    }

    private void buildStyleSheets() {
        standardCSS = readAndParseResource("/org/cobraparser/css/standard.css");
        userCSS = readAndParseResource("/org/cobraparser/css/user.css");
    }

    private void onLafChange() {
        // Re-resolve variables (--swing-* hex values may have changed for unmapped keys)
        loadThemeVariables();
        buildStyleSheets();
    }

    private StyleSheet readAndParseResource(String path) {
        String raw = readResource(path);
        String processed = CSSVariableResolver.INSTANCE.resolve(raw);
        return parseStyle(processed);
    }

    private String readResource(String path) {
        try {
            URL resource = getClass().getResource(path);
            Objects.requireNonNull(resource, path);
            return Files.readString(Path.of(resource.toURI()));
        }
        catch (IOException | URISyntaxException e) {
            throw new RuntimeException(path, e);
        }
    }

    @Override
    public StyleSheet getStandardCSS(boolean xhtml) {
        return standardCSS;
    }

    @Override
    public StyleSheet getUserCSS(boolean xhtml) {
        return userCSS;
    }
}
