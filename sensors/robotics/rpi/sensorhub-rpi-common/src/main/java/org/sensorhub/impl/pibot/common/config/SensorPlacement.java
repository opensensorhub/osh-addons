/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.pibot.common.config;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Configuration module for the OpenSensorHub driver pertaining to logical position of a sensor
 * within a personal residential environment, someones home.
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public class SensorPlacement {

    /**
     * List of locations
     */
    public enum Locations
    {
        LIVING_ROOM("Living Room"),
        DINING_ROOM("Dining Room"),
        DEN("Den"),
        KITCHEN("Kitchen"),
        FOYER("Foyer"),
        GARAGE("Garage"),
        MASTER_SUITE("Master Suite"),
        BEDROOM_1("Bedroom 1"),
        BEDROOM_2("Bedroom 2"),
        BEDROOM_3("Bedroom 3"),
        BEDROOM_4("Bedroom 4"),
        BEDROOM_5("Bedroom 5"),
        BATHROOM_1("Bathroom 1"),
        BATHROOM_2("Bathroom 2"),
        BATHROOM_3("Bathroom 3"),
        BATHROOM_4("Bathroom 4"),
        BATHROOM_5("Bathroom 5"),
        OTHER("Other");

        /**
         * Name of the enumerated value
         */
        private String name;

        /**
         * Constructor
         *
         * @param name the name to assign to new enumerated value
         */
        Locations(String name) {

            this.name = name;
        }

        @Override
        public String toString() {

            return name;
        }
    }

    @DisplayInfo.Required
    @DisplayInfo(desc="Location of sensor placement, for custom location select \"Other\"")
    public Locations location = Locations.FOYER;

    @DisplayInfo(desc="Location of sensor placement if other than specified list of locations")
    public String customLocation = "";
}
