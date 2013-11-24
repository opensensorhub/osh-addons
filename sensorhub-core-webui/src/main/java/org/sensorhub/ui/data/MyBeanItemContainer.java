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

import java.util.Map;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.VaadinPropertyDescriptor;


@SuppressWarnings("serial")
public class MyBeanItemContainer<BT> extends BeanItemContainer<BT>
{

    public MyBeanItemContainer(Class<? super BT> type) throws IllegalArgumentException
    {
        super(type);
        
        // remove all properties added by super type
        for (Object id: this.getContainerPropertyIds())
            this.removeContainerProperty(id);
        
        // add our property ids
        Map<String, VaadinPropertyDescriptor<BT>> pds = MyBeanItem.getPropertyDescriptors((Class<BT>)type);
        for (VaadinPropertyDescriptor<BT> pd: pds.values())
            addContainerProperty(pd.getName(), pd);
    }

    
    @Override
    protected BeanItem<BT> createBeanItem(BT bean)
    {
        return bean == null ? null : new MyBeanItem<BT>(bean);
    }
    
}
