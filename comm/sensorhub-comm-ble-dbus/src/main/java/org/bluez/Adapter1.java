/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.bluez;

import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;


/**
 * <p>
 * BlueZ D-Bus Adapter API
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.Adapter1<br/>
 * <b>Object path:</b> [variable prefix]/{hci0,hci1,...}<br/>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 25, 2016
 */
public interface Adapter1 extends DBusInterface
{
    // Properties
    
    /**
     * string Address [readonly]<br/>
     * 
     * The Bluetooth device address.
     */
    public final static String ADDRESS = "Address";
    
    
    /**
     * string Name [readonly]<br/>
     * 
     * The Bluetooth system name (pretty hostname).<br/>
     * 
     * This property is either a static system default
     * or controlled by an external daemon providing
     * access to the pretty hostname configuration.
     */
    public final static String NAME = "Name";
        
    
    /**
     * string Alias [readwrite]<br/>
     * 
     * The Bluetooth friendly name. This value can be
     * changed.<br/>
     * 
     * In case no alias is set, it will return the system
     * provided name. Setting an empty string as alias will
     * convert it back to the system provided name.<br/>
     * 
     * When resetting the alias with an empty string, the
     * property will default back to system name.<br/>

     * On a well configured system, this property never
     * needs to be changed since it defaults to the system
     * name and provides the pretty hostname. Only if the
     * local name needs to be different from the pretty
     * hostname, this property should be used as last
     * resort.
     */
    public final static String ALIAS = "Alias";
    
    
    
    /**
     * uint32 Class [readonly]<br/>
     * 
     * The Bluetooth class of device.<br/>
     * 
     * This property represents the value that is either
     * automatically configured by DMI/ACPI information
     * or provided as static configuration.
     */
    public final static String CLASS = "Class";
    
    
    /**
     * boolean Powered [readwrite]<br/>
     * 
     * Switch an adapter on or off. This will also set the
     * appropriate connectable state of the controller.<br/>
     * 
     * The value of this property is not persistent. After
     * restart or unplugging of the adapter it will reset
     * back to false.
     */
    public final static String POWERED = "Powered";
    
    
    /**
     * boolean Discoverable [readwrite]<br/>
     * 
     * Switch an adapter to discoverable or non-discoverable
     * to either make it visible or hide it. This is a global
     * setting and should only be used by the settings
     * application.<br/>
     * 
     * If the DiscoverableTimeout is set to a non-zero
     * value then the system will set this value back to
     * false after the timer expired.<br/>
     * 
     * In case the adapter is switched off, setting this
     * value will fail.<br/>
     * 
     * When changing the Powered property the new state of
     * this property will be updated via a PropertyChanged
     * signal.<br/>
     * 
     * For any new adapter this settings defaults to false.
     */
    public final static String DISCOVERABLE = "Discoverable";
    
    
    /**
     * uint32 DiscoverableTimeout [readwrite]<br/>
     * 
     * The discoverable timeout in seconds. A value of zero
     * means that the timeout is disabled and it will stay in
     * discoverable/limited mode forever.<br/>
     * 
     * The default value for the discoverable timeout should
     * be 180 seconds (3 minutes).
     */
    public final static String DISCOVERABLE_TIMEOUT = "DiscoverableTimeout";
    
    
    /**
     * boolean Pairable [readwrite]<br/>
     * 
     * Switch an adapter to pairable or non-pairable. This is
     * a global setting and should only be used by the
     * settings application.<br/>
     * 
     * Note that this property only affects incoming pairing
     * requests.<br/>
     * 
     * For any new adapter this settings defaults to true.
     */
    public final static String PAIRABLE = "Pairable";
    
    
    /**
     * uint32 PairableTimeout [readwrite]<br/>
     * 
     * The pairable timeout in seconds. A value of zero
     * means that the timeout is disabled and it will stay in
     * pairable mode forever.<br/>
     * 
     * The default value for pairable timeout should be
     * disabled (value 0).
     */
    public final static String PAIRABLE_TIMEOUT = "PairableTimeout";
        
    
    /**
     * boolean Discovering [readonly]<br/>
     * 
     * Indicates that a device discovery procedure is active.
     */
    public final static String DISCOVERING = "Discovering";
    
    
    /**
     * array{string} UUIDs [readonly]<br/>
     * 
     * List of 128-bit UUIDs that represents the available
     * local services.
     */
    public final static String SERVICE_UUIDS = "UUIDs";
    
    
    /**
     * string Modalias [readonly, optional]<br/>
     * 
     * Local Device ID information in modalias format
     * used by the kernel and udev.
     */
    public final static String MODALIAS = "Modalias";
    
    
    
