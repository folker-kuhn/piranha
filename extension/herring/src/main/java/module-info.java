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

/**
 * <p>
 *  This module delivers the integration of Manorrock Herring into Piranha.
 * </p>
 * <p>
 *  It includes the following:
 * </p>
 * <ul>
 *  <li>A WebApplicationExtension</li>
 *  <li>A ServletContextListener</li>
 *  <li>A ServletRequestListener</li>
 * </ul>
 * <h2>The WebApplicationExtension</h2>
 * <p>
 *  The extension is responsible for setting up the proper Context instance so
 *  it can be made available during webapplication initialization and 
 *  subsequently during request processing.
 * </p>
 * <h2>The ServletContextListener</h2>
 * <p>
 *  This listener is responsible for the corner case of removing the Context
 *  set by the WebApplicationExtension and it signals the end of initialization.
 * </p>
 * <h2>The ServletRequestListener</h2>
 * <p>
 *  This listener is responsible for making the correct Context instance 
 *  available on the current thread just before the request gets serviced and to
 *  remove the Context instance from the current thread at the end of the
 *  request.
 * </p>
 * 
 * @author Manfred Riem (mriem@manorrock.com)
 */
module cloud.piranha.extension.herring {
    exports cloud.piranha.extension.herring;
    opens cloud.piranha.extension.herring;
    requires cloud.piranha.webapp.api;
    requires transitive com.manorrock.herring;
    requires transitive com.manorrock.herring.thread;
    requires transitive java.naming;
}
