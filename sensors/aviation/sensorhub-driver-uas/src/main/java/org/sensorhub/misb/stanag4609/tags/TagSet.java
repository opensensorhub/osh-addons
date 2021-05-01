/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.tags;

/**
 * An enumerated identifier set for the various types of data set elements processed.
 */
public class TagSet {

    public static final TagSet UNKNOWN = new TagSet("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00", "UNKNOWN", "Unknown tag set");

    private String designator;
    private String name;
    private String description;

    public TagSet(String designator, String name, String description) {

        this.designator = designator;
        this.name = name;
        this.description = description;
    }

    public String getDesignator() {
        return designator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
