/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta.filter;


import org.apache.commons.lang3.reflect.FieldUtils;
import org.sensorhub.api.resource.ResourceFilter;
import org.sensorhub.api.resource.ResourceFilter.ResourceFilterBuilder;
import org.vast.util.IResource;
import com.google.common.collect.Sets;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.Equal;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.comparison.NotEqual;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.logical.And;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.EndsWith;
import de.fraunhofer.iosb.ilt.frostserver.query.expression.function.string.StartsWith;


/**
 * <p>
 * Abstract base class for all visitors used to build resource filters from
 * STA $filter expression
 * </p>
 * 
 * @param <T> Type of visitor (self)
 * @param <B> Type of resource builder
 * @param <F> Type of filter
 *
 * @author Alex Robin
 * @date Apr 16, 2021
 */
public abstract class ResourceFilterVisitor<T extends ResourceFilterVisitor<T,B>, B extends ResourceFilterBuilder<B,?,?>> extends EntityFilterVisitor<T>
{
    static final String NAME_PROP = "name";
    static final String DESC_PROP = "description";
    B builder;
    
    
    protected ResourceFilterVisitor(B builder)
    {
        this.builder = builder;
        this.propTypes.put("name", NameVisitor.class);
        this.propTypes.put("description", DescVisitor.class);
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public T visit(And node)
    {
        var v1 = node.getParameters().get(0).accept(getNewInstance());
        var v2 = node.getParameters().get(1).accept(getNewInstance());
        
        try {
            var f1 = (ResourceFilter<IResource>)v1.builder.build();
            var f2 = (ResourceFilter<IResource>)v2.builder.build();
            var builder = (ResourceFilterBuilder<?,IResource,ResourceFilter<IResource>>)this.builder;
            
            if (f1.getFullTextFilter() != null && f2.getFullTextFilter() != null)
            {
                // OR keywords so we get a chance to scan multiple properties
                // otherwise intersection will often be empty
                var kw1 = f1.getFullTextFilter().getKeywords();
                var kw2 = f2.getFullTextFilter().getKeywords();
                
                // need to first remove fulltext from one of the filters
                // hack to use reflection since filters are normally immutable
                FieldUtils.writeField(f2, "fullText", null, true);
                
                builder.copyFrom(f1.intersect(f2));
                builder.withKeywords(Sets.union(kw1, kw2));
            }
            else
                builder.copyFrom(f1.intersect(f2));
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid AND filter", e);
        }        
        
        return (T)this;
    }
    
    
    /*@Override
    public T visit(Or node)
    {
        return this.visitBinaryOp(node);
    }
    
    
    @Override
    public T visit(Not node)
    {
        return this.visitBinaryOp(node);
    }*/
    
    
    class NameVisitor extends StringPropVisitor
    {
        @Override
        public NameVisitor visit(Equal node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getName().equals(s));
            return this;
        }

        @Override
        public NameVisitor visit(NotEqual node)
        {
            var s = getStringLiteral(node);
            builder.withValuePredicate(r -> !r.getName().equals(s));
            return this;
        }

        @Override
        public NameVisitor visit(EndsWith node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getName().endsWith(s));
            return this;
        }

        @Override
        public NameVisitor visit(StartsWith node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getName().startsWith(s));
            return this;
        }        
    }
    
    
    class DescVisitor extends StringPropVisitor
    {
        @Override
        public DescVisitor visit(Equal node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getDescription().equals(s));
            return this;
        }

        @Override
        public DescVisitor visit(NotEqual node)
        {
            var s = getStringLiteral(node);
            builder.withValuePredicate(r -> !r.getDescription().equals(s));
            return this;
        }

        @Override
        public DescVisitor visit(EndsWith node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getDescription().endsWith(s));
            return this;
        }

        @Override
        public DescVisitor visit(StartsWith node)
        {
            var s = getStringLiteral(node);
            builder.withKeywords(s);
            builder.withValuePredicate(r -> r.getDescription().startsWith(s));
            return this;
        }        
    }
}
