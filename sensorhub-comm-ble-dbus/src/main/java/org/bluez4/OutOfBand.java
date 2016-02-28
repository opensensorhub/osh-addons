package org.bluez4;
import java.util.List;
import org.freedesktop.dbus.DBusInterface;
public interface OutOfBand extends DBusInterface
{

  public void AddRemoteData(String address, List<Byte> hash, List<Byte> randomizer);
  public void RemoveRemoteData(String address);
  public Pair<List<Byte>, List<Byte>> ReadLocalData();

}
