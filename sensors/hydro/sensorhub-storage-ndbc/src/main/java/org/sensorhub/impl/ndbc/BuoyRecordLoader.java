package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.opengis.swe.v20.DataBlock;

public class BuoyRecordLoader implements Iterator<DataBlock> {

	
	
	public void getRecords(DataFilter filter) throws IOException {
		for(BuoyEnums.ObsParam param: filter.parameters) {
			BuoyParser parser = new BuoyParser(filter, param);
			List<BuoyRecord> recs = parser.getRecords();
		}
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DataBlock next() {
		// TODO Auto-generated method stub
		return null;
	}

}
