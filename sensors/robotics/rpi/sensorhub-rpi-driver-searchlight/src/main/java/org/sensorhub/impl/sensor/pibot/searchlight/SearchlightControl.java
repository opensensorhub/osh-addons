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
package org.sensorhub.impl.sensor.pibot.searchlight;

import org.sensorhub.impl.pibot.common.control.BaseSensorControl;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.vast.swe.SWEHelper;

/**
 * Control specification and provider for PiBot SearchlightSensor Module
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class SearchlightControl extends BaseSensorControl<SearchlightSensor> {

    private static final String SENSOR_CONTROL_NAME = "SearchlightControl";

    protected SearchlightControl(SearchlightSensor parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {

        try {

            DataRecord commandData = commandDataStruct.copy();
            commandData.setData(command);
            SearchlightState searchLightColor = SearchlightState.fromString(commandData.getData().getStringValue());
            parentSensor.setSearchlightState(searchLightColor);

        } catch (Exception e) {

            throw new CommandException("Failed to command the SearchlightSensor module: ", e);
        }

        return true;
    }

    @Override
    protected void init() {

        SWEHelper factory = new SWEHelper();
        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("SearchlightSensor"))
                .label("SearchlightSensor")
                .description("An RGB light, whose color is given by one of the color choices specified")
                .addField("Color",
                        factory.createCategory()
                                .name("RGB Color")
                                .label("RGB Color")
                                .definition(SWEHelper.getPropertyUri("Color"))
                                .description("The color state of the searchlight")
                                .addAllowedValues(
                                        SearchlightState.OFF.name(),
                                        SearchlightState.WHITE.name(),
                                        SearchlightState.RED.name(),
                                        SearchlightState.MAGENTA.name(),
                                        SearchlightState.BLUE.name(),
                                        SearchlightState.CYAN.name(),
                                        SearchlightState.GREEN.name(),
                                        SearchlightState.YELLOW.name())
                                .value(SearchlightState.OFF.name())
                                .build())
                .build();
    }
}
