/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2017 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.sensorhub.impl.usgs.water.CodeEnums.SiteType;
import org.sensorhub.impl.usgs.water.CodeEnums.StateCode;
import org.slf4j.Logger;
import org.vast.util.Bbox;
import org.vast.util.DateTimeFormat;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;

/**
 * <p>
 * Streaming parser for USGS tabular data of observations.<br/>
 * See <a href="https://waterservices.usgs.gov/rest/IV-Service.html"> web
 * service documentation</a>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 15, 2017
 */
public class ObsRecordLoader implements Iterator<DataBlock> {
	static final String BASE_URL = USGSWaterDataArchive.BASE_USGS_URL + "iv?";

	AbstractModule<?> module;
	BufferedReader reader;
	DataFilter filter;
	ParamValueParser[] paramReaders;
	DataBlock templateRecord, nextRecord;

	public ObsRecordLoader(AbstractModule<?> module, DataComponent recordDesc) {
		this.module = module;
		this.templateRecord = recordDesc.createDataBlock();
	}

	protected String buildInstantValuesRequest(DataFilter filter) {
		StringBuilder buf = new StringBuilder(BASE_URL);

		// site ids
		if (!filter.siteIds.isEmpty()) {
			buf.append("sites=");
			for (String id : filter.siteIds)
				buf.append(id).append(',');
			buf.setCharAt(buf.length() - 1, '&');
		}

		// state codes
		else if (!filter.stateCodes.isEmpty()) {
			buf.append("stateCd=");
			for (StateCode state : filter.stateCodes)
				buf.append(state.name()).append(',');
			buf.setCharAt(buf.length() - 1, '&');
		}

		// county codes
		else if (!filter.countyCodes.isEmpty()) {
			buf.append("countyCd=");
			for (String countyCd : filter.countyCodes)
				buf.append(countyCd).append(',');
			buf.setCharAt(buf.length() - 1, '&');
		}

		// site bbox
		else if (filter.siteBbox != null && !filter.siteBbox.isNull()) {
			Bbox bbox = filter.siteBbox;
			buf.append("bbox=").append(bbox.getMinX()).append(",").append(bbox.getMaxY()).append(",")
					.append(bbox.getMaxX()).append(",").append(bbox.getMinY()).append("&");
		}

		// site types
		if (!filter.siteTypes.isEmpty()) {
			buf.append("siteType=");
			for (SiteType type : filter.siteTypes)
				buf.append(type.name()).append(',');
			buf.setCharAt(buf.length() - 1, '&');
		}

		// parameters
		if (!filter.parameters.isEmpty()) {
			buf.append("parameterCd=");
			for (ObsParam param : filter.parameters)
				buf.append(param.getCode()).append(',');
			buf.setCharAt(buf.length() - 1, '&');
		}

		// time range
		DateTimeFormat timeFormat = new DateTimeFormat();
		if (filter.startTime != null)
			buf.append("startDT=").append(timeFormat.formatIso(filter.startTime.getTime() / 1000., 0)).append("&");
		if (filter.endTime != null)
			buf.append("endDT=").append(timeFormat.formatIso(filter.endTime.getTime() / 1000., 0)).append("&");

		// constant options
		buf.append("format=rdb"); // output format

		return buf.toString();
	}

	public void sendRequest(DataFilter filter) throws IOException {
		String requestUrl = buildInstantValuesRequest(filter);

		module.getLogger().debug("Requesting observations from: " + requestUrl);
		URL url = new URL(requestUrl);

		reader = new BufferedReader(new InputStreamReader(url.openStream()));
		this.filter = filter;

		// preload first record
		nextRecord = null;
		preloadNext();
	}

