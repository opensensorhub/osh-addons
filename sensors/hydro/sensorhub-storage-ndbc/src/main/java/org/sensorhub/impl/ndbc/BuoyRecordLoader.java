package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

public class BuoyRecordLoader implements Iterator<DataBlock> {

	DataBlock block;
	//	List<BuoyRecord> records;
	List<DataBlock> blocks;
	Iterator<DataBlock> iterator; 
	DataComponent dataComponent;

	public BuoyRecordLoader(DataComponent dataComponent) {
		this.dataComponent = dataComponent;
	}

	public List<BuoyRecord> getRecords(NDBCConfig filter) throws IOException {
		BuoyParser parser = new BuoyParser(filter, filter.parameters.iterator().next());
		List<BuoyRecord> recs = parser.getRecords();
		blocks = new ArrayList<>();
		for(BuoyRecord rec: recs) {
			block = dataComponent.createDataBlock();
			block.setDoubleValue(0, rec.timeMs/1000.);
			block.setStringValue(1, rec.stationId);
			block.setStringValue(2, "N/A"); // MSG ID
			block.setDoubleValue(3, rec.waterTemperature);
			blocks.add(block);
		}
		iterator = blocks.iterator();
		return recs;
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
