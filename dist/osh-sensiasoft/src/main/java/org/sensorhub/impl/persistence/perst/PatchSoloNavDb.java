package org.sensorhub.impl.persistence.perst;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.impl.persistence.perst.BasicStorageConfig;
import org.sensorhub.impl.persistence.perst.BasicStorageImpl;
import org.vast.swe.SWEUtils;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Vector;


public class PatchSoloNavDb
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
        dbConf.storagePath = "solo-nav.dat";
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
        DataComponent platformAttRec = db.getRecordStores().get("platformAtt").getRecordDescription().copy();
        Vector att = (Vector)platformAttRec.getComponent("attitude");
        att.setReferenceFrame("http://www.opengis.net/def/cs/OGC/0/NED");
        att.getComponent(0).setDefinition("http://sensorml.com/ont/swe/property/TrueHeading");
        att.getComponent(1).setDefinition("http://sensorml.com/ont/swe/property/PitchAngle");
        att.getComponent(2).setDefinition("http://sensorml.com/ont/swe/property/RollAngle");
        sweUtils.writeComponent(System.out, platformAttRec, false, true);
        System.out.println("\n");        
        //db.updateRecordStore("platformAtt", platformAttRec);
        
        DataComponent gimbalAttRec = db.getRecordStores().get("gimbalAtt").getRecordDescription().copy();
        att = (Vector)gimbalAttRec.getComponent("attitude");
        att.getComponent(0).setDefinition("http://sensorml.com/ont/swe/property/YawAngle");
        att.getComponent(1).setDefinition("http://sensorml.com/ont/swe/property/PitchAngle");
        att.getComponent(2).setDefinition("http://sensorml.com/ont/swe/property/RollAngle");
        sweUtils.writeComponent(System.out, gimbalAttRec, false, true);
        System.out.println("\n");        
        //db.updateRecordStore("gimbalAtt", gimbalAttRec);
        
        DataComponent platformLocRec = db.getRecordStores().get("platformLoc").getRecordDescription().copy();
        att = (Vector)platformLocRec.getComponent("loc");
        att.getComponent(0).setDefinition("http://sensorml.com/ont/swe/property/GeodeticLatitude");
        att.getComponent(1).setDefinition("http://sensorml.com/ont/swe/property/Longitude");
        att.getComponent(2).setDefinition("http://sensorml.com/ont/swe/property/HeightAboveMSL");
        sweUtils.writeComponent(System.out, platformLocRec, false, true);
        System.out.println("\n");        
        //db.updateRecordStore("platformLoc", platformLocRec);      
        
        db.commit();
        db.stop();
    }

}
