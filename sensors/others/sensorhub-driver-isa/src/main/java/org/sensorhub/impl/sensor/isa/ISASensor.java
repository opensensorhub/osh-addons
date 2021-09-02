/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.isa;

import java.time.Instant;
import org.sensorhub.impl.sensor.AbstractSensorDriver;
import org.vast.ogc.om.SamplingSphere;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEConstants;
import com.google.common.collect.ImmutableMap;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>
 * Base class for all ISA sensor types supported by this driver
 * </p>
 *
 * @author Alex Robin
 * @since May 19, 2021
 */
public abstract class ISASensor extends AbstractSensorDriver
{
    enum StatusType
    {
        ELEC_DC,
        ELEC_AC,
        RADIO
    }
    
    SMLHelper sml = new SMLHelper();
    
    
    protected ISASensor(ISADriver parent, String id, String name)
    {
        super(parent, parent.getUniqueIdentifier() + ":" + id, id);
        
        this.smlDescription = sml.createPhysicalSystem()
            .id(getShortID())
            .uniqueID(getUniqueIdentifier())
            .name(name)
            .definition(SWEConstants.DEF_SENSOR)
            .addClassifier(sml.classifiers.sensorType(getSensorType()))
            .build();
    }
    
    
    protected String getSensorType()
    {
        return getClass().getSimpleName();
    }
    
    
    @Override
    public String getName()
    {
        return smlDescription.getName();
    }
    
    
    protected ISASensor setManufacturer(String manufacturer)
    {
        synchronized (smlDescription)
        {
            sml.edit((PhysicalSystem)smlDescription)
                .addIdentifier(sml.identifiers.manufacturer(manufacturer));
            return this;
        }
    }
    
    
    protected ISASensor setModelNumber(String modelNumber)
    {
        synchronized (smlDescription)
        {
            sml.edit((PhysicalSystem)smlDescription)
                .addIdentifier(sml.identifiers.modelNumber(modelNumber));
            return this;
        }
    }
    
    
    protected ISASensor setSoftwareVersion(String version)
    {
        synchronized (smlDescription)
        {
            sml.edit((PhysicalSystem)smlDescription)
                .addIdentifier(sml.identifiers.softwareVersion(version));
            return this;
        }
    }
    
    
    protected ISASensor setFixedLocation(Instant time, double lat, double lon, double alt)
    {
        setSamplingPointFoi(lat, lon, alt);
        addLocationOutput(Double.NaN);
        locationOutput.updateLocation(time.toEpochMilli(), lon, lat, alt, false);
        return this;
    }
    
    
    protected ISASensor setFixedLocationWithRadius(Instant time, double lat, double lon, double alt, double radius)
    {
        setSamplingSphereFoi(lat, lon, alt, radius);
        addLocationOutput(Double.NaN);
        locationOutput.updateLocation(time.toEpochMilli(), lon, lat, alt, false);
        return this;
    }


    protected void setSamplingSphereFoi(double lat, double lon, double alt, double radius)
    {
        var sf = new SamplingSphere();
        sf.setId("FOI_" + getShortID());
        sf.setUniqueIdentifier(getUniqueIdentifier() + ":foi");
        sf.setName(getName());
        sf.setDescription("Sampling volume for " + getName());
        sf.setHostedProcedureUID(getUniqueIdentifier());
        Point point = new GMLFactory(true).newPoint();
        point.setSrsName(SWEConstants.REF_FRAME_4979);
        point.setSrsDimension(3);
        point.setPos(new double[] {lat, lon, alt});
        sf.setShape(point);
        sf.setRadius(radius);
        foiMap = ImmutableMap.of(sf.getUniqueIdentifier(), sf);
    }
    
    
    protected ISASensor addStatusOutputs(StatusType... statusTypes)
    {
        for (var statusType: statusTypes)
        {
            switch (statusType)
            {
                case ELEC_DC:
                    addOutput(new ElecStatusOutput(this, true), true);
                    break;
                    
                case ELEC_AC:
                    addOutput(new ElecStatusOutput(this, false), true);
                    break;
                    
                case RADIO:
                    addOutput(new RadioStatusOutput(this), true);
                    break;
            }
        }
        
        return this;
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }

}
