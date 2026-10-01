/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/package org.sensorhub.impl.sensor.piAware;

public class AircraftJson {
	Double now;
	Integer messages;
	Aircraft[] aircraft;

	class Aircraft {
		String hex;		
		String flight; // This corresponds to callSign- not to be confused with flightId in the SBS format.    
		String category;
		long lastMessage;
	}

}
