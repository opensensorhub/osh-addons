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

package org.sensorhub.test.impl.sensor.simulatedcbrn;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.simulatedcbrn.SimCBRNConfig;
import org.sensorhub.impl.sensor.simulatedcbrn.SimCBRNSensor;
import org.vast.data.TextEncodingImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWEUtils;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertTrue;


/**
 * Created by Ian Patterson on 5/4/2017.
 */
public class TestSimCBRN implements IEventListener
{
	SimCBRNSensor driver;
	SimCBRNConfig config;
	AsciiDataWriter writer = new AsciiDataWriter();
	AsciiDataWriter writer1= new AsciiDataWriter();
	AsciiDataWriter writer2= new AsciiDataWriter();
	AsciiDataWriter writer3 = new AsciiDataWriter();
	AsciiDataWriter writer4 = new AsciiDataWriter();
	int sampleCount = 0;


	@Before
	public void init() throws Exception
	{
		config = new SimCBRNConfig();
		config.id = UUID.randomUUID().toString();

		driver = new SimCBRNSensor();
		driver.init(config);
	}


	@Test
	public void testGetOutputDesc() throws Exception
	{
		for (ISensorDataInterface di: driver.getObservationOutputs().values())
		{
			System.out.println();
			DataComponent dataMsg = di.getRecordDescription();
			new SWEUtils(SWEUtils.V2_0).writeComponent(System.out, dataMsg, false, true);
		}
	}


	@Test
	public void testGetSensorDesc() throws Exception
	{
		System.out.println();
		AbstractProcess smlDesc = driver.getCurrentDescription();
		new SMLUtils(SWEUtils.V2_0).writeProcess(System.out, smlDesc, true);
	}


	@Test
	public void testSendMeasurements() throws Exception
	{
		System.out.println();
		ISensorDataInterface idOutput = driver.getObservationOutputs().get("ID");
		ISensorDataInterface alertOutput = driver.getObservationOutputs().get("ALERTS");
		//ISensorDataInterface readingsOutput = driver.getObservationOutputs().get("READGS");
		ISensorDataInterface maintOutput = driver.getObservationOutputs().get("MAINT");
		ISensorDataInterface statusOutput = driver.getObservationOutputs().get("STATUS");


		writer = new AsciiDataWriter();
		writer1 = new AsciiDataWriter();
		writer1= new AsciiDataWriter();
		writer2= new AsciiDataWriter();
		writer3 = new AsciiDataWriter();
		//writer4 = new AsciiDataWriter();

		writer.setDataEncoding(new TextEncodingImpl(",", "\n"));
		writer1.setDataEncoding(new TextEncodingImpl(",", "\n"));
		writer2.setDataEncoding(new TextEncodingImpl(",", "\n"));
		writer3.setDataEncoding(new TextEncodingImpl(",", "\n"));
		//writer4.setDataEncoding(new TextEncodingImpl(",", "\n"));


		writer.setDataComponents(idOutput.getRecordDescription());
		writer1.setDataComponents(alertOutput.getRecordDescription());
		//writer2.setDataComponents(readingsOutput.getRecordDescription());
		writer3.setDataComponents(maintOutput.getRecordDescription());
		//writer4.setDataComponents(statusOutput.getRecordDescription());

		writer.setOutput(System.out);
		writer1.setOutput(System.out);
		//writer2.setOutput(System.out);
		writer3.setOutput(System.out);
		//writer4.setOutput(System.out);



		//idOutput.registerListener(this);
		alertOutput.registerListener(this);
		//readingsOutput.registerListener(this);
		//maintOutput.registerListener(this);
		//statusOutput.registerListener(this);
		driver.start();

		synchronized (this)
		{
			while (sampleCount < 20)
				wait();
		}

		System.out.println();
	}


	@Override
	public void handleEvent(Event<?> e)
	{
		assertTrue(e instanceof SensorDataEvent);
		SensorDataEvent newDataEvent = (SensorDataEvent)e;

		try
		{
			//System.out.print("\nNew data received from sensor " + newDataEvent.getSensorId());

//			writer.write(newDataEvent.getRecords()[0]);
//			writer.flush();
			writer1.write(newDataEvent.getRecords()[0]);
			writer1.flush();
//			writer2.write(newDataEvent.getRecords()[0]);
//			writer2.flush();
//			writer3.write(newDataEvent.getRecords()[0]);
//			writer3.flush();



			sampleCount++;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

		synchronized (this) { this.notify(); }
	}


	@After
	public void cleanup()
	{
		try
		{
			driver.stop();
		}
		catch (SensorHubException e)
		{
			e.printStackTrace();
		}
	}
}
