/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.plume;

import java.io.File;
import java.util.List;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * </p>
 *
 * @author Tony Cook
 * @since Sep 22, 2015
 */
public class PlumeOutput extends AbstractSensorOutput<PlumeSensor>
{
	DataComponent plumeStep;
	BinaryEncoding encoding;


	public PlumeOutput(PlumeSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "plumeModel";
	}


	protected void init()
	{
		/**
    		- sourceLat,sourceLon,sourceAlt

    		- time0, numPoints0
    		- x00,y00,z00
    		- x01,y01,z01
    		...
    		- x0N-1,y0N-1,z0N-1
    		- time1, numPoints1
    		- x10,y10,z10
    		- x11,y11,z11
		 */
		GeoPosHelper fac = new GeoPosHelper();
		//        plumeStruct = new DataRecordImpl(2);
		//        plumeStruct.setName(getName());
		//        plumeStruct.setDefinition("http://sensorml.com/ont/swe/property/LagrangianPlumeModel");

		// Location - don't include here?
		//        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		//        locVector.setLabel("PlumeSourceLocation");
		//        locVector.setDescription("Location of plume source");
		//        plumeStruct.addComponent("location", locVector);

		// Point data
		plumeStep = fac.newDataRecord(3);
		plumeStep.setDescription("plumeStep represents a single timeStep with point data");
		plumeStep.setDefinition("http://sensorml.com/ont/swe/property/LagrangianPlumeModel");
		plumeStep.setName(getName());

		// time
		plumeStep.addComponent("time",  fac.newTimeStampIsoUTC());
		
		// number of particles
		Count numPoints = fac.newCount();
		numPoints.setId("NUM_POINTS");
		plumeStep.addComponent("num_pos", numPoints);
		
		// and the actual particle data
		DataArray ptArr = fac.newDataArray();
		ptArr.setElementType("point", fac.newLocationVectorLLA(null));
		ptArr.setElementCount(numPoints);
		plumeStep.addComponent("points", ptArr);

		// Define Array of steps
		//		DataArray stepArr = fac.newDataArray();
		//		stepArr.addComponent("plumeStep", plumeStep);

		encoding = SWEHelper.getDefaultBinaryEncoding(plumeStep);
	}


	protected void start()
	{
		final File pfile = new File(parentSensor.getConfiguration().dataPath);
		
		Thread t = new Thread()
		{
		    public void run()
		    {
		        try
        		{
		            // initial delay so storage has time to start to receive data
	                Thread.sleep(1000);
		            
		            Plume plume = PlumeReader.read(pfile);
        			List<PlumeStep> steps = plume.getSteps();
        
        			for(PlumeStep step: steps)
        			{
        				DataBlock dataBlock = null;
        				DataArray ptArr = (DataArray)plumeStep.getComponent(2);
        				ptArr.updateSize(step.getNumParticles());
        				dataBlock = plumeStep.createDataBlock();
        
        				dataBlock.setDoubleValue(0, step.getTime());
        				dataBlock.setIntValue(1, step.getNumParticles());
        				((DataBlockMixed)dataBlock).getUnderlyingObject()[2].setUnderlyingObject(step.points1d);
        				
        				latestRecord = dataBlock;
        				latestRecordTime = System.currentTimeMillis();
        				eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, PlumeOutput.this, latestRecord));
        			}
        		}
        		catch (Exception e)
        		{
        			e.printStackTrace();
        		}
		    }
		};
		
		t.start();
	}


	protected void stop()
	{
		// Nothing to do here at this point?
	}


	@Override
	public double getAverageSamplingPeriod()
	{
//		return 600.0;  // need to set this based on value in output file
		return 1.0;  // need to set this based on value in output file
	}


	@Override
	public DataComponent getRecordDescription()
	{
		return plumeStep;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}

}
