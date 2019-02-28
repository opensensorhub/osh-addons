package org.bluez;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;


/**
 * <p>
 * BlueZ D-Bus Profile Manager API
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.ProfileManager1<br/>
 * <b>Object path:</b> /org/bluez<br/>
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 29, 2016
 */
public interface ProfileManager1 extends DBusInterface
{
    
    /**
     * This registers a profile implementation.<br/>
     * 
     * If an application disconnects from the bus all
     * its registered profiles will be removed.
     * 
            HFP HS UUID: 0000111e-0000-1000-8000-00805f9b34fb

                Default RFCOMM channel is 6. And this requires
                authentication.

            Available options:

                string Name

                    Human readable name for the profile

                string Service

                    The primary service class UUID
                    (if different from the actual
                     profile UUID)

                string Role

                    For asymmetric profiles that do not
                    have UUIDs available to uniquely
                    identify each side this
                    parameter allows specifying the
                    precise local role.

                    Possible values: "client", "server"

                uint16 Channel

                    RFCOMM channel number that is used
                    for client and server UUIDs.

                    If applicable it will be used in the
                    SDP record as well.

                uint16 PSM

                    PSM number that is used for client
                    and server UUIDs.

                    If applicable it will be used in the
                    SDP record as well.

                boolean RequireAuthentication

                    Pairing is required before connections
                    will be established. No devices will
                    be connected if not paired.

                boolean RequireAuthorization

                    Request authorization before any
                    connection will be established.

                boolean AutoConnect

                    In case of a client UUID this will
                    force connection of the RFCOMM or
                    L2CAP channels when a remote device
                    is connected.

                string ServiceRecord

                    Provide a manual SDP record.

                uint16 Version

                    Profile version (for SDP record)

                uint16 Features

                    Profile features (for SDP record)
     * @param profile
     * @param UUID
     * @param options
     */
    public void RegisterProfile(DBusInterface profile, String UUID, Map<String,Variant<?>> options);
  
  
    /**
     * This unregisters the profile that has been previously
     * registered. The object path parameter must match the
     * same value that has been used on registration.
     * @param profile
     */
    public void UnregisterProfile(DBusInterface profile);

}
