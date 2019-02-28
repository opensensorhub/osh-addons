package org.bluez;
import org.freedesktop.dbus.DBusInterface;
public interface AgentManager1 extends DBusInterface
{

  public void RegisterAgent(DBusInterface agent, String capability);
  public void UnregisterAgent(DBusInterface agent);
  public void RequestDefaultAgent(DBusInterface agent);

}
