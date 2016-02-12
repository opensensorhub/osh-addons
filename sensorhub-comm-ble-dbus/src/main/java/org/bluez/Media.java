package org.bluez;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;
public interface Media extends DBusInterface
{

  public void RegisterEndpoint(DBusInterface endpoint, Map<String,Variant> properties);
  public void UnregisterEndpoint(DBusInterface endpoint);
  public void RegisterPlayer(DBusInterface player, Map<String,Variant> properties, Map<String,Variant> metadata);
  public void UnregisterPlayer(DBusInterface player);

}