    // Methods
    
    /**
     * This method starts the device discovery session. This
     * includes an inquiry procedure and remote device name
     * resolving. Use StopDiscovery to release the sessions
     * acquired.<br/>
     *
     * This process will start creating Device objects as
     * new devices are discovered.<br/>

     * During discovery RSSI delta-threshold is imposed.
     */
    public void StartDiscovery();
    
    
    /**
     * This method will cancel any previous StartDiscovery
     * transaction.<br/>
     * 
     * Note that a discovery procedure is shared between all
     * discovery sessions thus calling StopDiscovery will only
     * release a single session.
     */
    public void StopDiscovery();
    
    
    /**
     * This method sets the device discovery filter for the
     * caller. When this method is called with no filter
     * parameter, filter is removed.<br/>
     * 
     * Parameters that may be set in the filter dictionary
     * include the following:<br/>
     * 
     * array{string} UUIDs : filtered service UUIDs<br/>
     * int16         RSSI  : RSSI threshold value<br/>
     * uint16        Pathloss  : Pathloss threshold value<br/>
     * string        Transport : type of scan to run<br/>

     * When a remote device is found that advertises any UUID
     * from UUIDs, it will be reported if:
     * - Pathloss and RSSI are both empty,
     * - only Pathloss param is set, device advertise TX pwer,
     *   and computed pathloss is less than Pathloss param,
     * - only RSSI param is set, and received RSSI is higher
     *   than RSSI param,
     * 
     * Transport parameter determines the type of scan:
     *      "auto"  - interleaved scan, default value
     *      "bredr" - BR/EDR inquiry
     *      "le"    - LE scan only
     * 
     * If "le" or "bredr" Transport is requested, and the
     * controller doesn't support it, org.bluez.Error.Failed
     * error will be returned. If "auto" transport is
     * requested, scan will use LE, BREDR, or both, depending
     * on what's currently enabled on the controller.<br/>
     * 
     * When discovery filter is set, Device objects will be
     * created as new devices with matching criteria are
     * discovered. PropertiesChanged signals will be emitted
     * for already existing Device objects, with updated RSSI
     * value. If one or more discovery filters have been set,
     * the RSSI delta-threshold, that is imposed by
     * StartDiscovery by default, will not be applied.<br/>
     * 
     * When multiple clients call SetDiscoveryFilter, their
     * filters are internally merged, and notifications about
     * new devices are sent to all clients. Therefore, each
     * client must check that device updates actually match
     * its filter.<br/>
     * 
     * When SetDiscoveryFilter is called multiple times by the
     * same client, last filter passed will be active for
     * given client.<br/>
     * 
     * SetDiscoveryFilter can be called before StartDiscovery.
     * It is useful when client will create first discovery
     * session, to ensure that proper scan will be started
     * right after call to StartDiscovery.
     * @param properties
     */
    public void SetDiscoveryFilter(Map<String,Variant<?>> properties);
    
    
    /**
     * This removes the remote device object at the given
     * path. It will remove also the pairing information.
     * @param device
     */
    public void RemoveDevice(Device1 device);
}
