package org.neuinfo.foundry.common.config;

import org.neuinfo.foundry.common.util.ConfigUtils;

/**
* Created by bozyurt on 10/2/14.
 * dwv 2019-03-26. Environment Variables can be used in configuration
 * ${envVar:defaultValue}
 * not all properties support this, at present. No int values (aka ports).
 * only params in workflows
 * */
public class ServerInfo {
    private final String host;
    private final int port;
    private final String user;
    private final String pwd;

    public ServerInfo(String host, int port, String user, String pwd) {
        this.host = ConfigUtils.envVarParser(host);
        this.port = port;
        this.pwd = ConfigUtils.envVarParser(pwd);
        this.user = ConfigUtils.envVarParser(user);
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
