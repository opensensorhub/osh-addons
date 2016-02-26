package org.bluez4;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt32;
public interface Service extends DBusInterface
{

  public UInt32 AddRecord(String record);
  public void UpdateRecord(UInt32 handle, String record);
  public void RemoveRecord(UInt32 handle);
  public void RequestAuthorization(String address, UInt32 handle);
  public void CancelAuthorization();

}
