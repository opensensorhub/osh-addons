/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

//  TODO - fix missing values correctly (see Winds)
public class BuoyRecordLoader implements Iterator<DataBlock> {

	DataBlock block;
	List<DataBlock> blocks;
	Iterator<DataBlock> iterator; 
	DataComponent dataComponent;

	public BuoyRecordLoader(DataComponent dataComponent) {
		this.dataComponent = dataComponent;
	}

	public List<BuoyRecord> getRecords(NDBCConfig filter, BuoyParam param) throws IOException {
		BuoyParser parser = new BuoyParser(filter, param);
		List<BuoyRecord> recs = parser.getRecords();
		blocks = new ArrayList<>();
		for(BuoyRecord rec: recs) {
			block = dataComponent.createDataBlock();
			block.setDoubleValue(0, rec.timeMs/1000.);
			block.setStringValue(1, rec.stationId);
			block.setStringValue(2, "N/A"); // MSG ID
			switch(param) {
			case SEA_WATER_TEMPERATURE:
				block.setDoubleValue(3, rec.waterTemperature);
				break;
			case AIR_TEMPERATURE:
				block.setDoubleValue(3, rec.airTemperature);
				break;
			case SEA_WATER_ELECTRICAL_CONDUCTIVITY:
				block.setDoubleValue(3, rec.conductivity);
				break;
			case AIR_PRESSURE_AT_SEA_LEVEL:
				block.setDoubleValue(3, rec.airPressure);
				break;
			case WINDS:
				setDoubleValue(block, 3, rec.winds.wind_from_direction);
				setDoubleValue(block, 4, rec.winds.wind_speed);
				setDoubleValue(block, 5, rec.winds.wind_speed_of_gust);
				setDoubleValue(block, 6, rec.winds.upward_air_velocity);
				break;
			case GPS:
				block.setDoubleValue(3, rec.lat);
				block.setDoubleValue(4, rec.lon);
				break;
			default:
				throw new IOException("BuoyRecordLoader- record type not recognized: " + param);
			}
			blocks.add(block);
		}
		iterator = blocks.iterator();
		return recs;
	}
	
	private void setDoubleValue(DataBlock block, int index, Double value) {
		if(value == null) {
			block.setDoubleValue(index, -999.9);  // fix this to proper SWE missing value convention
			return; 
		}
		block.setDoubleValue(index, value);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public DataBlock next() {
		if (!hasNext())
			throw new NoSuchElementException();
		return iterator.next();
	}
}
