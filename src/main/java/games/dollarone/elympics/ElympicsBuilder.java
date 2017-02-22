package games.dollarone.elympics;

import java.net.HttpURLConnection;
import java.net.Proxy;

/**
 * Configures connection details and produces {@link Elympics}
 */
public final class ElympicsBuilder {

    private String endpoint = Elympics.ELYMPICS_URL;
    private String key;

    private HttpConnector connector;

    public ElympicsBuilder() {

    }

    public ElympicsBuilder withEndpoint(final String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ElympicsBuilder withKey(final String key) {
        this.key = key;
        return this;
    }

    public ElympicsBuilder withConnector(final HttpConnector connector) {
        this.connector = connector;
        return this;
    }

    /**
     * Configures {@linkplain #withConnector(HttpConnector) connector} that uses HTTP library in JRE but use a specific
     * proxy, instead of the system default one.
     * @param proxy The proxy
     * @return The builder
     */
    public ElympicsBuilder withProxy(final Proxy proxy) {
        return withConnector(new ImpatientHttpConnector(url -> (HttpURLConnection) url.openConnection(proxy)));
    }

    public Elympics build() {
        return new Elympics(endpoint, key, connector);
    }

}
