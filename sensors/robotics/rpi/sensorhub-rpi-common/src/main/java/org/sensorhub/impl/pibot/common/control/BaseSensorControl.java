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
package org.sensorhub.impl.pibot.common.control;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.AbstractSensorControl;

/**
 * Base Sensor Control for Smart Sensors
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public abstract class BaseSensorControl<T extends ISensorModule<?>> extends AbstractSensorControl<T> {

    protected DataRecord commandDataStruct;

    protected BaseSensorControl(String sensorControlName, T parentSensor) {
        super(sensorControlName, parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {

        return commandDataStruct;
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {
        return false;
    }

    protected abstract void init();
}
