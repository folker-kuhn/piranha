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
package cloud.piranha.webapp.impl;

import static cloud.piranha.webapp.api.CurrentRequestHolder.CURRENT_REQUEST_ATTRIBUTE;
import static java.util.Arrays.asList;
import static javax.servlet.AsyncContext.ASYNC_CONTEXT_PATH;
import static javax.servlet.AsyncContext.ASYNC_PATH_INFO;
import static javax.servlet.AsyncContext.ASYNC_QUERY_STRING;
import static javax.servlet.AsyncContext.ASYNC_REQUEST_URI;
import static javax.servlet.AsyncContext.ASYNC_SERVLET_PATH;
import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.FORWARD;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import cloud.piranha.webapp.api.CurrentRequestHolder;
import cloud.piranha.webapp.api.ServletEnvironment;
import cloud.piranha.webapp.api.ServletInvocation;
import cloud.piranha.webapp.api.WebApplicationRequest;

/**
 * The default ServletRequestDispatcher.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class DefaultServletRequestDispatcher implements RequestDispatcher {

    private static final List<String> ASYNC_ATTRIBUTES = asList(ASYNC_CONTEXT_PATH, ASYNC_PATH_INFO, ASYNC_QUERY_STRING, ASYNC_REQUEST_URI, ASYNC_SERVLET_PATH);

    /**
     * The servletEnvironment corresponding to the target resource to which this
     * dispatcher forwards or includes.
     *
     * <p>
     * It contains the actual Servlet, to process the forwarded or included
     * request, as well as meta data for this Servlet.
     */
    private final ServletEnvironment servletEnvironment;

    /**
     * Stores the path.
     */
    private final String path;

    /**
     * Constructor.
     *
     * @param servletInvocation The servlet invocation containing all info this dispatcher uses to dispatch to the contained Servlet.
     */
    public DefaultServletRequestDispatcher(ServletInvocation servletInvocation) {
        this.servletEnvironment = servletInvocation.getServletEnvironment();
        this.path = servletInvocation.getInvocationPath();
    }

    /**
     * Forward the request and response.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws ServletException when a servlet error occurs.
     * @throws IOException when an I/O error occurs.
     */
    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        if (servletRequest.getDispatcherType().equals(ASYNC)) {

            // Asynchronous forward
            asyncForward(servletRequest, servletResponse);
            return;
        }

        // Regular (synchronous) forward
        syncForward(servletRequest, servletResponse);
    }

    /**
     * Include the request and response.
     *
     * @param servletRequest the request.
     * @param servletResponse the response.
     * @throws ServletException when a servlet error occurs.
     * @throws IOException when an I/O error occurs.
     */
    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        try (DefaultWebApplicationRequest includedRequest = new DefaultWebApplicationRequest()) {
            HttpServletRequest originalRequest = (HttpServletRequest) servletRequest;
            includedRequest.setWebApplication(servletEnvironment.getWebApplication());
            includedRequest.setContextPath(originalRequest.getContextPath());
            includedRequest.setAttribute(INCLUDE_REQUEST_URI, originalRequest.getRequestURI());
            includedRequest.setAttribute(INCLUDE_CONTEXT_PATH, originalRequest.getContextPath());
            includedRequest.setAttribute(INCLUDE_SERVLET_PATH, originalRequest.getServletPath());
            includedRequest.setAttribute(INCLUDE_PATH_INFO, originalRequest.getPathInfo());
            includedRequest.setAttribute(INCLUDE_QUERY_STRING, originalRequest.getQueryString());
            includedRequest.setServletPath(path);
            includedRequest.setPathInfo(null);
            includedRequest.setQueryString(null);

            servletEnvironment.getServlet().service(servletRequest, servletResponse);
        }
    }

    // #### SYNC forward private methods
    private void syncForward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        try (DefaultWebApplicationRequest forwardedRequest = new DefaultWebApplicationRequest()) {

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            response.resetBuffer();

            forwardedRequest.setWebApplication(servletEnvironment.getWebApplication());
            forwardedRequest.setContextPath(request.getContextPath());
            forwardedRequest.setDispatcherType(FORWARD);
            forwardedRequest.setAsyncSupported(request.isAsyncSupported());

            if (path != null) {
                setForwardAttributes(request, forwardedRequest,
                        FORWARD_CONTEXT_PATH,
                        FORWARD_PATH_INFO,
                        FORWARD_QUERY_STRING,
                        FORWARD_REQUEST_URI,
                        FORWARD_SERVLET_PATH);

                forwardedRequest.setServletPath(getServletPath(path));
                forwardedRequest.setQueryString(getQueryString(path));

            } else {
                forwardedRequest.setServletPath("/" + servletEnvironment.getServletName());
            }

            CurrentRequestHolder currentRequestHolder = updateCurrentRequest(request, forwardedRequest);

            try {
                servletEnvironment.getWebApplication().linkRequestAndResponse(forwardedRequest, servletResponse);
                servletEnvironment.getServlet().service(forwardedRequest, servletResponse);
                servletEnvironment.getWebApplication().unlinkRequestAndResponse(forwardedRequest, servletResponse);
            } finally {
                restoreCurrentRequest(currentRequestHolder, request);
            }

            response.flushBuffer();
        }
    }

    private void setForwardAttributes(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest, String... dispatcherKeys) {
        for (String dispatcherKey : dispatcherKeys) {
            setForwardAttribute(originalRequest, forwardedRequest, dispatcherKey);
        }
    }

    /**
     * Set forward attribute.
     *
     * @param originalRequest the original request
     * @param forwardedRequest the forward request.
     * @param dispatcherKey the dispatcher key.
     */
    private void setForwardAttribute(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest, String dispatcherKey) {
        String value = null;

        if (originalRequest.getAttribute(dispatcherKey) != null) {
            value = (String) originalRequest.getAttribute(dispatcherKey);
        } else {
            if (dispatcherKey.equals(FORWARD_CONTEXT_PATH)) {
                value = originalRequest.getContextPath();
            }
            if (dispatcherKey.equals(FORWARD_PATH_INFO)) {
                value = originalRequest.getPathInfo();
            }
            if (dispatcherKey.equals(FORWARD_QUERY_STRING)) {
                value = originalRequest.getQueryString();
            }
            if (dispatcherKey.equals(FORWARD_REQUEST_URI)) {
                value = originalRequest.getRequestURI();
            }
            if (dispatcherKey.equals(FORWARD_SERVLET_PATH)) {
                value = originalRequest.getServletPath();
            }
        }

        forwardedRequest.setAttribute(dispatcherKey, value);
    }

    private CurrentRequestHolder updateCurrentRequest(HttpServletRequest originalRequest, HttpServletRequest forwardedRequest) {
        CurrentRequestHolder currentRequestHolder = (CurrentRequestHolder) originalRequest.getAttribute(CURRENT_REQUEST_ATTRIBUTE);
        if (currentRequestHolder != null) {
            currentRequestHolder.setRequest(forwardedRequest);
            forwardedRequest.setAttribute(CURRENT_REQUEST_ATTRIBUTE, currentRequestHolder);
        }

        forwardedRequest.setAttribute("PREVIOUS_REQUEST", originalRequest);

        return currentRequestHolder;
    }

    private void restoreCurrentRequest(CurrentRequestHolder currentRequestHolder, HttpServletRequest originalRequest) {
        if (currentRequestHolder != null) {
            currentRequestHolder.setRequest(originalRequest);
        }
    }

    // #### ASYNC forward private methods
    private void asyncForward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        if (servletRequest instanceof AsyncHttpDispatchWrapper
                || servletRequest instanceof AsyncNonHttpDispatchWrapper) {

            if (servletRequest instanceof AsyncHttpDispatchWrapper) {
                // The caller provided or let us default to an HttpServletRequest
                asyncHttpForward((AsyncHttpDispatchWrapper) servletRequest, servletResponse);
                return;
            }

            // The caller provided a ServletRequest
            asyncNonHttpForward((AsyncNonHttpDispatchWrapper) servletRequest, servletResponse);
        } else {
            throw new IllegalStateException("Async invocations without wrapper not supported at this moment.");
        }
    }

    private void asyncHttpForward(AsyncHttpDispatchWrapper asyncHttpDispatchWrapper, ServletResponse servletResponse) throws ServletException, IOException {
        // A typical chain to arrive here is DefaultAsyncContext#dispatch -> DefaultAsyncDispatcher#dispatch -> forward -> asyncForwrd -> asyncHttpForward

        HttpServletRequest asyncStartRequest = asyncHttpDispatchWrapper.getRequest();

        if (asyncStartRequest instanceof WebApplicationRequest) {
            // original request or previously dispatched request passed-in, not an application wrapped one
            // In this case our asyncHttpDispatchWrapper is both the object with which the Servlet will be invoked, as well as the
            // object on which the path and attributes for the previous path will be set.

            invokeTargetAsyncServlet(asyncHttpDispatchWrapper, servletResponse);

        } else if (asyncStartRequest instanceof HttpServletRequestWrapper) {
            // Application wrapped request passed-in. We now need no make sure that the applications sees this request

            // We swap our asyncHttpDispatchWrapper from being the head of the chain, to be in between the request that was provided by the application
            // and the request it is wrapping.
            HttpServletRequestWrapper applicationProvidedWrapper = (HttpServletRequestWrapper) asyncStartRequest;

            ServletRequest wrappedRequest = applicationProvidedWrapper.getRequest();

            applicationProvidedWrapper.setRequest(asyncHttpDispatchWrapper);
            asyncHttpDispatchWrapper.setRequest(wrappedRequest);

            // Original chain: asyncHttpDispatchWrapper -> applicationProvidedWrapper (asyncStartRequest) -> wrappedRequest
            // New chain: applicationProvidedWrapper (asyncStartRequest) -> asyncHttpDispatchWrapper -> wrappedRequest
            invokeTargetAsyncServlet(applicationProvidedWrapper, asyncHttpDispatchWrapper, servletResponse);

        } else {
            throw new IllegalStateException("Async invocation with a request that was neither the original one nor a wrapped one: " + asyncStartRequest);
        }
    }

    private void asyncNonHttpForward(AsyncNonHttpDispatchWrapper asyncNonHttpDispatchWrapper, ServletResponse servletResponse) throws ServletException, IOException {
        // A typical chain to arrive here is DefaultAsyncContext#dispatch -> DefaultAsyncDispatcher#dispatch -> forward -> asyncForward -> asyncNonHttpForward

        ServletRequest asyncStartRequest = asyncNonHttpDispatchWrapper.getRequest();

        if (asyncStartRequest instanceof ServletRequestWrapper) {

            ServletRequestWrapper applicationProvidedWrapper = (ServletRequestWrapper) asyncStartRequest;

            HttpServletRequest httpServletRequestInChain = findHttpServletRequestInChain(applicationProvidedWrapper);

            if (httpServletRequestInChain != null) {

                // We swap our asyncHttpDispatchWrapper from being the head of the chain, with a new wrapper, wrapping the HttpServletRequest that we found, and put
                // that in between the request that was provided by the application and the request it is wrapping.
                ServletRequest wrappedRequest = applicationProvidedWrapper.getRequest();

                AsyncHttpDispatchWrapper newAsyncHttpDispatchWrapper = new AsyncHttpDispatchWrapper(null);
                // Note that by doing this, methods called on HttpServletRequestWrapper itself (and not its super interface) will throw.
                newAsyncHttpDispatchWrapper.setRequest(wrappedRequest);

                applicationProvidedWrapper.setRequest(newAsyncHttpDispatchWrapper);

                // Original chain: asyncNonHttpDispatchWrapper -> applicationProvidedWrapper (asyncStartRequest) -> wrappedRequest -> .... -> HttpServletRequest
                // New chain: applicationProvidedWrapper (asyncStartRequest) -> newAsyncHttpDispatchWrapper -> wrappedRequest -> .... -> HttpServletRequest
                invokeTargetAsyncServlet(asyncStartRequest, httpServletRequestInChain, newAsyncHttpDispatchWrapper, servletResponse);
            }

        }
    }

    private void invokeTargetAsyncServlet(AsyncHttpDispatchWrapper asyncHttpDispatchWrapper, ServletResponse servletResponse) throws ServletException, IOException {
        invokeTargetAsyncServlet(asyncHttpDispatchWrapper, asyncHttpDispatchWrapper, servletResponse);
    }

    private void invokeTargetAsyncServlet(HttpServletRequest invokeServletRequest, AsyncHttpDispatchWrapper asyncHttpDispatchWrapper, ServletResponse servletResponse) throws ServletException, IOException {
        invokeTargetAsyncServlet(invokeServletRequest, invokeServletRequest, asyncHttpDispatchWrapper, servletResponse);
    }

    private void invokeTargetAsyncServlet(ServletRequest invokeServletRequest, HttpServletRequest previousPathRequest, AsyncHttpDispatchWrapper asyncHttpDispatchWrapper, ServletResponse servletResponse) throws ServletException, IOException {
        // A typical call chain to arrive here is DefaultAsyncContext#dispatch -> DefaultAsyncDispatcher#dispatch -> forward -> asyncForwrd -> asyncHttpForward -> invokeTargetAsyncServlet

        if (path != null) {

            setAsyncAttributes(previousPathRequest, asyncHttpDispatchWrapper);

            asyncHttpDispatchWrapper.setServletPath(getServletPath(path));

            String queryString = getQueryString(path);
            if (queryString != null && !queryString.trim().equals("")) {
                asyncHttpDispatchWrapper.setQueryString(queryString);
                setRequestParameters(queryString, asyncHttpDispatchWrapper);
            } else {
                asyncHttpDispatchWrapper.setQueryString(previousPathRequest.getQueryString());
            }

            asyncHttpDispatchWrapper.setRequestURI(previousPathRequest.getServletContext().getContextPath() + getServletPath(path));
            asyncHttpDispatchWrapper.setAsWrapperAttribute("PREVIOUS_REQUEST", invokeServletRequest);

        } else {
            asyncHttpDispatchWrapper.setServletPath("/" + servletEnvironment.getServletName());
        }



        servletEnvironment.getWebApplication().linkRequestAndResponse(invokeServletRequest, servletResponse);
        servletEnvironment.getServlet().service(invokeServletRequest, servletResponse);
        servletEnvironment.getWebApplication().unlinkRequestAndResponse(invokeServletRequest, servletResponse);
    }

    private void setAsyncAttributes(HttpServletRequest asyncStartRequest, AsyncHttpDispatchWrapper asyncHttpDispatchWrapper) {
        for (String asyncAttribute : ASYNC_ATTRIBUTES) {
            // Set the spec demanded attributes on asyncHttpDispatchWrapper with the values taken from asyncStartRequest
            setAsyncAttribute(asyncStartRequest, asyncHttpDispatchWrapper, asyncAttribute);
        }
    }

    private void setAsyncAttribute(HttpServletRequest originalRequest, AsyncHttpDispatchWrapper asyncHttpDispatchWrapper, String dispatcherKey) {
        String value = null;

        if (originalRequest.getAttribute(dispatcherKey) != null) {
            value = (String) originalRequest.getAttribute(dispatcherKey);
        } else {
            if (dispatcherKey.equals(ASYNC_CONTEXT_PATH)) {
                value = originalRequest.getContextPath();
            }
            if (dispatcherKey.equals(ASYNC_PATH_INFO)) {
                value = originalRequest.getPathInfo();
            }
            if (dispatcherKey.equals(ASYNC_QUERY_STRING)) {
                value = originalRequest.getQueryString();
            }
            if (dispatcherKey.equals(ASYNC_REQUEST_URI)) {
                value = originalRequest.getRequestURI();
            }
            if (dispatcherKey.equals(ASYNC_SERVLET_PATH)) {
                value = originalRequest.getServletPath();
            }
        }

        asyncHttpDispatchWrapper.setAsWrapperAttribute(dispatcherKey, value);
    }

    private void setRequestParameters(String queryString, AsyncHttpDispatchWrapper asyncHttpDispatchWrapper) {
        try {
            Map<String, String[]> parameters = asyncHttpDispatchWrapper.getWrapperParameters();

            if (queryString != null) {
                for (String param : queryString.split("&")) {
                    String pair[] = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }
                    String[] values = parameters.get(key);
                    if (values == null) {
                        values = new String[]{value};
                        parameters.put(key, values);
                    } else {
                        String[] newValues = new String[values.length + 1];
                        System.arraycopy(values, 0, newValues, 0, values.length);
                        newValues[values.length] = value;
                        parameters.put(key, newValues);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

    }

    private String getServletPath(String path) {
        return !path.contains("?") ? path : path.substring(0, path.indexOf("?"));
    }

    private String getQueryString(String path) {
        return !path.contains("?") ? null : path.substring(path.indexOf("?") + 1);
    }

    private HttpServletRequest findHttpServletRequestInChain(ServletRequest request) {
        ServletRequest currentRequest = request;
        while (currentRequest instanceof ServletRequestWrapper) {
            ServletRequestWrapper wrapper = (ServletRequestWrapper) currentRequest;
            currentRequest = wrapper.getRequest();

            if (currentRequest instanceof HttpServletRequest) {
                return (HttpServletRequest) currentRequest;
            }
        }
        return null;
    }
}
