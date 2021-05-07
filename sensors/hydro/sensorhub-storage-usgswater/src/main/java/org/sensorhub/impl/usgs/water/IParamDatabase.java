/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;


public interface IParamDatabase
{
    
    public static class USGSParam
    {
        public String code;
        public String uri;
        public String group;
        public String name;
        public String unit;
        public boolean isCategory;
        public boolean isCount;
    }
    
    
    public USGSParam getParamById(String id);
    
    
    public String getParamCode(String uri);
    
}
