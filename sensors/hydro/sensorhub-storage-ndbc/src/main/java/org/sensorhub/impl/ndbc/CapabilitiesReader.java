package org.sensorhub.impl.ndbc;

import java.io.InputStream;
import java.time.Instant;

import org.vast.ogc.gml.GMLUtils;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReader;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.util.Bbox;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * CapabilitiesReader - Currently used to get Time Ranges from NDBC Caps on a
 * 						schedule. Other harvesting could be added here if needed
 * @author tcook
 *
 */

public class CapabilitiesReader {

	String capsUrl;
	long updateTimeMs;
	Bbox bbox = new Bbox(-90.0, -180.0, 90.0, 180.0);  // default to global
	
	public CapabilitiesReader(String capsUrl, long updateTimeMs) {
		this.capsUrl = capsUrl;
		this.updateTimeMs = updateTimeMs;		
	}
	
    //  NDBC Uses EPSG:4326 coord ordering (lat,lon), so reverse our config for filtering 
	public void setBbox(double minLat, double minLon, double maxLat, double maxLon) {
		this.bbox = new Bbox(minLat, minLon, maxLat, maxLon);
	}
	
	DOMHelper sendRequest(OWSRequest request) throws Exception
	{
		OWSUtils utils = new OWSUtils();
		InputStream is;
		is = utils.sendGetRequest(request).getInputStream();
		System.out.println(utils.buildURLQuery(request));

		DOMHelper dom = new DOMHelper(is, false);
		OWSExceptionReader.checkException(dom, dom.getBaseElement());
		return dom;
	}
	
	public  void getTimes() throws Exception {
		GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
		getCaps.setGetServer(capsUrl);
		getCaps.setService("SOS");
	    GMLUtils gmlUtils = new GMLUtils(GMLUtils.V3_2);
	    
	    System.err.println("Reading caps");
		DOMHelper dom = sendRequest(getCaps);
//	    InputStream is = new BufferedInputStream((new FileInputStream("C:/Users/tcook/root/sensorHub/oot/buoy/Caps.xml")));
//		DOMHelper dom = new DOMHelper(is, false);

		NodeList obsOffs = dom.getElements("Contents/ObservationOfferingList/ObservationOffering");
		for(int i=0; i<obsOffs.getLength(); i++) {
			Element obsElt = (Element)obsOffs.item(i);
			// get Bounded by and check first if bbox has been configured
        	Element envElt = dom.getElement(obsElt, "boundedBy/Envelope");
        	if (envElt == null)  continue;
        	Bbox foiBbox = gmlUtils.readEnvelopeAsBbox(dom, envElt);
			Element nameElt = dom.getElement(obsElt, "name");
        	if(!bbox.contains(foiBbox))  continue;
			System.err.println(nameElt.getTextContent() + " , " + foiBbox);
			// get time
        	Element timeElt = dom.getElement(obsElt, "time/TimePeriod");
        	if (timeElt != null) {
                TimeExtent timeExt = gmlUtils.readTimePrimitiveAsTimeExtent(dom, timeElt);
                double startTime = timeExt.getBaseTime();
                double stopTime;
                // isBeginNow should never be true
                if(timeExt.isEndNow()) {
                	stopTime = (System.currentTimeMillis() + updateTimeMs) / 1000.0; // 
                } else {
                	stopTime = startTime + timeExt.getLeadTimeDelta();
                }
                Instant iStart = Instant.ofEpochMilli((long)startTime * 1000L);
                Instant iStop = Instant.ofEpochMilli((long)stopTime * 1000L);
                System.err.println("\t" + iStart + " : " + iStop);
        	} else {
        		//  What to use here?  We should always get times in theory but handle this case
        	}
		}
		System.err.println(obsOffs.getLength());
	}
}
