/*
 * Copyright (c) 2002-2019 Manorrock.com. All Rights Reserved.
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
package com.manorrock.piranha;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import com.manorrock.piranha.api.ResourceManager;
import com.manorrock.piranha.api.ResourceManagerClassLoader;

/**
 * The default WebApplicationClassLoader.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class DefaultResourceManagerClassLoader extends ClassLoader implements ResourceManagerClassLoader {

    /**
     * Stores the resource manager.
     */
    private ResourceManager resourceManager;

    private ClassLoader delegateClassLoader;

    /**
     * Stores the loaded classes.
     */
    private final ConcurrentHashMap<String, Class<?>> classes = new ConcurrentHashMap<>();

    /**
     * Constructor.
     */
    public DefaultResourceManagerClassLoader() {
        this(getSystemClassLoader());
    }
    
    /**
     * Another Constructor.
     * 
     * @param delegateClassLoader classloader which is consulted first
     */
    public DefaultResourceManagerClassLoader(ClassLoader delegateClassLoader) {
        this.delegateClassLoader = delegateClassLoader;
    }

    /**
     * Constructor.
     *
     * @param resourceManager the resource manager.
     */
    public DefaultResourceManagerClassLoader(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Load the class.
     *
     * @param name the name.
     * @param resolve the resolve flag.
     * @return the class.
     * @throws ClassNotFoundException when the class cannot be found.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result;
        try {
            result = delegateClassLoader.loadClass(name);
        } catch (ClassNotFoundException cnfe) {
            result = null;
        }
        if (result == null) {
            if (classes.containsKey(name)) {
                result = classes.get(name);
            } else {
                try {
                    // Define class
                    
                    String normalizedName = name.replaceAll("\\.", "/") + ".class";
                    BufferedInputStream inputStream = new BufferedInputStream(resourceManager.getResourceAsStream(normalizedName));
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    int read = inputStream.read();
                    while (read != -1) {
                        outputStream.write((byte) read);
                        read = inputStream.read();
                    }
                    byte[] bytes = outputStream.toByteArray();
                    result = defineClass(name, bytes, 0, bytes.length);
                    
                    // Define package
                    
                    String packageName = null;
                    int lastDotPosition = name.lastIndexOf('.');
                    if (lastDotPosition != -1) {
                        packageName = name.substring(0, lastDotPosition);
                    }

                    if (packageName != null) {
                        Package classPackage = getPackage(packageName);

                        if (classPackage == null) {
                            try {
                            definePackage(packageName, null, null, null, null, null, null, null);
                            } catch (IllegalArgumentException e) {
                                // Ignore, package already defined
                            }
                        }
                    }
                    
                    if (resolve) {
                        resolveClass(result);
                    }
                    classes.put(name, result);
                } catch (Throwable throwable) {
                    throw new ClassNotFoundException("Unable to load class: " + name, throwable);
                }
            }
        }
        return result;
    }

    /**
     * Find the resource.
     *
     * @param name the name.
     * @return the resource, or null if not found.
     */
    @Override
    protected URL findResource(String name) {
        URL result = null;
        try {
            result = resourceManager.getResource(name);
        } catch (MalformedURLException mue) {
        }
        return result;
    }

    /**
     * Find the resources.
     *
     * @param name the name of the resource.
     * @return the enumeration of the resource urls.
     * @throws IOException when an I/O error occurs.
     */
    @Override
    protected Enumeration<URL> findResources(String name) throws IOException  {
        return Collections.enumeration(resourceManager.getResources(name));
    }

    /**
     * Set the resource manager.
     *
     * @param resourceManager the resource manager.
     */
    @Override
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    @Override
    public ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    public void setDelegateClassLoader(ClassLoader delegateClassLoader) {
        this.delegateClassLoader = delegateClassLoader;
    }
    
    public ClassLoader getDelegateClassLoader() {
        return delegateClassLoader;
    }
}
