/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.freedesktop.dbus;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.exceptions.DBusException;


@DBusInterfaceName("org.freedesktop.DBus.Properties")
public interface Properties extends DBusInterface
{
    public class PropertiesChanged extends DBusSignal
    {
        public final String interfaceName;
        public final Map<String,Variant<?>> changedProps;
        public final List<String> invalidatedProps;
        public PropertiesChanged(String path, String interface_name, Map<String,Variant<?>> changed_properties, List<String> invalidated_properties) throws DBusException
        {
           super(path, changed_properties, invalidated_properties);
           this.interfaceName = interface_name;
           this.changedProps = changed_properties;
           this.invalidatedProps = invalidated_properties;
        }
    }
    
    /**
     * Get the value for the given property.
     * @param interface_name The interface this property is associated with.
     * @param property_name The name of the property.
     * @return The value of the property (may be any valid DBus type).
     */
    public <A> A Get(String interface_name, String property_name);
    
    /**
     * Set the value for the given property.
     * @param interface_name The interface this property is associated with.
     * @param property_name The name of the property.
     * @param value The new value of the property (may be any valid DBus type).
     */
    public <A> void Set(String interface_name, String property_name, A value);
    
    /**
     * Get all properties and values.
     * @param interface_name The interface the properties is associated with.
     * @return The properties mapped to their values.
     */
    public Map<String, Variant<?>> GetAll(String interface_name);
}
