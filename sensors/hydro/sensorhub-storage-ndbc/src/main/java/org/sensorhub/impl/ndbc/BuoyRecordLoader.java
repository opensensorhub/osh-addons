package org.sensorhub.impl.ndbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.opengis.swe.v20.DataBlock;

public class BuoyRecordLoader implements Iterator<DataBlock> {

	public List<BuoyRecord> getRecords(DataFilter filter) throws IOException {
		List<BuoyRecord> allRecs = new ArrayList<>();
		for (BuoyParam param : filter.parameters) {
			BuoyParser parser = new BuoyParser(filter, param);
			List<BuoyRecord> recs = parser.getRecords();
			allRecs.addAll(recs);
		}
		return allRecs;
	}

	@Override
	public boolean hasNext() {
//		return (nextRecord != null);
		return false;
	}

	@Override
	public DataBlock next() {
		if (!hasNext())
			throw new NoSuchElementException();
//		return preloadNext();
		return null;
	}
}
