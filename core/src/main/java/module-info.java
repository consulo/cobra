/**
 * @author VISTALL
 * @since 2024-11-02
 */
module cobra.core {
    requires java.desktop;
    requires java.xml;
    requires jdk.xml.dom;

    requires org.slf4j;

    requires static net.sf.cssbox.jstyleparser;
    requires static xml.apis.ext;

    exports org.cobraparser.html;
    exports org.cobraparser.html.parser;
    exports org.cobraparser.html.domimpl;
    exports org.cobraparser.html.gui;
    exports org.cobraparser.html.renderer;
    exports org.cobraparser.ua;
    exports org.cobraparser.util;
    exports org.cobraparser.js;
    exports org.cobraparser.html.js;

    uses org.cobraparser.js.JavaScriptEngine;

    uses org.cobraparser.css.DefaultCssFactory;
    uses org.cobraparser.css.StandardColorProvider;
}