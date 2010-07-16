// -------------------------------------------------------------------
//
// Copyright (c) 2007-2010 Basho Technologies, Inc.  All Rights Reserved.
//
// -------------------------------------------------------------------

package raptor.server;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import raptor.store.RaptorIndex;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class RaptorServer {
    final private static Logger log =
            Logger.getLogger(RaptorServer.class);

    public static RaptorIndex idx;
    public static boolean shuttingDown = false;
    public static boolean debugging = false;

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    public void run() {
                        RaptorServer.shuttingDown = true;
                        log.info("shutting down...");
                        try {
                            log.info("sync, close, shutdown ...");
                            RaptorServer.idx.shutdown();
                            log.info("closing...");
                        } catch (Exception ex) {
                            log.info("Problem sync/close [crash] - verify and restore recommended", ex);
                        }
                    }
                });
    }

    public static void main(String[] argv) throws Exception {
        log.info("starting");
        int raptorPort = Integer.parseInt(argv[0]);
        String dataDir = argv[1];
        configureStorage(dataDir);
        buildRaptorServer(raptorPort);
        buildHeartbeatServer(raptorPort + 1);
    }

    private static void configureStorage(String dataDir) {
        try {
            idx = new RaptorIndex(dataDir);
        } catch (Exception ex) {
            idx = null;
            log.error("Error configuring Raptor storage", ex);
            System.exit(-1);
        }
    }

    private static void buildRaptorServer(int port) {
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool(),
                        Runtime.getRuntime().availableProcessors()));
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setPipelineFactory(new RaptorPipelineFactory());
        bootstrap.bind(new InetSocketAddress(port));
    }

    private static void buildHeartbeatServer(int port) {
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setPipelineFactory(new HeartbeatPipelineFactory());
        bootstrap.bind(new InetSocketAddress(port));
    }
}

