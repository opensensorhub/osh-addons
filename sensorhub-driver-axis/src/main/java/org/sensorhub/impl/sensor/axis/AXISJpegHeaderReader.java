/***************************************************************
Copyright (C) 2007 by 52 North Initiative for Geospatial Open 
Source Software GmbH

Contact: Andreas Wytzisk,
52 North Initiative for Geospatial Open Source Software GmbH,
Martin-Luther-King-Weg 24,
48155 Muenster, Germany, info@52north.org

This program is free software; you can redistribute and/or  modify 
it under the terms of the GNU General Public License version 2 as 
published by the Free Software Foundation.

This program is distributed WITHOUT ANY WARRANTY; even without the 
implied WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR 
PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program (see gnu-gpl v2.txt). If not, write to 
the Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
Boston, MA 02111-1307, USA or visit the Free Software Foundation 
web page, http://www.fsf.org.
 ***************************************************************/

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
