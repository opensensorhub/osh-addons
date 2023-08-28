/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nexrad;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.nexrad.aws.AwsNexradUtil;
import org.sensorhub.impl.sensor.nexrad.aws.MomentDataBlock;
import org.sensorhub.impl.sensor.nexrad.aws.Radial;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkPathQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.SWEFactory;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;

/**
 * 
 * <p>Title: NexradOutput.java</p>
 * <p>Description: </p>
 *
 * @author Tony Cook
 * @date Jul 15, 2016
 * 
 *  TODO - verify that rangeToCenterOfFirstGate and gateSize are constants; how do we specify UOM for a count
 */

public class NexradOutput extends AbstractSensorOutput<NexradSensor>
{
	private static final Logger logger = LoggerFactory.getLogger(NexradOutput.class);
	DataRecord nexradStruct;
	DataEncoding encoding;
	boolean sendData;
	Timer timer;	
	InputStream is;
	int numListeners;
//	NexradSensor nexradSensor;
	//	LdmFilesProvider ldmFilesProvider;
	ChunkPathQueue chunkQueue;

	//  Listener Check needed to know if anyone is receiving events to know when to delete the AWS queue
	static final long LISTENER_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1); 


	public NexradOutput(NexradSensor parentSensor)
	{
		super("nexradOutput", parentSensor);
//		nexradSensor = parentSensor;
		Timer queueTimer = new Timer();  
		queueTimer.scheduleAtFixedRate(new CheckNumListeners(), 0, LISTENER_CHECK_INTERVAL); //delay in milliseconds
	}


	protected void init()
	{
		//  Add Location only as ouptut- Alex is adding support for this
		//		SweHelper.newLocationVectorLLa(...);
		SWEFactory fac = new SWEFactory();
		GeoPosHelper geoHelper = new GeoPosHelper();
		
		// SWE Common data structure
		nexradStruct = new DataRecordImpl();  
		nexradStruct.setName(getName());
		nexradStruct.setDefinition("http://sensorml.com/ont/swe/propertyx/NexradRadial");

		//  0
		Time time = new TimeImpl();
		time.getUom().setHref(Time.ISO_TIME_UNIT);
		time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
		nexradStruct.addComponent("time", time);

		// 88D site identifier (1) (Not supported for Binary)
//		nexradStruct.addComponent("siteId", fac.newText());
//		nexradStruct.getFieldList().getProperty(1).setRole(SWEConstants.DEF_SYSTEM_ID); // use site ID as entity ID     
		
		// 1
		Vector locationVector = geoHelper.createLocationVectorLLA().build();
		nexradStruct.addField("location", locationVector);  // ???
				
		// 2
		Quantity el = new QuantityImpl();
		el.getUom().setCode("deg");
		el.setDefinition("http://sensorml.com/ont/swe/property/ElevationAngle");
		nexradStruct.addComponent("elevation",el);

		// 3
		Quantity az = new QuantityImpl();
		az.getUom().setCode("deg");
		az.setDefinition("http://sensorml.com/ont/swe/property/AzimuthAngle");
		nexradStruct.addComponent("azimuth",az);

		// 4
		Quantity rangeToCenterOfFirstRefGate = new QuantityImpl(DataType.SHORT);
		rangeToCenterOfFirstRefGate.setDefinition("http://sensorml.com/ont/swe/property/Range.html");
		rangeToCenterOfFirstRefGate.getUom().setCode("m");
		nexradStruct.addComponent("rangeToCenterOfFirstRefGate", rangeToCenterOfFirstRefGate);

		// 5
		Quantity refGateSize = new QuantityImpl(DataType.SHORT);
		refGateSize.setDefinition("http://sensorml.com/ont/swe/property/RangeSampleSpacing.html"); 
		refGateSize.getUom().setCode("m");
		nexradStruct.addComponent("refGateSize", refGateSize);

		// 6
		Count numRefGates = fac.newCount(DataType.INT);
		numRefGates.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numRefGates.setId("NUM_REF_GATES");
		nexradStruct.addComponent("numRefGates",numRefGates);

		// 7
		Quantity rangeToCenterOfFirstVelGate = new QuantityImpl(DataType.SHORT);
		rangeToCenterOfFirstVelGate.setDefinition("http://sensorml.com/ont/swe/property/Range.html"); 
		rangeToCenterOfFirstVelGate.getUom().setCode("m");
		nexradStruct.addComponent("rangeToCenterOfFirstVelGate", rangeToCenterOfFirstVelGate);

		// 8
		Quantity velGateSize = new QuantityImpl(DataType.SHORT);
		velGateSize.setDefinition("http://sensorml.com/ont/swe/property/RangeSampleSpacing.html"); 
		velGateSize.getUom().setCode("m");
		nexradStruct.addComponent("velGateSize", velGateSize);

		// 9
		Count numVelGates = fac.newCount(DataType.INT);
		numVelGates.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numVelGates.setId("NUM_VEL_GATES");
		nexradStruct.addComponent("numVelGates",numVelGates);

		// 10
		Quantity rangeToCenterOfFirstSwGate = new QuantityImpl(DataType.SHORT);
		rangeToCenterOfFirstSwGate.setDefinition("http://sensorml.com/ont/swe/property/Range.html"); 
		rangeToCenterOfFirstSwGate.getUom().setCode("m");
		nexradStruct.addComponent("rangeToCenterOfFirstSwGate", rangeToCenterOfFirstSwGate);

		// 11
		Quantity swGateSize = new QuantityImpl(DataType.SHORT);
		swGateSize.setDefinition("http://sensorml.com/ont/swe/property/RangeSampleSpacing.html"); 
		swGateSize.getUom().setCode("m");
		nexradStruct.addComponent("swGateSize", swGateSize);

		// 12
		Count numSwGates = fac.newCount(DataType.INT);
		numSwGates.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
		numSwGates.setId("NUM_SW_GATES");
		nexradStruct.addComponent("numSwGates",numSwGates);

		// 13
		Quantity reflQuant = fac.newQuantity(DataType.FLOAT);
		reflQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/Reflectivity");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		reflQuant.getUom().setCode("db");
		DataArray reflData = fac.newDataArray();
		reflData.setElementType("Reflectivity", reflQuant);
		reflData.setElementCount(numRefGates); //  alex adding support for this
		nexradStruct.addComponent("Reflectivity", reflData);

		// 14
		Quantity velQuant = fac.newQuantity(DataType.FLOAT);
		velQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/Velocity");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		velQuant.getUom().setCode("m/s");
		DataArray velData = fac.newDataArray();
		velData.setElementType("Velocity", velQuant);
		//		velData.setElementCount(numVelBins); 
		velData.setElementCount(numVelGates); 
		nexradStruct.addComponent("Velocity", velData);

		// 15
		Quantity swQuant = fac.newQuantity(DataType.FLOAT);
		swQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/SpectrumWidth");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		swQuant.getUom().setCode("1"); // ? or db
		DataArray swData = fac.newDataArray();
		swData.setElementType("SpectrumWidth", swQuant);
		swData.setElementCount(numSwGates); 
		nexradStruct.addComponent("SpectrumWidth", swData);

		encoding = SWEHelper.getDefaultBinaryEncoding(nexradStruct);
		//		encoding = fac.newTextEncoding();
	}

	RadialProvider radialProvider;
	protected void start(RadialProvider provider)
	{
		this.radialProvider = provider;

		if (sendData)
			return;

		sendData = true;

		NexradConfig config = parent.getConfiguration();
		for(String site: config.siteIds) {
			Thread t = new GetRadialsThread(site);
			t.start();
		}

		// start sending of radials
		//		Thread t = new Thread(new Runnable()
		//		{
		//			public void run()
		//			{
		//				while (sendData)
		//				{
		//					try {
		//						List<LdmRadial> radials = radialProvider.getNextRadials();
		//						if(radials == null)
		//							continue;
		////						System.err.println("Read " + radials.size() + " radials");
		//						sendRadials(radials);
		//					} catch (IOException e) {
		//						e.printStackTrace();
		//						continue;
		//					}
		//				}
		//			}
		//		});
		//		t.start();
	}

	class GetRadialsThread extends Thread {
		String site;

		public GetRadialsThread(String site) {
			this.site = site;
		}

		@Override
		public void run() {
			while (sendData)
			{
				try {
					List<Radial> radials = parent.getConfiguration().saveDataAsFiles 
								? radialProvider.getNextRadialsFile(site)
								: radialProvider.getNextRadials(site);
					if(radials == null)
						continue;
					//					System.err.println("Read " + radials.size() + " radials");
					sendRadials(radials);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	private void sendRadials(List<Radial> radials) throws Exception
	{
		int i=0;
		Radial r = radials.get(0);
//		System.err.println(r);
		logger.debug(r.toString());
		
		int startArrayIdx = 13;
		for(Radial radial: radials) {
			// build and publish datablock
			// Check which products are present in this radial first			
			MomentDataBlock refMomentData = radial.momentData.get("REF");
			MomentDataBlock velMomentData = radial.momentData.get("VEL");
			MomentDataBlock swMomentData = radial.momentData.get("SW ");

			//
			DataArray refArr = (DataArray)nexradStruct.getComponent(startArrayIdx);
			DataArray velArr = (DataArray)nexradStruct.getComponent(startArrayIdx);
			DataArray swArr = (DataArray)nexradStruct.getComponent(startArrayIdx);
			//
			//
			if(refMomentData == null) {
				refArr.updateSize(0);
			} else {
				refArr.updateSize(refMomentData.numGates);
			}

			if(velMomentData == null) {
				velArr.updateSize(0);
			} else {
				velArr.updateSize(velMomentData.numGates);
			}

			if(swMomentData == null) {
				swArr.updateSize(0);
			} else {
				swArr.updateSize(swMomentData.numGates);
			}
			DataBlock nexradBlock = nexradStruct.createDataBlock();
			//
			long days = radial.dataHeader.daysSince1970;
			long ms = radial.dataHeader.msSinceMidnight;
			double utcTime = (double)(AwsNexradUtil.toJulianTime(days, ms)/1000.);
//			Long utcTimeMs = AwsNexradUtil.toJulianTime(days, ms);
//			byte[] b = Longs.toByteArray(utcTimeMs);
//			for(int ii=0;ii<8;ii++) {
//				int ib = b[ii] & 0xFF;
//				System.err.println(ii + ":  "  + b[ii] + " " + Integer.toHexString(ib));
//			}
//			Long l = Longs.fromByteArray(b);
//			System.err.println("Longs.fromByteArray: " + l);
//			Instant inst = Instant.ofEpochMilli(utcTimeMs);
//			System.err.println("Radial time: " + inst);
			int idx = 0;
			nexradBlock.setDoubleValue(idx++, utcTime);
//			nexradBlock.setStringValue(idx++, radial.dataHeader.siteId);
			nexradBlock.setDoubleValue(idx++, radial.volumeDataBlock.latitude);
			nexradBlock.setDoubleValue(idx++, radial.volumeDataBlock.longitude);
			nexradBlock.setDoubleValue(idx++, radial.volumeDataBlock.feedhornHeightAboveGroundMeters);
			nexradBlock.setDoubleValue(idx++, radial.dataHeader.elevationAngle);
			nexradBlock.setDoubleValue(idx++, radial.dataHeader.azimuthAngle);
			//
//			float [] f = new float[0];
			if(refMomentData != null) {
				//				System.err.println(refMomentData.numGates);
				nexradBlock.setShortValue(idx++, refMomentData.rangeToCenterOfFirstGate);
				nexradBlock.setShortValue(idx++, refMomentData.rangeSampleInterval);
				nexradBlock.setIntValue(idx++, refMomentData.numGates);
			} else {
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setIntValue(idx++, 0);
			}
			if(velMomentData != null) {
				nexradBlock.setShortValue(idx++, velMomentData.rangeToCenterOfFirstGate);
				nexradBlock.setShortValue(idx++, velMomentData.rangeSampleInterval);
				nexradBlock.setIntValue(idx++, velMomentData.numGates);
			} else {
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setIntValue(idx++, 0);
			}
			if(swMomentData != null) {
				nexradBlock.setShortValue(idx++, swMomentData.rangeToCenterOfFirstGate);
				nexradBlock.setShortValue(idx++, swMomentData.rangeSampleInterval);
				nexradBlock.setIntValue(idx++, swMomentData.numGates);
			} else {
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setShortValue(idx++, (short)0);
				nexradBlock.setIntValue(idx++, 0);
			}
			idx = 13;

			if(refMomentData != null) {
//				nexradBlock.setIntValue(idx++, refMomentData.numGates);
				((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(refMomentData.getData());
			} else {
				idx++;
				//((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(f);
			}
			if(velMomentData != null) {
//				nexradBlock.setIntValue(idx++, velMomentData.numGates);
				((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(velMomentData.getData());
			} else {
				idx++;
				//((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(f);
			} 	

			if(swMomentData != null) {
//				nexradBlock.setIntValue(idx++, swMomentData.numGates);
				((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(swMomentData.getData());
			} else {
				idx++;
				//((DataBlockMixed)nexradBlock).getUnderlyingObject()[idx++].setUnderlyingObject(f);
			}

			String siteUID = NexradSensor.SITE_UID_PREFIX + radial.dataHeader.siteId;
			String outputName = getName();
			latestRecord = nexradBlock;			
			latestRecordTime = System.currentTimeMillis();
			String traceMsg = "SEND RADIALS: name, siteUID, az, el: " + outputName + "," + siteUID + "," +
					radial.dataHeader.elevationAngle + "," + radial.dataHeader.azimuthAngle;
			logger.trace(traceMsg);
			eventHandler.publish(new DataEvent(latestRecordTime, NexradSensor.SENSOR_UID, outputName, siteUID, nexradBlock));
		}

	}
	
	protected void stop()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
		sendData = false;
	}


	@Override
	public double getAverageSamplingPeriod()
	{
		return 0.1;
	}


	@Override
	public DataComponent getRecordDescription()
	{
		return nexradStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}


	@Override
	public DataBlock getLatestRecord()
	{
		return latestRecord;
	}


	@Override
	public long getLatestRecordTime()
	{
		if (latestRecord != null) {
			return latestRecord.getLongValue(0) * 1000;
		}

		return 0;
	}

	boolean noListeners = false;
	class CheckNumListeners extends TimerTask {
		@Override
		public void run() {
			int numListeners = eventHandler.getNumListeners();
			logger.debug("CheckNumListeners = {}",numListeners);
			if (numListeners > 0) { 
				try {
					parent.setQueueActive();
				} catch (SensorHubException e) {
					//  What should happen in this case? We can't really proceed 
					e.printStackTrace(System.err);
				}
				noListeners = true;
			} else {
				if(!noListeners) { 
					parent.setQueueIdle();
					noListeners = true;
				}

			}

		}

	}
}
