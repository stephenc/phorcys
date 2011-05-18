package com.github.stephenc.phorcys;

import me.prettyprint.cassandra.connection.LoadBalancingPolicy;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ExhaustedPolicy;
import me.prettyprint.hector.api.ClockResolution;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Random;

public class CassandraSessionIdManager extends AbstractSessionIdManager {

    private Server server;
    private String clusterName;
    private String keyspaceName;
    private String columnFamilyName;
    private final CassandraHostConfigurator configurator = new CassandraHostConfigurator();
    private transient Cluster cluster;
    private transient Keyspace keyspace;

    public CassandraSessionIdManager(Server server) {
        super();
        this.server = server;
    }

    public CassandraSessionIdManager(Server server, Random random) {
        super(random);
        this.server = server;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    public void setColumnFamilyName(String columnFamilyName) {
        this.columnFamilyName = columnFamilyName;
    }

    Cluster getCluster() {
        return cluster;
    }

    Keyspace getKeyspace() {
        return keyspace;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        cluster = HFactory.getOrCreateCluster(clusterName, configurator);
        keyspace = HFactory.createKeyspace(keyspaceName, cluster);
    }

    @Override
    protected void doStop() throws Exception {
        cluster.getConnectionManager().shutdown();
        super.doStop();
    }

    /**
     * {@inheritDoc}
     */
    public boolean idInUse(String id) {
        if (id == null) {
            return false;
        }
        ColumnQuery<String, String, String> columnQuery =
                HFactory.createStringColumnQuery(keyspace);
        columnQuery.setColumnFamily(columnFamilyName).setKey(id).setName("inUse");
        QueryResult<HColumn<String, String>> result = columnQuery.execute();
        return result.get().getValue() != null;
    }

    public void addSession(HttpSession session) {
        if (session == null) {
            return;
        }
    }

    public void removeSession(HttpSession session) {
        if (session == null) {
            return;
        }
    }

    public void invalidateAll(String id) {
        removeSession(id);
        synchronized (_sessionIds) {
            //tell all contexts that may have a session object with this id to
            //get rid of them
            Handler[] contexts = server.getChildHandlersByClass(ContextHandler.class);
            for (int i = 0; contexts != null && i < contexts.length; i++) {
                SessionHandler sessionHandler =
                        ((ContextHandler) contexts[i]).getChildHandlerByClass(SessionHandler.class);
                if (sessionHandler != null) {
                    SessionManager manager = sessionHandler.getSessionManager();

                    if (manager != null && manager instanceof CassandraSessionManager) {
                        ((CassandraSessionManager) manager).invalidateSession(id);
                    }
                }
            }
        }
    }

    public String getClusterId(String nodeId) {
        int dot = nodeId.lastIndexOf('.');
        return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
    }

    public String getNodeId(String clusterId, HttpServletRequest request) {
        String worker = request == null ? null : (String) request.getAttribute("org.eclipse.http.ajp.JVMRoute");
        if (worker != null) {
            return clusterId + '.' + worker;
        }

        if (_workerName != null) {
            return clusterId + '.' + _workerName;
        }

        return clusterId;
    }

    // delegate all the setters and getters to allow configuring the Cassandra connection.

    public void setClockResolution(String resolutionString) {
        configurator.setClockResolution(resolutionString);
    }

    public boolean getLifo() {
        return configurator.getLifo();
    }

    public void setLifo(boolean lifo) {
        configurator.setLifo(lifo);
    }

    public long getMinEvictableIdleTimeMillis() {
        return configurator.getMinEvictableIdleTimeMillis();
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        configurator.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        configurator.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return configurator.getTimeBetweenEvictionRunsMillis();
    }

    public int getPort() {
        return configurator.getPort();
    }

    public void setPort(int port) {
        configurator.setPort(port);
    }

    public void setUseThriftFramedTransport(boolean useThriftFramedTransport) {
        configurator.setUseThriftFramedTransport(useThriftFramedTransport);
    }

    public ClockResolution getClockResolution() {
        return configurator.getClockResolution();
    }

    public void setClockResolution(ClockResolution clockResolution) {
        configurator.setClockResolution(clockResolution);
    }

    public boolean getAutoDiscoverHosts() {
        return configurator.getAutoDiscoverHosts();
    }

    public void setAutoDiscoverHosts(boolean autoDiscoverHosts) {
        configurator.setAutoDiscoverHosts(autoDiscoverHosts);
    }

    public int getAutoDiscoveryDelayInSeconds() {
        return configurator.getAutoDiscoveryDelayInSeconds();
    }

    public void setAutoDiscoveryDelayInSeconds(int autoDiscoveryDelayInSeconds) {
        configurator.setAutoDiscoveryDelayInSeconds(autoDiscoveryDelayInSeconds);
    }

    public LoadBalancingPolicy getLoadBalancingPolicy() {
        return configurator.getLoadBalancingPolicy();
    }

    public void setLoadBalancingPolicy(LoadBalancingPolicy loadBalancingPolicy) {
        configurator.setLoadBalancingPolicy(loadBalancingPolicy);
    }

    public int getHostTimeoutCounter() {
        return configurator.getHostTimeoutCounter();
    }

    public void setHostTimeoutCounter(int hostTimeoutCounter) {
        configurator.setHostTimeoutCounter(hostTimeoutCounter);
    }

    public int getHostTimeoutWindow() {
        return configurator.getHostTimeoutWindow();
    }

    public void setHostTimeoutWindow(int hostTimeoutWindow) {
        configurator.setHostTimeoutWindow(hostTimeoutWindow);
    }

    public int getHostTimeoutSuspensionDurationInSeconds() {
        return configurator.getHostTimeoutSuspensionDurationInSeconds();
    }

    public void setHostTimeoutSuspensionDurationInSeconds(int hostTimeoutSuspensionDurationInSeconds) {
        configurator.setHostTimeoutSuspensionDurationInSeconds(hostTimeoutSuspensionDurationInSeconds);
    }

    public int getHostTimeoutUnsuspendCheckDelay() {
        return configurator.getHostTimeoutUnsuspendCheckDelay();
    }

    public void setHostTimeoutUnsuspendCheckDelay(int hostTimeoutUnsuspendCheckDelay) {
        configurator.setHostTimeoutUnsuspendCheckDelay(hostTimeoutUnsuspendCheckDelay);
    }

    public boolean getUseHostTimeoutTracker() {
        return configurator.getUseHostTimeoutTracker();
    }

    public void setUseHostTimeoutTracker(boolean useHostTimeoutTracker) {
        configurator.setUseHostTimeoutTracker(useHostTimeoutTracker);
    }

    public boolean getRunAutoDiscoveryAtStartup() {
        return configurator.getRunAutoDiscoveryAtStartup();
    }

    public int getRetryDownedHostsDelayInSeconds() {
        return configurator.getRetryDownedHostsDelayInSeconds();
    }

    public void setRetryDownedHostsDelayInSeconds(int retryDownedHostsDelayInSeconds) {
        configurator.setRetryDownedHostsDelayInSeconds(retryDownedHostsDelayInSeconds);
    }

    public int getRetryDownedHostsQueueSize() {
        return configurator.getRetryDownedHostsQueueSize();
    }

    public void setRetryDownedHostsQueueSize(int retryDownedHostsQueueSize) {
        configurator.setRetryDownedHostsQueueSize(retryDownedHostsQueueSize);
    }

    public void setRetryDownedHosts(boolean retryDownedHosts) {
        configurator.setRetryDownedHosts(retryDownedHosts);
    }

    public boolean getRetryDownedHosts() {
        return configurator.getRetryDownedHosts();
    }

    public void setExhaustedPolicy(ExhaustedPolicy exhaustedPolicy) {
        configurator.setExhaustedPolicy(exhaustedPolicy);
    }

    public void setCassandraThriftSocketTimeout(int cassandraThriftSocketTimeout) {
        configurator.setCassandraThriftSocketTimeout(cassandraThriftSocketTimeout);
    }

    public void setMaxWaitTimeWhenExhausted(long maxWaitTimeWhenExhausted) {
        configurator.setMaxWaitTimeWhenExhausted(maxWaitTimeWhenExhausted);
    }

    public void setMaxIdle(int maxIdle) {
        configurator.setMaxIdle(maxIdle);
    }

    public void setMaxActive(int maxActive) {
        configurator.setMaxActive(maxActive);
    }

    public void setHosts(String hosts) {
        configurator.setHosts(hosts);
    }

    public void setRunAutoDiscoveryAtStartup(boolean runAutoDiscoveryAtStartup) {
        configurator.setRunAutoDiscoveryAtStartup(runAutoDiscoveryAtStartup);
    }


}
