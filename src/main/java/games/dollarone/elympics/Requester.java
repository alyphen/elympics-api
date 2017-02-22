package games.dollarone.elympics;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.IOUtils;

import javax.annotation.WillClose;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static games.dollarone.elympics.Elympics.MAPPER;
import static java.util.Arrays.asList;
import static java.util.logging.Level.FINE;

/**
 * A builder pattern for making HTTP call and parsing its output.
 */
public class Requester {

    private static final List<String> METHODS_WITHOUT_BODY = asList("GET", "DELETE");
    private static final Logger LOGGER = Logger.getLogger(Requester.class.getName());

    private final Elympics root;
    private final List<Entry> args = new ArrayList<>();
    private final Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Request method.
     */
    private String method = "POST";
    private String contentType = "application/x-www-form-urlencoded";
    private InputStream body;

    /**
     * Current connection.
     */
    private HttpURLConnection connection;
    private boolean forceBody;

    private static class Entry {
        String key;
        Object value;

        private Entry(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    Requester(Elympics root) {
        this.root = root;
    }

    /**
     * Sets the request HTTP header.
     *
     * If a header of the same name is already set, this method overrides it.
     *
     * @param name The name of the header
     * @param value The value of the header
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public Requester withHeader(String name, String value) {
        setHeader(name, value);
        return this;
    }

    Requester withPreview(String name) {
        return withHeader("Accept", name);
    }

    public Requester with(String key, int value) {
        return _with(key, value);
    }

    public Requester with(String key, Integer value) {
        if (value != null) {
            _with(key, value);
        }
        return this;
    }

    public Requester with(String key, BigInteger value) {
        if (value != null) {
            _with(key, value);
        }
        return this;
    }

    public Requester with(String key, boolean value) {
        return _with(key, value);
    }
    public Requester with(String key, Boolean value) {
        return _with(key, value);
    }

    public Requester with(String key, Enum e) {
        if (e==null)    return _with(key, null);

        return with(key, e.toString().toLowerCase(Locale.ENGLISH).replace('_', '-'));
    }

    public Requester with(String key, String value) {
        return _with(key, value);
    }

    public Requester with(String key, Collection<String> value) {
        return _with(key, value);
    }

    public Requester with(String key, Map<String, String> value) {
        return _with(key, value);
    }

    public Requester with(@WillClose/*later*/ InputStream body) {
        this.body = body;
        return this;
    }

    public Requester _with(String key, Object value) {
        if (value!=null) {
            args.add(new Entry(key,value));
        }
        return this;
    }

    /**
     * Unlike {@link #with(String, String)}, overrides the existing value
     */
    public Requester set(String key, Object value) {
        for (Entry e : args) {
            if (e.key.equals(key)) {
                e.value = value;
                return this;
            }
        }
        return _with(key,value);
    }

    public Requester method(String method) {
        this.method = method;
        return this;
    }

    public Requester contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * For APIs which use HTTP methods somewhat inconsistently, and use a body where it's not expected.
     * Normally whether parameters go as query parameters or a body depends on the HTTP verb in use,
     * but this method forces the parameters to be sent as a body.
     */
    /*package*/ Requester inBody() {
        forceBody = true;
        return this;
    }

    public void to(String tailApiUrl) throws IOException {
        to(tailApiUrl,null);
    }

    /**
     * Sends a request to the specified URL, and parses the response into the given type via databinding.
     *
     * @throws IOException
     *      if the server returns 4xx/5xx responses.
     * @return
     *      {@link Reader} that reads the response.
     */
    public <T> T to(String tailApiUrl, Class<T> type) throws IOException {
        return _to(tailApiUrl, type, null);
    }

    /**
     * Like {@link #to(String, Class)} but updates an existing object instead of creating a new instance.
     */
    public <T> T to(String tailApiUrl, T existingInstance) throws IOException {
        return _to(tailApiUrl, null, existingInstance);
    }

