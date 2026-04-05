package org.cobraparser.css;

import cz.vutbr.web.domassign.SingleMapNodeData;

import javax.swing.UIManager;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves CSS custom properties (var()) including built-in dynamic variables
 * for Swing/UIManager integration and L&F state.
 *
 * Built-in variables:
 * - {@code --laf-dark}  — "true" when the current L&F has a dark background, "false" otherwise
 * - {@code --swing-panel-background}, {@code --swing-label-foreground}, etc. — map to UIManager colors
 *
 * @author VISTALL
 */
public class CSSVariableResolver {
    // --swing-<name> → UIManager key
    private static final Map<String, String> SWING_UI_KEYS;

    static {
        Map<String, String> swingKeys = new LinkedHashMap<>();
        swingKeys.put("--swing-panel-background", "Panel.background");
        swingKeys.put("--swing-label-foreground", "Label.foreground");
        swingKeys.put("--swing-text-background", "TextField.background");
        swingKeys.put("--swing-text-foreground", "TextField.foreground");
        swingKeys.put("--swing-button-background", "Button.background");
        swingKeys.put("--swing-button-foreground", "Button.foreground");
        swingKeys.put("--swing-selection-background", "TextArea.selectionBackground");
        swingKeys.put("--swing-selection-foreground", "TextArea.selectionForeground");
        swingKeys.put("--swing-separator-foreground", "Separator.foreground");
        swingKeys.put("--swing-disabled-foreground", "Label.disabledForeground");
        swingKeys.put("--swing-link-color", "Component.linkColor");
        SWING_UI_KEYS = Collections.unmodifiableMap(swingKeys);
    }

    private static final Pattern CUSTOM_PROP_PATTERN =
            Pattern.compile("--([\\w-]+)\\s*:\\s*([^;}{\\n]+)");
    private static final Pattern VAR_PATTERN =
            Pattern.compile("var\\(\\s*(--[\\w-]+)(?:\\s*,\\s*([^)]+))?\\s*\\)");

    // INSTANCE must be declared after all static patterns/maps so they are initialized first
    public static final CSSVariableResolver INSTANCE = new CSSVariableResolver();

    // User-defined variables loaded from theme CSS
    private final Map<String, String> variables = new LinkedHashMap<>();
    private final java.util.List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private CSSVariableResolver() {
        loadBuiltinTheme();
        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("lookAndFeel".equals(evt.getPropertyName())) {
                    onLafChange();
                }
            }
        });
        // Register this as the AST-level var() resolver for jStyleParser's SingleMapNodeData
        SingleMapNodeData.setVarResolver(this::resolveVar);
    }

    /**
     * Public entry point for AST-based var() resolution called from jStyleParser.
     */
    public String resolveVar(String varName, String fallback) {
        return resolveVarName(varName, fallback, 0);
    }

    private void loadBuiltinTheme() {
        try (java.io.InputStream in = CSSVariableResolver.class.getResourceAsStream("/org/cobraparser/css/theme.css")) {
            if (in != null) {
                loadFromCssText(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException ignored) {
        }
    }

    /**
     * Returns true if the current Swing L&F has a dark background.
     * Uses FlatLaf's "laf.dark" UIManager property when available.
     */
    public static boolean isLafDark() {
        Object dark = UIManager.get("laf.dark");
        if (dark instanceof Boolean) return (Boolean) dark;
        // Fallback: luminance check on panel background
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) return false;
        double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return lum < 0.5;
    }

    /**
     * Parses {@code --name: value} declarations from a CSS text and stores them as variables.
     * Existing variables are replaced.
     */
    public synchronized void loadFromCssText(String cssText) {
        variables.clear();
        Matcher m = CUSTOM_PROP_PATTERN.matcher(cssText);
        while (m.find()) {
            String name = "--" + m.group(1).trim();
            String value = m.group(2).trim();
            variables.put(name, value);
        }
    }

    /**
     * Resolves all {@code var(--name)} and {@code var(--name, fallback)} references in the given CSS text.
     */
    public String resolve(String cssText) {
        if (cssText == null || !cssText.contains("var(")) return cssText;
        Matcher m = VAR_PATTERN.matcher(cssText);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String fallback = m.group(2);
            String resolved = resolveVarName(name, fallback, 0);
            m.appendReplacement(sb, Matcher.quoteReplacement(resolved != null ? resolved : ""));
        }
        m.appendTail(sb);
        // Recursively resolve in case of nested vars (limit depth)
        String result = sb.toString();
        if (result.contains("var(") && !result.equals(cssText)) {
            return resolve(result);
        }
        return result;
    }

    /**
     * Resolves a single {@code var(--name)} reference value.
     * Convenience method for resolving a property value string.
     */
    public String resolveValue(String value) {
        return resolve(value);
    }

    private String resolveVarName(String name, String fallback, int depth) {
        if (depth > 10) return fallback != null ? fallback.trim() : null;

        // Built-in: --laf-dark
        if ("--laf-dark".equals(name)) {
            return isLafDark() ? "true" : "false";
        }

        // Built-in: --swing-label-font-size — returns the Label.font size in px
        if ("--swing-label-font-size".equals(name)) {
            java.awt.Font f = UIManager.getFont("Label.font");
            return (f != null ? f.getSize() : 13) + "px";
        }

        // Built-in: --swing-* UIManager color variables — always resolves to hex
        // so jStyleParser can parse the resulting value without issue.
        // StyleSheets are rebuilt on L&F change (see DefaultCssFactoryImpl).
        String uiKey = SWING_UI_KEYS.get(name);
        if (uiKey != null) {
            Color c = UIManager.getColor(uiKey);
            if (c != null) return colorToHex(c);
        }

        // User-defined variables
        String val = variables.get(name);
        if (val != null) {
            // Resolve nested var() references in the variable value
            if (val.contains("var(")) {
                val = resolveNested(val, depth + 1);
            }
            return val;
        }

        return fallback != null ? fallback.trim() : null;
    }

    private String resolveNested(String value, int depth) {
        if (!value.contains("var(")) return value;
        Matcher m = VAR_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String fallback = m.group(2);
            String resolved = resolveVarName(name, fallback, depth);
            m.appendReplacement(sb, Matcher.quoteReplacement(resolved != null ? resolved : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String colorToHex(Color c) {
        if (c.getAlpha() < 255) {
            return String.format("rgba(%d,%d,%d,%.3f)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0f);
        }
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /**
     * Adds a listener that is notified when the L&F changes, so consumers can rebuild their CSS.
     */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void onLafChange() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    /**
     * Returns an unmodifiable view of all registered {@code --swing-*} variable names.
     */
    public static Set<String> getSwingVariableNames() {
        return SWING_UI_KEYS.keySet();
    }
}
