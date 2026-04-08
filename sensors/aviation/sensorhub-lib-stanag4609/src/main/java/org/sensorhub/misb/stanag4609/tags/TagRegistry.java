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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton registry for all data element tags.  Provides a repository to manage all registered tags.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class TagRegistry {

    private static TagRegistry theInstance = null;

    private Map<TagSet, ArrayList<Tag>> tagsByElementClass = new HashMap<>();

    private TagRegistry() {
    }

    public static TagRegistry getInstance() {

        if (null == theInstance) {

            theInstance = new TagRegistry();
        }

        return theInstance;
    }

    public void registerTag(Tag tag) {
        
        TagSet memberOf = tag.getMemberOf();

        if (!tagsByElementClass.containsKey(memberOf)) {

            tagsByElementClass.put(memberOf, new ArrayList<>());
        }

        tagsByElementClass.get(memberOf).add(tag);
    }

    public Tag getByTagSetAndId(TagSet tagSet, byte tagId) {

        Tag selectedTag =
                new Tag(TagSet.UNKNOWN, "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00", tagId, Encoding.NONE, "UNKNOWN TAG");

        List<Tag> selectedTags = tagsByElementClass.get(tagSet);

        for (Tag tag : selectedTags) {

            if (tag.getLocalSetTag() == tagId) {

                selectedTag = tag;
                break;
            }
        }

        return selectedTag;
    }
}
