package org.bluez;
import org.freedesktop.dbus.DBusInterface;
public interface ThermometerManager1 extends DBusInterface
{

  public void RegisterWatcher(DBusInterface agent);
  public void UnregisterWatcher(DBusInterface agent);
  public void EnableIntermediateMeasurement(DBusInterface agent);
  public void DisableIntermediateMeasurement(DBusInterface agent);

}
