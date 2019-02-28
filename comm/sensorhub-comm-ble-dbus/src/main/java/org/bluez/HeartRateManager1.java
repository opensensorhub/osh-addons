
package org.bluez;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Path;


public interface HeartRateManager1 extends DBusInterface
{

    public void RegisterWatcher(Path agent);


    public void UnregisterWatcher(Path agent);

}
