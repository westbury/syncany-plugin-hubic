/*
 * Syncany, www.syncany.org
 * Copyright (C) 2017 Nigel Westbury
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.hubic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.syncany.api.transfer.LocalDiskCache;
import org.syncany.api.transfer.PropertyVisitor;
import org.syncany.api.transfer.StorageException;
import org.syncany.api.transfer.TransferManager;
import org.syncany.api.transfer.TransferSettings;
import org.syncany.plugins.hubic.adioss.HttpAPI;
import org.syncany.plugins.hubic.adioss.Response;

import com.google.gson.JsonObject;

public class HubicTransferSettings implements TransferSettings {

	/**
	 * the client id, provided by Hubic when the syncany.org
	 * application was registered
	 */
	private static final String clientId = "api_hubic_0caV8Zn0vrTS8nLYu7W6wVMvYyuIkuPc";

	/**
	 * the redirect URL, registered with Hubic when the syncany.org
	 * application was registered so do not change this unless re-registering
	 * with Hubic
	 */
	private static final String redirectUrl = "https://www.syncany.org/oauth/";

	private String token;

	private String endpoint;

	private String getAccessToken() {
		return "this is transient";
	}

	private void setAccessToken(String accessToken) {
		if (accessToken.equals("this is transient")) {
			// In this case endpoint and token should have been separately
			// set as this is being read back from saved properties.
			return;
		}

		try {
			HttpAPI httpAPI = new HttpAPI();

			String authorization = "Bearer " + accessToken;
			Response r = httpAPI.query("https://api.hubic.com/1.0/account/credentials")
					.header("Authorization", authorization)
					.json()
					.get();

			JsonObject postContent = (JsonObject) r.getContent();

			if (r.getCode() == 401) {
				// The code has expired.
				// The user must manually authenticate again.
				throw new RuntimeException("Authentication has expired.  The user must manually get a new token.");
			}

			token = postContent.get("token").getAsString();
			endpoint = postContent.get("endpoint").getAsString();
		} catch (IOException e) {
			// TODO doing this in the setter is not good.
			// We need to think about how this interaction
			// should take place.
			throw new RuntimeException("fetching of token failed", e);
		}
	}

	private String getToken() {
		return token;
	}

	private void setToken(String token) {
		this.token = token;
	}

	private String getEndpoint() {
		return endpoint;
	}

	private void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public void visitProperties(PropertyVisitor visitor) {
		String accessTokenMessage = buildAccessTokenMessage();

		visitor.stringProperty("accessToken", accessTokenMessage, true, true, true, true, true, this::getAccessToken, this::setAccessToken);
		visitor.stringProperty("token", "Token", true, true, true, true, false, this::getToken, this::setToken);
		visitor.stringProperty("endpoint", "Endpoint", true, true, true, true, false, this::getEndpoint, this::setEndpoint);
	}

	private String buildAccessTokenMessage() {
		String scope = "account.r,credentials.r,getAllLinks.r,links.drw";

		try {
			String encodedRedirectUrl = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8.toString());

			String url = "https://api.hubic.com/oauth/auth/?" +
					"client_id=" + clientId +
					"&redirect_uri=" + encodedRedirectUrl +
					"&scope=" + scope +
					"&response_type=token" +
					"&state=RandomString_" + UUID.randomUUID();

			//        	try {
			//        		if(Desktop.isDesktopSupported())   	{
			//        			Desktop.getDesktop().browse(new URI(url));
			//        		}
			//        	} catch (IOException e) {
			//        		// Ignore if we can't open a browser.
			//        	}

			return "Go to this URL, authorize, then copy the access token from the returned URL and paste here: " + url;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Impossible to encode redirect URL.", e);
			//		} catch (URISyntaxException e) {
			//            throw new RuntimeException("Internal URI error", e);
		}

	}

	@Override
	public TransferManager createTransferManager(LocalDiskCache cache) throws StorageException {
		if (token == null || endpoint == null) {
			// The access token was not set, or the token and endpoint could not be obtained
			// from the access token.
			throw new StorageException("Cannot create Hubic transfer manager because the access token was not successfully set.");
		}
		return new HubicTransferManager(token, endpoint);
	}

	@Override
	public String getType() {
		return "hubic";
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getReasonForLastValidationFail() {
		// TODO Auto-generated method stub
		return null;
	}

}
