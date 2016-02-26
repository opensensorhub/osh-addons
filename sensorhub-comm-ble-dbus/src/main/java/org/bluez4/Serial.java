package org.bluez4;
import org.freedesktop.dbus.DBusInterface;
public interface Serial extends DBusInterface
{

  public String Connect(String pattern);
  public String ConnectFD(String pattern);
  public void Disconnect(String device);

}
