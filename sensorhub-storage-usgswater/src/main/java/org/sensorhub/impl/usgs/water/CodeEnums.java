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


public class CodeEnums
{
    /**
     * US state codes
     */
    public enum StateCode
    {
        AL, AK, AZ, AR, CA, CO, CT, DE, DC, FL, GA, HI, ID, IL, IN, IA, KS, KY,
        LA, ME, MD, MA, MI, MN, MS, MO, MT, NE, NV, NH, NJ, NM, NY, NC, ND, OH,
        OK, OR, PA, PR, RI, SC, SD, TN, TX, UT, VT, VA, VI, WA, WV, WI, WY
    }
    
    
    /**
     * USGS site type codes
     */
    public enum SiteType
    {        
        AT("Atmosphere"),
        GL("Glacier"),
        OC("Ocean"),
        ES("Estuary"),
        LK("Lake"),
        ST("Stream"),
        SP("Spring"),
        GW("Well"),
        SB("Other Subsurface"),
        WE("Wetland"),
        LA("Glacier"),
        FA("Facility");
        
        final String text;
        
        private SiteType(String text)
        {
            this.text = text;
        }
        
        public String toString()
        {
            return text;
        }
    }
    
    
    /**
     * USGS parameter codes
     */
    public enum ObsParam
    {
        WATER_TEMP("00010", "Water Temperature"),
        DISCHARGE("00060", "Discharge"),
        GAGE_HEIGHT("00065", "Gage Height"),
        CONDUCTANCE("00095", "Specific Conductance"),
        OXY("00300", "Dissolved Oxygen"),
        PH("00400", "Water pH");
        
        final String code;
        final String text;
        
        private ObsParam(String code, String text)
        {
            this.code = code;
            this.text = text;
        }
        
        public String toString()
        {
            return text;
        }
        
        public String getCode()
        {
            return code;
        }
    }
}
