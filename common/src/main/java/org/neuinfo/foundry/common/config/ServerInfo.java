package org.neuinfo.foundry.common.config;

/**
* Created by bozyurt on 10/2/14.
*/
public class ServerInfo {
    private final String host;
    private final int port;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
