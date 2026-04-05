package cz.vutbr.web.css;

/**
 * Callback for resolving CSS custom property (var()) references.
 * Defined in jstyleparser so SingleMapNodeData can use it without
 * creating a circular dependency on the core module.
 *
 * @author VISTALL
 * @since 2024-11-18
 */
public interface VarResolver {
    /**
     * Resolves a CSS custom property reference.
     *
     * @param varName  the variable name, e.g. {@code "--cobra-fg"}
     * @param fallback CSS fallback value declared inside the var() call, or {@code null}
     * @return resolved string value, or {@code null} if unresolvable
     */
    String resolve(String varName, String fallback);
}
