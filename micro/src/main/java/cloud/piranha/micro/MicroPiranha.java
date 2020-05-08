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
package cloud.piranha.micro;

import cloud.piranha.api.Piranha;
import cloud.piranha.appserver.impl.DefaultWebApplicationServer;
import cloud.piranha.extension.servlet.ServletExtension;
import cloud.piranha.http.api.HttpServer;
import cloud.piranha.http.impl.DefaultHttpServer;
import cloud.piranha.resource.DirectoryResource;
import cloud.piranha.webapp.api.WebApplication;
import cloud.piranha.webapp.impl.DefaultWebApplication;
import cloud.piranha.webapp.impl.DefaultWebApplicationClassLoader;
import cloud.piranha.webapp.impl.DefaultWebApplicationExtensionContext;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The micro version of Piranha.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class MicroPiranha implements Piranha, Runnable {

    /**
     * Defines the attribute name for the MicroPiranha reference.
     */
    static final String MICRO_PIRANHA = "cloud.piranha.micro.MicroPiranha";

    /**
     * Stores the HTTP server.
     */
    private DefaultHttpServer httpServer;

    /**
     * Stores the HTTP port.
     */
    private int port = 8080;

    /**
     * Stores the WAR file.
     */
    private File warFile;

    /**
     * Stores the web application.
     */
    private WebApplication webApplication;

    /**
     * Stores the (exploded) web application directory.
     */
    private File webApplicationDirectory = new File("webapp");

    /**
     * Stores the web application server.
     */
    private DefaultWebApplicationServer webApplicationServer;

    /**
     * Configure.
     *
     * @param arguments the arguments.
     * @return the web application.
     */
    public WebApplication configure(String[] arguments) {
        if (arguments.length > 0) {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i].equals("--webapp")) {
                    webApplicationDirectory = new File(arguments[i + 1]);
                }
                if (arguments[i].equals("--war")) {
                    warFile = new File(arguments[i + 1]);
                }
                if (arguments[i].equals("--port")) {
                    port = Integer.parseInt(arguments[i + 1]);
                }
            }
        }
        if (warFile != null) {
            extractWarFile();
        }
        webApplication = new DefaultWebApplication();
        webApplication.setAttribute(MICRO_PIRANHA, this);
        if (webApplicationDirectory != null) {
            webApplication.setClassLoader(new DefaultWebApplicationClassLoader(webApplicationDirectory));
            webApplication.addResource(new DirectoryResource(webApplicationDirectory));
        }
        DefaultWebApplicationExtensionContext extensionContext = new DefaultWebApplicationExtensionContext();
        extensionContext.add(ServletExtension.class);
        extensionContext.configure(webApplication);
        return webApplication;
    }

    /**
     * Extract the zip input stream.
     *
     * @param zipInput the zip input stream.
     * @param filePath the file path.
     * @throws IOException when an I/O error occurs.
     */
    private void extractZipInputStream(ZipInputStream zipInput, String filePath) throws IOException {
        try (BufferedOutputStream bufferOutput = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[8192];
            int read;
            while ((read = zipInput.read(bytesIn)) != -1) {
                bufferOutput.write(bytesIn, 0, read);
            }
        }
    }

    /**
     * Extract the WAR file.
     */
    private void extractWarFile() {
        if (!webApplicationDirectory.exists()) {
            webApplicationDirectory.mkdirs();
        }
        try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(warFile))) {
            ZipEntry entry = zipInput.getNextEntry();
            while (entry != null) {
                String filePath = webApplicationDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    extractZipInputStream(zipInput, filePath);
                }
                zipInput.closeEntry();
                entry = zipInput.getNextEntry();
            }
        } catch (IOException ioe) {
        }
    }

    /**
     * Get the HTTP server.
     *
     * @return the HTTP server.
     */
    public HttpServer getHttpServer() {
        return httpServer;
    }

    /**
     * Get the version.
     *
     * @return the version.
     */
    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    /**
     * Get the web application.
     *
     * @return the web application.
     */
    public WebApplication getWebApplication() {
        return webApplication;
    }

    /**
     * Main method.
     *
     * @param arguments the arguments.
     */
    public static void main(String[] arguments) {
        System.setProperty("java.naming.factory.initial", "cloud.piranha.jndi.memory.DefaultInitialContextFactory");
        MicroPiranha runner = new MicroPiranha();
        runner.configure(arguments);
        runner.run();
    }

    /**
     * Start method.
     */
    @Override
    public void run() {
        webApplicationServer = new DefaultWebApplicationServer();
        webApplicationServer.addWebApplication(webApplication);
        webApplicationServer.initialize();
        webApplicationServer.start();
        httpServer = new DefaultHttpServer(port, webApplicationServer, false);
        httpServer.start();
        while (httpServer.isRunning()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop method.
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }
}
