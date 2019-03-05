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

import net.opengis.swe.v20.DataChoice;
import org.sensorhub.impl.SensorHub;
import org.vast.data.TextEncodingImpl;
import org.vast.ows.sps.DescribeTaskingRequest;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.SubmitRequest;
import org.vast.swe.SWEData;


public class TestSensorHubAxisSosSps
{
    private final static String ENDPOINT = "http://localhost:8080/sensorhub/sps";
    //private final static String ENDPOINT = "http://bottsgeo.simple-url.com:2015/sensorhub/sps";
    private final static String PROC_ID = "d136b6ea-3951-4691-bf56-c84ec7d89d72";
    
    
    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"src/test/resources/config_axis_sos_sps.json", "storage"});
        
        // get procedure ID
        String procID = PROC_ID;
        /*for (IModule<?> module: SensorHub.getInstance().getModuleRegistry().getLoadedModules())
        {
            if (module instanceof AxisCameraDriver)
                procID = module.getLocalID();
        }*/
        
        SPSUtils utils = new SPSUtils();
        
        // send SPS describe tasking
        DescribeTaskingRequest dtReq = new DescribeTaskingRequest();
        dtReq.setVersion("2.0");
        dtReq.setGetServer(ENDPOINT);
        dtReq.setProcedureID(procID);
        utils.writeXMLQuery(System.out, dtReq);
        DescribeTaskingResponse dtResp = utils.sendRequest(dtReq, false);
        utils.writeXMLResponse(System.out, dtResp);
        
        // send tasking requests
        SWEData taskParams = new SWEData();
        taskParams.setElementType(dtResp.getTaskingParameters());
        taskParams.setEncoding(new TextEncodingImpl());
        DataChoice ptzParams = (DataChoice)dtResp.getTaskingParameters().copy();
        for (int i=0; i<4; i++)
        {
            // generate new tasking parameters
            ptzParams.renewDataBlock();
            ptzParams.setSelectedItem("rpan");
            ptzParams.getComponent("rpan").getData().setDoubleValue(-45.0);
            taskParams.clearData();
            taskParams.addData(ptzParams.getData());
            
            // build request
            SubmitRequest subReq = new SubmitRequest();
            subReq.setVersion("2.0");
            subReq.setPostServer(ENDPOINT);
            subReq.setProcedureID(procID);
            subReq.setParameters(taskParams);
            utils.writeXMLQuery(System.out, subReq);
            utils.sendRequest(subReq, false);
            
            Thread.sleep(2000L);
        }
        
        // go back to home position
        // generate new tasking parameters
        ptzParams.renewDataBlock();
        ptzParams.setSelectedItem("gotoserverpresetname");
        ptzParams.getComponent("gotoserverpresetname").getData().setStringValue("Drive_wide");
        taskParams.clearData();
        taskParams.addData(ptzParams.getData());
        
        // build request
        SubmitRequest subReq = new SubmitRequest();
        subReq.setVersion("2.0");
        subReq.setPostServer(ENDPOINT);
        subReq.setProcedureID(procID);
        subReq.setParameters(taskParams);
        utils.writeXMLQuery(System.out, subReq);
        utils.sendRequest(subReq, false);
    }

}
