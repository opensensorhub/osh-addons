package org.sensorhub.impl.datastore.postgis.feature;

import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.vast.util.Asserts;

import java.time.Instant;

public class PostgisFeatureKey extends FeatureKey {

    protected long parentID; // 0 indicates no parent


    public PostgisFeatureKey(long parentID, BigId internalID, Instant validStartTime)
    {
        super(internalID, validStartTime);

        Asserts.checkArgument(parentID >= 0, "Invalid parentID");
        this.parentID = parentID;
    }


    public PostgisFeatureKey(int idScope, long parentID, long internalID, Instant validStartTime)
    {
        super(idScope, internalID, validStartTime);

        Asserts.checkArgument(parentID >= 0, "Invalid parentID");
        this.parentID = parentID;
    }


    public long getParentID()
    {
        return parentID;
    }
}
