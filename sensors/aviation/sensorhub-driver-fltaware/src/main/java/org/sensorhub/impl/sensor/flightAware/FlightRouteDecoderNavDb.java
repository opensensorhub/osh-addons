/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;
import org.sensorhub.utils.aero.INavDatabase;
import org.slf4j.Logger;
import com.google.common.base.Strings;
import j2html.rendering.FlatHtml;
import j2html.rendering.HtmlBuilder;
import j2html.tags.DomContent;
import static j2html.TagCreator.*;


/**
 * Decoder implementation using the ARINC 424 navigation database provided
 * in a separate OSH module
 * @author Alex Robin
 * @since Nov 26, 2021
 */
public class FlightRouteDecoderNavDb implements IFlightRouteDecoder
{
    Logger log;
    INavDatabase navDB;
        
    
    public FlightRouteDecoderNavDb(FlightAwareDriver driver, String navDbModuleID)
    {
        this.log = driver.getLogger();
        this.navDB = INavDatabase.getInstance(driver.getParentHub(), navDbModuleID);
    }
    
    
    @Override
    public List<Waypoint> decode(FlightObject fltPlan, String route) throws SensorHubException
    {
        try
        {
            // call decoder
            var decodeOut = navDB.decodeRoute(route);
            if (decodeOut == null || decodeOut.getWaypoints() == null || decodeOut.getWaypoints().isEmpty())
                throw new SensorHubException("Empty response from route decoder");
            int numWaypoints = decodeOut.getWaypoints().size();
            
            // build waypoint list and set altitude according to filed altitude
            int i = 0;
            ArrayList<Waypoint> waypoints = new ArrayList<>(numWaypoints);
            for (var dbEntry: decodeOut.getWaypoints())
            {
                Waypoint wp = new Waypoint(dbEntry.getCode(), dbEntry.getType(), dbEntry.getLatitude(), dbEntry.getLongitude());
                if (i == 0 || i == numWaypoints-1)
                    wp.altitude = 0.0;
                else if (!Strings.nullToEmpty(fltPlan.alt).trim().isEmpty())
                    wp.altitude = Double.parseDouble(fltPlan.alt);
                i++;
                waypoints.add(wp);
            }
            
            // debug output as HTML
            if (log.isDebugEnabled())
                writeToDebugHtmlOutput(fltPlan, route, waypoints);
            
            return waypoints;
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
                writeToDebugHtmlOutput(fltPlan, route, null);
            throw new SensorHubException("Error decoding Firehose route using ARINC database", e);
        }
    }
    
    
    long lastWriteTime = 0;
    LinkedList<DomContent> htmlFlights = new LinkedList<>();
    synchronized void writeToDebugHtmlOutput(FlightObject fltPlan, String route, List<Waypoint> decodedRoute)
    {
        StringBuilder buf = new StringBuilder();
        if (decodedRoute != null)
        {
            for (Waypoint wpt: decodedRoute)
                buf.append(wpt.toString()).append(" ");
            buf.setLength(buf.length()-1);
        }
        
        htmlFlights.add(tr(
            td(strong(fltPlan.ident + "_" + fltPlan.dest),
               br(),
               text("Open in "),
               a("UAT")
                 .withHref("https://uat.fltnavwx.com/dci/latest?id=" + fltPlan.ident + "&dest=" + fltPlan.dest)
                 //.withHref("wwxapp://loadflight?id=" + fltPlan.ident + "&dest=" + fltPlan.dest)
                 .withTarget("WidgetWx_UAT")/*,
               text(" or "),
               a("PROD")
                 .withHref("https://fltnavwx.com/dci/latest?id=" + fltPlan.ident + "&dest=" + fltPlan.dest)
                 //.withHref("wwxapp://loadflight?id=" + fltPlan.ident + "&dest=" + fltPlan.dest)
                 .withTarget("WidgetWx_PROD")*/
            ),
            td(Instant.ofEpochSecond(Long.parseLong(fltPlan.fdt)).toString()),
            td(route),
            td(decodedRoute != null ?
                    text(buf.toString()) :
                    span("ERROR").withStyle("color:red;"))
        ));
        
        if (htmlFlights.size() > 200)
            htmlFlights.removeFirst();
        
        // write out to file every so often
        long now = System.currentTimeMillis();
        //if (now - lastWriteTime > 10000)
        {
            lastWriteTime = now;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("decode_output.html")))
            {
                HtmlBuilder<Writer> html = FlatHtml.into(writer);//IndentedHtml.into(writer);
                
                html.appendStartTag(html().getTagName()).completeTag();
                head(
                    style().withText(
                        "table, th, td {\n" + 
                        "  border: 1px solid black;\n" + 
                        "  border-collapse: collapse;\n" +
                        "  font-family: monospace;\n" +
                        "}\n" + 
                        "table tr:first-child {\n" + 
                        "  font-weight: bold;\n" + 
                        "  background-color: lightgray;\n" + 
                        "}"
                    )
                ).render(html);
                html.appendStartTag(body().getTagName()).completeTag();
                html.appendStartTag(table().getTagName()).appendAttribute("style", "table-layout: fixed; width: 100%").completeTag();
                
                tr(
                    td("Flight Number").withStyle("width: 180px;"),
                    td("Filed Departure Time").withStyle("width: 180px;"),
                    td("Route String"),
                    td("Decoded Route")
                ).render(html);
                
                for (DomContent row: htmlFlights)
                    row.render(html);
                
                html.appendEndTag(table().getTagName());
                html.appendEndTag(body().getTagName());
                html.appendEndTag(html().getTagName());
            }
            catch (IOException e)
            {
                
            }
        }
    }

}
