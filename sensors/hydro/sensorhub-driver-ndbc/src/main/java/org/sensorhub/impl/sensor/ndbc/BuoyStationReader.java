package org.sensorhub.impl.sensor.ndbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Deprecate this class if it is not providing useful info
public class BuoyStationReader {
	static String stationsFile = "https://www.ndbc.noaa.gov/activestations.xml";

	static public List<BuoyStationRecord> readStations(String stationUrl) throws IOException {
		List<BuoyStationRecord> buoys = new ArrayList<>();

		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			URL url = new URL(stationUrl);
			InputStream is = url.openStream();
			Document doc = builder.parse(is);
//			Document doc = builder.parse(new File("src/test/resources/example_jdom.xml"));
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("station");
			int numBuoys = nodeList.getLength();
			for (int i = 0; i < numBuoys; i++) {
				Node n = nodeList.item(i);
				NamedNodeMap attrList = n.getAttributes();
				Node id = attrList.getNamedItem("id");
				// <station id="21413" lat="30.514" lon="152.123" elev="0"
				// name="SOUTHEAST TOKYO - 700NM ESE of Tokyo, JP"
				// owner="NDBC" pgm="Tsunami" type="dart" met="n" currents="n" waterquality="n"
				// dart="n"/>
				BuoyStationRecord rec = new BuoyStationRecord();
				rec.id = id.getNodeValue();
				Node lat = attrList.getNamedItem("lat");
				Node lon = attrList.getNamedItem("lon");
				Node elev = attrList.getNamedItem("elev");
				Node name = attrList.getNamedItem("name");
				Node owner = attrList.getNamedItem("owner");
				Node pgm = attrList.getNamedItem("pgm");
				Node type = attrList.getNamedItem("type");
				Node met = attrList.getNamedItem("met");
				Node currents = attrList.getNamedItem("currents");
				Node quality = attrList.getNamedItem("waterquality");
				Node dart = attrList.getNamedItem("dart");
				rec.lat = getDoubleValue(lat);
				rec.lon = getDoubleValue(lon);
				rec.elevation = getDoubleValue(elev);
				rec.name = getStringValue(name);
				rec.owner = getStringValue(owner);
				rec.pgm = getStringValue(pgm);
				rec.type = getStringValue(type);
				rec.met = getStringValue(met);
				rec.currents = getStringValue(currents);
				rec.waterQuality = getStringValue(quality);
				rec.dart = getStringValue(dart);

				buoys.add(rec);
			}
		} catch (Exception e) {
			throw new IOException("Error reading station xml file: " + stationUrl, e);
		}

		return buoys;
	}
	
	public static Double getDoubleValue(Node n) {
		if (n == null) return null; // or 0.0
		return Double.parseDouble(n.getNodeValue());
	}
	
	public static String getStringValue(Node n) {
		if (n == null) return null; // or ""
		return n.getNodeValue();
	}

	public static void main(String[] args) throws Exception {
		System.err.println("Reading stations...");
		List<BuoyStationRecord> buoys = BuoyStationReader.readStations(stationsFile);
		for (BuoyStationRecord b : buoys) {
			System.out.println(b);
		}
	}
}