/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its 
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.http.grizzly;

import cloud.piranha.http.impl.DefaultHttpServerProcessor;
import cloud.piranha.http.api.HttpServerProcessor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * The Grizzly implementation of HTTP Server.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 * @see cloud.piranha.http.api.HttpServer
 */
public class GrizzlyHttpServer implements cloud.piranha.http.api.HttpServer {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(GrizzlyHttpServer.class.getName());

    /**
     * Stores the Grizzly HttpServer.
     */
    private HttpServer httpServer;

    /**
     * Stores the HTTP server processor.
     */
    private HttpServerProcessor httpServerProcessor;

    /**
     * Constructor.
     */
    public GrizzlyHttpServer() {
        httpServer = HttpServer.createSimpleServer();
        httpServerProcessor = new DefaultHttpServerProcessor();
        addHttpHandler();
    }

    /**
     * Constructor
     *
     * @param serverPort the server port.
     */
    public GrizzlyHttpServer(int serverPort) {
        httpServer = HttpServer.createSimpleServer(null, serverPort);
        httpServerProcessor = new DefaultHttpServerProcessor();
        addHttpHandler();
    }

    /**
     * Constructor
     *
     * @param serverPort the server port.
     * @param httpServerProcessor the HTTP server processor;
     */
    public GrizzlyHttpServer(int serverPort, HttpServerProcessor httpServerProcessor) {
        httpServer = HttpServer.createSimpleServer(null, serverPort);
        this.httpServerProcessor = httpServerProcessor;
        addHttpHandler();
    }

    /**
     * Constructor
     *
     * @param httpServer the Grizzly HTTP server.
     */
    public GrizzlyHttpServer(HttpServer httpServer) {
        this.httpServer = httpServer;
        this.httpServerProcessor = new DefaultHttpServerProcessor();
        addHttpHandler();
    }

    /**
     * Add the HTTP handler.
     */
    private void addHttpHandler() {
        httpServer.getServerConfiguration().addHttpHandler(new HttpHandler() {
            @Override
            public void service(Request request, Response response) throws Exception {
                GrizzlyHttpServerRequest gRequest = new GrizzlyHttpServerRequest(request);
                GrizzlyHttpServerResponse gResponse = new GrizzlyHttpServerResponse(response);
                httpServerProcessor.process(gRequest, gResponse);
            }
        });
    }

    /**
     * @see cloud.piranha.http.api.HttpServer#isRunning() 
     */
    @Override
    public boolean isRunning() {
        return httpServer != null;
    }

    /**
     * @see cloud.piranha.http.api.HttpServer#start() 
     */
    @Override
    public void start() {
        try {
            httpServer.start();
        } catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "An I/O error occurred while starting the HTTP server", ioe);
        }
    }

    /**
     * @see cloud.piranha.http.api.HttpServer#stop() 
     */
    @Override
    public void stop() {
        Object shutdownLock = new Object();
        httpServer.shutdown().addCompletionHandler(new CompletionHandler<HttpServer>() {
            @Override
            public void cancelled() {
                synchronized (shutdownLock) {
                    shutdownLock.notifyAll();
                }
            }

            @Override
            public void failed(Throwable thrwbl) {
                synchronized (shutdownLock) {
                    shutdownLock.notifyAll();
                }
            }

            @Override
            public void completed(HttpServer e) {
                synchronized (shutdownLock) {
                    shutdownLock.notifyAll();
                }
            }

            @Override
            public void updated(HttpServer e) {
                synchronized (shutdownLock) {
                    shutdownLock.notifyAll();
                }
            }
        });
        try {
            synchronized (shutdownLock) {
                shutdownLock.wait();
            }
        } catch (InterruptedException ie) {
        }
        httpServer = null;
    }
}
