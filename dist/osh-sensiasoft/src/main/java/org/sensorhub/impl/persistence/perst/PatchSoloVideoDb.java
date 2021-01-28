package org.sensorhub.impl.persistence.perst;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.persistence.perst.BasicStorageConfig;
import org.sensorhub.impl.persistence.perst.BasicStorageImpl;
import org.vast.swe.SWEUtils;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteOrder;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;


public class PatchSoloVideoDb
{

    public static void main(String[] args) throws Exception
    {
        SWEUtils sweUtils = new SWEUtils(SWEUtils.V2_0);
        
        // create simple storage config
        BasicStorageConfig dbConf = new BasicStorageConfig();
        dbConf.name = "DB";
        dbConf.autoStart = true;
        dbConf.memoryCacheSize = 1024;
        
        // open storage
        dbConf.storagePath = "solo-video.dat";
        BasicStorageImpl db = new BasicStorageImpl();
        db.init(dbConf);
        db.start();
        
        for (IRecordStoreInfo rs: db.getRecordStores().values())
        {
            System.out.println(rs.getName());
            System.out.println("*******************");
            sweUtils.writeComponent(System.out, rs.getRecordDescription(), false, true);
            System.out.println("\n");
            sweUtils.writeEncoding(System.out, rs.getRecommendedEncoding(), true);
            System.out.println("\n");
        }
        
        // update record descriptions
        DataComponent camOutputRec = db.getRecordStores().get("camOutput").getRecordDescription().copy();
        DataArray frame = (DataArray) ((DataArray)camOutputRec.getComponent("videoFrame")).getElementType();
        frame.getElementType().getComponent(0).setDefinition("http://sensorml.com/ont/swe/property/RedChannel");
        frame.getElementType().getComponent(1).setDefinition("http://sensorml.com/ont/swe/property/GreenChannel");
        frame.getElementType().getComponent(2).setDefinition("http://sensorml.com/ont/swe/property/BlueChannel");
        sweUtils.writeComponent(System.out, camOutputRec, false, true);
        System.out.println("\n");        
        //db.updateRecordStore("camOutput", camOutputRec);
        
        TimeSeriesImpl videoStore = (TimeSeriesImpl)db.getRecordStores().get("camOutput");
        BinaryEncoding dataEncoding = (BinaryEncoding)videoStore.getRecommendedEncoding().copy();
        dataEncoding.setByteOrder(ByteOrder.BIG_ENDIAN);
        //videoStore.recommendedEncoding = dataEncoding;
        //videoStore.modify();
        
        db.commit();
        db.stop();
    }

}
