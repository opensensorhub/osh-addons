/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.helpers;

import net.opengis.swe.v20.Text;
import org.vast.swe.SWEBuilders.*;
import org.vast.swe.SWEHelper;


public class NmeaAisHelper extends SWEHelper{

    public CountBuilder createNmeaMessageId() {
        return createCount()
                .label("Message Id")
                .description("AIS Message Identifier")
                .definition(getPropertyUri("MessageId"));

    }

    public TextBuilder createReportDescription() {
        return createText()
                .label("Report Description")
                .description("Describes the report based on the Message Id provided")
                .definition(getPropertyUri("ReportDescription"));
    }

    public CountBuilder createRepeatIndicator(){
        return createCount()
                .label("Repeat Indicator")
                .description("Used by the repeater to indicate how many times a message has been repeated; 0-3; 0 = default")
                .definition(SWEHelper.getPropertyUri("repeat"));
    }

    public TextBuilder createMssi() {
        return createText()
                .label("MMSI Number")
                .description("MMSI Number")
                .definition(SWEHelper.getPropertyUri("Mmsi"));
    }

    public TextBuilder createPositionAccuracy() {
        return createText()
                .label("Position Accuracy")
                .description("1 = high (<= 10 m); 0 = low (> 10 m); 0 = default")
                .definition(SWEHelper.getPropertyUri("PositionAccuracy"));
    }

    public TextBuilder createEpfd() {
        return createText()
                .label("EPFD Type")
                .description("Type of Electronic Position Fixing Device: 0 = undefined, 1 = GPS, 2 = GLONASS, 3 = Combined GPS/GLONASS, 4 = Loran-C, 5 = Chayka, 6 = Integrated navigation system, 7 = Surveyed, 8 = Galileo, 15 = internal GNSS")
                .definition(SWEHelper.getPropertyUri("Epfd"));
    }

    public TextBuilder createRAIM(){
        return createText()
                .label("RAIM Flag")
                .description("Receiver autonomous integrity monitoring flag; 0 = RAIM not in use; 1 = RAIM in use")
                .definition(SWEHelper.getPropertyUri("Raim"));
    }

    public TextBuilder createAssignedMode(){
        return createText()
                .label("Assigned Mode Flag")
                .description("0 = station operating in autonomous and continuous mode = default; 1 = station operating in assigned mode")
                .definition(SWEHelper.getPropertyUri("AssignedMode"));
    }
}
