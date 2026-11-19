/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi.mf;

import java.io.IOException;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.impl.service.consys.feature.AbstractFeatureBindingHtml;
import org.sensorhub.impl.service.consys.feature.FeatureUtils;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.system.SystemHandler;
import org.vast.ogc.gml.IFeature;
import j2html.tags.specialized.DivTag;
import static j2html.TagCreator.*;


/**
 * <p>
 * HTML formatter for moving feature resources
 * </p>
 *
 * @author Alex Robin
 * @since Aug 15, 2022
 */
public class MFBindingHtml extends AbstractFeatureBindingHtml<IFeature, IObsSystemDatabase>
{
    final String collectionTitle;
    
    
    public MFBindingHtml(RequestContext ctx, IdEncoders idEncoders, boolean isSummary, IObsSystemDatabase db) throws IOException
    {
        super(ctx, idEncoders, db, isSummary, true);
        
        // set collection title depending on path
        if (ctx.getParentID() != null)
        {
            if (ctx.getParentRef().type instanceof SystemHandler)
            {
                // fetch parent system name
                var parentSys = FeatureUtils.getClosestToNow(db.getSystemDescStore(), ctx.getParentID());
                this.collectionTitle = "Features observed by " + parentSys.getName();
            }
            else
            {
                // fetch parent feature name
                var parentFeature = FeatureUtils.getClosestToNow(db.getFoiStore(), ctx.getParentID());
                
                if (isHistory)
                    this.collectionTitle = "History of " + parentFeature.getName();
                else
                    this.collectionTitle = "Members of " + parentFeature.getName();
            }
        }
        else
            this.collectionTitle = "All Moving Features";
    }
    
    
    @Override
    protected String getResourceName()
    {
        return "Feature";
    }
    
    
    @Override
    protected String getCollectionTitle()
    {
        return collectionTitle;
    }
    
    
    @Override
    protected String getResourceUrl(FeatureKey key)
    {
        var featureId = idEncoders.getFoiIdEncoder().encodeID(key.getInternalID());
        return isCollection ? ctx.getRequestUrl() + "/" + featureId : ctx.getRequestUrl();
    }
    
    
    @Override
    protected DivTag getLinks(String resourceUrl, FeatureKey key, IFeature f)
    {
        return div(
            a("Temporal Geometries").withHref(resourceUrl + "/tgeometries").withClasses(CSS_LINK_BTN_CLASSES),
            a("Temporal Properties").withHref(resourceUrl + "/tproperties").withClasses(CSS_LINK_BTN_CLASSES)
        ).withClass("mt-4");
    }


    @Override
    protected void serializeDetails(FeatureKey key, IFeature res) throws IOException
    {
        // do nothing since simple features don't have a details page
    }
}