    private <T> T _to(String tailApiUrl, Class<T> type, T instance) throws IOException {
        if (!isMethodWithBody() && !args.isEmpty()) {
            boolean questionMarkFound = tailApiUrl.indexOf('?') != -1;
            tailApiUrl += questionMarkFound ? '&' : '?';
            for (Iterator<Entry> it = args.listIterator(); it.hasNext();) {
                Entry arg = it.next();
                tailApiUrl += arg.key + '=' + URLEncoder.encode(arg.value.toString(),"UTF-8");
                if (it.hasNext()) {
                    tailApiUrl += '&';
                }
            }
        }

        while (true) {// loop while API rate limit is hit
            setupConnection(root.getApiURL(tailApiUrl));

            buildRequest();

            try {
                T result = parse(type, instance);
                if (type != null && type.isArray()) { // we might have to loop for pagination - done through recursion
                    final String links = connection.getHeaderField("link");
                    if (links != null && links.contains("rel=\"next\"")) {
                        Pattern nextLinkPattern = Pattern.compile(".*<(.*)>; rel=\"next\"");
                        Matcher nextLinkMatcher = nextLinkPattern.matcher(links);
                        if (nextLinkMatcher.find()) {
                            final String link = nextLinkMatcher.group(1);
                            T nextResult = _to(link, type, instance);

                            final int resultLength = Array.getLength(result);
                            final int nextResultLength = Array.getLength(nextResult);
                            T concatResult = (T) Array.newInstance(type.getComponentType(), resultLength + nextResultLength);
                            System.arraycopy(result, 0, concatResult, 0, resultLength);
                            System.arraycopy(nextResult, 0, concatResult, resultLength, nextResultLength);
                            result = concatResult;
                        }
                    }
                }
                return result;
            } catch (IOException e) {
                handleApiError(e);
            }
        }
    }

    /**
     * Makes a request and just obtains the HTTP status code.
     */
    public int asHttpStatusCode(String tailApiUrl) throws IOException {
        while (true) {// loop while API rate limit is hit
            method("GET");
            setupConnection(root.getApiURL(tailApiUrl));

            buildRequest();

            try {
                return connection.getResponseCode();
            } catch (IOException e) {
                handleApiError(e);
            }
        }
    }

    public InputStream asStream(String tailApiUrl) throws IOException {
        while (true) {// loop while API rate limit is hit
            setupConnection(root.getApiURL(tailApiUrl));

            buildRequest();

            try {
                return wrapStream(connection.getInputStream());
            } catch (IOException e) {
                handleApiError(e);
            }
        }
    }

    public String getResponseHeader(String header) {
        return connection.getHeaderField(header);
    }


    /**
     * Set up the request parameters or POST payload.
     */
    private void buildRequest() throws IOException {
        if (isMethodWithBody()) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-type", contentType);

            if (body == null) {
                StringBuilder requestBuilder = new StringBuilder();
                for (Entry e : args) {
                    requestBuilder.append(e.key).append('=').append(e.value).append('&');
                }
                requestBuilder.deleteCharAt(requestBuilder.length() - 1);
                connection.getOutputStream().write(requestBuilder.toString().getBytes("UTF-8"));
            } else {
                try {
                    byte[] bytes = new byte[32768];
                    int read = 0;
                    while ((read = body.read(bytes)) != -1) {
                        connection.getOutputStream().write(bytes, 0, read);
                    }
                } finally {
                    body.close();
                }
            }
        }
    }

    private boolean isMethodWithBody() {
        return forceBody || !METHODS_WITHOUT_BODY.contains(method);
    }

    private void setupConnection(URL url) throws IOException {
        connection = root.getConnector().connect(url);

        // if the authentication is needed but no credential is given, try it anyway (so that some calls
        // that do work with anonymous access in the reduced form should still work.)
        if (root.key !=null)
            connection.setRequestProperty("key", root.key);

        for (Map.Entry<String, String> e : headers.entrySet()) {
            String v = e.getValue();
            if (v!=null)
                connection.setRequestProperty(e.getKey(), v);
        }

        setRequestMethod(connection);
        connection.setRequestProperty("Accept-Encoding", "gzip");
    }

