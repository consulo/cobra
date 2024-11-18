package cobra;

import cz.vutbr.web.domassign.SupportedCSS3;
import org.cobraparser.html.AbstractHtmlRendererContext;
import org.cobraparser.html.FormInput;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.gui.HtmlPanel;
import org.cobraparser.html.parser.DocumentBuilderImpl;
import org.cobraparser.html.parser.InputSourceImpl;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.html.HTMLElement;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Policy;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
public class Main {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        URL resource = Main.class.getResource("/cobra/SomeHtml.html");

        byte[] bytes = resource.openStream().readAllBytes();
        String str = new String(bytes, StandardCharsets.UTF_8);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        frame.setLocationByPlatform(true);

        JPanel panel = new JPanel(new BorderLayout());
        frame.add(panel);

        JButton r = new JButton("Render");
        r.addActionListener(e -> {
            HtmlPanel htmlPanel = new HtmlPanel();

            AbstractHtmlRendererContext rcontext = new AbstractHtmlRendererContext() {
                @Override
                public UserAgentContext getUserAgentContext() {
                    return new UserAgentContext() {
                        @Override
                        public boolean isRequestPermitted(Request request) {
                            return false;
                        }

                        @Override
                        public NetworkRequest createHttpRequest() {
                            return null;
                        }

                        @Override
                        public String getAppCodeName() {
                            return "consulo";
                        }

                        @Override
                        public String getAppName() {
                            return "consulo";
                        }

                        @Override
                        public String getAppVersion() {
                            return "1.0";
                        }

                        @Override
                        public String getAppMinorVersion() {
                            return "1.0";
                        }

                        @Override
                        public String getBrowserLanguage() {
                            return "en";
                        }

                        @Override
                        public boolean isCookieEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isScriptingEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isExternalCSSEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isInternalCSSEnabled() {
                            return false;
                        }

                        @Override
                        public String getPlatform() {
                            return null;
                        }

                        @Override
                        public String getUserAgent() {
                            return "Consulo";
                        }

                        @Override
                        public String getCookie(URL url) {
                            return null;
                        }

                        @Override
                        public void setCookie(URL url, String cookieSpec) {

                        }

                        @Override
                        public Policy getSecurityPolicy() {
                            return null;
                        }

                        @Override
                        public int getScriptingOptimizationLevel() {
                            return 0;
                        }

                        @Override
                        public boolean isMedia(String mediaName) {
                            return false;
                        }

                        @Override
                        public String getVendor() {
                            return "consulo";
                        }

                        @Override
                        public String getProduct() {
                            return "Consulo";
                        }
                    };
                }

                @Override
                public void navigate(URL url, String target) {
                    try {
                        Desktop.getDesktop().browse(url.toURI());
                    }
                    catch (IOException | URISyntaxException ignored) {
                    }
                }

                @Override
                public void linkClicked(HTMLElement linkNode, URL url, String target) {
                    try {
                        Desktop.getDesktop().browse(url.toURI());
                    }
                    catch (IOException | URISyntaxException ignored) {
                    }
                }

                @Override
                public void submitForm(String method, URL action, String target, String enctype, FormInput[] formInputs) {

                }

                @Override
                public boolean onMiddleClick(HTMLElement element, MouseEvent event) {
                    return false;
                }

                @Override
                public void setCursor(Optional<Cursor> cursorOpt) {
                    cursorOpt.ifPresentOrElse(htmlPanel::setCursor, () -> htmlPanel.setCursor(Cursor.getDefaultCursor()));
                }

                @Override
                public void jobsFinished() {

                }

                @Override
                public void setJobFinishedHandler(Runnable runnable) {
                    runnable.run();
                }
            };

            ForkJoinPool.commonPool().execute(() -> {
                try {
                    final DocumentBuilderImpl builder = new DocumentBuilderImpl(rcontext.getUserAgentContext(), rcontext);
                    try (
                        final Reader reader = new StringReader(str)) {
                        final InputSourceImpl is = new InputSourceImpl(reader, resource.toString());

                        final HTMLDocumentImpl document = (HTMLDocumentImpl) builder.parse(is);

                        document.finishModifications();

                        htmlPanel.setDocument(document, rcontext);
                    }
                }
                catch (final IOException | SAXException ioe) {
                    throw new IllegalStateException("Unexpected condition.", ioe);
                }
            });

            panel.add(htmlPanel);
        });

        panel.add(r, BorderLayout.NORTH);

        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
        });
    }
}
