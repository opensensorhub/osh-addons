package org.bluez4;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Device extends DBusInterface
{
   public static class PropertyChanged extends DBusSignal
   {
      public final String name;
      public final Variant value;
      public PropertyChanged(String path, String name, Variant value) throws DBusException
      {
         super(path, name, value);
         this.name = name;
         this.value = value;
      }
   }
   public static class DisconnectRequested extends DBusSignal
   {
      public DisconnectRequested(String path) throws DBusException
      {
         super(path);
      }
   }

  public Map<String,Variant> GetProperties();
  public void SetProperty(String name, Variant value);
  public Map<UInt32,String> DiscoverServices(String pattern);
  public void CancelDiscovery();
  public void Disconnect();

}
