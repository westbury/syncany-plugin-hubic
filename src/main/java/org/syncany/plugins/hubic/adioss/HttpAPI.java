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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonParser;

public class HttpAPI {
    private URL url;
    private Map<String, String> headers;
    private Map<String, String> urlParameters;
    private Map<String, String> postData;
    private Output output;
    private Path outputPath;


    public HttpAPI query(String url) {
        try {
            this.url = new URL(url);
            this.headers = new HashMap<>();
            this.urlParameters = new HashMap<>();
            this.postData = new HashMap<>();
            this.output = Output.PLAIN;
            this.outputPath = null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return this;
    }

    public HttpAPI header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }


    public HttpAPI urlParameter(String key, String value) {
        this.urlParameters.put(key, value);
        return this;
    }


    public HttpAPI postData(String key, String value) {
        this.postData.put(key, value);
        return this;
    }

    public HttpAPI plain() {
        this.output = Output.PLAIN;
        return this;
    }

    public HttpAPI binary(Path target) {
        outputPath = target;
        this.output = Output.BINARY;
        return this;
    }

    public HttpAPI json() {
        this.output = Output.JSON;
        return this;
    }

    public HttpAPI jsonArray() {
        this.output = Output.JSON_ARRAY;
        return this;
    }


    // HTTP GET request
    public Response get() throws IOException {
        return basicHttpQuery("GET");
    }

    // HTTP HEAD request
    public Response head() throws IOException {
        return basicHttpQuery("HEAD");
    }

    // HTTP DELETE request
    public Response delete() throws IOException {
        return basicHttpQuery("DELETE");
    }

    // HTTP PUT request
    public Response put() throws IOException {
        return put((Path)null);
    }

    public Response put(Path sourcePath) throws IOException {
    	HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
    	httpsURLConnection.setRequestMethod("PUT");
    	appendUrlParameter(httpsURLConnection);
    	appendHeaders(httpsURLConnection);
    	httpsURLConnection.setDoOutput(true);
    	if (sourcePath != null) {
    		try (BufferedOutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
    				BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourcePath.toFile()))) {
    			int i;
    			while ((i = inputStream.read()) >= 0) {
    				outputStream.write(i);
    			}
    		}
    	} else {
    		httpsURLConnection.setFixedLengthStreamingMode(0);
    	}

    	return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
    }

    public Response put(InputStream inputStream) throws IOException {
    	HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
    	httpsURLConnection.setRequestMethod("PUT");
    	appendUrlParameter(httpsURLConnection);
    	appendHeaders(httpsURLConnection);
    	httpsURLConnection.setDoOutput(true);
    	try (BufferedOutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream())) {
    		int i;
    		while ((i = inputStream.read()) >= 0) {
    			outputStream.write(i);
    		}
    	}

    	return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
    }

    private Response basicHttpQuery(String verb) throws IOException {
    	HttpsURLConnection httpsURLConnection = (HttpsURLConnection) this.url.openConnection();
    	httpsURLConnection.setRequestMethod(verb);
    	appendUrlParameter(httpsURLConnection);
    	appendHeaders(httpsURLConnection);
    	return new Response(httpsURLConnection.getResponseCode(), httpsURLConnection.getHeaderFields(), getResponseContent(httpsURLConnection));
    }

    private Object getResponseContent(HttpsURLConnection httpsURLConnection) throws IOException {
        if (this.output == Output.BINARY) {
        	try (InputStream inputStream = httpsURLConnection.getInputStream();
        			FileOutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
        		int bytesRead = -1;
        		byte[] buffer = new byte[1024];
        		while ((bytesRead = inputStream.read(buffer)) != -1) {
        			outputStream.write(buffer, 0, bytesRead);
        		}
        	} catch (IOException e) {
        		// If this method is called for a failure response, we get here.
        		// Ideally the code should be checked before we attempt to get the content
        		// but for time being just return null content.
        		return null;
        	}
            return outputPath;
        } else {
        	try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()))) {
        		String inputLine;
        		StringBuilder response = new StringBuilder();
        		while ((inputLine = bufferedReader.readLine()) != null) {
        			response.append(inputLine);
        		}
        		bufferedReader.close();
        		return convertResult(response);
        	} catch (IOException e) {
        		// If this method is called for a failure response, we get here.
        		// Ideally the code should be checked before we attempt to get the content
        		// but for time being just return null content.
        		return null;
        	}
        }
    }

    private void appendHeaders(HttpsURLConnection httpsURLConnection) {
        for (String key : this.headers.keySet()) {
            httpsURLConnection.setRequestProperty(key, this.headers.get(key));
        }
    }

    private void appendUrlParameter(HttpsURLConnection httpsURLConnection) {
        for (String key : this.urlParameters.keySet()) {
            httpsURLConnection.setRequestProperty(key, this.urlParameters.get(key));
        }
    }

    private Object convertResult(StringBuilder response) {
        switch (this.output) {
            case JSON: {
                JsonParser parser = new JsonParser();
                return parser.parse(response.toString());
            }
            case JSON_ARRAY: {
                JsonParser parser = new JsonParser();
                return parser.parse(response.toString()).getAsJsonArray();
            }
            default: {
                return response.toString();
            }

        }
    }

    private enum Output {
        PLAIN, JSON, JSON_ARRAY, BINARY
    }
}
