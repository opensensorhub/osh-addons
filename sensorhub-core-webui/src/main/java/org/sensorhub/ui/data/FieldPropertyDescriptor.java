/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.data;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.vaadin.data.Property;
import com.vaadin.data.util.VaadinPropertyDescriptor;
import com.vaadin.util.SerializerHelper;


public class FieldPropertyDescriptor<BT> implements VaadinPropertyDescriptor<BT>
{
    private static final long serialVersionUID = 2205054225621863646L;
    
    private final String name;
    private Class<?> propertyType;
    private transient Field field;


    /**
     * Creates a property descriptor that can create FieldProperty instances to
     * access the underlying bean property.
     * 
     * @param name of the property
     * @param propertyType type (class) of the property
     * @param field {@link Field} mirrored by the property
     */
    public FieldPropertyDescriptor(String name, Class<?> propertyType, Field field)
    {
        this.name = name;
        this.propertyType = propertyType;
        this.field = field;
    }


    /* Special serialization to handle field references */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        SerializerHelper.writeClass(out, propertyType);

        if (field != null)
        {
            out.writeObject(field.getName());
            SerializerHelper.writeClass(out, field.getDeclaringClass());
        }
        else
        {
            out.writeObject(null);
            out.writeObject(null);
        }
    }


    /* Special serialization to handle field references */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        
        try
        {
            @SuppressWarnings("unchecked")
            Class<BT> class1 = (Class<BT>) SerializerHelper.readClass(in);
            propertyType = class1;

            String name = (String) in.readObject();
            Class<?> parentClass = SerializerHelper.readClass(in);
            if (name != null)
            {
                field = parentClass.getField(name);
            }
            else
            {
                field = null;
            }
        }
        catch (SecurityException e)
        {
            getLogger().log(Level.SEVERE, "Internal deserialization error", e);
        }
        catch (NoSuchFieldException e)
        {
            getLogger().log(Level.SEVERE, "Internal deserialization error", e);
        }
    };


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public Class<?> getPropertyType()
    {
        return propertyType;
    }


    @Override
    public Property<?> createProperty(Object bean)
    {
        return new FieldProperty(bean, field);
    }


    private static final Logger getLogger()
    {
        return Logger.getLogger(FieldPropertyDescriptor.class.getName());
    }
}