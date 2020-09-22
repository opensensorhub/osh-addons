package org.sensorhub.impl.ndbc;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.sensorhub.api.persistence.ObsPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.gml.GMLUtils;
import org.vast.ogc.om.SamplingPoint;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReader;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.swe.SWEHelper;
import org.vast.util.Bbox;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;

/***************************** BEGIN LICENSE BLOCK ***************************

Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
******************************* END LICENSE BLOCK ***************************/
public class CapsReaderTask extends TimerTask  {

	String capsUrl;
	long updateTimeMs;
	Bbox bbox = new Bbox(-90.0, -180.0, 90.0, 180.0);  // default to global

	Map<String, AbstractFeature> fois;
    Map<String, ObsPeriod> foiTimeRanges;
    GMLFactory gmlFac = new GMLFactory(true);
    
    Consumer<List<BuoyMetadata>> callback;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    
	public CapsReaderTask(String capsUrl, long updateTimeMs, Consumer< List<BuoyMetadata>> callback) {
		this.capsUrl = capsUrl;
		this.updateTimeMs = updateTimeMs;
		this.callback = callback;
		fois = new LinkedHashMap<>();
		foiTimeRanges = new ConcurrentHashMap<>();
	}

	//  NDBC Uses EPSG:4326 coord ordering (lat,lon), so reverse our config for filtering 
	public void setBbox(double minLat, double minLon, double maxLat, double maxLon) {
		this.bbox = new Bbox(minLat, minLon, maxLat, maxLon);
	}

	@Override
	public void run() {
//		System.out.println("Caps updating at : " + LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledExecutionTime()), 
//						ZoneId.of("UTC")));
		try {
			List<BuoyMetadata> metadata = getBuoyMetadata();
			callback.accept(metadata);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	DOMHelper sendRequest(OWSRequest request) throws Exception
	{
		OWSUtils utils = new OWSUtils();
		InputStream is;
		is = utils.sendGetRequest(request).getInputStream();
		logger.info("Requesting caps from {}",utils.buildURLQuery(request));

		DOMHelper dom = new DOMHelper(is, false);
		OWSExceptionReader.checkException(dom, dom.getBaseElement());
		return dom;
	}

	public List<String> getBuoyIds() throws Exception {
		GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
		getCaps.setGetServer(capsUrl);
		getCaps.setService("SOS");
		GMLUtils gmlUtils = new GMLUtils(GMLUtils.V3_2);

		DOMHelper dom = sendRequest(getCaps);
		NodeList obsOffs = dom.getElements("Contents/ObservationOfferingList/ObservationOffering");
		List<String> ids = new ArrayList<>();
		for(int i=0; i<obsOffs.getLength(); i++) {
			Element obsElt = (Element)obsOffs.item(i);
			Element nameElt = dom.getElement(obsElt, "name");
			ids.add(nameElt.getTextContent());
		}
		return ids;
	}

	public List<BuoyMetadata> getBuoyMetadata() throws Exception {
		GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
		getCaps.setGetServer(capsUrl);
		getCaps.setService("SOS");
		GMLUtils gmlUtils = new GMLUtils(GMLUtils.V3_2);

		DOMHelper dom = sendRequest(getCaps);
		// Uncomment to test local cached file
//	    InputStream is = new BufferedInputStream((new FileInputStream("C:/Users/tcook/root/sensorHub/oot/buoy/Caps.xml")));
//		DOMHelper dom = new DOMHelper(is, false);

		List<BuoyMetadata> mdList = new ArrayList<>();
		
		NodeList obsOffs = dom.getElements("Contents/ObservationOfferingList/ObservationOffering");
		for(int i=0; i<obsOffs.getLength(); i++) {
			Element obsElt = (Element)obsOffs.item(i);
			// get Bounded by and check first if bbox has been configured
			Element envElt = dom.getElement(obsElt, "boundedBy/Envelope");
			if (envElt == null) {
				continue;
			}
			Bbox foiBbox = gmlUtils.readEnvelopeAsBbox(dom, envElt);
			Element nameElt = dom.getElement(obsElt, "name");
			String uid = nameElt.getTextContent();
			int idx = uid.lastIndexOf(':');
			if(idx == -1)  continue;
			if(!bbox.contains(foiBbox))  continue;
			BuoyMetadata metadata = new BuoyMetadata();
			metadata.uid = uid;
			metadata.name = uid.substring(idx + 1);
//			SamplingPoint foiPt = createFoi(name, uid, foiBbox.getMinY(), foiBbox.getMinX());
			metadata.foi = createFoi(metadata.name, uid, foiBbox.getMinY(), foiBbox.getMinX());
//			fois.put(name, foiPt);
			// get time
			Element timeElt = dom.getElement(obsElt, "time/TimePeriod");
			if (timeElt != null) {
				TimeExtent timeExt = gmlUtils.readTimePrimitiveAsTimeExtent(dom, timeElt);
				double startTime = timeExt.getBaseTime();
				double stopTime;
				// isBeginNow should never be true
				//  if timeExt.isEndNow, add updateTimeMs to stopTime so that stopTime is in the future enough
				//  to not miss measurements between updates
				if(timeExt.isEndNow()) {
					stopTime = (System.currentTimeMillis() + updateTimeMs) / 1000.0; // 
				} else {
					stopTime = startTime + timeExt.getLeadTimeDelta();
				}
				Instant iStart = Instant.ofEpochMilli((long)startTime * 1000L);
				Instant iStop = Instant.ofEpochMilli((long)stopTime * 1000L);
//				System.err.println("\t" + uid + "," + iStart + " : " + iStop);
//				foiTimeRanges.put(uid, new ObsPeriod(uid, startTime, stopTime));
				metadata.startTime = startTime;
				metadata.stopTime = stopTime;
				mdList.add(metadata);
			} else {
				//  What to use here?  We should always get times in theory but handle this case
			}
		}
		return mdList;
	}
	
	public SamplingPoint createFoi(String name, String uid, double lat, double lon) {
		SamplingPoint station = new SamplingPoint();
		
		// Get Buoy ID
		station.setId(name);
		station.setUniqueIdentifier(uid);
		station.setName("NDBC Buoy Station " + name);

		// Get Buoy Location
		Point stnLoc = gmlFac.newPoint();
		stnLoc.setSrsDimension(2);
		stnLoc.setSrsName(SWEHelper.getEpsgUri(4326)); // NAD83
		stnLoc.setPos(new double[] { lat, lon });
		station.setShape(stnLoc);
		
		return station;
	}
	
	public static void main(String[] args) throws InterruptedException {
		Timer timer = new Timer();
		CapsReaderTask capsTask = new CapsReaderTask("https://sdf.ndbc.noaa.gov/sos/server.php", TimeUnit.MINUTES.toMillis(60L),
				(c)-> {
		            System.out.println("NumBuoys: " + c.size());
		        });
		capsTask.setBbox(31.0, -120.0, 35.0, -115.0);

		timer.scheduleAtFixedRate(capsTask, 0, 60_000L);
	}

}
