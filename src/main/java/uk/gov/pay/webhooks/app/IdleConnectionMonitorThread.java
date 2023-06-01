package uk.gov.pay.webhooks.app;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.concurrent.TimeUnit;

public class IdleConnectionMonitorThread extends Thread {
    private final HttpClientConnectionManager connMgr;
    private volatile boolean shutdown;
    private final long idleConnectionTimeToLive;

    public IdleConnectionMonitorThread(
            PoolingHttpClientConnectionManager connMgr, long idleConnectionTimeToLive) {
        super();
        this.connMgr = connMgr;
        this.idleConnectionTimeToLive = idleConnectionTimeToLive;
    }
    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(1000);
                    connMgr.closeExpiredConnections();
                    connMgr.closeIdleConnections(idleConnectionTimeToLive, TimeUnit.SECONDS);
                }
            }
        } catch (InterruptedException ex) {
            shutdown();
        }
    }
    public void shutdown() {
        shutdown = true;
        synchronized (this) {
            notifyAll();
        }
    }
}
