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
package cloud.piranha.extension.security.jakarta;

import java.util.Set;

import cloud.piranha.extension.security.servlet.ServletSecurityAllInitializer;
import cloud.piranha.extension.soteria.SoteriaInitializer;
import cloud.piranha.extension.soteria.SoteriaPreCDIInitializer;
import cloud.piranha.extension.weld.WeldInitializer;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * The Jakarta Security All initializer.
 *
 * @author Arjan Tijms
 */
public class JakartaSecurityAllInitializer implements ServletContainerInitializer {

    /**
     * Stores the initializers.
     */
    ServletContainerInitializer[] initializers = {

        // Makes web.xml login-config available to Soteria
        new SoteriaPreCDIInitializer(),
        
        // Initializes the Servlet Security primitives, on which Jakarta Security is based
        new ServletSecurityAllInitializer(),

        // Initializes CDI, on which Jakarta Security is also based
        new WeldInitializer(),

        // Configures Soteria, which implements Jakarta Security and sets itself as a Servlet 
        // authentication module
        new SoteriaInitializer(),
    };

    /**
     * Initialize Jakarta Security
     *
     * @param classes the classes.
     * @param servletContext the Servlet context.
     * @throws ServletException when a Servlet error occurs.
     */
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        for (ServletContainerInitializer initializer : initializers) {
            initializer.onStartup(classes, servletContext);
        }
    }
}
