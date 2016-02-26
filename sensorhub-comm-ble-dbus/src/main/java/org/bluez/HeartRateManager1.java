package org.bluez;
import org.freedesktop.dbus.DBusInterface;
public interface HeartRateManager1 extends DBusInterface
{

  public void RegisterWatcher(DBusInterface agent);
  public void UnregisterWatcher(DBusInterface agent);

}
