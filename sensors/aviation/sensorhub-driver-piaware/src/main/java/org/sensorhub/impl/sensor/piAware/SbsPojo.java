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
	String flightID; // Database Flight record number
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

}
