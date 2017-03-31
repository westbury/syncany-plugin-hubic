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

import org.syncany.api.transfer.TransferPlugin;
import org.syncany.api.transfer.TransferSettings;

public class HubicTransferPlugin implements TransferPlugin {

	@Override
	public String getId() {
		return "hubic";
	}

	@Override
	public String getName() {
		return "Hubic";
	}

	@Override
	public String getVersion() {
		return "0.5.0-alpha";
	}

	@Override
	public TransferSettings createEmptySettings() {
		return new HubicTransferSettings();
	}

}
