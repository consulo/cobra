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
    private StyleSheet standardCSS;
    private StyleSheet userCSS;

    public DefaultCssFactoryImpl() {
        standardCSS = readFromResource("/org/cobraparser/css/standard.css");
        userCSS = readFromResource("/org/cobraparser/css/user.css");
    }

    private StyleSheet readFromResource(String path) {
        try {
            URL resource = getClass().getResource(path);
            Objects.requireNonNull(resource, path);
            String body = Files.readString(Path.of(resource.toURI()));
            return parseStyle(body);
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
