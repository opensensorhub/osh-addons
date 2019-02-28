
package org.bluez;

import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Variant;


public interface HeartRateWatcher1 extends DBusInterface
{
    public void MeasurementReceived(Path device, Map<String,Variant<?>> measurement);
}
