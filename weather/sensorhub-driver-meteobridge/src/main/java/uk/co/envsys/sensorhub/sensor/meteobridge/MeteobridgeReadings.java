package uk.co.envsys.sensorhub.sensor.meteobridge;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;	


/**
 * Utility class to store and parse meteobridge readings from XML
 * 
 * @author Environment Systems - www.envsys.co.uk
 *
 */
public class MeteobridgeReadings {
	private final Date date;				// Sample date when meteobridge queried
	private final Float th0Temp; 			// outdoor temperature in degrees Celsius
	private final Float th0Hum; 			// relative outdoor humidity as percentage
	private final Float th0Dew; 			// outdoor dew point in degrees Celsius
	private final Float th0HeatIndex; 		// outdoor heat index in degrees Celsius
	private final Float thb0Temp; 			// indoor temperature in degrees Celsius
	private final Float thb0Hum; 			// indoor humidity as percentage
	private final Float thb0Dew; 			// indoor dewpoint in degrees Celsius
	private final Float thb0Press; 		// station pressure in hPa
	private final Float thb0SeaPress; 		// normalized pressure (computed to sea level) in hPa
	private final Float wind0Wind; 		// wind speed in m/s
	private final Float wind0AvgWind; 		// average windspeed in m/s (time used for average depends on station)
	private final Float wind0Dir; 			// wind direction in degress (0° is North)
	private final Float wind0Chill; 		// wind chill temperature in degrees Celsius
	private final Float wind0Gust;			// gust rate of wind
	private final Float rain0Rate;			// rain rate in mm/h
	private final Float rain0Total; 		// rain fall in mm
	private final Float uv0Index; 			// uv index
	private final Float sol0Rad; 			// solar radiation in W/m^2
	private final Float sol0Evo; 			// evapotranspiration in mm (only supported on Davis Vantage stations)
	
	public MeteobridgeReadings(Date date, Float th0Temp, Float th0Hum, Float th0Dew, Float th0HeatIndex, Float thb0Temp,
			Float thb0Hum, Float thb0Dew, Float thb0Press, Float thb0SeaPress, Float wind0Wind, Float wind0AvgWind,
			Float wind0Dir, Float wind0Chill, Float wind0Gust, Float rain0Rate, Float rain0Total, Float uv0Index,
			Float sol0Rad, Float sol0Evo) {
		this.date = date;
		this.th0Temp = th0Temp;
		this.th0Hum = th0Hum;
		this.th0Dew = th0Dew;
		this.th0HeatIndex = th0HeatIndex;
		this.thb0Temp = thb0Temp;
		this.thb0Hum = thb0Hum;
		this.thb0Dew = thb0Dew;
		this.thb0Press = thb0Press;
		this.thb0SeaPress = thb0SeaPress;
		this.wind0Wind = wind0Wind;
		this.wind0AvgWind = wind0AvgWind;
		this.wind0Dir = wind0Dir;
		this.wind0Chill = wind0Chill;
		this.wind0Gust = wind0Gust;
		this.rain0Rate = rain0Rate;
		this.rain0Total = rain0Total;
		this.uv0Index = uv0Index;
		this.sol0Rad = sol0Rad;
		this.sol0Evo = sol0Evo;
	}

	// Getters
	public Date getDate() {
		return date;
	}
	public Float getTh0Temp() {
		return th0Temp;
	}
	public Float getTh0Hum() {
		return th0Hum;
	}
	public Float getTh0Dew() {
		return th0Dew;
	}
	public Float getTh0HeatIndex() {
		return th0HeatIndex;
	}
	public Float getThb0Temp() {
		return thb0Temp;
	}
	public Float getThb0Hum() {
		return thb0Hum;
	}
	public Float getThb0Dew() {
		return thb0Dew;
	}
	public Float getThb0Press() {
		return thb0Press;
	}
	public Float getThb0SeaPress() {
		return thb0SeaPress;
	}
	public Float getWind0Wind() {
		return wind0Wind;
	}
	public Float getWind0AvgWind() {
		return wind0AvgWind;
	}
	public Float getWind0Dir() {
		return wind0Dir;
	}
	public Float getWind0Chill() {
		return wind0Chill;
	}
	public Float getWind0Gust() {
		return wind0Gust;
	}
	public Float getRain0Rate() {
		return rain0Rate;
	}
	public Float getRain0Total() {
		return rain0Total;
	}
	public Float getUv0Index() {
		return uv0Index;
	}
	public Float getSol0Rad() {
		return sol0Rad;
	}
	public Float getSol0Evo() {
		return sol0Evo;
	}

