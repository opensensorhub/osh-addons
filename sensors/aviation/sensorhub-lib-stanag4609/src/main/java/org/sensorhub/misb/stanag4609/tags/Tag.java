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

import com.google.gson.JsonObject;

/**
 * An implementation of tags describing a KLV key having a <code>designator</code> value but may also have a local set
 * tag specified as the <code>localSetTag</code>.  In the least, keys of this class will contain a <code>keyValue</code>,
 * <code>encoding</code> information for the associated data value represented by the key, and a <code>name</code> as a
 * string.
 *
 * @author Nicolas Garay
 * @since Feb. 3, 2020
 */
public class Tag implements JsonPrinter {

    private final TagSet memberOf;
    private final String designator;
    private final int localSetTag;
    private final Encoding encoding;
    private final String name;
    private final String description;
    private final String uom;
    private final String standard;

    /**
     * Constructor
     *
     * @param memberOf    parent tag set this tag belongs to
     * @param designator  the associated UL KLV key
     * @param encoding    format specification for accompanying data
     * @param name        the name of the tag
     */
    public Tag(TagSet memberOf, String designator, Encoding encoding, String name) {

        this(memberOf, designator, 0, encoding, name, null, null, null);
    }

    /**
     * Constructor
     *
     * @param memberOf    parent tag set this tag belongs to
     * @param designator  the associated UL KLV key
     * @param localSetTag the integer value of the tag
     * @param encoding    format specification for accompanying data
     * @param name        the name of the tag
     */
    public Tag(TagSet memberOf, String designator, int localSetTag, Encoding encoding, String name) {

        this(memberOf, designator, localSetTag, encoding, name, null, null, null);
    }

    /**
     * Constructor
     *
     * @param memberOf    parent tag set this tag belongs to
     * @param designator  the associated UL KLV key
     * @param localSetTag the integer value of the tag
     * @param encoding    format specification for accompanying data
     * @param name        the name of the tag
     * @param standard    the reference standard for this tag
     */
    public Tag(TagSet memberOf, String designator, int localSetTag, Encoding encoding, String name, String standard) {

        this(memberOf, designator, localSetTag, encoding, name, standard, null, null);
    }

    /**
     * Constructor
     *
     * @param memberOf    parent tag set this tag belongs to
     * @param designator  the associated UL KLV key
     * @param localSetTag the integer value of the tag
     * @param encoding    format specification for accompanying data
     * @param name        the name of the tag
     * @param standard    the reference standard for this tag
     * @param description the tag description of the enumerated value
     */
    public Tag(TagSet memberOf, String designator, int localSetTag, Encoding encoding, String name, String standard, String description) {

        this(memberOf, designator, localSetTag, encoding, name, standard, description, null);
    }

    /**
     * Constructor
     *
     * @param memberOf    parent tag set this tag belongs to
     * @param designator  the associated UL KLV key
     * @param localSetTag the integer value of the tag
     * @param encoding    format specification for accompanying data
     * @param name        the name of the tag
     * @param standard    the reference standard for this tag
     * @param description the tag description of the enumerated value
     * @param units       the units of measure
     */
    public Tag(TagSet memberOf, String designator, int localSetTag, Encoding encoding, String name, String standard, String description, String units) {

        this.memberOf = memberOf;
        this.designator = designator;
        this.localSetTag = localSetTag;
        this.encoding = encoding;
        this.name = name;
        this.description = description;
        this.uom = units;
        this.standard = standard;
    }

    /**
     * Reports the tags membership, the {@link TagSet} it belongs to
     *
     * @return the tags membership
     */
    public TagSet getMemberOf() {

        return memberOf;
    }

    /**
     * Gets the tag id associated with the enumerated value.
     *
     * @return an integer value associated with the enumeration.
     */
    public int getLocalSetTag() {

        return localSetTag;
    }

    /**
     * Gets the name associated with the enumerated value.
     *
     * @return the name associated with the enumerated value.
     */
    public String getName() {

        return name;
    }

    /**
     * Gets the key value for the enumerated value.
     *
     * @return the key value for the enumerated value.
     */
    public String getDesignator() {

        return designator;
    }

    /**
     * Gets the description for the enumerated value.
     *
     * @return a string description for the enumerated value.
     */
    public String getDescription() {

        return description;
    }

    /**
     * Retrieves the encoding for the data
     *
     * @return the encoding for the data
     */
    public Encoding getEncoding() {

        return encoding;
    }

    /**
     * Creates a string representation of the enumeration.
     *
     * @return a string representation of the the enumerated value.
     */
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append(designator).append(" : ").append(localSetTag).append(" : ").append(name);

        if (null != description) {
            builder.append(" : ").append(description);
        }

        if (null != uom) {
            builder.append(" : ").append(uom);
        }

        return builder.toString();
    }

    @Override
    public String toJsonString() {

        return getAsJsonObject().toString();
    }

    @Override
    public JsonObject getAsJsonObject() {

        JsonObject jsonObject = new JsonObject();

        if (localSetTag > 0) {

            jsonObject.addProperty("tag", localSetTag);
        }

        jsonObject.addProperty("designator", designator);
        jsonObject.addProperty("name", name);

        if (null != description) {

            jsonObject.addProperty("description", description);
        }

        if (null != uom) {

            jsonObject.addProperty("uom", uom);
        }

        jsonObject.add("encoding", encoding.getAsJsonObject());

        if (null != standard) {

            jsonObject.addProperty("standard", standard);
        }

        return jsonObject;
    }
}
