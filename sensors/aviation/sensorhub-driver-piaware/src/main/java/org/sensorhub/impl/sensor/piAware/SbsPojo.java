/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.piAware;

public class SbsPojo {
	enum MessageType {
		MSG, SEL, ID, AIR, STA, CLK
	}

	MessageType messageType;
	int transmissionType;

	Integer sessionId;  //  always 1 ?
	Integer aircraftId; //  always 1 ?
	String hexIdent; // Aircraft Mode S hexadecimal code
	String flightID; // taken from aircraft.json, not SbsParser
	String category; // taken from aircraft.json, not SbsParser
	String dateMessageGeneratedStr;
	String timeMessageGeneratedStr;
	String dateMessageLoggedStr;
	String timeMessageLoggedStr;

	Long timeMessageGenerated;
	Long timeMessageLogged;
	
	String callsign; // An eight digit flight ID - can be flight number or registration (or even
						// nothing).
	Double altitude; // Mode C altitude. Height relative to 1013.2mb (Flight Level). Not height
						// AMSL..
	Double groundSpeed; // Speed over ground (not indicated airspeed)
	Double track; // Track of aircraft (not heading). Derived from the velocity E/W and velocity
					// N/S
	Double latitude; // North and East positive. South and West negative.
	Double longitude; // North and East positive. South and West negative.
	Double verticalRate; // 64ft resolution
	String squawk; // Mode A squawk code.
	Boolean squawkChange; // Flag to indicate squawk has changed.
	Boolean emergency; // Flag to indicate emergency code has been set
	Boolean spiIdent; // Flag to indicate transponder Ident has been activated.
	Boolean isOnGround; // Flag to indicate ground squat switch is active

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("messageTyoe: " +  messageType + "\n");
		b.append("transmissionType: " + transmissionType + "\n");
		b.append("sessionId: " + sessionId + "\n");
		b.append("aircraftId: " + aircraftId + "\n");
		b.append("hexIdent: " + hexIdent + "\n");
		b.append("flightID: " + flightID + "\n");
		b.append("dateMessageStr: " + dateMessageGeneratedStr + "\n");
		b.append("timeMessageStr: " + timeMessageGeneratedStr + "\n");
		b.append("callsign: " + callsign + "\n");
		b.append("altitude: " + altitude + "\n");
		b.append("groundSpeed: " + groundSpeed + "\n");
		b.append("track: " + track + "\n");
		b.append("latitude: " + latitude + "\n");
		b.append("longitude: " + longitude + "\n");
		b.append("altitude: " + altitude + "\n");
		b.append("isOnGround: " + isOnGround + "\n");
		b.append("verticalRate: " + verticalRate + "\n");
		b.append("spiIdent: " + spiIdent + "\n");
		b.append("squawk: " + squawk + "\n");
		b.append("squawkChange: " + squawkChange + "\n");
		b.append("emergency: " + emergency + "\n");
		return b.toString();
	}
	
}
