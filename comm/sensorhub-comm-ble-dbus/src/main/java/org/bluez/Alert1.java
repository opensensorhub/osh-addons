package org.bluez;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt16;
public interface Alert1 extends DBusInterface
{

  public void RegisterAlert(String category, DBusInterface agent);
  public void NewAlert(String category, UInt16 count, String description);
  public void UnreadAlert(String category, UInt16 count);

}
