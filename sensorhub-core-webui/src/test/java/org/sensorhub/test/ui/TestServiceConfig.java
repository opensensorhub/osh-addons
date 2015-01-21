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

package org.sensorhub.test.ui;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.impl.service.ogc.OGCServiceConfig;


@SuppressWarnings("serial")
public class TestServiceConfig extends OGCServiceConfig
{
    public class TestOption
    {
        public String name;
        public int number;
        public String text;
        
        public TestOption(String name, int number, String text)
        {
            this.name = name;
            this.number = number;
            this.text = text;
        }
    }
    
    
    public List<TestOption> optionList = new ArrayList<TestOption>();
    
    
    public TestServiceConfig()
    {
        optionList.add(new TestOption("opt1", 10, "text"));
        optionList.add(new TestOption("opt2", 20, "text text"));
        optionList.add(new TestOption("opt3", 30, "text text text"));
    }
}