    private void setRequestMethod(HttpURLConnection connection) throws IOException {
        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException e) {
            // JDK only allows one of the fixed set of verbs. Try to override that
            try {
                Field $method = HttpURLConnection.class.getDeclaredField("method");
                $method.setAccessible(true);
                $method.set(connection,method);
            } catch (Exception x) {
                throw new IOException("Failed to set the custom verb", x);
            }
            // sun.net.www.protocol.https.DelegatingHttpsURLConnection delegates to another HttpURLConnection
            try {
                Field $delegate = connection.getClass().getDeclaredField("delegate");
                $delegate.setAccessible(true);
                Object delegate = $delegate.get(connection);
                if (delegate instanceof HttpURLConnection) {
                    HttpURLConnection nested = (HttpURLConnection) delegate;
                    setRequestMethod(nested);
                }
            } catch (NoSuchFieldException x) {
                // no problem
            } catch (IllegalAccessException x) {
                throw new IOException("Failed to set the custom verb", x);
            }
        }
        if (!connection.getRequestMethod().equals(method))
            throw new IllegalStateException("Failed to set the request method to "+method);
    }

    private <T> T parse(Class<T> type, T instance) throws IOException {
        InputStreamReader r = null;
        int responseCode = -1;
        String responseMessage = null;
        try {
            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();
            if (responseCode == 304) {
                return null;    // special case handling for 304 unmodified, as the content will be ""
            }
            if (responseCode == 204 && type!=null && type.isArray()) {
                // no content
                return type.cast(Array.newInstance(type.getComponentType(),0));
            }

            r = new InputStreamReader(wrapStream(connection.getInputStream()), "UTF-8");
            String data = IOUtils.toString(r);
            if (type!=null)
                try {
                    return MAPPER.readValue(data,type);
                } catch (JsonMappingException e) {
                    throw new IOException("Failed to deserialize " + data, e);
                }
            if (instance!=null)
                return MAPPER.readerForUpdating(instance).readValue(data);
            return null;
        } catch (FileNotFoundException e) {
            // java.net.URLConnection handles 404 exception has FileNotFoundException, don't wrap exception in HttpException
            // to preserve backward compatibility
            throw e;
        } catch (IOException e) {
            throw new HttpException(responseCode, responseMessage, connection.getURL(), e);
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

    /**
     * Handles the "Content-Encoding" header.
     */
    private InputStream wrapStream(InputStream in) throws IOException {
        String encoding = connection.getContentEncoding();
        if (encoding==null || in==null) return in;
        if (encoding.equals("gzip"))    return new GZIPInputStream(in);

        throw new UnsupportedOperationException("Unexpected Content-Encoding: "+encoding);
    }

    /**
     * Handle API error by either throwing it or by returning normally to retry.
     */
    void handleApiError(IOException e) throws IOException {
        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e2) {
            // likely to be a network exception (e.g. SSLHandshakeException),
            // connection.getResponseCode() and any other getter on the response will cause an exception
            if (LOGGER.isLoggable(FINE))
                LOGGER.log(FINE, "Silently ignore exception retrieving response code for '" + connection.getURL() + "'" +
                        " handling exception " + e, e);
            throw e;
        }
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) // 401 / Unauthorized == bad creds
            throw e;

        // Retry-After is not documented but apparently that field exists
        if (responseCode == HttpURLConnection.HTTP_FORBIDDEN &&
                connection.getHeaderField("Retry-After") != null) {
            return;
        }

        InputStream es = wrapStream(connection.getErrorStream());
        try {
            if (es!=null) {
                String error = IOUtils.toString(es, "UTF-8");
                if (e instanceof FileNotFoundException) {
                    // pass through 404 Not Found to allow the caller to handle it intelligently
                    throw (IOException) new FileNotFoundException(error).initCause(e);
                } else if (e instanceof HttpException) {
                    HttpException http = (HttpException) e;
                    throw new HttpException(error, http.getResponseCode(), http.getResponseMessage(), http.getUrl(), e);
                } else {
                    throw new IOException(error, e);
                }
            } else
                throw e;
        } finally {
            IOUtils.closeQuietly(es);
        }
    }

}
