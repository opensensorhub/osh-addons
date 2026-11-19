/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.sat;

import org.sensorhub.algo.sat.orbit.MechanicalState;
import org.sensorhub.algo.sat.orbit.OrbitPredictor;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Time;


/**
 * <p>
 * Process for fetching latest TLE and computing satellite pos/vel at
 * the desired time.
 * </p>
 *
 * @author Alex Robin
 * @since Apr 5, 2015
 */
public class TLEPredictor extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("TLEPredictor", "TLE Predictor", "Estimator of satellite position using Two-Line Elements orbit data", ECItoECEF.class);
    
    protected static final Logger log = LoggerFactory.getLogger(TLEPredictor.class);
    
    Time utcTime;
    DataRecord stateOut;
    MechanicalState state;
    OrbitPredictor orbitPredictor;
    
    
    public TLEPredictor()
    {
        super(INFO);
        GeoPosHelper swe = new GeoPosHelper();
        
        // create time input
        utcTime = swe.createTime()
            .asSamplingTimeIsoUTC()
            .build();
        inputData.add(utcTime);
        
        // create state output
        stateOut = swe.createRecord()
            .name("state")
            .definition(SWEConstants.DEF_PLATFORM_LOC.replace("Location", "State"))
            .addField("time", swe.createTime().asSamplingTimeIsoUTC())
            .addField("position", swe.newLocationVectorECEF(GeoPosHelper.DEF_LOCATION, "m"))
            .addField("velocity", swe.newVelocityVectorECEF(GeoPosHelper.DEF_VELOCITY, "m/s"))
            .build();
        
        // create params
        
    }
    
    
    @Override
    public void init() throws ProcessException
    {
        this.state = MechanicalState.withPosOrder1();
    }


    @Override
    public void execute() throws ProcessException
    {
        double time = utcTime.getData().getDoubleValue();
        orbitPredictor.getECEFState(time, state);
        
        // send to output
        DataBlock stateData = stateOut.getData();
        stateData.setDoubleValue(0, state.epochTime);
        stateData.setDoubleValue(1, state.linearPosition.x);
        stateData.setDoubleValue(2, state.linearPosition.y);
        stateData.setDoubleValue(3, state.linearPosition.z);
        stateData.setDoubleValue(4, state.linearVelocity.x);
        stateData.setDoubleValue(5, state.linearVelocity.y);
        stateData.setDoubleValue(6, state.linearVelocity.z);
    }
}
