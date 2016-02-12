package org.bluez;
import java.util.Map;
import org.freedesktop.DBus.Deprecated;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Control extends DBusInterface
{
  @Deprecated
   public static class Connected extends DBusSignal
   {
      public Connected(String path) throws DBusException
      {
         super(path);
      }
   }
  @Deprecated
   public static class Disconnected extends DBusSignal
   {
      public Disconnected(String path) throws DBusException
      {
         super(path);
      }
   }
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

  @Deprecated
  public boolean IsConnected();
  public Map<String,Variant> GetProperties();
  public void VolumeUp();
  public void VolumeDown();

}
