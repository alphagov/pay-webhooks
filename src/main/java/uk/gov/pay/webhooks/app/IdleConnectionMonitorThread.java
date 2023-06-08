package uk.gov.pay.webhooks.app;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.queue.EventQueue;

import java.util.concurrent.TimeUnit;

public class IdleConnectionMonitorThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleConnectionMonitorThread.class);
    private final HttpClientConnectionManager connMgr;
    private volatile boolean shutdown;
    private final long idleConnectionTimeToLive;
    private long connectionPoolMonitorInterval;

    public IdleConnectionMonitorThread(
            PoolingHttpClientConnectionManager connMgr, long idleConnectionTimeToLive,
            long connectionPoolMonitorInterval) {
        super();
        this.connMgr = connMgr;
        this.idleConnectionTimeToLive = idleConnectionTimeToLive;
        this.connectionPoolMonitorInterval = connectionPoolMonitorInterval;
    }
    @Override
    public void run() {
        try {
            while (!shutdown) {
                synchronized (this) {
                    wait(connectionPoolMonitorInterval);
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
