/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.data;

import java.lang.reflect.Field;
import com.vaadin.data.util.AbstractProperty;


@SuppressWarnings("serial")
class FieldProperty extends AbstractProperty<Object>
{
    Object instance;
    Field f;
    boolean readOnly;


    public FieldProperty(Object instance, Field f)
    {
        this.instance = instance;
        this.f = f;
    }


    @Override
    public Object getValue()
    {
        try
        {
            return f.get(instance);
        }
        catch (Exception e)
        {
            return null;
        }
    }


    @Override
    public void setValue(Object newValue) throws ReadOnlyException
    {
        try
        {
            f.set(instance, newValue);
            fireValueChange();
        }
        catch (Exception e)
        {
        }
    }


    @Override
    public Class<?> getType()
    {
        return convertPrimitiveType(f.getType());
    }


    @Override
    public boolean isReadOnly()
    {
        return readOnly;
    }


    @Override
    public void setReadOnly(boolean newStatus)
    {
        this.readOnly = newStatus;
    }


    private Class<?> convertPrimitiveType(Class<?> type)
    {
        // Gets the return type from get method
        if (type.isPrimitive())
        {
            if (type.equals(Boolean.TYPE))
            {
                type = Boolean.class;
            }
            else if (type.equals(Integer.TYPE))
            {
                type = Integer.class;
            }
            else if (type.equals(Float.TYPE))
            {
                type = Float.class;
            }
            else if (type.equals(Double.TYPE))
            {
                type = Double.class;
            }
            else if (type.equals(Byte.TYPE))
            {
                type = Byte.class;
            }
            else if (type.equals(Character.TYPE))
            {
                type = Character.class;
            }
            else if (type.equals(Short.TYPE))
            {
                type = Short.class;
            }
            else if (type.equals(Long.TYPE))
            {
                type = Long.class;
            }
        }
        return type;
    }
}