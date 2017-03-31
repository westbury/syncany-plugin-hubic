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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.syncany.api.transfer.RemoteFile;
import org.syncany.api.transfer.RemoteFileFactory;
import org.syncany.api.transfer.StorageException;
import org.syncany.api.transfer.TransferManager;
import org.syncany.api.transfer.features.PathAwareRemoteFileType;
import org.syncany.plugins.hubic.adioss.HubicClient;
import org.syncany.plugins.hubic.adioss.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class HubicTransferManager implements TransferManager {

	private final HubicClient hubicClient;
	
	private String repoPath;

	private final String multichunksPath;
	private final String databasesPath;
	private final String actionsPath;
	private final String transactionsPath;
	private final String temporaryPath;

	public HubicTransferManager(String token, String endpoint) {
		hubicClient = new HubicClient(endpoint, token);
		
		this.repoPath = "";
		if (!repoPath.isEmpty() && !repoPath.endsWith("/")) repoPath = repoPath + "/"; //.substring(0, repoPath.length()-1);

		this.multichunksPath = repoPath + "multichunks/";
		this.databasesPath = repoPath + "databases/";
		this.actionsPath = repoPath + "actions/";
		this.transactionsPath = repoPath + "transactions/";
		this.temporaryPath = repoPath + "temporary/";
	}

	@Override
	public void connect() throws StorageException {
		// Nothing to do here for HTTP protocol
	}

	@Override
	public void disconnect() throws StorageException {
		// Nothing to do here for HTTP protocol
	}

	@Override
	public void init(boolean createIfRequired, RemoteFile syncanyRemoteFile) throws StorageException {
		try {
			if (!testRepoFileExists(syncanyRemoteFile) && createIfRequired) {
				hubicClient.createDirectory(repoPath);
			}

			hubicClient.createDirectory(multichunksPath);
			hubicClient.createDirectory(databasesPath);
			hubicClient.createDirectory(actionsPath);
			hubicClient.createDirectory(transactionsPath);
			hubicClient.createDirectory(temporaryPath);
		}
		catch (IOException e) {
			throw new StorageException("Cannot create directory " + multichunksPath + ", or " + databasesPath, e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		try {
			String source = getRemoteFile(remoteFile);
			Response response = hubicClient.download(source, localFile);
			if (response.getCode() != 200) {
				throw new StorageException("download failed");
			}
		} catch (IOException e) {
			throw new StorageException("download failed", e);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		try {
			InputStream source = new FileInputStream(localFile);
			String targetPath = getRemoteFile(remoteFile);
			Response response = hubicClient.upload(source, targetPath);
			if (response.getCode() != 201) {
				throw new StorageException("upload failed");
			}
		} catch (IOException e) {
			throw new StorageException("upload failed", e);
		}
	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {
		String sourcePath = getRemoteFile(sourceFile);
		String targetPath = getRemoteFile(targetFile);

		try {
			Response copyResponse = hubicClient.copy(sourcePath, targetPath);
			if (copyResponse.getCode() != 201) {
				throw new StorageException("Unable to copy, code = " + copyResponse.getCode());
			}

			Response deleteResponse = hubicClient.delete(sourcePath);
			if (deleteResponse.getCode() != 200 && deleteResponse.getCode() != 204) {
				throw new StorageException("Unable to delete, code = " + deleteResponse.getCode());
			}
		} catch (IOException e) {
			throw new StorageException("move failed", e);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		try {
			String tempRemoteFilePath = getRemoteFile(remoteFile);

			Response response = hubicClient.delete(tempRemoteFilePath);
			if (response.getCode() == 200) {
				return true;
			} else if (response.getCode() == 204) {
				// No idea what causes this code, but the delete seems to happen
				// when we get this code.
				return true;
			} else if (response.getCode() == 404) {
				// The file did not exist anyway, so this is success
				return true;
			} else {
				return false;
			}
		}
		catch (Exception e) {
			throw new StorageException("Unexpected http error", e);
		}
	}

	@Override
	public <T extends RemoteFile> Collection<T> list(PathAwareRemoteFileType remoteFileType,
			RemoteFileFactory<T> factory) throws StorageException {
		try {
			// List folder
			String remoteFilePath = getRemoteFilePath(remoteFileType);
			JsonArray fileArray = listDirectory(remoteFilePath);

			Set<T> remoteFiles = new HashSet<T>();

			for (JsonElement fileElement : fileArray) {
				JsonObject fileObject = (JsonObject)fileElement;
				String fileName = fileObject.get("name").getAsString();
				String contentType = fileObject.get("content_type").getAsString();
				if (contentType.equals("application/octet-stream")) {
					if (!fileName.startsWith(remoteFilePath)) {
						throw new StorageException("unexpected file directory");
					}
					String simpleFileName = fileName.substring(remoteFilePath.length());
					T remoteFile = factory.createRemoteFile(simpleFileName);
					remoteFiles.add(remoteFile);
				}
			}

			return remoteFiles;
		}
		catch (Exception ex) {
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		// If we can get credentials then presumably the target must exist
		return true;
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		try {
			String tempRemoteFilePath = "/syncany-write-test";

			InputStream source = new ByteArrayInputStream(new byte[] { 0x01, 0x02, 0x03 });
			Response r = hubicClient.upload(source, tempRemoteFilePath);

			if (r.getCode() == 201) {
				hubicClient.delete(tempRemoteFilePath);
				return true;
			}
			else {
				return false;
			}
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean testRepoFileExists(RemoteFile repoFile) throws StorageException {
		try {
			String repoFilePath = getRemoteFile(repoFile);
			
			String repoFileParentPath = (repoFilePath.indexOf("/") != -1) ? repoFilePath.substring(0, repoFilePath.lastIndexOf("/")) : "";
			JsonArray fileArray = listDirectory(repoFileParentPath);
			for (JsonElement fileElement : fileArray) {
				JsonObject fileObject = (JsonObject)fileElement;
				String fileName = fileObject.get("name").getAsString();
				String contentType = fileObject.get("content_type").getAsString();
				if (fileName.equals(repoFile.getName())) {
					return true;
				}
			}

			return false;				
		}
		catch (Exception e) {
			return false;
		}
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getPathAwareType()) + remoteFile.getName();
	}

	private JsonArray listDirectory(String path) throws StorageException {
		try {
			Response response = hubicClient.listDirectory(path);
			if (response.getCode() == 200) {
				return (JsonArray)response.getContent();
			} else {
				throw new StorageException("Failed to get directory listing for " + path + ", error code = " + response.getCode());
			}
		} catch (IOException e) {
			throw new StorageException("list directory failed", e);
		}
	}

	@Override
	public String getRemoteFilePath(PathAwareRemoteFileType remoteFileType) {
		switch (remoteFileType) {
		case Multichunk: 
			return multichunksPath;
		case Database:
		case Cleanup:
			return databasesPath;
		case Action:
			return actionsPath;
		case Transaction:
			return transactionsPath;
		case Temp:
			return temporaryPath;
		default:
			return repoPath;
		}
	}

}
