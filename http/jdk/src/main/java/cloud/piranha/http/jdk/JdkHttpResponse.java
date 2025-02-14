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
package cloud.piranha.http.jdk;

import cloud.piranha.http.api.HttpServerResponse;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The JDK HttpServer variant of a HttpServerResponse.
 * 
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class JdkHttpResponse implements HttpServerResponse {
    
    /**
     * Stores the HTTP exchange.
     */
    private HttpExchange exchange;
    
    /**
     * Stores the status.
     */
    private int status;

    /**
     * Constructor.
     * 
     * @param exchange the HTTP exchange.
     */
    public JdkHttpResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void addHeader(String name, String value) {
        exchange.getResponseHeaders().add(name, value);
    }

    @Override
    public String getHeader(String name) {
        return exchange.getResponseHeaders().getFirst(name);
    }

    @Override
    public OutputStream getOutputStream() {
        return exchange.getResponseBody();
    }

    @Override
    public void setHeader(String name, String value) {
        exchange.getResponseHeaders().set(name, value);
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public void writeHeaders() throws IOException {
        exchange.sendResponseHeaders(status, 0);
    }

    @Override
    public void writeStatusLine() throws IOException {
        // writing the status line is taken care of when writing out the headers.
    }
}
