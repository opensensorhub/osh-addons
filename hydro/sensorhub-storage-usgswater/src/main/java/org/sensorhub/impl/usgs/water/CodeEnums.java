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
     * US state codes- updated to be FIPS code compliant, which is what USGS is using
     */
    public enum StateCode
    {
        AL("01"), AK("02"), AZ("04"), AR("05"), CA("06"), CO("08"), CT("09"), DE("10"), DC("11"), FL("12"), 
        GA("13"), HI("15"), ID("16"), IL("17"), IN("18"), IA("19"), KS("20"), KY("21"), LA("22"), ME("23"), 
        MD("24"), MA("25"), MI("26"), MN("27"), MS("28"), MO("29"), MT("30"), NE("31"), NV("32"), NH("33"), 
        NJ("34"), NM("35"), NY("36"), NC("37"), ND("38"), OH("39"), OK("40"), OR("41"), PA("42"), PR("43"), 
        RI("44"), SC("45"), SD("46"), TN("47"), TX("48"), UT("49"), VT("50"), VA("51"), VI("52"), WA("53"),
        WV("54"), WI("55"), WY("56");
        
    	final String fipsCode;
    	
        StateCode(String fips) {
            this.fipsCode = fips;
        }
        
        public static StateCode get(String fips) {
        	for(StateCode sc: StateCode.values()) {
        		if(sc.fipsCode.equals(fips))
        			return sc;
        	}
        	return null;	
        }
    }

    public static void main(String[] args) {
		System.err.println(StateCode.AL);
		System.err.println(StateCode.get("36"));
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
