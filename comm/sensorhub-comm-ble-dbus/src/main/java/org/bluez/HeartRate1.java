
package org.bluez;

import org.freedesktop.dbus.DBusInterface;


public interface HeartRate1 extends DBusInterface
{
    public final static String IFACE_NAME = HeartRate1.class.getCanonicalName();
    
    
    // Properties    
    
    /**
     * string Location (optional) [readonly]<br/>
     * Possible values: "other", "chest", "wrist","winger", "hand", "earlobe", "foot"
     */
    public final static String PROP_LOCATION = "Location";
    
    
    /**
     * boolean ResetSupported [readonly]<br/>
     * True if energy expended is supported.
     */
    public final static String PROP_RESET_SUPPORTED = "ResetSupported";
                
    
    /**
     * Restart the accumulation of energy expended from zero.
     */
    public void Reset();
}
