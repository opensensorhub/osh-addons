/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Process for converting 2D LatLon coordinates to 3D LLA by appending
 * a configurable default altitude value.
 * </p>
 *
 * @author Kalyn Stricklin
 * @since May 6, 2026
 */
public class LLToLLA extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:LL2LLA", "LatLon to LLA",
        "LatLon to LLA conversion using a default altitude", LLToLLA.class);

    protected Vector latLonInput;
    protected Quantity defaultAlt;
    protected Vector llaOutput;


    public LLToLLA()
    {
        super(INFO);
        GeoPosHelper sweHelper = new GeoPosHelper();

        // create LatLon input (2D: lat, lon)
        latLonInput = sweHelper.newLocationVectorLatLon(null);
        inputData.add("latLonLocation", latLonInput);

        // create default altitude parameter
        defaultAlt = sweHelper.createQuantity()
            .definition(GeoPosHelper.DEF_ALTITUDE_ELLIPSOID)
            .label("Default Altitude")
            .description("Default altitude value appended to produce LLA output")
            .uom("m")
            .build();
        var altData = defaultAlt.createDataBlock();
        altData.setDoubleValue(0.0);
        defaultAlt.setData(altData);
        paramData.add("defaultAltitude", defaultAlt);

        // create LLA output (3D: lat, lon, alt)
        llaOutput = sweHelper.newLocationVectorLLA(null);
        outputData.add("llaLocation", llaOutput);
    }


    @Override
    public void init() throws ProcessException
    {
        super.init();
    }


    @Override
    public void execute() throws ProcessException
    {
        DataBlock llData = latLonInput.getData();
        double lat = llData.getDoubleValue(0);
        double lon = llData.getDoubleValue(1);

        double alt = defaultAlt.getData().getDoubleValue();

        DataBlock llaData = llaOutput.getData();
        llaData.setDoubleValue(0, lat);
        llaData.setDoubleValue(1, lon);
        llaData.setDoubleValue(2, alt);
    }
}
