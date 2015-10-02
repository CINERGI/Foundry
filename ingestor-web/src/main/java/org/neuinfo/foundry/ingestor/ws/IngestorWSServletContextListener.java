package org.neuinfo.foundry.ingestor.ws;

import org.neuinfo.foundry.common.util.ScigraphMappingsHandler;
import org.neuinfo.foundry.common.util.ScigraphUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by bozyurt on 7/18/14.
 */
public class IngestorWSServletContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            System.out.println("starting mongoService");
            MongoService.getInstance();
            ScigraphMappingsHandler smHandler = ScigraphMappingsHandler.getInstance();
            ScigraphUtils.setHandler(smHandler);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            System.out.println("shutting down mongoService");
            MongoService.getInstance().shutdown();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
