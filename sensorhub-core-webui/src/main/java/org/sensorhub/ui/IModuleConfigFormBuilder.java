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

package org.sensorhub.ui;

import org.sensorhub.api.module.ModuleConfig;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.ui.FormLayout;


/**
 * <p>
 * Interface for all form panel used to configure modules
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 11, 2013
 */
public interface IModuleConfigFormBuilder<ConfigType extends ModuleConfig>
{
    
    /**
     * Gets the title of the form
     * @param config
     * @return
     */
    public String getTitle(ModuleConfig config);
    
    
    /**
     * Builds the whole form for a given config object.
     * @param form
     * @param config
     */
    public void buildForm(FormLayout form, FieldGroup fieldGroup);
    
}
