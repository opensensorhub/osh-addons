/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.trupulse;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.NadirPointing;
import org.sensorhub.algo.vecmath.Mat3d;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.sensor.trupulse.TruPulseOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Example process for geolocating the range finder target knowing the
 * device position either as provided by user, or from an associated GPS.
 * This works because TruPulse range finder also provides the azimuth
 * and inclination/elevation of the beam, in addition to the range.
 * </p>
 *
 * <p>Copyright (c) 2015 Sensia Software LLC</p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since June 21, 2015
 */
public class TargetGeolocProcess extends ExecutableProcessImpl {
    protected static final Logger log = LoggerFactory.getLogger(TargetGeolocProcess.class);

    protected GeoTransforms geoConv = new GeoTransforms();
    protected NadirPointing nadirPointing = new NadirPointing();

    protected Vect3d lastSensorPosEcef = new Vect3d();
    protected Vect3d lla = new Vect3d();
    protected Mat3d ecefRot = new Mat3d();

    protected DataRecord sensorLocInput;
    protected DataComponent rangeMeasInput;

    protected DataRecord targetLocOutput;

    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:TargetGeoloc", "Target Geoloc",
            "Geolocate the target of the Trupulse laser target designator", TargetGeolocProcess.class);

    public TargetGeolocProcess() {

        super(INFO);

        GeoPosHelper fac = new GeoPosHelper();

        sensorLocInput = fac.createRecord()
                .name("sensorLocation")
                .addSamplingTimeIsoUTC("time")
                .addField("loc", fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC))
                .build();
        inputData.add(sensorLocInput.getName(), sensorLocInput);

        rangeMeasInput = TruPulseOutput.getOutputDescription();
        inputData.add(rangeMeasInput.getName(), rangeMeasInput);

        // create outputs
        targetLocOutput = fac.createRecord()
                .name("targetLocation")
                .addSamplingTimeIsoUTC("time")
                .addField("location", fac.newLocationVectorLLA(SWEHelper.getPropertyUri("TargetLocation")))
                .build();
        outputData.add(targetLocOutput.getName(), targetLocOutput);
    }

    @Override
    public void init() throws ProcessException {
        super.init();
    }

    @Override
    public void execute() throws ProcessException {

        // data received is LLA in degrees
        DataBlock locInputData = sensorLocInput.getData();
        double lat = locInputData.getDoubleValue(1);
        double lon = locInputData.getDoubleValue(2);
        double alt = locInputData.getDoubleValue(3);
        log.trace("Last GPS pos = [{},{},{}]", lat, lon, alt);

        // convert to radians and then ECEF
        lla.y = Math.toRadians(lat);
        lla.x = Math.toRadians(lon);
        lla.z = alt;
        geoConv.LLAtoECEF(lla, lastSensorPosEcef);

        DataBlock rangeMeasInputData = rangeMeasInput.getData();
        double time = rangeMeasInputData.getDoubleValue(0);
        double range = rangeMeasInputData.getDoubleValue(2);
        double az = rangeMeasInputData.getDoubleValue(3);
        double inc = rangeMeasInputData.getDoubleValue(4);
        log.debug("TruPulse meas: range={}, az={}, inc={}", range, az, inc);

        if (Double.isNaN(range)) {

            throw new ProcessException("Computed range is NaN");

        } else {

            // express LOS in ENU frame
            Vect3d los = new Vect3d(0.0, range, 0.0);
            los.rotateX(Math.toRadians(inc));
            los.rotateZ(Math.toRadians(-az));

            // transform to ECEF frame
            nadirPointing.getRotationMatrixENUToECEF(lastSensorPosEcef, ecefRot);
            los.rotate(ecefRot);
            los.add(lastSensorPosEcef);

            // convert target location back to LLA
            geoConv.ECEFtoLLA(los, lla);

            targetLocOutput.getData().setDoubleValue(0, time);
            targetLocOutput.getData().setDoubleValue(1, lat);
            targetLocOutput.getData().setDoubleValue(2, lon);
            targetLocOutput.getData().setDoubleValue(3, alt);
            log.debug("Target pos = [{},{},{}]", lat, lon, alt);
        }
    }
}
