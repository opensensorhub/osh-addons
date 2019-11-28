/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.comm.MessageQueueConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;
import org.sensorhub.api.sensor.SensorConfig;


public class FlightAwareConfig extends SensorConfig
{    
    enum Mode {
        FIREHOSE,
        PUBSUB,
        PUBSUB_THEN_FIREHOSE,
        FIREHOSE_THEN_PUBSUB
    }
    
    @DisplayInfo(desc="Type of connection")
    public Mode connectionType = Mode.FIREHOSE;
    
    @DisplayInfo(label="Initial Retry Interval", desc="Delay before the first retry of the exponential backoff strategy, in seconds)")
    public int initRetryInterval = 20;
    
    @DisplayInfo(label="Max Retry Interval", desc="Max interval between retries, in seconds)")
    public int maxRetryInterval = 300;
    
    @DisplayInfo(desc="FlightAware Firehose hostname")
    public String hostname = "firehose.flightaware.com";
    
    @DisplayInfo(desc="FlightAware Firehose user name")
    public String userName;
    
    @DisplayInfo(desc="FlightAware Firehose API key")
    @FieldType(Type.PASSWORD)
    public String password;
    
    @DisplayInfo(desc="Types of FA messages to listen for")
    public List<String> messageTypes = new ArrayList<>();
    
    @DisplayInfo(desc="Airline codes to listen for")
    public List<String> airlines = new ArrayList<>();
    
    @DisplayInfo(desc="Flight filter configuration")
    public FlightObjectFilterConfig filterConfig;
    
    @DisplayInfo(desc="Pub/sub configuration")
    public MessageQueueConfig pubSubConfig;
}
