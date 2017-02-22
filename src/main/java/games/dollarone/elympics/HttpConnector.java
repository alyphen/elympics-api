package games.dollarone.elympics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Pluggability for customising HTTP request behaviours or using altogether different library.
 *
 * For example, you can implement this to set custom timeouts.
 */
public interface HttpConnector {

    /**
     * Opens a connection to the given URL.
     *
     * @param url The URL
     * @return The connection
     * @throws IOException If the connection fails
     */
    HttpURLConnection connect(URL url) throws IOException;

    /**
     * Default implementation that uses {@link URL#openConnection()}.
     */
    HttpConnector DEFAULT = new ImpatientHttpConnector((HttpConnector) url -> (HttpURLConnection) url.openConnection());

}
