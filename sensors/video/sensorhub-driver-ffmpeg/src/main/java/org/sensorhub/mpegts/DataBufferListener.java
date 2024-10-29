/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.mpegts;

/**
 * Interface definition for callback registration to handle demuxed MPEG-TS data
 * buffers.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public interface DataBufferListener {

    void onDataBuffer(DataBufferRecord record);
}