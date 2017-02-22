package games.dollarone.elympics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * {@link HttpConnector} wrapper that sets timeout
 */
public class ImpatientHttpConnector implements HttpConnector {

    /**
     * Default connection timeout in milliseconds
     */
    public static final int CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    /**
     * Default read timeout in milliseconds
     */
    public static final int READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    private final HttpConnector base;
    private final int readTimeout;
    private final int connectTimeout;

    /**
     * {@link ImpatientHttpConnector} constructor, taking base {@link HttpConnector}, and both connection and read
     * timeouts.
     *
     * @param base The base {@link HttpConnector} to use
     * @param connectTimeout HTTP connection timeout in milliseconds
     * @param readTimeout HTTP read timeout in milliseconds
     */
    public ImpatientHttpConnector(HttpConnector base, int connectTimeout, int readTimeout) {
        this.base = base;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * {@link ImpatientHttpConnector} constructor, taking base {@link HttpConnector}, and a timeout.
     *
     * @param base The base {@link HttpConnector} to use
     * @param timeout HTTP timeout in milliseconds
     */
    public ImpatientHttpConnector(HttpConnector base, int timeout) {
        this(base, timeout, timeout);
    }

    /**
     * {@link ImpatientHttpConnector} constructor, taking only base {@link HttpConnector}. Default timeouts are used.
     *
     * @param base The base {@link HttpConnector} to use
     */
    public ImpatientHttpConnector(HttpConnector base) {
        this(base, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Override
    public HttpURLConnection connect(URL url) throws IOException {
        HttpURLConnection connection = base.connect(url);
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection;
    }

}
