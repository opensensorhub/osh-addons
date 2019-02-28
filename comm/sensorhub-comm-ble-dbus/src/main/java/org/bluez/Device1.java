package org.bluez;
import org.freedesktop.dbus.DBusInterface;


/**
 * <p>
 * BlueZ D-Bus Device API
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.Device1<br/>
 * <b>Object path:</b> [variable prefix]/{hci0,hci1,...}/dev_XX_XX_XX_XX_XX_XX<br/>
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 29, 2016
 */
public interface Device1 extends DBusInterface
{
    public final static String IFACE_NAME = Device1.class.getCanonicalName();
    
    
    // Properties
    
    /**
     * string Address [readonly]<br/>
     * 
     * The Bluetooth device address of the remote device.
     */
    public final static String ADDRESS = "Address";
    
    
    /**
     * string Name [readonly, optional]<br/>
     * 
     * The Bluetooth remote name. This value can not be
     * changed. Use the Alias property instead.<br/>
     * 
     * This value is only present for completeness. It is
     * better to always use the Alias property when
     * displaying the devices name.<br/>
     * 
     * If the Alias property is unset, it will reflect
     * this value which makes it more convenient.
     */
    public final static String NAME = "Name";
    
    
    /**
     * uint32 Class [readonly, optional]<br/>
     * 
     * The Bluetooth class of device of the remote device.
     */
    public final static String CLASS = "Class";
    
    
    /**
     * uint16 Appearance [readonly, optional]<br/>
     * 
     * External appearance of device, as found on GAP service.
     */
    public final static String APPEARANCE = "Appearance";
    
    
    /**
     * array{string} UUIDs [readonly, optional]<br/>
     * 
     * List of 128-bit UUIDs that represents the available
     * remote services.
     */
    public final static String UUIDS = "UUIDs";
    
    
    /**
     * boolean Paired [readonly]<br/>
     * 
     * Indicates if the remote device is paired.
     */
    public final static String PAIRED = "Paired";
    
    
    /**
     * boolean Connected [readonly]<br/>
     * 
     * Indicates if the remote device is currently connected.
     * A PropertiesChanged signal indicate changes to this
     * status.
     */
    public final static String CONNECTED = "Connected";
    
    
    /**
     * boolean Trusted [readwrite]<br/>
     * 
     * Indicates if the remote is seen as trusted. This
     * setting can be changed by the application.
     */
    public final static String TRUSTED = "Trusted";
    
    
    /**
     * boolean Blocked [readwrite]<br/>
     * 
     * If set to true any incoming connections from the
     * device will be immediately rejected. Any device
     * drivers will also be removed and no new ones will
     * be probed as long as the device is blocked.
     */
    public final static String BLOCKED = "Blocked";
    
    
    /**
     * string Alias [readwrite]<br/>
     * 
     * The name alias for the remote device. The alias can
     * be used to have a different friendly name for the
     * remote device.<br/>
     * 
     * In case no alias is set, it will return the remote
     * device name. Setting an empty string as alias will
     * convert it back to the remote device name.<br/>
     * 
     * When resetting the alias with an empty string, the
     * property will default back to the remote name.
     */
    public final static String ALIAS = "Alias";
    
    
    /**
     * object Adapter [readonly]<br/>
     * 
     * The object path of the adapter the device belongs to.
     */
    public final static String ADAPTER = "Adapter";
    
    
    /**
     * boolean LegacyPairing [readonly]<br/>
     * 
     * Set to true if the device only supports the pre-2.1
     * pairing mechanism. This property is useful during
     * device discovery to anticipate whether legacy or
     * simple pairing will occur if pairing is initiated.<br/>
     * 
     * Note that this property can exhibit false-positives
     * in the case of Bluetooth 2.1 (or newer) devices that
     * have disabled Extended Inquiry Response support.
     */
    public final static String LEGACY_PAIRING = "LegacyPairing";
    
    
    /**
     * string Modalias [readonly, optional]<br/>
     * 
     * Remote Device ID information in modalias format
     * used by the kernel and udev.
     */
    public final static String MODALIAS = "Modalias";
    
    
    /**
     * int16 RSSI [readonly, optional]<br/>
     * 
     * Received Signal Strength Indicator of the remote
     * device (inquiry or advertising).
     */
    public final static String RSSI = "RSSI";
    
    
    /**
     * int16 TxPower [readonly, optional, experimental]<br/>
     * 
     * Advertised transmitted power level (inquiry or
     * advertising).
     */
    public final static String TXPOWER = "TxPower";
    
    
    /**
     * dict ManufacturerData [readonly, optional]<br/>
     * 
     * Manufacturer specific advertisement data. Keys are
     * 16 bits Manufacturer ID followed by its byte array
     * value.
     */
    public final static String MANUFACTURER_DATA = "ManufacturerData";
    
    
    /**
     * dict ServiceData [readonly, optional]<br/>
     * 
     * Service advertisement data. Keys are the UUIDs in
     * string format followed by its byte array value.
     */
    public final static String SERVICE_DATA = "ServiceData";
    
    
    /**
     * array{object} GattServices [readonly, optional]<br/>
     * 
     * List of GATT service object paths. Each referenced
     * object exports the org.bluez.GattService1 interface and
     * represents a remote GATT service. This property will be
     * updated once all remote GATT services of this device
     * have been discovered and exported over D-Bus.
     */
    public final static String GATT_SERVICES = "GattServices";
    
    
    // Methods
    
    /**
     * This is a generic method to connect any profiles
     * the remote device supports that can be connected
     * to and have been flagged as auto-connectable on
     * our side. If only subset of profiles is already
     * connected it will try to connect currently disconnected
     * ones.<br/>
     * 
     * If at least one profile was connected successfully this
     * method will indicate success.
     */
    public void Connect();
    
    
    /**
     * This method gracefully disconnects all connected
     * profiles and then terminates low-level ACL connection.<br/>
     * 
     * ACL connection will be terminated even if some profiles
     * were not disconnected properly e.g. due to misbehaving
     * device.<br/>
     * 
     * This method can be also used to cancel a preceding
     * Connect call before a reply to it has been received.<br/>
     */
    public void Disconnect();
    
        
    /**
     * This method connects a specific profile of this
     * device. The UUID provided is the remote service
     * UUID for the profile.
     * @param UUID
     */
    public void ConnectProfile(String UUID);
    
    
    /**
     * This method disconnects a specific profile of
     * this device. The profile needs to be registered
     * client profile.<br/>
     * 
     * There is no connection tracking for a profile, so
     * as long as the profile is registered this will always
     * succeed.
     * @param UUID
     */
    public void DisconnectProfile(String UUID);
    
    
    /**
     * This method will connect to the remote device,
     * initiate pairing and then retrieve all SDP records
     * (or GATT primary services).<br/>
     * 
     * If the application has registered its own agent,
     * then that specific agent will be used. Otherwise
     * it will use the default agent.<br/>
     * 
     * Only for applications like a pairing wizard it
     * would make sense to have its own agent. In almost
     * all other cases the default agent will handle
     * this just fine.<br/>
     * 
     * In case there is no application agent and also
     * no default agent present, this method will fail.
     */
    public void Pair();
    
    
    /**
     * This method can be used to cancel a pairing
     * operation initiated by the Pair method.
     */
    public void CancelPairing();

}
