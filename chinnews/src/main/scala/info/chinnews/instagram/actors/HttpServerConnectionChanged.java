package info.chinnews.instagram.actors;

import org.apache.http.impl.AbstractHttpClientConnection;

import java.io.IOException;

/**
 * Created by Tsarevskiy
 */
public class HttpServerConnectionChanged extends AbstractHttpClientConnection {

    public HttpServerConnectionChanged() {
        super();
    }

    /**
     * Asserts if the connection is open.
     *
     * @throws IllegalStateException if the connection is not open.
     */
    protected void assertOpen() throws IllegalStateException {
    }

    @Override
    public void shutdown() throws IOException {

    }

    public void close() throws IOException {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void setSocketTimeout(int timeout) {

    }

    @Override
    public int getSocketTimeout() {
        return 0;
    }
}
