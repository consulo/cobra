/**
 * @author VISTALL
 * @since 2024-11-02
 */
module consulo.internal.cobra {
    requires java.desktop;
    requires java.xml;
    requires jdk.xml.dom;

    requires slf4j.api;

    requires static net.sf.cssbox.jstyleparser;
    requires static sac;
    requires static rhino;

    exports org.cobraparser.html;
    exports org.cobraparser.html.parser;
    exports org.cobraparser.html.domimpl;
    exports org.cobraparser.html.gui;
}