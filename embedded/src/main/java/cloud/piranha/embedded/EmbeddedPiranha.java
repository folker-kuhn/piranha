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
package cloud.piranha.embedded;

import cloud.piranha.resource.ByteArrayResourceStreamHandlerProvider;
import cloud.piranha.webapp.api.WebApplication;
import cloud.piranha.webapp.impl.DefaultWebApplication;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The embeddable servlet container version of Piranha.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class EmbeddedPiranha {

    /**
     * Stores the web application.
     */
    private final WebApplication webApplication;

    /**
     * Constructor.
     */
    public EmbeddedPiranha() {
        webApplication = new DefaultWebApplication();
    }
    
    /**
     * Another constructor.
     * 
     * @param webApplication the web application to use.
     */
    public EmbeddedPiranha(WebApplication webApplication) {
        this.webApplication = webApplication;
    }

    /**
     * Destroy the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha destroy() {
        webApplication.destroy();
        return this;
    }

    /**
     * {@return the web application}
     */
    public WebApplication getWebApplication() {
        return webApplication;
    }

    /**
     * Initialize the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha initialize() {
        webApplication.initialize();
        return this;
    }
    
    /**
     * This method services a request by dispatching it to the configured Servlet and/or Filters.
     * 
     * <p>
     * This is a convenience method for simple cases. Use the other service methods for more
     * control of the request.
     *
     * @param servletPath the request path, e.g. <code>/foo/bar</code>
     * @param parameters the request parameters, with each even parameter the name, and odd parameter the value. e.g. <code>/foo, 1, bar, 2</code>
     * @return the response generated by the Servlet and/or Filters
     * @throws IOException when an I/O error occurs.
     * @throws ServletException when a Servlet error occurs.
     */
    public EmbeddedResponse service(String servletPath, String... parameters) throws IOException, ServletException {
        EmbeddedResponse embeddedResponse = new EmbeddedResponse();
        
        EmbeddedRequest embeddedRequest = new EmbeddedRequestBuilder()
                .servletPath(servletPath)
                .build();

        if (parameters != null && parameters.length > 0) {
            if (parameters.length % 2 != 0) {
                throw new IllegalStateException("Parameters must be provided in pairs of two");
            }
            Map<String, ArrayList<String>> parameterMap = new HashMap<>();
            for (int i = 0; i < parameters.length-2; i+=2) {
                parameterMap.computeIfAbsent(parameters[i], e -> new ArrayList<>()).add(parameters[i+1]);
            }
            for (Map.Entry<String, ArrayList<String>> parameterEntry : parameterMap.entrySet()) {
                embeddedRequest.setParameter(parameterEntry.getKey(), parameterEntry.getValue().toArray(String[]::new));
            }
        }
        
        service(embeddedRequest, embeddedResponse);
        
        return embeddedResponse;
    }
    
    /**
     * This method services a request by dispatching it to the configured Servlet and/or Filters.
     *
     * <p>
     * This is a convenience method uses
     *
     * @param servletRequest the request.
     * 
     * @return the response generated by the Servlet and/or Filters
     * 
     * @throws IOException when an I/O error occurs.
     * @throws ServletException when a Servlet error occurs.
     */
    public EmbeddedResponse service(ServletRequest servletRequest) throws IOException, ServletException {
        EmbeddedResponse embeddedResponse = new EmbeddedResponse();
        service(servletRequest, embeddedResponse);
        
        return embeddedResponse;
    }

    /**
     * Service method.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws IOException when an I/O error occurs.
     * @throws ServletException when a Servlet error occurs.
     */
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        try {
            ByteArrayResourceStreamHandlerProvider.setGetResourceAsStreamFunction(webApplication::getResourceAsStream);

            if (servletRequest.getServletContext() == null && servletRequest instanceof EmbeddedRequest embeddedRequest) {
                embeddedRequest.setWebApplication(webApplication);
            }
            if (servletResponse instanceof EmbeddedResponse embeddedResponse) {
                embeddedResponse.setWebApplication(webApplication);
            }
            
            webApplication.linkRequestAndResponse(servletRequest, servletResponse);
            webApplication.service(servletRequest, servletResponse);
            webApplication.unlinkRequestAndResponse(servletRequest, servletResponse);

        } finally {
            ByteArrayResourceStreamHandlerProvider.setGetResourceAsStreamFunction(null);
        }
    }

    /**
     * Start the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha start() {
        webApplication.start();
        return this;
    }

    /**
     * Stop the web application.
     *
     * @return the instance.
     */
    public EmbeddedPiranha stop() {
        webApplication.stop();
        return this;
    }
}
