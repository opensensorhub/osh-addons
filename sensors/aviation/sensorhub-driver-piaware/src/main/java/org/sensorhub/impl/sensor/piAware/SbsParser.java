package org.sensorhub.impl.sensor.piAware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.sensorhub.impl.sensor.piAware.SbsPojo.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author tcook
 *
 *		SBS Format information:
 *         http://woodair.net/sbs/article/barebones42_socket_data.htm
 *
 */

public class SbsParser 
{
	Logger logger;
	@Deprecated // Don't think I need this
	Map<String, String>  flightNums = new HashMap<>();  // <hexIdent, flightNumber>
	
	public SbsParser() {
		logger = LoggerFactory.getLogger(SbsParser.class);
	}
	
	public SbsParser(Logger logger) {
		this.logger = logger;
	}
	
	public SbsPojo parse(String inline) throws IOException {
		try {
			SbsPojo rec = new SbsPojo();
			String[] vals = inline.split(",", -1);

			rec.messageType = MessageType.valueOf(vals[0]);
			if (rec.messageType == MessageType.MSG) {
				rec.transmissionType = Integer.parseInt(vals[1]);
				parseCommonFields(rec, vals);
				rec.timeMessageGenerated = dateTimeToUtc(rec.dateMessageGeneratedStr, rec.timeMessageGeneratedStr);
				rec.timeMessageLogged = dateTimeToUtc(rec.dateMessageLoggedStr, rec.timeMessageLoggedStr);
				switch (rec.transmissionType) {
				case 1:
					rec.callsign = vals[10].trim();
					logger.trace("Parser Message 1: callsign: {}", rec.callsign );
					if(!flightNums.containsKey(rec.hexIdent) || flightNums.get(rec.hexIdent) == null) {
						flightNums.put(rec.hexIdent, rec.callsign);
					}
					break;
				case 2:
					rec.altitude = parseDouble(vals[11]);
					rec.groundSpeed = parseDouble(vals[12]);
					rec.track = parseDouble(vals[13]);
					rec.latitude = parseDouble(vals[14]);
					rec.longitude = parseDouble(vals[15]);
					rec.isOnGround = parseFlag(vals[21]);
					break;
				case 3:
					rec.altitude = parseDouble(vals[11]);
					rec.latitude = parseDouble(vals[14]);
					rec.longitude = parseDouble(vals[15]);
					rec.squawkChange = parseFlag(vals[18]);
					rec.emergency = parseFlag(vals[18]);
					rec.spiIdent = parseFlag(vals[19]);
					rec.isOnGround = parseFlag(vals[21]);
					break;
				case 4:  //  speed and track are always subType == 4 based on my obs
					rec.groundSpeed = parseDouble(vals[12]);
					rec.track = parseDouble(vals[13]);
					rec.verticalRate = parseDouble(vals[16]);
					break;
				case 5:
					rec.altitude = parseDouble(vals[11]);
					rec.squawkChange = parseFlag(vals[18]);
					rec.spiIdent = parseFlag(vals[19]);
					rec.isOnGround = parseFlag(vals[21]);
					break;
				case 6:
					rec.altitude = parseDouble(vals[11]);
					rec.squawk = vals[17];
					break;
				case 7:
					rec.altitude = parseDouble(vals[11]);
					rec.isOnGround = parseFlag(vals[21]);
					break;
				case 8:
					rec.isOnGround = parseFlag(vals[21]);
					break;
				default:
					logger.debug("MsgType not recognized: {}", rec.transmissionType);
					logger.debug(inline);
				}
				return rec;
			}
			// TODO- support other MessageTypes, but not seeing any other types in PiAware feed
		} catch (Exception e) {
			throw new IOException(e);
		}
		return null;
	}

	public static boolean parseFlag(String s) {
		 return "1".equals(s) ? true: false;
	}
	
	public static Double parseDouble(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return null;
		}
		
	}
	
	public static void parseCommonFields(SbsPojo rec, String[] vals) {
		rec.sessionId = Integer.parseInt(vals[2]);
		rec.aircraftId = Integer.parseInt(vals[3]);
		rec.hexIdent = vals[4];
//		rec.flightID = vals[5];  // always 1 in SBS messages.  Override to use Flight number when available
		rec.dateMessageGeneratedStr = vals[6];
		rec.timeMessageGeneratedStr = vals[7];
		rec.dateMessageLoggedStr = vals[8];
		rec.timeMessageLoggedStr = vals[9];
		rec.callsign = vals[10]; // ???
	}
	
	public static long dateTimeToUtc(String date, String time) {
		String isoTime = date.replaceAll("/", "-") + "T" + time + "Z";
		long utcTime = Instant.parse(isoTime).toEpochMilli();
		return utcTime;
	}
	
	public static void main(String[] args) {
		SbsParser parser = new SbsParser();
		PiAwareConfig config = new PiAwareConfig();
		try (Socket socket = new Socket("192.168.1.126", config.sbsOutboundPort);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

			String line = null;
			do  {
				try {
					line = in.readLine();
					//System.err.println(line);
					SbsPojo rec = parser.parse(line);
					if(rec.transmissionType == 3 ||  rec.transmissionType ==4)
						System.err.println("TType = " + rec.transmissionType + ", " + rec.hexIdent);
//					System.err.println(rec.callsign + "," + rec.dateMessageGeneratedStr + "," + rec.timeMessageGeneratedStr);
					//if(rec.hexIdent.startsWith("A62F"))
//						System.err.println(line);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (line != null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
