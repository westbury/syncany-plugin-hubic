/*
 * This file is based on the HubicPOC project found at 
 * https://github.com/adioss/HubicPOC.  This file is licensed under
 * Apache 2.0 even though Syncany as a whole is licensed under GPL 3.
 * 
 * Copyright 2016 Adrien Pailhes
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncany.plugins.hubic.adioss;

import java.util.List;
import java.util.Map;


public class Response {
    private final int code;
    private final Map<String, List<String>> headers;
    private final Object content;

    public Response(int code, Map<String, List<String>> headers, Object content) {
        this.code = code;
        this.headers = headers;
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Object getContent() {
        return content;
    }
}
