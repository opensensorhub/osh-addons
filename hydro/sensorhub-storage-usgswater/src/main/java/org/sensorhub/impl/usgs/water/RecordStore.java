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

import java.util.Set;
import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.usgs.water.CodeEnums.ObsParam;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;


public class RecordStore implements IRecordStoreInfo
{
    DataRecord dataStruct;
    DataEncoding encoding;
    
    
    public RecordStore(String name, Set<ObsParam> parameters)
    {
        SWEHelper helper = new SWEHelper();
        
        // TODO sort params by code?        
        
        // build record structure with requested parameters
        dataStruct = helper.newDataRecord();
        dataStruct.setName(name);
        
        dataStruct.addField("time", helper.newTimeStampIsoUTC());
        dataStruct.addField("site", helper.newText("http://sensorml.com/ont/swe/property/SiteID", "Site ID", null));
        dataStruct.getFieldList().getProperty(1).setRole(IMultiSourceDataInterface.ENTITY_ID_URI);
        
        for (ObsParam param: parameters)
        {
            String paramName = param.name().toLowerCase();
            
            DataComponent c = helper.newQuantity(
                    getDefUri(param),
                    getLabel(param),
                    getDesc(param),
                    getUom(param),
                    DataType.FLOAT);
            
            dataStruct.addComponent(paramName, c);
        }        
        
        // use text encoding with default separators
        encoding = helper.newTextEncoding();
    }
    
    
    protected String getDefUri(ObsParam param)
    {
        String name = param.toString().replaceAll(" ", "");
        return SWEHelper.getPropertyUri(name);
    }
    
    
    protected String getLabel(ObsParam param)
    {
        return param.toString();
    }
    
    
    protected String getDesc(ObsParam param)
    {
        return param.toString() + " parameter, USGS code " + param.getCode();
    }
    
    
    protected String getUom(ObsParam param)
    {
        switch (param)
        {
            case WATER_TEMP:
                return "Cel";                
            case DISCHARGE:
                return "[cft_i]/s";
            case GAGE_HEIGHT:
                return "[ft_i]";
            case CONDUCTANCE:
                return "uS/cm";
            case OXY:
                return "mg/L";
            case PH:
                return "1";
        }
        
        return null;
    }
    
    
    @Override
    public String getName()
    {
        return dataStruct.getName();
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return encoding;
    }

}
