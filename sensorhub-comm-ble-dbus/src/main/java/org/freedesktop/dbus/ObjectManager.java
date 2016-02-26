package org.freedesktop.dbus;
import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

@DBusInterfaceName("org.freedesktop.DBus.ObjectManager")
public interface ObjectManager extends DBusInterface
{
   public static class InterfacesAdded extends DBusSignal
   {
      public final DBusInterface object;
      public final Map<String,Map<String,Variant<?>>> interfaces;
      public InterfacesAdded(String path, DBusInterface object, Map<String,Map<String,Variant<?>>> interfaces) throws DBusException
      {
         super(path, object, interfaces);
         this.object = object;
         this.interfaces = interfaces;
      }
   }
   
   public static class InterfacesRemoved extends DBusSignal
   {
      public final DBusInterface object;
      public final List<String> interfaces;
      public InterfacesRemoved(String path, DBusInterface object, List<String> interfaces) throws DBusException
      {
         super(path, object, interfaces);
         this.object = object;
         this.interfaces = interfaces;
      }
   }

  public Map<Path, Map<String,Map<String,Variant<?>>>> GetManagedObjects();

}
