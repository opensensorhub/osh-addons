/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.ros.controller.buttons;

/**
 * Enumeration of the types of buttons typically available on a controller
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public enum ButtonClass {

    UNDEFINED,
    NAV,
    ACTION,
    TRIGGER,
    ANALOG,
    MISCELLANEOUS
}
