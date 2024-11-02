/**
 * @author VISTALL
 * @since 2024-11-02
 */
module cobra.rhino.js {
    requires cobra.core;

    requires java.desktop;
    requires jdk.xml.dom;

    requires slf4j.api;

    requires rhino;

    provides org.cobraparser.js.JavaScriptEngine with org.cobraparser.js.rhinojs.RhinoJavaScriptEngine;
}