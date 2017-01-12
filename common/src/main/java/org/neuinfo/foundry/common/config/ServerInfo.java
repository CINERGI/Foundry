package org.neuinfo.foundry.common.config;

/**
* Created by bozyurt on 10/2/14.
*/
public class ServerInfo {
    private final String host;
    private final int port;
    private final String user;
    private final String pwd;

    public ServerInfo(String host, int port, String user, String pwd) {
        this.host = host;
        this.port = port;
        this.pwd = pwd;
        this.user = user;
    }


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPwd() {
        return pwd;
    }
}
