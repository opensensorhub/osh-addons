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

package org.sensorhub.impl.sensor.axis;

/**
 * TODO implement better header reader
 * 
 * @author Johannes Echterhoff
 *
 */
public class AXISJpegHeaderReader
{

	public static long getTimestampAsDateTime(byte[] axisJpeg)
	{
		// TODO need to update to better DateTime object o get time zone support
		return AXISJpegHeaderReader.getTimestamp(axisJpeg);
	}

	public static long getTimestamp(byte[] axisJpeg)
	{

		byte timestamp1 = axisJpeg[25];
		byte timestamp2 = axisJpeg[26];
		byte timestamp3 = axisJpeg[27];
		byte timestamp4 = axisJpeg[28];
		byte timestamp5 = axisJpeg[29];

		long secondsSinceEpoc = (((long) ((int) timestamp1 & 0xFF)) << 24) + (((long) ((int) timestamp2 & 0xFF)) << 16) + (((long) ((int) timestamp3 & 0xFF)) << 8) + ((long) ((int) timestamp4 & 0xFF));

		long subseconds = (long) ((int) timestamp5 & 0xFF);

		long millisSinceEpoc = (secondsSinceEpoc * 1000) + subseconds;

		return millisSinceEpoc;
	}
}
