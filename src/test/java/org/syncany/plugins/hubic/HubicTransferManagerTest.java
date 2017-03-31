/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.syncany.plugins.tests.AbstractTransferManagerTest;

/**
 * @author Vincent Wiencek <vwiencek@gmail.com>
 */
public class HubicTransferManagerTest extends AbstractTransferManagerTest {
	@BeforeClass
	public static void beforeTestSetup() throws Exception {
//		EmbeddedTestFtpServer.startServer();
	}
	
	@AfterClass
	public static void stop(){
//		EmbeddedTestFtpServer.stopServer();
	}
	
	@Override
	public Map<String, String> createPluginSettings() {
		Map<String, String> pluginSettings = new HashMap<String, String>();
		
		pluginSettings.put("accessToken", "4876fxYXiO44nA4ITngzIQfpxEdjfoaSKGpf97B7Nl2JVSa9Q6Kb45KFRzJDJwU9");

		return pluginSettings;
	}

	@Override
	public String getPluginId() {
		return "hubic";
	}
}
