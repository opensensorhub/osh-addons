package org.bluez;
import java.util.Map;
import org.freedesktop.DBus.Deprecated;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Headset extends DBusInterface
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
   public static class AnswerRequested extends DBusSignal
   {
      public AnswerRequested(String path) throws DBusException
      {
         super(path);
      }
   }
  @Deprecated
   public static class Stopped extends DBusSignal
   {
      public Stopped(String path) throws DBusException
      {
         super(path);
      }
   }
  @Deprecated
   public static class Playing extends DBusSignal
   {
      public Playing(String path) throws DBusException
      {
         super(path);
      }
   }
  @Deprecated
   public static class SpeakerGainChanged extends DBusSignal
   {
      public final UInt16 gain;
      public SpeakerGainChanged(String path, UInt16 gain) throws DBusException
      {
         super(path, gain);
         this.gain = gain;
      }
   }
  @Deprecated
   public static class MicrophoneGainChanged extends DBusSignal
   {
      public final UInt16 gain;
      public MicrophoneGainChanged(String path, UInt16 gain) throws DBusException
      {
         super(path, gain);
         this.gain = gain;
      }
   }
   public static class CallTerminated extends DBusSignal
   {
      public CallTerminated(String path) throws DBusException
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

  public void Connect();
  public void Disconnect();
  public boolean IsConnected();
  public void IndicateCall();
  public void CancelCall();
  @Deprecated
  public void Play();
  public void Stop();
  @Deprecated
  public boolean IsPlaying();
  @Deprecated
  public UInt16 GetSpeakerGain();
  @Deprecated
  public UInt16 GetMicrophoneGain();
  @Deprecated
  public void SetSpeakerGain(UInt16 gain);
  @Deprecated
  public void SetMicrophoneGain(UInt16 gain);
  public Map<String,Variant> GetProperties();
  public void SetProperty(String name, Variant value);

}
