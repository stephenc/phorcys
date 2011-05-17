<h1>Phorcys</h1>
<h2>Introduction</h2>
Phorcys is a session store for <a href="">Jetty</a> using <a href="">Hector</a> and <a href="http://cassandra.apache.org">Apache Cassandra</a> as a back-end.
<h2>Configuration</h2>

There are two components to session management in Jetty: a session ID manager and a session manager.

The session ID manager ensures that session IDs are unique across all webapps hosted on a Jetty instance, and thus there can only be one session ID manager per Jetty instance.
The session manager handles the session lifecycle (create/update/invalidate/expire) on behalf of a web application, so there is one session manager per web application instance.
These managers also cooperate and collaborate with the org.eclipse.jetty.server.session.SessionHandler to enable cross-context dispatch.

<h3>Configuring the CassandraSessionIdManager</h3>
You need to configure a com.github.stephenc.phorcys.CassandraSessionIdManager instance, either in embedded code or in a jetty.xml file. Here is an example of a jetty.xml setup:

    <Set name="sessionIdManager">
      <New id="jdbcidmgr" class="com.github.stephenc.phorcys.CassandraSessionIdManager">
        <Arg><Ref id="Server"/></Arg>
        <Set name="workerName">fred</Set>
        <Set name="hosts">192.168.1.5</Set>
        <Set name="clusterName">Test Cluster</Set>
        <Set name="keyspaceName">Jetty</Set>
        <Set name="columnFamilyName">Sessions</Set>
      </New>
    </Set>
    <Call name="setAttribute">
      <Arg>jdbcIdMgr</Arg>
      <Arg><Ref id="jdbcidmgr"/></Arg>
    </Call>

Notice that the CassandraSessionIdManager needs access to Cassandra.

As Jetty configuration files are direct mappings of XML to Java, it is straightforward to see how to do this in code, but here's an example anyway:

    Server server = new Server();
    ...
    CassandraSessionIdManager idMgr = new CassandraSessionIdManager(server);
    idMgr.setWorkerName("fred");
    idMgr.setHosts("192.168.1.5,192.168.1.6");
    idMgr.setClusterName("Jetty");
    idMgr.setKeyspaceName("Jetty");
    idMgr.setColumnFamilyName("Sessions");
    server.setSessionIdManager(idMgr);

You must configure the CassandraSessionIdManager with a workerName that is unique across the cluster. Typically the name relates to the physical node on which the instance is executing. If this name is not unique, your load balancer might fail to distribute your sessions correctly.

You can also configure how often the persistent session mechanism sweeps the database looking for old, expired sessions with the scavengeInterval setting. The default value is 60 seconds. We recommend that you not increase the frequency because doing so increases the load on the database with very little gain; old, expired sessions can harmlessly sit in the database.

<h3>Configuring a CassandraSessionManager</h3>
The way you configure a CassandraSessionManager depends on whether you're configuring from a context xml file or a jetty-web.xml file or code. The basic difference is how you get a reference to the Jetty org.eclipse.jetty.server.Jetty instance.

From a context xml file, you reference the Server instance as a Ref:

    <Ref name="Server" id="Server">
      <Call id="jdbcIdMgr" name="getAttribute">
        <Arg>jdbcIdMgr</Arg>
      </Call>
    </Ref>
    <Set name="sessionHandler">
      <New class="org.eclipse.jetty.server.session.SessionHandler">
        <Arg>
          <New id="jdbcmgr" class="com.github.stephenc.phorcys.CassandraSessionManager">
            <Set name="idManager">
              <Ref id="jdbcIdMgr"/>
            </Set>
          </New>
        </Arg>
      </New>
    </Set>

From a WEB-INF/jetty-web.xml file, you can reference the Server instance directly:

    <Get name="server">
      <Get id="jdbcIdMgr" name="sessionIdManager"/>
    </Get>
    <Set name="sessionHandler">
      <New class="org.eclipse.jetty.server.session.SessionHandler">
        <Arg>
          <New class="com.github.stephenc.phorcys.CassandraSessionManager">
            <Set name="idManager">
              <Ref id="jdbcIdMgr"/>
            </Set>
          </New>
        </Arg>
      </New>
    </Set>

If you're embedding this in code:

    //assuming you have already set up the JDBCSessionIdManager as shown earlier
    //and have a reference to the Server instance:
    WebAppContext wac = new WebAppContext();
    ... //configure your webapp context
    CassandraSessionManager jdbcMgr = new CassandraSessionManager();
    jdbcMgr.setIdManager(server.getSessionIdManager());
    wac.setSessionHandler(jdbcMgr);
