package org.bluez;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;
public interface ProfileManager1 extends DBusInterface
{

  public void RegisterProfile(DBusInterface profile, String UUID, Map<String,Variant> options);
  public void UnregisterProfile(DBusInterface profile);

}
