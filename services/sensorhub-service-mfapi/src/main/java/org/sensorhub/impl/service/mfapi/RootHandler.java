/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi;

import java.io.IOException;
import org.sensorhub.impl.service.mfapi.home.HomePageHandler;
import org.sensorhub.impl.service.consys.BaseHandler;
import org.sensorhub.impl.service.consys.ServiceErrors;
import org.sensorhub.impl.service.consys.resource.IResourceHandler;
import org.sensorhub.impl.service.consys.resource.RequestContext;


public class RootHandler extends BaseHandler
{
    static final String READ_ONLY_ERROR = "API is configured as read-only";
    
    HomePageHandler homePage;
    boolean readOnly;
    
    
    public RootHandler(HomePageHandler homePage, boolean readOnly)
    {
        this.homePage = homePage;
        this.readOnly = readOnly;
    }


    @Override
    public void doGet(RequestContext ctx) throws IOException
    {
        IResourceHandler resource = ctx.isEndOfPath() ?
            homePage :
            getSubResource(ctx);
        resource.doGet(ctx);
    }


    @Override
    public void doPost(RequestContext ctx) throws IOException
    {
        checkReadOnly();
        IResourceHandler resource = getSubResource(ctx);
        resource.doPost(ctx);
    }


    @Override
    public void doPut(RequestContext ctx) throws IOException
    {
        checkReadOnly();
        IResourceHandler resource = getSubResource(ctx);
        resource.doPut(ctx);
    }


    @Override
    public void doDelete(RequestContext ctx) throws IOException
    {
        checkReadOnly();
        IResourceHandler resource = getSubResource(ctx);
        resource.doDelete(ctx);
    }
    
    
    @Override
    public String[] getNames()
    {
        return new String[0];
    }
    
    
    protected void checkReadOnly() throws IOException
    {
        if (readOnly)
            throw ServiceErrors.unsupportedOperation(READ_ONLY_ERROR);
    }
}
