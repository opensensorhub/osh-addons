/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;


public class OshAuthorizers implements PublishAuthorizer, SubscriptionAuthorizer
{
    OshSubscribeHandler subscribeHandler;
    //OshPublishHandler publishHandler;
    
    
    OshAuthorizers(OshExtension oshExt)
    {
        this.subscribeHandler = new OshSubscribeHandler(oshExt);
        //this.publishHandler = new OshPublishHandler(oshExt);
    }
        
    
    @Override
    public void authorizeSubscribe(SubscriptionAuthorizerInput authInput, SubscriptionAuthorizerOutput authOutput)
    {
        subscribeHandler.authorizeSubscribe(authInput, authOutput);
    }
    

    @Override
    public void authorizePublish(PublishAuthorizerInput authInput, PublishAuthorizerOutput authOutput)
    {
        //publishHandler.authorizePublish(authInput, authOutput); 
    }
}
