/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.model.uxs;

public class PrintManeuverCommands
{

    public static void main(String[] args) throws Exception
    {
        var uxs = new UxsHelper();
        
        var uavState = uxs.createUavMechanicalState().build();
        PrintUtils.print(uavState, false, true);
        
        var usvState = uxs.createUsvMechanicalState().build();
        PrintUtils.print(usvState, false, true);
        
        var uuvState = uxs.createUuvMechanicalState().build();
        PrintUtils.print(uuvState, false, true);

    }

}
