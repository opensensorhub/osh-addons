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

import java.lang.reflect.Field;
import org.sensorhub.api.config.Annotations.DisplayInfo;
import com.vaadin.data.Container;
import com.vaadin.data.util.AbstractProperty;


@SuppressWarnings("serial")
public class ContainerProperty extends AbstractProperty<Container>
{
    Object instance;
    Field f;
    Container container;
    

    public ContainerProperty(Object instance, Field f, Container container)
    {
        this.instance = instance;
        this.f = f;
        this.container = container;
    }


    @Override
    public Container getValue()
    {
        return container;
    }


    @Override
    public void setValue(Container newValue) throws ReadOnlyException
    {
        // container instance cannot be changed
    }


    @Override
    public Class<? extends Container> getType()
    {
        return container.getClass();
    }
    
    
    public String getLabel()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null)
            return ann.label();
        else
            return null;
    }
}