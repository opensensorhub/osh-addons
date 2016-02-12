package org.bluez;
import java.util.List;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;
public interface SerialProxyManager extends DBusInterface
{
   public static class ProxyCreated extends DBusSignal
   {
      public final String path;
      public ProxyCreated(String path, String path2) throws DBusException
      {
         super(path, path2);
         this.path = path;
      }
   }
   public static class ProxyRemoved extends DBusSignal
   {
      public final String path;
      public ProxyRemoved(String path, String path2) throws DBusException
      {
         super(path, path2);
         this.path = path;
      }
   }

  public String CreateProxy(String pattern, String address);
  public List<String> ListProxies();
  public void RemoveProxy(String path);

}
