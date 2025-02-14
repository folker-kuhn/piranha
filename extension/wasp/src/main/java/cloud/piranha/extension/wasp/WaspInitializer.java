/*
 * Copyright (c) 2002-2021 Manorrock.com. All Rights Reserved.
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
package cloud.piranha.extension.wasp;

import cloud.piranha.webapp.api.JspManager;
import cloud.piranha.webapp.api.WebApplication;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.jsp.JspFactory;
import java.io.File;
import static java.io.File.pathSeparator;
import java.lang.System.Logger;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.runtime.TldScanner;

/**
 * The WaSP initializer.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class WaspInitializer implements ServletContainerInitializer {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = System.getLogger(WaspInitializer.class.getName());

    /**
     * Initialize WaSP.
     *
     * @param classes the classes.
     * @param servletContext the Servlet context.
     * @throws ServletException when a Servlet error occurs.
     */
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        LOGGER.log(DEBUG, "Determining if WaSP should be initialized");
        WebApplication application = (WebApplication) servletContext;
        boolean jspFound = false;
        Map<String, ? extends ServletRegistration> registrations = application.getServletRegistrations();
        for (Map.Entry<String, ? extends ServletRegistration> entry : registrations.entrySet()) {
            ServletRegistration registration = entry.getValue();
            Collection<String> mappings = registration.getMappings();
            if (mappings.contains("*.jsp")) {
                jspFound = true;
                LOGGER.log(DEBUG, "Found *.jsp mapping, skipping WaSP integration");
            }
        }
        if (!jspFound) {
            LOGGER.log(DEBUG, "Initializing WaSP");
            if (JspFactory.getDefaultFactory() == null) {
                JspFactory.setDefaultFactory(new JspFactoryImpl());
            }
            ServletRegistration.Dynamic registration = application.addServlet(
                    "jsp", "org.apache.jasper.servlet.JspServlet");
            registration.addMapping("*.jsp");
            String classpath = System.getProperty("jdk.module.path",
                    System.getProperty("java.class.path"))
                    + getClassesDirectory(application)
                    + getJarFiles(application);
            LOGGER.log(TRACE, () -> "WaSP classpath is: " + classpath);
            registration.setInitParameter("classpath", classpath);
            registration.setInitParameter("compilerSourceVM", "1.8");
            registration.setInitParameter("compilerTargetVM", "1.8");
            application.setManager(JspManager.class, new WaspJspManager());
            application.setAttribute("org.glassfish.wasp.useMultiJarScanAlgo", true);
            LOGGER.log(DEBUG, "Initialized WaSP");
            
            TldScanner scanner = new TldScanner();
            scanner.onStartup(classes, servletContext);
        }
    }

    /**
     * Get the WEB-INF/classes directory.
     *
     * @param servletContext the servlet context.
     * @return the classes directory.
     */
    private String getClassesDirectory(ServletContext servletContext) {
        String classesDirectory = servletContext.getRealPath("/WEB-INF/classes");
        if (classesDirectory == null) {
            return "";
        }
        return pathSeparator + classesDirectory;
    }

    /**
     * Get the WEB-INF/lib JAR files.
     *
     * @param servletContext the servlet context.
     * @return the location of the JAR files.
     */
    private String getJarFiles(ServletContext servletContext) {
        StringBuilder jarFiles = new StringBuilder();
        String realPath = servletContext.getRealPath("/WEB-INF/lib");
        if (realPath != null) {
            File directory = new File(realPath);
            if (directory.isDirectory()) {
                String[] files = directory.list();
                if (files != null) {
                    for (String file : files) {
                        if (file.toLowerCase().endsWith(".jar")) {
                            jarFiles.append(pathSeparator);
                            jarFiles.append(file);
                        }
                    }
                }
            }
        }
        return jarFiles.toString();
    }

    static {
        // This is a hack until Jasper handles JPMS
        if (WaspInitializer.class.getModule().isNamed()) {
            String oldExtDirs = System.getProperty("java.ext.dirs", "");
            System.setProperty("java.ext.dirs", System.getProperty("jdk.module.path") + pathSeparator + oldExtDirs);
        }
    }
}
