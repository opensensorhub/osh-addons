package org.bluez;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;
public interface HealthManager extends DBusInterface
{

  public DBusInterface CreateApplication(Map<String,Variant> config);
  public void DestroyApplication(DBusInterface application);

}
