package org.sensorhub.impl.sensor.nexrad;

/**
 * <p>Title: VCP.java</p>
 * <p>Description: Enum of VCP modes employed by Nexrad 88D scanning radars.
 *  http://wx.db.erau.edu/faculty/mullerb/Wx365/Volume_coverage_patterns/vcp.html
 * </p>
 *
 * @author tcook
 * @date Sep 16, 2016
 * 
 *   TODO: Externalize all of this to a resource so we can dynamically add VCP without need to rebuild
 */
public enum VCP {
	vcp11("11", 14),
	vcp211("211", 14),
	vcp12("12", 14),
	vcp212("212", 14),
	vcp21("21", 9),
	vcp121("121", 9),
	vcp221("121", 9),
	vcp31("31", 5),
	vcp32("31", 5),
	;
	
	private final String name;
	private final int numElevations;
	private final double [] elevationAngles = null;  // TODO

    VCP(String name, int numElevations) {
    	this.name = name;
    	this.numElevations = numElevations;
    }
    
    public boolean hasSplitCuts() {
    	switch(this) {
		case vcp11:
		case vcp12:
		case vcp21:
		case vcp121:
		case vcp211:
		case vcp212:
		case vcp221:
			return true;
		case vcp31:
		case vcp32:
			return false;
		default:  // 
			return false;
    	}
    }
    
    public static VCP getVCP(int num) {
    	switch(num) {
    	case 11:
    		return vcp11;
    	case 12:
    		return vcp12;
    	case 21:
    		return vcp21;
    	case 121:
    		return vcp121;
    	case 211:
    		return vcp211;
    	case 212:
    		return vcp212;
    	case 221:
    		return vcp221;
    	case 31:
    		return vcp31;
    	case 32:
    		return vcp32;
    	default:
    		return null;
//    		throw new IllegalArgumentException("Unknown VCP: " + num);
    	}
    }
}
