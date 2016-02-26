package org.bluez4;
import java.util.List;
import java.util.Map;
import org.freedesktop.DBus.Deprecated;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Manager extends DBusInterface
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
   public static class AdapterAdded extends DBusSignal
   {
      public final DBusInterface adapter;
      public AdapterAdded(String path, DBusInterface adapter) throws DBusException
      {
         super(path, adapter);
         this.adapter = adapter;
      }
   }
   public static class AdapterRemoved extends DBusSignal
   {
      public final DBusInterface adapter;
      public AdapterRemoved(String path, DBusInterface adapter) throws DBusException
      {
         super(path, adapter);
         this.adapter = adapter;
      }
   }
   public static class DefaultAdapterChanged extends DBusSignal
   {
      public final DBusInterface adapter;
      public DefaultAdapterChanged(String path, DBusInterface adapter) throws DBusException
      {
         super(path, adapter);
         this.adapter = adapter;
      }
   }

  public Map<String,Variant> GetProperties();
  public Path DefaultAdapter();
  public Path FindAdapter(String pattern);
  @Deprecated
  public List<Path> ListAdapters();

}