	protected void parseHeader() throws IOException {
		// skip header comments
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.startsWith("#"))
				break;
		}

		// parse field names and prepare corresponding readers
		String[] fieldNames = line.split("\t");
		initParamReaders(filter.parameters, fieldNames);

		// skip field sizes
		reader.readLine();
	}

	protected void initParamReaders(Set<ObsParam> params, String[] fieldNames) {
		ArrayList<ParamValueParser> readers = new ArrayList<>();
		int i = 0;

		// always add time stamp and site ID readers
		readers.add(new TimeStampParser(2, i++));
		readers.add(new SiteNumParser(1, i++));

		// create a reader for each selected param
		for (ObsParam param : params) {
			// look for field with same param code
			int fieldIndex = -1;
			for (int j = 0; j < fieldNames.length; j++) {
				if (fieldNames[j].endsWith(param.getCode())) {
					fieldIndex = j;
					break;
				}
			}

			readers.add(new FloatValueParser(fieldIndex, i++, module.getLogger()));
		}

		paramReaders = readers.toArray(new ParamValueParser[0]);
	}

	protected DataBlock preloadNext() {
		try {
			DataBlock currentRecord = nextRecord;
			nextRecord = null;

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				// parse section header when data for next site begins
				if (line.startsWith("#")) {
					parseHeader();
					line = reader.readLine();
					if (line == null || line.trim().isEmpty())
						return null;
				}

				String[] fields = line.split("\t");
				nextRecord = templateRecord.renew();

				// read all requested fields to datablock
				for (ParamValueParser reader : paramReaders)
					reader.parse(fields, nextRecord);

				break;
			}

			return currentRecord;
		} catch (IOException e) {
			module.getLogger().error("Error while reading tabular data", e);
		}

		return null;
	}

	@Override
	public boolean hasNext() {
		return (nextRecord != null);
	}

	@Override
	public DataBlock next() {
		if (!hasNext())
			throw new NoSuchElementException();
		return preloadNext();
	}

	public void close() {
		try {
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			module.getLogger().error("Error while closing reader", e);
		}
	}

	// The following classes are used to parse individual values from the tabular
	// data
	// A list of parsers is built according to the desired output and parsers are
	// applied
	// in sequence to fill the datablock with the proper values

	/*
	 * Base value parser class
	 */
	static abstract class ParamValueParser {
		int fromIndex;
		int toIndex;

		public ParamValueParser(int fromIndex, int toIndex) {
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}

		public abstract void parse(String[] tokens, DataBlock data) throws IOException;
	}

	/*
	 * Parser for time stamp field, including time zone
	 */
	static class TimeStampParser extends ParamValueParser {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
		StringBuilder buf = new StringBuilder();

		public TimeStampParser(int fromIndex, int toIndex) {
			super(fromIndex, toIndex);
		}

		public void parse(String[] tokens, DataBlock data) throws IOException {
			buf.setLength(0);
			buf.append(tokens[fromIndex].trim());
			buf.append(' ');
			buf.append(tokens[fromIndex + 1].trim());

			try {
				long ts = dateFormat.parse(buf.toString()).getTime();
				data.setDoubleValue(toIndex, ts / 1000.);
			} catch (ParseException e) {
				throw new IOException("Invalid time stamp " + buf.toString());
			}
		}
	}

	/*
	 * Parser for site ID
	 */
	static class SiteNumParser extends ParamValueParser {
		public SiteNumParser(int fromIndex, int toIndex) {
			super(fromIndex, toIndex);
		}

		public void parse(String[] tokens, DataBlock data) {
			String val = tokens[fromIndex].trim();
			data.setStringValue(toIndex, val);
		}
	}

	/*
	 * Parser for floating point value
	 */
	static class FloatValueParser extends ParamValueParser {
		Logger logger;
		public FloatValueParser(int fromIndex, int toIndex, Logger logger) {
			super(fromIndex, toIndex);
			this.logger = logger;
		}

		public void parse(String[] tokens, DataBlock data) throws IOException {
			try {
				float f = Float.NaN;

				if (fromIndex >= 0 && fromIndex < tokens.length) {
					String val = tokens[fromIndex].trim();
					if (!val.isEmpty() && !val.startsWith("*"))
						try {
							f = Float.parseFloat(val);
						} catch (NumberFormatException e) {
							//  If value is non-numeric, leave field as NaN
							logger.trace("Special value: {}", val);
						}
				}

				data.setFloatValue(toIndex, f);
			} catch (NumberFormatException e) {
				throw new IOException("Invalid numeric value " + tokens[fromIndex], e);
			}
		}
	}
}
