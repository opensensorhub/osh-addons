/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;


public class TestHttpServer
{

    @Test
    public void testStartServer() throws Exception
    {
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.rootURL + "/test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        assertTrue(resp.equals(HttpServer.TEST_MSG));        
        server.stop();
    }
    
    
    @Test
    public void testDeployServlet() throws Exception
    {
        // start server
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        
        final String testText = "Deploying hot servlet in SensorHub works";
        
        // deploy new servlet dynamically
        server.deployServlet("/junit", new HttpServlet() {
            private static final long serialVersionUID = 1L;
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
            {
                try
                {
                    resp.getOutputStream().print(testText);
                }
                catch (IOException e)
                {
                    throw new ServletException(e);
                }
            }
        });
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.rootURL + "/junit");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        assertTrue(resp.equals(testText));
        server.stop();
    }
    
    
    @Test
    public void testChangeConfig() throws Exception
    {
        // start server
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        
        final String testText = "Deploying hot servlet in SensorHub works";
        
        // deploy new servlet dynamically
        server.deployServlet("/junit", new HttpServlet() {
            private static final long serialVersionUID = 1L;
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
            {
                try
                {
                    resp.getOutputStream().print(testText);
                }
                catch (IOException e)
                {
                    throw new ServletException(e);
                }
            }
        });
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.rootURL + "/junit");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        assertTrue(resp.equals(testText));
        server.stop();
    }
}
