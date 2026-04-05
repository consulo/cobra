package cobra;

import org.cobraparser.ua.ImageResponse;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.NetworkRequestListener;
import org.cobraparser.ua.UserAgentContext.Request;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

/**
 * Minimal {@link NetworkRequest} implementation for the test harness.
 * Supports asynchronous HTTP GET for image loading.
 */
public class SimpleNetworkRequest implements NetworkRequest {
    private final List<NetworkRequestListener> listeners = new CopyOnWriteArrayList<>();

    private volatile int readyState = STATE_UNINITIALIZED;
    private volatile int status = 0;
    private volatile byte[] responseBytes;
    private volatile URL requestUrl;
    private volatile boolean async;

    @Override
    public int getReadyState() {
        return readyState;
    }

    @Override
    public String getResponseText() {
        byte[] b = responseBytes;
        return b == null ? null : new String(b);
    }

    @Override
    public Document getResponseXML() {
        return null;
    }

    @Override
    public ImageResponse getResponseImage() {
        byte[] b = responseBytes;
        if (b == null) return new ImageResponse(ImageResponse.State.error, null);
        try {
            Image img = ImageIO.read(new ByteArrayInputStream(b));
            if (img == null) return new ImageResponse(ImageResponse.State.error, null);
            return new ImageResponse(ImageResponse.State.loaded, img);
        } catch (IOException e) {
            return new ImageResponse(ImageResponse.State.error, null);
        }
    }

    @Override
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getStatusText() {
        return status == 200 ? "OK" : String.valueOf(status);
    }

    @Override
    public void abort() {
        setReadyState(STATE_ABORTED);
    }

    @Override
    public String getAllResponseHeaders(List<String> excludedHeadersLowerCase) {
        return "";
    }

    @Override
    public String getResponseHeader(String headerName) {
        return null;
    }

    @Override
    public void open(String method, String url) throws IOException {
        open(method, new URL(url), true);
    }

    @Override
    public void open(String method, URL url) throws IOException {
        open(method, url, true);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag) throws IOException {
        this.requestUrl = url;
        this.async = asyncFlag;
        setReadyState(STATE_LOADING);
    }

    @Override
    public void open(String method, String url, boolean asyncFlag) throws IOException {
        open(method, new URL(url), asyncFlag);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName) throws IOException {
        open(method, url, asyncFlag);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName, String password) throws IOException {
        open(method, url, asyncFlag);
    }

    @Override
    public void send(String content, Request requestType) throws IOException {
        if (async) {
            ForkJoinPool.commonPool().execute(this::execute);
        } else {
            execute();
        }
    }

    private void execute() {
        try {
            URL url = requestUrl;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 Cobra/1.0");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.connect();
            status = conn.getResponseCode();
            try (InputStream in = conn.getInputStream()) {
                responseBytes = in.readAllBytes();
            }
        } catch (Exception e) {
            status = 0;
            responseBytes = null;
        }
        setReadyState(STATE_COMPLETE);
    }

    @Override
    public void addNetworkRequestListener(NetworkRequestListener listener) {
        listeners.add(listener);
    }

    @Override
    public Optional<URL> getURL() {
        return Optional.ofNullable(requestUrl);
    }

    @Override
    public boolean isAsnyc() {
        return async;
    }

    @Override
    public void addRequestedHeader(String header, String value) {
        // Not needed for test purposes
    }

    private void setReadyState(int state) {
        this.readyState = state;
        for (NetworkRequestListener l : listeners) {
            l.readyStateChanged(null);
        }
    }
}
