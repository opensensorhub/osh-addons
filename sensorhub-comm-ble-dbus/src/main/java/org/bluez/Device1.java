package org.bluez;
import org.freedesktop.dbus.DBusInterface;
public interface Device1 extends DBusInterface
{

  public void Disconnect();
  public void Connect();
  public void ConnectProfile(String UUID);
  public void DisconnectProfile(String UUID);
  public void Pair();
  public void CancelPairing();

}
