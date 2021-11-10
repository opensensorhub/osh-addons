/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.audio;

import java.time.Instant;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteOrder;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;


/**
 * 
 * @author Tony Cook
 *
 */
public class AudioOutput extends AbstractSensorOutput<AudioDriver> 
{
	DataRecord struct;
    protected DataEncoding encoding;

	int  numSamplesPerArray;
	static final double SAMPLE_OUTPUT_RATE = 0.1;  //  Not sure what to set this to 
		
	// baseTime in seconds for formats that do not contain embedded base time or absolute time in format 
    double baseTime;  // = System.currentTimeMillis() / 1000.0;  

	public AudioOutput(AudioDriver parentSensor)
	{
		super(parentSensor);
		numSamplesPerArray = parentSensor.getConfiguration().numSamplesPerArray;
		init();
	}


	@Override
	public String getName()
	{
		return "AudioOutput";
	}


	protected void init()
	{
		SWEHelper swe = new SWEHelper();

		//		data record
		//		ts
		//		samplingRate
		//		dataarray
		//			samples
		struct = swe.createDataRecord()
			.definition("urn:osh:audio:wav:sampleArray")
			.label("WavRecord")
			.description("Array of Wav files samples")
			.addSamplingTimeIsoUTC("IsoTime")
			.addField("samplingRate", swe.createQuantity()
				.definition(SWEConstants.OGC_PROP_URI + "SamplingRate")
				.description("Sampling Rate")
				.uomCode("Hz")
				.dataType(DataType.INT)
				.build())
			.addField("numSamples", swe.createCount()
					.description("Number of Samples in this array")
					.definition(SWEConstants.OGC_PROP_URI + "NumSamples")
					.dataType(DataType.INT)
					.build())
			.addField("amplitudeArray", swe.createArray()
				//  Fixed size works, but what about last sample array?
				.withFixedSize(numSamplesPerArray)
				.withElement("amplitude", swe.createQuantity()
					.definition(SWEConstants.OGC_PROP_URI + "Amplitude")
					.description("Amplitude of Sample")
					.uomCode("db")  //  really normalized amplitude, not db
//					.dataType(DataType.DOUBLE)
					.dataType(DataType.FLOAT)
//					.dataType(DataType.BYTE)
					.build())
				.build())
			.build();
		
		encoding = SWEHelper.getDefaultBinaryEncoding(struct);
		((BinaryEncoding)encoding).setByteOrder(ByteOrder.LITTLE_ENDIAN);
//		encoding = swe.newTextEncoding();
	}
	
    // Remove if not needed
    public final DataBlock getNewDataBlock()
    {
        if (latestRecord == null)
            return struct.createDataBlock();
        else
            return latestRecord.renew();
    }

    @Deprecated
    public void publishChunk(double [] buffer, int sampleCount, long samplingRateHz) { // pass these in from config
		DataBlock block = getNewDataBlock();
		double offsetTime = (double) sampleCount / (double) samplingRateHz; 
		block.setDoubleValue(0, (baseTime + offsetTime));
		block.setLongValue(1, (long)(baseTime + offsetTime) * 1000L);
		block.setLongValue(2, samplingRateHz);
		AbstractDataBlock wavData = ((DataBlockMixed)block).getUnderlyingObject()[3];
        wavData.setUnderlyingObject(buffer);
        
        // send event
        latestRecord = block;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AudioOutput.this, latestRecord));
	}
	
    int recCnt = 0;
	public void publishRecord(AudioRecord rec) { // pass these in from config
		DataBlock block = getNewDataBlock();
		double offsetTime = (double) rec.sampleIndex / (double) rec.samplingRate;
//		System.err.println(" \n>>Offset time" + offsetTime);
		double latestTime = baseTime + offsetTime;
		block.setDoubleValue(0, latestTime);
		block.setIntValue(1, rec.samplingRate);
		block.setIntValue(2, rec.sampleData.length);
		AbstractDataBlock wavData = ((DataBlockMixed)block).getUnderlyingObject()[3];
        wavData.setUnderlyingObject(rec.getFloatData());
        
        // send event
        latestRecord = block;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, AudioOutput.this, latestRecord));
//        System.err.println(++recCnt + " Records published");
//        System.err.println("AudioOutput.publish().. RecTime is: " + Instant.ofEpochSecond((long)latestTime));
	}
	
	public void setBaseTime(long baseTime) {
		this.baseTime = baseTime;
	}
	
	@Override
	public DataComponent getRecordDescription() {
		return struct;
	}


	@Override
	public DataEncoding getRecommendedEncoding() {
		return encoding;
	}


	@Override
	public double getAverageSamplingPeriod() {
		return SAMPLE_OUTPUT_RATE;  
	}
	
}
