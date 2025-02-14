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
package cloud.piranha.extension.webannotations;

import cloud.piranha.webapp.api.AnnotationInfo;
import cloud.piranha.webapp.api.AnnotationManager;
import cloud.piranha.webapp.api.WebApplication;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import java.lang.System.Logger;
import static java.lang.System.Logger.Level.WARNING;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.EnumSet.noneOf;
import java.util.EventListener;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import static java.util.stream.Collectors.toCollection;

/**
 * The web annotations initializer.
 *
 * <p>
 * This class initializes web artifacts based on annotations in a similar way as
 *
 *
 *
 *
 * @author Arjan Tijms
 */
public class WebAnnotationsInitializer implements ServletContainerInitializer {

    /**
     * Stores the logger.
     */
    private static final Logger LOGGER = System.getLogger(WebAnnotationsInitializer.class.getName());

    /**
     * On startup.
     *
     * @param classes the classes.
     * @param servletContext the servlet context.
     * @throws ServletException when a servlet error occurs.
     */
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        WebApplication webApp = (WebApplication) servletContext;

        AnnotationManager annotationManager = webApp.getManager(AnnotationManager.class);

        // Process @WebServlet

        for (AnnotationInfo<WebServlet> annotationInfo : annotationManager.getAnnotations(WebServlet.class)) {

            WebServlet webServlet = annotationInfo.getInstance();

            String servletName = webServlet.name();
            if ("".equals(servletName)) {
                servletName = annotationInfo.getTargetType().getName(); // WebServlet only has target Type
            }

            // Add the Servlet
            Dynamic registration = webApp.addServlet(servletName, annotationInfo.getTargetType().getName());

            if (registration != null) {
                // Add params
                if (webServlet.initParams().length != 0) {
                    stream(webServlet.initParams()).forEach(initParam -> registration.setInitParameter(initParam.name(), initParam.value()));
                }
            
                registration.setAsyncSupported(webServlet.asyncSupported());
            }

            String[] urlPatterns = webServlet.value();
            if (urlPatterns.length == 0) {
                urlPatterns = webServlet.urlPatterns();
            }

            // Add mapping
            webApp.addServletMapping(servletName, urlPatterns);
        }

        // Process @WebFilter

        for (AnnotationInfo<WebFilter> annotationInfo : annotationManager.getAnnotations(WebFilter.class)) {

            WebFilter webFilter = annotationInfo.getInstance();

            String filterName = webFilter.filterName();
            if ("".equals(filterName)) {
                filterName = annotationInfo.getTargetType().getName(); // WebServlet only has target Type
            }

            // Add the Filter
            FilterRegistration.Dynamic registration = webApp.addFilter(filterName, annotationInfo.getTargetType().getName());

            // Add params
            if (webFilter.initParams().length != 0) {
                stream(webFilter.initParams()).forEach(initParam ->
                    registration.setInitParameter(initParam.name(), initParam.value())
                );
            }

            if (registration != null)
                registration.setAsyncSupported(webFilter.asyncSupported());

            String[] urlPatterns = webFilter.value();
            if (urlPatterns.length == 0) {
                urlPatterns = webFilter.urlPatterns();
            }

            // Add mapping for URL patterns, if any
            if (urlPatterns.length > 0) {
                webApp.addFilterMapping(filterName, urlPatterns);
            }

            // Add mapping for Servlet names, if any
            if (webFilter.servletNames().length > 0) {
                registration.addMappingForServletNames(
                    stream(webFilter.dispatcherTypes())
                        .collect(toCollection(() -> noneOf(DispatcherType.class))),
                    true,
                    webFilter.servletNames());
            }
        }
        
        // Process @MultipartConfig
        // This assumes all applicable Servlets have been registered, either via web.xml, annotations or programmatically prior to this point.
        for (AnnotationInfo<MultipartConfig> annotationInfo : annotationManager.getAnnotations(MultipartConfig.class)) {
            for (ServletRegistration servletRegistration : servletContext.getServletRegistrations().values()) {
                if (servletRegistration instanceof Dynamic dynamicRegistration) {
                    Class<?> targetType = annotationInfo.getTargetType();
                    if (targetType != null && targetType.getName().equals(servletRegistration.getClassName())) {
                        dynamicRegistration.setMultipartConfig(new MultipartConfigElement(annotationInfo.getInstance()));
                    }
                }
            }
        }

        // Process @ServletSecurity
        List<Entry<List<String>, ServletSecurity>> securityAnnotations = new ArrayList<>();
        for (AnnotationInfo<ServletSecurity> annotationInfo : annotationManager.getAnnotations(ServletSecurity.class)) {

            Class<?> servlet = getTargetServlet(annotationInfo);

            // Take into account mixed mapped (annotation + web.xml later)
            WebServlet webServlet = servlet.getAnnotation(WebServlet.class);

            if (webServlet != null) {
                String[] urlPatterns = webServlet.value();
                if (urlPatterns.length == 0) {
                    urlPatterns = webServlet.urlPatterns();
                }

                securityAnnotations.add(new SimpleImmutableEntry<>(
                    asList(urlPatterns),
                    annotationInfo.getInstance()));
            } else {
                LOGGER.log(WARNING,
                    "@ServletSecurity encountered on Servlet " + servlet +
                    "but no @WebServlet encountered");
            }
        }
        webApp.setAttribute(
            "cloud.piranha.authorization.exousia.AuthorizationPreInitializer.security.annotations",
            securityAnnotations
        );

        // Collect the roles from various annotations
        for (AnnotationInfo<RolesAllowed> rolesAllowedInfo : annotationManager.getAnnotations(RolesAllowed.class)) {
            webApp.declareRoles(rolesAllowedInfo.getInstance().value());
        }

        for (AnnotationInfo<DeclareRoles> declareRolesInfo : annotationManager.getAnnotations(DeclareRoles.class)) {
            webApp.declareRoles(declareRolesInfo.getInstance().value());
        }

        // Process @WebListener
        for (AnnotationInfo<WebListener> annotationInfo : annotationManager.getAnnotations(WebListener.class)) {
            webApp.addListener(getTargetListener(annotationInfo));
        }

        webApp.initializeDeclaredFinish();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EventListener> getTargetListener(AnnotationInfo<WebListener> annotationInfo) {
        return (Class<? extends EventListener>) annotationInfo.getTargetType();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends HttpServlet> getTargetServlet(AnnotationInfo<ServletSecurity> annotationInfo) {
        return (Class<? extends HttpServlet>) annotationInfo.getTargetType();
    }

}
