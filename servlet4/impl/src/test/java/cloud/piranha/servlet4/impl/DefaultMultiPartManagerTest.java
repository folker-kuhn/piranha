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
package cloud.piranha.servlet4.impl;

import cloud.piranha.servlet4.impl.DefaultWebApplicationRequest;
import cloud.piranha.servlet4.impl.DefaultWebApplication;
import cloud.piranha.servlet4.impl.DefaultMultiPartManager;
import java.util.Collection;
import javax.servlet.http.Part;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * The JUnit tests for the DefaultMultiPartManager.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
class DefaultMultiPartManagerTest {

    /**
     * Test getParts method.
     * 
     * @throws Exception when a serious error occurs.
     */
    @Test
    void testGetParts() throws Exception {
        DefaultWebApplication webApplication = new DefaultWebApplication();
        DefaultMultiPartManager manager = new DefaultMultiPartManager();
        DefaultWebApplicationRequest request = new DefaultWebApplicationRequest() {
        };
        request.setContentType("multipart/form-data; boundary=------------------------12345");
        Collection<Part> result = manager.getParts(webApplication, request);
        assertTrue(result.isEmpty());
    }

    /**
     * Test getPart method.
     * 
     * @throws Exception when a serious error occurs.
     */
    @Test
    void testGetPart() throws Exception {
        DefaultWebApplication webApplication = new DefaultWebApplication();
        DefaultMultiPartManager manager = new DefaultMultiPartManager();
        DefaultWebApplicationRequest request = new DefaultWebApplicationRequest() {
        };
        request.setContentType("multipart/form-data; boundary=------------------------12345");
        assertNull(manager.getPart(webApplication, request, "notfound"));
    }
}
