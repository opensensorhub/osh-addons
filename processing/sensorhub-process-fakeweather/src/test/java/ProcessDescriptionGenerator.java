import com.botts.impl.process.helpers.ProcessHelper;
import net.opengis.sensorml.v20.AggregateProcess;
import org.junit.Test;
import org.sensorhub.process.weather.WeatherProcess;
import org.vast.data.SWEFactory;
import org.vast.process.ProcessException;
import org.vast.xml.XMLWriterException;

import java.io.IOException;

public class ProcessDescriptionGenerator {
    SWEFactory fac = new SWEFactory();
    ProcessHelper processHelper = new ProcessHelper();

    public AggregateProcess generateDescription() throws ProcessException {
        WeatherProcess p1 = new WeatherProcess();
        p1.init();

        return processHelper.createProcessChain()
                .name("Process Chain")
                .uid("urn:osh:process:weather")
                .description("Example process chain that converts units from the Simulated Weather Sensor")
                .addDataSource("source0", "urn:osh:sensor:simweather:001")
                .addOutputList(p1.getOutputList())
                .addProcess("process0", p1)
                .addConnection("components/source0/outputs/weather",
                        "components/process0/inputs/weather")
                .addConnection("components/process0/outputs/weather",
                        "outputs/weather")
                .build();
    }

    @Test
    public void generateDescJSON() throws ProcessException, IOException {
        // Write JSON process description to System.out
        processHelper.writeProcessJSON(generateDescription(), System.out);
    }

    @Test
    public void generateDescXML() throws ProcessException, XMLWriterException {
        // Write XML process description to System.out
        processHelper.writeProcess(System.out, generateDescription(), true);
    }

}
