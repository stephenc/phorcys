package com.github.stephenc.phorcys;

import org.eclipse.jetty.server.session.AbstractSessionManager;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class CassandraSessionManager extends AbstractSessionManager {

    @Override
    public Map getSessionMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void addSession(Session session) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session getSession(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void invalidateSessions() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Session newSession(HttpServletRequest request) {
        return new CassandraSession(request);
    }

    @Override
    protected boolean removeSession(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected class CassandraSession extends Session {

        protected CassandraSession(HttpServletRequest request) {
            super(request);
        }

        protected CassandraSession(long created, long accessed, String clusterId) {
            super(created, accessed, clusterId);
        }
    }

    protected class ClassLoadingObjectInputStream extends ObjectInputStream {
        public ClassLoadingObjectInputStream(java.io.InputStream in) throws IOException {
            super(in);
        }

        public ClassLoadingObjectInputStream() throws IOException {
            super();
        }

        @Override
        public Class<?> resolveClass(java.io.ObjectStreamClass cl) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(cl.getName(), false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                return super.resolveClass(cl);
            }
        }
    }

}
