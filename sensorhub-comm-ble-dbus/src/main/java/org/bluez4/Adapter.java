package org.bluez4;
import java.util.List;
import java.util.Map;
import org.freedesktop.DBus.Deprecated;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Adapter extends DBusInterface
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
   public static class DeviceCreated extends DBusSignal
   {
      public final DBusInterface device;
      public DeviceCreated(String path, DBusInterface device) throws DBusException
      {
         super(path, device);
         this.device = device;
      }
   }
   public static class DeviceRemoved extends DBusSignal
   {
      public final DBusInterface device;
      public DeviceRemoved(String path, DBusInterface device) throws DBusException
      {
         super(path, device);
         this.device = device;
      }
   }
   public static class DeviceFound extends DBusSignal
   {
      public final String address;
      public final Map<String,Variant> values;
      public DeviceFound(String path, String address, Map<String,Variant> values) throws DBusException
      {
         super(path, address, values);
         this.address = address;
         this.values = values;
      }
   }
   public static class DeviceDisappeared extends DBusSignal
   {
      public final String address;
      public DeviceDisappeared(String path, String address) throws DBusException
      {
         super(path, address);
         this.address = address;
      }
   }

  public Map<String,Variant> GetProperties();
  public void SetProperty(String name, Variant value);
  public void RequestSession();
  public void ReleaseSession();
  public void StartDiscovery();
  public void StopDiscovery();
  @Deprecated
  public List<DBusInterface> ListDevices();
  public DBusInterface CreateDevice(String address);
  public DBusInterface CreatePairedDevice(String address, DBusInterface agent, String capability);
  public void CancelDeviceCreation(String address);
  public void RemoveDevice(DBusInterface device);
  public DBusInterface FindDevice(String address);
  public void RegisterAgent(DBusInterface agent, String capability);
  public void UnregisterAgent(DBusInterface agent);

}
