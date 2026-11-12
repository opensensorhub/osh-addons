package org.sensorhub.impl.persistence.es;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

public class EsRecordStoreInfo extends DataStreamInfo {
    String indexName;

    public EsRecordStoreInfo(String name,String indexName, DataComponent recordDescription, DataEncoding recommendedEncoding) {
        super(name, recordDescription, recommendedEncoding);
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }
}
