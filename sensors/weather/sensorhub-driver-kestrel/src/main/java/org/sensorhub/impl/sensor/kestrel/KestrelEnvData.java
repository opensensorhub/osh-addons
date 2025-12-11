/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.kestrel;

public class KestrelEnvData {
    // Sensor measurements (SENSOR_MEASUREMENTS_CHAR)
    public double windSpeed = Double.NaN;
    public double dryBulbTemp = Double.NaN;
    public double globeTemp = Double.NaN;
    public double relativeHumidity = Double.NaN;
    public double stationPress = Double.NaN;
    public double magDirection = Double.NaN;
    public double airSpeed = Double.NaN;

    // Derived measurements 1 (DERIVED_MEASUREMENTS_1_CHAR)
    public double trueDirection = Double.NaN;
    public double airDensity = Double.NaN;
    public double altitude = Double.NaN;
    public double pressure = Double.NaN;
    public double crosswind = Double.NaN;
    public double headwind = Double.NaN;
    public double densityAlt = Double.NaN;
    public double relativeAirDensity = Double.NaN;

    // Derived measurements 2 (DERIVED_MEASUREMENTS_2_CHAR)
    public double dewPoint = Double.NaN;
    public double heatIndex = Double.NaN;
    public double wetBulb = Double.NaN;
    public double chill = Double.NaN;

    private boolean hasSensorMeasurements = false;
    private boolean hasDerived1 = false;
    private boolean hasDerived2 = false;
    private boolean hasDerived3 = false;
    private boolean hasDerived4 = false;

    public void markSensorMeasurementsReceived() {
        hasSensorMeasurements = true;
    }

    public void markDerived1Received() {
        hasDerived1 = true;
    }

    public void markDerived2Received() {
        hasDerived2 = true;
    }

    public void markDerived3Received() {
        hasDerived3 = true;
    }

    public void markDerived4Received() {
        hasDerived4 = true;
    }

    public boolean isComplete() {
        return hasDerived1 && hasDerived2 && hasSensorMeasurements;
    }

    public void reset() {
        windSpeed = dryBulbTemp = globeTemp = relativeHumidity = Double.NaN;
        stationPress = magDirection = airSpeed = Double.NaN;
        trueDirection = airDensity = altitude = pressure = Double.NaN;
        crosswind = headwind = densityAlt = relativeAirDensity = Double.NaN;
        dewPoint = heatIndex = chill = wetBulb = Double.NaN;

        // Reset flags
        hasSensorMeasurements = false;
        hasDerived1 = false;
        hasDerived2 = false;
        hasDerived3 = false;
        hasDerived4 = false;
    }

    public KestrelEnvData snapshot() {
        KestrelEnvData copy = new KestrelEnvData();
        copy.windSpeed = this.windSpeed;
        copy.dryBulbTemp = this.dryBulbTemp;
        copy.globeTemp = this.globeTemp;
        copy.relativeHumidity = this.relativeHumidity;
        copy.stationPress = this.stationPress;
        copy.magDirection = this.magDirection;
        copy.airSpeed = this.airSpeed;
        copy.trueDirection = this.trueDirection;
        copy.airDensity = this.airDensity;
        copy.altitude = this.altitude;
        copy.pressure = this.pressure;
        copy.crosswind = this.crosswind;
        copy.headwind = this.headwind;
        copy.densityAlt = this.densityAlt;
        copy.relativeAirDensity = this.relativeAirDensity;
        copy.dewPoint = this.dewPoint;
        copy.heatIndex = this.heatIndex;
        copy.chill = this.chill;
        copy.wetBulb = this.wetBulb;
        copy.hasSensorMeasurements = this.hasSensorMeasurements;
        copy.hasDerived1 = this.hasDerived1;
        copy.hasDerived2 = this.hasDerived2;
        copy.hasDerived3 = this.hasDerived3;
        copy.hasDerived4 = this.hasDerived4;
        return copy;
    }
}