	/**
	 * Static function to parse meteobridge readings from XML and return them as MeteobridgeReadings
	 * 
	 * @param xmlString The XML to read from, as a string
	 * @return The parsed readings, packaged up in a MeteobridgeReadings object
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static MeteobridgeReadings fromXML(String xmlString) throws SAXException, IOException, ParserConfigurationException {
		Float th0temp, th0hum, th0dew, th0heatindex, thb0temp, thb0hum, thb0dew, thb0press, thb0seapress, wind0wind, 
			wind0avgwind, wind0dir, wind0chill, rain0rate, rain0total, uv0index, sol0rad, sol0evo, wind0gust;
		
		// Get root element
		Element logger = getXMLFromString(xmlString).getDocumentElement();
		
		// Read values (if exist, otherwise null)
		th0temp = parseFloat(logger, "TH", "temp");
		th0hum = parseFloat(logger, "TH", "hum");
		th0dew = parseFloat(logger, "TH", "dew");
		th0heatindex = parseFloat(logger, "TH", "heatindex");
		thb0temp = parseFloat(logger, "THB", "temp");
		thb0hum = parseFloat(logger, "THB", "hum");
		thb0dew = parseFloat(logger, "THB", "dew");
		thb0press = parseFloat(logger, "THB", "press");
		thb0seapress = parseFloat(logger, "THB", "seapress");
		wind0wind = parseFloat(logger, "WIND", "wind");
		wind0avgwind = parseFloat(logger, "WIND", "avgwind");
		wind0dir = parseFloat(logger, "WIND", "dir");
		wind0chill = parseFloat(logger, "WIND", "chill");
		wind0gust = parseFloat(logger, "WIND", "gust");
		rain0rate = parseFloat(logger, "RAIN", "rate");
		rain0total = parseFloat(logger, "RAIN", "total");
		uv0index = parseFloat(logger, "UV", "index");
		sol0rad = parseFloat(logger, "SOL", "rad");
		sol0evo = parseFloat(logger, "SOL", "evo");
		
		// Construct readings
		return new MeteobridgeReadings(new Date(), th0temp, th0hum, th0dew, th0heatindex, thb0temp, thb0hum, thb0dew, thb0press, 
				thb0seapress, wind0wind, wind0avgwind, wind0dir, wind0chill, wind0gust, rain0rate, rain0total, uv0index, sol0rad, sol0evo);
		
	}
	
	/**
	 * Looks for a float value in given attribute name within given tag name and returns it.
	 * If no such value is found in the document, null is returned.
	 * 
	 * @param document XML Document
	 * @param tagName The name of the tag containing the attribute
	 * @param attributeName The name of the attribute containing the value
	 * @return Float object representing the value, or null if not found
	 */
	public static Float parseFloat(Element document, String tagName, String attributeName) {
		NodeList foundElements = document.getElementsByTagName(tagName);
		if(foundElements.getLength() > 0) {
			Element tag = (Element) foundElements.item(0);
			if(tag.hasAttribute(attributeName)) {
				return Float.valueOf(tag.getAttribute(attributeName));
			}	
		}
		return null;
	}
	
	/**
	 * Reads an XML document from a String
	 * 
	 * @param xml as a string
	 * @return XML as Document
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static Document getXMLFromString(String xml) throws SAXException, IOException, ParserConfigurationException {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xml));
	    return builder.parse(is);
	}	
}
