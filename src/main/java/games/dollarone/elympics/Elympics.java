package games.dollarone.elympics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Root of the Elympics API.
 */
public final class Elympics {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setVisibilityChecker(new VisibilityChecker.Std(NONE, NONE, NONE, NONE, ANY));
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    static final String ELYMPICS_URL = "https://dollarone.games/elympics";

    private final String apiUrl;

    final String key;

    private HttpConnector connector = HttpConnector.DEFAULT;

    Elympics(String apiUrl, String key, HttpConnector connector) {
        if (apiUrl.endsWith("/")) apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        this.apiUrl = apiUrl;
        if (connector != null) this.connector = connector;
        this.key = key;
    }

    public static Elympics connect(String key) {
        return new ElympicsBuilder().withKey(key).build();
    }

    /**
     * Sets the custom connector used to make requests to Elympics.
     *
     * @param connector The connector
     */
    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public HttpConnector getConnector() {
        return connector;
    }

    URL getApiURL(String tailApiUrl) throws IOException {
        if (tailApiUrl.startsWith("/")) {
            return new URL(apiUrl + tailApiUrl);
        } else {
            return new URL(tailApiUrl);
        }
    }

    private Requester retrieve() {
        return new Requester(this).method("GET");
    }

    public List<ElympicsHighscore> getHighscores() throws IOException {
        return Arrays.asList(
                new Requester(this)
                        .with("key", key)
                        .method("POST")
                        .to("/getHighscores", ElympicsHighscore[].class)
        );
    }

    public void submitHighscore(String name, BigInteger score) throws IOException {
        new Requester(this)
                .with("key", key)
                .with("name", name)
                .with("score", score)
                .method("POST")
                .to("/submitHighscore");
    }

    public void submitHighscore(ElympicsHighscore highscore) throws IOException {
        submitHighscore(highscore.getName(), highscore.getScore());
    }

    public void submitHighscore(String name, long score) throws IOException {
        submitHighscore(name, BigInteger.valueOf(score));
    }

}
