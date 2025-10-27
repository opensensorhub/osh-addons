/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.comm.jssc;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.impl.comm.UARTConfig;


public class JsscSerialCommProviderConfig extends CommProviderConfig<UARTConfig>
{

    public JsscSerialCommProviderConfig()
    {
        this.moduleClass = JsscSerialCommProvider.class.getCanonicalName();
        this.protocol = new UARTConfig();
    }
}
