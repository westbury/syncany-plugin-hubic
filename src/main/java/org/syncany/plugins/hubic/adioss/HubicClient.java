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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.syncany.api.transfer.StorageException;

public class HubicClient {

	private final HttpAPI httpAPI = new HttpAPI();

	private final String token;

	private final String endpoint;


    public HubicClient(String endpoint, String token) {
		this.token = token;
		this.endpoint = endpoint;
	}

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default?format=json -i -X GET
     *
     * @param path of the directory
     * @return a {@link Response}
     * @throws StorageException 
     */
	public Response listDirectory(String path) throws IOException {
        return httpAPI.query(endpoint + "/default?path=" + path + "&format=json")
        		.jsonArray()
                .header("X-Auth-Token", token)
        		.get();
	}

	/**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -H "Content-Length: 0" -H "Content-Type: application/directory" ENDPOINT_URL/default{path} -i -X PUT
     *
     * @param path of the new directory
     * @return a {@link Response}
	 * @throws IOException 
     */
    public Response createDirectory(String path) throws IOException {
        return httpAPI.query(endpoint + "/default/" + path)
                .header("X-Auth-Token", token)
                .header("Content-Length", "0")
                .header("Content-Type", "application/directory")
                .put();
    }

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -T "/home/adio/Bureau/README.md" ENDPOINT_URL/default/titi/README.md -i -X PUT
     *
     * @param source     {@link Path} of the source file to upload
     * @param targetPath target path
     * @return a {@link Response}
     */
	public Response upload(InputStream source, String targetPath) throws IOException {
		return httpAPI.query(endpoint + "/default/" + targetPath)
				.header("X-Auth-Token", token)
				.put(source);
	}

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default/titi/README.md -i -X DELETE
     *
     * @param path of the element to delete
     * @return a {@link Response}
     */
	public Response delete(String path) throws IOException {
		return httpAPI.query(endpoint + "/default/" + path)
				.header("X-Auth-Token", token)
				.delete();
	}

    /**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" ENDPOINT_URL/default/Documents/zap.sh -i -X GET -o zap.sh
     *
     * @param source of the element to delete
     * @param target output path
     * @return a {@link Response}
     */
	public Response download(String source, File localFile) throws IOException {
		return httpAPI.query(endpoint + "/default/" + source)
				.binary(localFile.toPath())
				.header("X-Auth-Token", token)
				.get();
	}

	/**
     * curl -H "X-Auth-Token: YOUR_AUTH_TOKEN" -H "Content-Length: 0" -H "X-Copy-From: default/Documents/zap.sh" ENDPOINT_URL/default/Documents/titi/zap.sh -i -X PUT
     *
     * @param sourcePath of the element to copy
     * @param targetPath of the copied element
     * @return a {@link Response}
     * @throws IOException 
     */
    public Response copy(String sourcePath, String targetPath) throws IOException {
        return httpAPI.query(endpoint + "/default/" + targetPath)
                .header("X-Auth-Token", token)
                .header("X-Copy-From", "default/" + sourcePath)
                .header("Content-Length", "0")
                .put();
    }
}
