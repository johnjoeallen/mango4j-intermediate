package ie.bitstep.mango.spring.crypto.caas.delegate.net;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.pool.PoolStats;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.INFO;

public class ConnectionPoolMonitor {

	private System.Logger logger = System.getLogger(ConnectionPoolMonitor.class.getName());

	private final String poolName;
	private final PoolingHttpClientConnectionManager poolingConnectionManager;
	private final Duration period;

	private ScheduledExecutorService executorService;

	public ConnectionPoolMonitor(String poolName, PoolingHttpClientConnectionManager poolingConnectionManager, Duration period) {
		this.poolName = poolName;
		this.poolingConnectionManager = poolingConnectionManager;
		this.period = period;
	}

	public void start() {
		executorService = createExecutor();
		executorService.scheduleAtFixedRate(poolStatsLoggerTask(), period.getSeconds(), period.getSeconds(), TimeUnit.SECONDS);
	}

	private Runnable poolStatsLoggerTask() {
		return () -> {
			StringBuilder builder = new StringBuilder()
					.append(poolName)
					.append(":");
			appendPoolStats(builder, "Total Pool Stats", poolingConnectionManager.getTotalStats());
			for (HttpRoute route : poolingConnectionManager.getRoutes()) {
				appendPoolStats(builder, "Route Pool Stats (" + route + ")", poolingConnectionManager.getStats(route));
			}
			logger.log(INFO, builder.toString());
		};
	}

	public void close() {
		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}
	}

	private void appendPoolStats(StringBuilder builder, String statsTitle, PoolStats poolStats) {
		builder.append(" ")
				.append(statsTitle)
				.append(" - Available: ")
				.append(poolStats.getAvailable())
				.append(", Leased: ")
				.append(poolStats.getLeased())
				.append(", Pending: ")
				.append(poolStats.getPending())
				.append(", Max: ")
				.append(poolStats.getMax())
				.append(".");
	}

	protected ScheduledExecutorService createExecutor() {
		return Executors.newSingleThreadScheduledExecutor();
	}
}
