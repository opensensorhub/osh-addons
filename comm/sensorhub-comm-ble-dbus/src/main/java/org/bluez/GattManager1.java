package org.bluez;
import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;


/**
 * <p>
 * BlueZ D-Bus GATT Manager API
 * </p>
 * <br/>
 * <p>
 * <b>Service:</b> org.bluez<br/>
 * <b>Interface:</b> org.bluez.GattManager1 [Experimental]<br/>
 * <b>Object path:</b> [variable prefix]/{hci0,hci1,...}<br/>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 29, 2016
 */
public interface GattManager1 extends DBusInterface
{

  public void RegisterService(DBusInterface service, Map<String,Variant<?>> options);
  public void UnregisterService(DBusInterface service);
  public void RegisterProfile(DBusInterface profile, List<String> UUIDs, Map<String,Variant<?>> options);
  public void UnregisterProfile(DBusInterface profile);

}
