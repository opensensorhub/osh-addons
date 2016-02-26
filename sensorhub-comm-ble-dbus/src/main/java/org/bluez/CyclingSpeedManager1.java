package org.bluez;
import org.freedesktop.dbus.DBusInterface;
public interface CyclingSpeedManager1 extends DBusInterface
{

  public void RegisterWatcher(DBusInterface agent);
  public void UnregisterWatcher(DBusInterface agent);

}
