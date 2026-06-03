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

public class MessageIdDescriptions {

    public final String[] descriptions = {
            "Scheduled position report; Class A shipborne mobile equipment",
            "Assigned scheduled position report; Class A shipborne mobile equipment",
            "Special position report, response to interrogation; Class A shipborne mobile equipment",
            "Position, UTC, date and current slot number of base station",
            "Scheduled static and voyage related vessel data report, Class A shipborne mobile equipment",
            "Binary data for addressed communication",
            "Acknowledgement of received addressed binary data",
            "Binary data for broadcast communication",
            "Position report for airborne stations involved in SAR operations only",
            "Request UTC and date",
            "Current UTC and date if available",
            "Safety related data for addressed communication",
            "Acknowledgement of received addressed safety related message",
            "Safety related data for broadcast communication",
            "Request for a specific message type can result in multiple responses from one or several stations",
            "Assignment of a specific report behaviour by competent authority using a Base station",
            "DGNSS corrections provided by a base station",
            "Standard position report for Class B shipborne mobile equipment to be used instead of Messages 1, 2, 3",
            "No longer required. Extended position report for Class B shipborne mobile equipment; contains additional static information",
            "Reserve slots for Base station(s)",
            "Position and status report for aids-to-navigation",
            "Management of channels and transceiver modes by a Base station",
            "Assignment of a specific report behaviour by competent authority using a Base station to a specific group of mobiles",
            "Additional data assigned to an MMSI Part A: Name Part B: Static Data",
            "Short unscheduled binary data transmission Broadcast or addressed",
            "Scheduled binary data transmission Broadcast or addressed",
            "Class A and Class B \"SO\" shipborne mobile equipment outside base station coverage"
    };



}

