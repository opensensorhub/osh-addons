import org.sensorhub.impl.SensorHub;


public class TestSensorHub
{
    private TestSensorHub()
    {
    }


    public static void main(String[] args) throws Exception
    {
        //SensorHub.main(new String[] {"src/main/resources/config_v2_sensors_swe_sta_swa.json", "storage"});
        SensorHub.main(new String[] {"src/main/resources/config_postgis.json", "storage"});
        //SensorHub.main(new String[] {"src/main/resources/config_v2_oauth.json", "storage"});
        //SensorHub.main(new String[] {"src/main/resources/config_v2_keycloak.json", "storage"});
    }

}
