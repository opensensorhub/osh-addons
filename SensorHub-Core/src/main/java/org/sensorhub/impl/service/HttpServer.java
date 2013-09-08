/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;


/**
 * <p><b>Title:</b>
 * HttpServer
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Wrapper module for the HTTP server engine (Jetty for now)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class HttpServer implements IModule<HttpServerConfig>
{
    public static String TEST_MSG = "SensorHub web server is up";
    private static HttpServer instance;
        
    HttpServerConfig config;
    Server server;
    ServletContextHandler handler;
    
    
    public HttpServer()
    {
        if (instance != null)
            throw new RuntimeException("Cannot start several HTTP server instances");
        
        instance = this;
        
        // create handler
        this.handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        
        // add default test servlet
        handler.addServlet(new ServletHolder(new HttpServlet() {
            private static final long serialVersionUID = 1L;
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
            {
                try
                {
                    resp.getOutputStream().print(TEST_MSG);
                }
                catch (IOException e)
                {
                    throw new ServletException(e);
                }
            }
        }),"/test");
    }
    
    
    public static HttpServer getInstance()
    {
        if (instance == null)
            instance = new HttpServer();
        
        return instance;
    }
    
    
    @Override
    public void init(HttpServerConfig config) throws SensorHubException
    {
        this.config = config;
        
        try
        {
            server = new Server(config.httpPort);
            handler.setContextPath(config.rootURL);
            server.setHandler(handler);
            server.start();
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while starting SensorHub embedded HTTP server", e);
        }
    }
    

    @Override
    public void updateConfig(HttpServerConfig config) throws SensorHubException
    {
        stop();
        init(config);
    }

    
    @Override
    public HttpServerConfig getConfiguration()
    {
        return config;
    }
    
    
    public void deployServlet(String path, HttpServlet servlet)
    {
        handler.addServlet(new ServletHolder(servlet), path);
    }
    
    
    public void stop() throws SensorHubException
    {
        try
        {
            server.stop();
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while stopping SensorHub embedded HTTP server", e);
        }
    }
    

    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        stop();
        server = null;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }

}
