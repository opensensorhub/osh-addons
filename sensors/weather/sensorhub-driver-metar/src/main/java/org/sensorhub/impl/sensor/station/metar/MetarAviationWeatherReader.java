/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.station.metar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Pull Metar data from https://aviationweather.gov/adds/dataserver_current/current/metars.cache.csv
 * Updated every 5 minutes- using as a backup and possible primary data source for Metars
 * </p>
 *
 * @author tcook
 * @since Apr 24, 2017
 * 
 */
public class MetarAviationWeatherReader 
{
	public final String serverUrl;
	
	public MetarAviationWeatherReader(String serverUrl) {
		this.serverUrl = serverUrl;
	}
	
	public List<Metar> read() throws IOException {
		List<Metar> metars = new ArrayList<>();
		MetarParserNew parser = new MetarParserNew();
		URL url = new URL(serverUrl);
		InputStream is = url.openStream();
		boolean startProcessing = false;
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String inline = "";
			while (inline != null) {
				inline = reader.readLine();
				if(inline == null)  break;
				if(inline.startsWith("raw_text")) {
					startProcessing = true;
					continue;
				}
				if(!startProcessing)  continue;
				int commaIdx = inline.indexOf(",");
				if(commaIdx == -1)  continue;
				String rawText = inline.substring(0, commaIdx);
				Metar metar = parser.parseMetar(rawText);
				metars.add(metar);
				System.err.println(metar);
			} 
		}
		System.err.println(metars.size());
		
		return metars;
	}
	
	public static void main(String[] args) throws Exception {
		MetarAviationWeatherReader reader = new MetarAviationWeatherReader("https://aviationweather.gov/adds/dataserver_current/current/metars.cache.csv");
		reader.read();
	}
}
