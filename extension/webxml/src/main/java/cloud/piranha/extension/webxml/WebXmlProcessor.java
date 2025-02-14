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
package cloud.piranha.extension.webxml;

import cloud.piranha.webapp.api.AuthenticationManager;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import static java.util.stream.Collectors.toSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.System.Logger;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;

import cloud.piranha.webapp.api.LocaleEncodingManager;
import cloud.piranha.webapp.api.MimeTypeManager;
import cloud.piranha.webapp.api.SecurityManager;
import cloud.piranha.webapp.api.WebApplication;
import cloud.piranha.webapp.api.WelcomeFileManager;

/**
 * The web.xml / web-fragment.xml processor.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class WebXmlProcessor {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = System.getLogger(WebXmlProcessor.class.getName());

    /**
     * Stores the empty string array.
     */
    private static final String[] STRING_ARRAY = new String[0];

    /**
     * Process the web.xml into the web application.
     *
     * @param webXml the web.xml
     * @param webApplication the web application.
     */
    public void process(WebXml webXml, WebApplication webApplication) {
        LOGGER.log(TRACE, "Started WebXmlProcessor.process");
        processContextParameters(webApplication, webXml);
        processDefaultContextPath(webApplication, webXml);
        processDenyUncoveredHttpMethods(webApplication, webXml);
        processDisplayName(webApplication, webXml);
        processDistributable(webApplication, webXml);
        processErrorPages(webApplication, webXml);
        processFilters(webApplication, webXml);
        processFilterMappings(webApplication, webXml);
        processListeners(webApplication, webXml);
        processMimeMappings(webApplication, webXml);
        processRequestCharacterEncoding(webApplication, webXml);
        processResponseCharacterEncoding(webApplication, webXml);
        processRoleNames(webApplication, webXml);
        processSecurityConstraints(webApplication, webXml);
        processServlets(webApplication, webXml);
        processServletMappings(webApplication, webXml);
        processWebApp(webApplication, webXml);
        processWelcomeFiles(webApplication, webXml);
        processLocaleEncodingMapping(webApplication, webXml);
        processSessionConfig(webApplication, webXml);
        LOGGER.log(TRACE, "Finished WebXmlProcessor.process");
    }

    /**
     * Process the context parameters.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processContextParameters(WebApplication webApplication, WebXml webXml) {
        for (WebXmlContextParam contextParam : webXml.getContextParams()) {
            webApplication.setInitParameter(contextParam.name(), contextParam.value());
        }
    }

    /**
     * Process the default context path.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDefaultContextPath(WebApplication webApplication, WebXml webXml) {
        if (webXml.getDefaultContextPath() != null) {
            webApplication.setContextPath(webXml.getDefaultContextPath());
        }
    }

    /**
     * Process the deny uncovered HTTP methods flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDenyUncoveredHttpMethods(WebApplication webApplication, WebXml webXml) {
        webApplication.setDenyUncoveredHttpMethods(webXml.getDenyUncoveredHttpMethods());
    }

    /**
     * Process the display name flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDisplayName(WebApplication webApplication, WebXml webXml) {
        webApplication.setServletContextName(webXml.getDisplayName());
    }

    /**
     * Process the distributable flag.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processDistributable(WebApplication webApplication, WebXml webXml) {
        webApplication.setDistributable(webXml.isDistributable());
    }

    /**
     * Process the error pages.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processErrorPages(WebApplication webApplication, WebXml webXml) {
        for (WebXmlErrorPage errorPage : webXml.getErrorPages()) {
            if (errorPage.errorCode() != null && !errorPage.errorCode().isEmpty()) {
                webApplication.addErrorPage(Integer.parseInt(errorPage.errorCode()), errorPage.location());
            } else if (errorPage.exceptionType() != null && !errorPage.exceptionType().isEmpty()) {
                webApplication.addErrorPage(errorPage.exceptionType(), errorPage.location());
            }
        }
    }

    /**
     * Process the filter mappings mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processFilterMappings(WebApplication webApplication, WebXml webXml) {
        webXml.getFilterMappings().forEach(filterMapping -> {
            // Filter is mapped to a URL pattern, e.g. /path/customer
            webApplication.addFilterMapping(
                    toDispatcherTypes(filterMapping.getDispatchers()),
                    filterMapping.getFilterName(),
                    true,
                    filterMapping.getUrlPatterns().toArray(STRING_ARRAY));

            // Filter is mapped to a named Servlet, e.g. FacesServlet
            webApplication.addFilterMapping(
                    toDispatcherTypes(filterMapping.getDispatchers()),
                    filterMapping.getFilterName(),
                    true,
                    filterMapping.getServletNames().stream().map(e -> "servlet:// " + e).toArray(String[]::new));
        });
    }

    private Set<DispatcherType> toDispatcherTypes(List<String> dispatchers) {
        if (dispatchers == null) {
            return null;
        }

        return dispatchers.stream()
                .map(DispatcherType::valueOf)
                .collect(toSet());
    }

    /**
     * Process the filters.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processFilters(WebApplication webApplication, WebXml webXml) {
        webXml.getFilters().forEach(filter -> {
            FilterRegistration.Dynamic dynamic = null;

            if (filter.getClassName() != null) {
                dynamic = webApplication.addFilter(filter.getFilterName(), filter.getClassName());
            } else if (filter.getServletName() != null) {
                dynamic = webApplication.addFilter(filter.getFilterName(), filter.getServletName());
            }

            if (dynamic != null && filter.isAsyncSupported()) {
                dynamic.setAsyncSupported(true);
            }

            if (dynamic != null) {
                for (WebXmlFilterInitParam initParam : filter.getInitParams()) {
                    dynamic.setInitParameter(initParam.name(), initParam.value());
                }
            }
        });
    }

    /**
     * Process the listeners.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processListeners(WebApplication webApplication, WebXml webXml) {
        for (WebXmlListener listener : webXml.getListeners()) {
            webApplication.addListener(listener.className());
        }
    }

    /**
     * Process the mime mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processMimeMappings(WebApplication webApplication, WebXml webXml) {
        if (webApplication.getAttribute(MimeTypeManager.class.getName()) instanceof MimeTypeManager manager) {
            webXml.getMimeMappings().forEach(mapping -> {
                manager.addMimeType(mapping.extension(), mapping.mimeType());
            });
        }
    }

    /**
     * Process the request character encoding.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processRequestCharacterEncoding(WebApplication webApplication, WebXml webXml) {
        if (webXml.getRequestCharacterEncoding() != null) {
            webApplication.setRequestCharacterEncoding(webXml.getRequestCharacterEncoding());
        }
    }

    /**
     * Process the response character encoding.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processResponseCharacterEncoding(WebApplication webApplication, WebXml webXml) {
        if (webXml.getResponseCharacterEncoding() != null) {
            webApplication.setResponseCharacterEncoding(webXml.getResponseCharacterEncoding());
        }
    }

    private void processRoleNames(WebApplication webApplication, WebXml webXml) {
        webApplication.getManager(SecurityManager.class).declareRoles(webXml.getRoleNames());
    }

    /**
     * Process the servlet mappings.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processServletMappings(WebApplication webApplication, WebXml webXml) {
        for (WebXmlServletMapping mapping : webXml.getServletMappings()) {
            webApplication.addServletMapping(
                    mapping.servletName(), mapping.urlPattern());
        }
    }

    /**
     * Process the web app. This is basically only the version contained within
     * it.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processWebApp(WebApplication webApplication, WebXml webXml) {
        if (webXml.getMajorVersion() != -1) {
            webApplication.setEffectiveMajorVersion(webXml.getMajorVersion());
        }
        if (webXml.getMinorVersion() != -1) {
            webApplication.setEffectiveMinorVersion(webXml.getMinorVersion());
        }
    }

    /**
     * Process the servlets.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processServlets(WebApplication webApplication, WebXml webXml) {
        LOGGER.log(DEBUG, "Configuring Servlets");

        for (WebXmlServlet servlet : webXml.getServlets()) {
            LOGGER.log(DEBUG, () -> "Configuring Servlet: " + servlet.getServletName());

            ServletRegistration.Dynamic dynamic = webApplication.addServlet(servlet.getServletName(), servlet.getClassName());

            String jspFile = servlet.getJspFile();
            if (!isEmpty(jspFile)) {
                webApplication.addJspFile(servlet.getServletName(), jspFile);
            }

            if (servlet.isAsyncSupported()) {
                dynamic.setAsyncSupported(true);
            }

            WebXmlServletMultipartConfig multipartConfig = servlet.getMultipartConfig();
            if (multipartConfig != null) {
                dynamic.setMultipartConfig(
                        new MultipartConfigElement(
                                multipartConfig.getLocation(),
                                multipartConfig.getMaxFileSize(),
                                multipartConfig.getMaxRequestSize(),
                                multipartConfig.getFileSizeThreshold()));
            }

            servlet.getInitParams().forEach(initParam -> {
                ServletRegistration servletRegistration = webApplication.getServletRegistration(servlet.getServletName());
                if (servletRegistration != null) {
                    servletRegistration.setInitParameter(initParam.name(), initParam.value());
                }
            });

            LOGGER.log(DEBUG, () -> "Configured Servlet: " + servlet.getServletName());
        }
    }

    /**
     * Process the welcome files.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processWelcomeFiles(WebApplication webApplication, WebXml webXml) {
        LOGGER.log(DEBUG, "Adding welcome files");

        Iterator<String> iterator = webXml.getWelcomeFiles().iterator();
        WelcomeFileManager welcomeFileManager = webApplication.getManager(WelcomeFileManager.class);
        while (iterator.hasNext()) {
            String welcomeFile = iterator.next();
            LOGGER.log(DEBUG, () -> "Adding welcome file: " + welcomeFile);
            welcomeFileManager.addWelcomeFile(welcomeFile);
        }
    }

    private void processLocaleEncodingMapping(WebApplication webApplication, WebXml webXml) {
        Map<String, String> localeMapping = webXml.getLocaleEncodingMapping();
        if (localeMapping == null) {
            return;
        }

        LocaleEncodingManager localeEncodingManager = webApplication.getManager(LocaleEncodingManager.class);
        if (localeEncodingManager == null) {
            return;
        }

        localeMapping.forEach(localeEncodingManager::addCharacterEncoding);
    }

    /**
     * Process the session config.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processSessionConfig(WebApplication webApplication, WebXml webXml) {
        WebXmlSessionConfig sessionConfig = webXml.getSessionConfig();
        if (sessionConfig == null) {
            return;
        }
        webApplication.setSessionTimeout(sessionConfig.sessionTimeout());
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Process the security constraints.
     *
     * @param webApplication the web application.
     * @param webXml the web.xml.
     */
    private void processSecurityConstraints(WebApplication webApplication, WebXml webXml) {
        AuthenticationManager manager = webApplication.getManager(AuthenticationManager.class);
        for (WebXmlSecurityConstraint constraint : webXml.getSecurityConstraints()) {
            for (WebXmlSecurityConstraint.WebResourceCollection collection : constraint.getWebResourceCollections()) {
                for (String urlPattern : collection.getUrlPatterns()) {
                    manager.addSecurityMapping(urlPattern);
                }
            }
        }
    }
}
