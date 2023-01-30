package org.sensorhub.impl.model.uxs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.vast.cdm.common.CDMException;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.JSONEncodingImpl;
import org.vast.swe.DataTreeVisitor;
import org.vast.swe.SWEHelper;
import org.vast.swe.SWEStaxBindings;
import org.vast.swe.json.SWEJsonStreamWriter;
import org.vast.xml.IndentingXMLStreamWriter;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.ScalarComponent;



public class PrintUtils
{

    protected static void print(DataComponent rec, boolean printBinaryEncoding, boolean printDataSample) throws Exception
    {
        /*new SWEUtils(SWEUtils.V2_0)
            .writeComponent(System.out, rec, false, true);
        System.out.println();
        System.out.println();*/

        new SWEStaxBindings().writeDataComponent(
            new SWEJsonStreamWriter(System.out, StandardCharsets.UTF_8), rec, false);
        System.out.println();
        System.out.println();

        // print binary encoding example
        if (printBinaryEncoding)
        {
            BinaryEncoding binaryEncoding = SWEHelper.getDefaultBinaryEncoding(rec);
            new SWEStaxBindings().writeBinaryEncoding(
                new SWEJsonStreamWriter(System.out, StandardCharsets.UTF_8), binaryEncoding);
            System.out.println();
            System.out.println();
        }

        // also print example message
        if (printDataSample)
        {
            rec.assignNewDataBlock();

            DataTreeVisitor randomValueGenerator = new DataTreeVisitor(false) {
                @Override
                protected void processAtom(ScalarComponent component) throws CDMException, IOException
                {
                    if (component instanceof Quantity)
                    {
                        double val = Math.random() * 10.0;
                        if (component.hasConstraints())
                        {
                            double[] interval = ((Quantity)component).getConstraint().getIntervalList().get(0);
                            val = interval[0] + (float)(Math.random() * (interval[1]-interval[0]));
                        }

                        component.getData().setDoubleValue(val);
                    }
                    else if (component instanceof Count)
                    {
                        int val = (int)(Math.random() * 10.0);
                        if (component.hasConstraints())
                        {
                            double[] interval = ((Count)component).getConstraint().getIntervalList().get(0);
                            val = (int)(interval[0] + Math.random() * (interval[1]-interval[0]));
                        }

                        component.getData().setIntValue(val);
                    }
                    else if (component instanceof Category)
                    {
                        String val = "test";
                        if (component.hasConstraints())
                        {
                            List<String> valList = ((Category)component).getConstraint().getValueList();
                            double randVal = Math.random()*(valList.size()-1);
                            val = valList.get((int)Math.round(randVal));
                        }

                        component.getData().setStringValue(val);
                    }
                }

                @Override
                protected boolean processBlock(DataComponent component) throws CDMException, IOException
                {
                	if (component instanceof DataChoice)
                    {
                    	int maxIdx = ((DataChoice) component).getNumItems() - 1;
                    	int selectIdx = (int)Math.round(Math.random()*maxIdx);
                    	((DataChoice) component).setSelectedItem(selectIdx);
                    }

                	else if (component instanceof DataArray)
                    {
                	    if (((DataArray)component).isVariableSize())
                	        ((DataArray)component).updateSize(10);
                    }

                	return true;
                }
            };

            randomValueGenerator.setDataComponents(rec);
            do randomValueGenerator.processNextElement();
            while(!randomValueGenerator.isEndOfDataBlock());

            // and print example data
            DataStreamWriter writer = SWEHelper.createDataWriter(new JSONEncodingImpl());
            writer.setOutput(System.out);
            writer.setDataComponents(rec);
            writer.write(rec.getData());
            writer.flush();
            System.out.println();
            System.out.println();
        }
    }

    final static String RNG_NS_URI = "http://relaxng.org/ns/structure/1.0";
    final static String RNG_ANNOT_NS_URI = "http://relaxng.org/ns/compatibility/annotations/1.0";


    protected static void printWithRelaxNG(String group, ScalarComponent comp) throws Exception
    {
        print(comp, false, false);

        XMLOutputFactory output = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = new IndentingXMLStreamWriter(output.createXMLStreamWriter(System.out));

        writer.setPrefix("", RNG_NS_URI);
        writer.setPrefix("a", RNG_ANNOT_NS_URI);

        writer.writeStartElement(RNG_NS_URI, "define");
        writer.writeAttribute("name", getRngName(group, comp));

        writer.writeStartElement(RNG_ANNOT_NS_URI, "documentation");
        writer.writeCharacters(comp.getLabel());
        writer.writeEndElement();

        writer.writeStartElement(RNG_NS_URI, "element");
        writer.writeAttribute("name", "swe:" + getQName(comp));

        writer.writeStartElement(RNG_NS_URI, "attribute");
        writer.writeAttribute("name", "definition");
        writer.writeStartElement(RNG_NS_URI, "value");
        writer.writeCharacters(comp.getDefinition());
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement(RNG_NS_URI, "element");
        writer.writeAttribute("name", "label");
        writer.writeStartElement(RNG_NS_URI, "value");
        writer.writeCharacters(comp.getLabel());
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement(RNG_NS_URI, "element");
        writer.writeAttribute("name", "description");
        writer.writeStartElement(RNG_NS_URI, "value");
        writer.writeCharacters(comp.getDescription());
        writer.writeEndElement();
        writer.writeEndElement();

        if (comp instanceof HasUom)
        {
            String uom = ((HasUom)comp).getUom().hasHref() ? ((HasUom)comp).getUom().getHref() : ((HasUom)comp).getUom().getCode();
            writer.writeStartElement(RNG_NS_URI, "element");
            writer.writeAttribute("name", "swe:uom");
            writer.writeStartElement(RNG_NS_URI, "attribute");
            writer.writeAttribute("name", "code");
            writer.writeStartElement(RNG_NS_URI, "value");
            writer.writeCharacters(uom);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }

        writer.writeStartElement(RNG_NS_URI, "ref");
        writer.writeAttribute("name", "swe." + getQName(comp).toLowerCase() + "Value");
        writer.writeEndElement();

        writer.writeEndElement();
        writer.writeEndElement();
        writer.close();

        /*
        <define name="sml-x.typicalPower">
            <a:documentation>
               Typical Power Unqualified
            </a:documentation>
            <element name="swe:QuantityRange">
               <attribute name="definition">
                  <value>http://sensorml.com/ont/swe/property/typicalPowerConsumption</value>
               </attribute>
               <element name="swe:label">
                  <value> Typical Power Consumption</value>
               </element>
               <optional>
                  <ref name="swe.description"/>
               </optional>
               <element name="swe:uom">
                  <attribute name="code">
                     <value>W</value>
                  </attribute>
               </element>
               <ref name="swe.quantityRangeValue"/>
            </element>
         </define>
         */

        System.out.println();
        System.out.println();
    }


    static String getRngName(String group, DataComponent comp)
    {
        return "oot." + group + "." + comp.getLabel().toLowerCase().replace(" ", "_");
    }


    static String getQName(DataComponent comp)
    {
        return comp.getClass().getSimpleName().replace("Impl", "");
    }
}
