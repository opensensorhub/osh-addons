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
      public final String objPath;
      public final Map<String,Map<String,Variant<?>>> interfaces;
      public InterfacesAdded(String path, Path object_path, Map<String,Map<String,Variant<?>>> interfaces) throws DBusException
      {
         super(path, object_path, interfaces);
         this.objPath = object_path.getPath();
         this.interfaces = interfaces;
      }
   }
   
   public static class InterfacesRemoved extends DBusSignal
   {
      public final String objectPath;
      public final List<String> interfaces;
      public InterfacesRemoved(String path, Path object_path, List<String> interfaces) throws DBusException
      {
         super(path, object_path, interfaces);
         this.objectPath = object_path.getPath();;
         this.interfaces = interfaces;
      }
   }

  public Map<Path, Map<String,Map<String,Variant<?>>>> GetManagedObjects();

}
