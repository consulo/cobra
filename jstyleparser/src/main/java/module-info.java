/**
 * jStyleParser — CSS parser library.
 *
 * Original project: https://github.com/radkovo/jStyleParser
 * License: GNU Lesser General Public License 3.0
 */
module net.sf.cssbox.jstyleparser {
    requires org.antlr.antlr4.runtime;
    requires unbescape;
    requires org.slf4j;
    requires java.xml;
    requires java.desktop;
    requires static xml.apis.ext;

    exports cz.vutbr.web.css;
    exports cz.vutbr.web.csskit;
    exports cz.vutbr.web.csskit.antlr4;
    exports cz.vutbr.web.csskit.fn;
    exports cz.vutbr.web.domassign;
    exports cz.vutbr.web.domassign.decode;
    exports org.fit.net;
}
