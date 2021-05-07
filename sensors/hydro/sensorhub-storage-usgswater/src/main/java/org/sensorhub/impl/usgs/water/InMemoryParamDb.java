/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;
import org.sensorhub.api.common.SensorHubException;
import org.slf4j.Logger;
import org.vast.swe.SWEConstants;
import org.vast.unit.UnitParserUCUM;
import org.vast.util.Asserts;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;


/**
 * <p>
 * In-memory parameter database implementation backed by a Guava loading cache
 * </p>
 *
 * @author Alex Robin
 * @since May 3, 2021
 */
public class InMemoryParamDb implements IParamDatabase
{
    Logger logger;
    File paramTableFile;
    Map<String, String> paramUriMap;
    LoadingCache<String, USGSParam> paramCache;
    
    static Map<String, String> uomAtomMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static Map<String, String> uomMappings = new TreeMap<>();
    static {
        uomAtomMappings.put("in", "[in_us]");
        uomAtomMappings.put("inches", "[in_us]");
        uomAtomMappings.put("mi", "[mi_us]");
        uomAtomMappings.put("curb-mi", "[mi_us]");
        uomAtomMappings.put("meters", "m");
        uomAtomMappings.put("mi2", "[mi_us]2");
        uomAtomMappings.put("ac", "[acr_us]");
        uomAtomMappings.put("knots", "[kn_i]");
        uomAtomMappings.put("gal", "[gal_us]");
        uomAtomMappings.put("barrel", "[bbl_us]");
        uomAtomMappings.put("lb", "[lb_av]");
        uomAtomMappings.put("tons", "[ston_av]");
        uomAtomMappings.put("ton", "[ston_av]");
        uomAtomMappings.put("deg C", "Cel");
        uomAtomMappings.put("deg F", "[degF]");
        uomAtomMappings.put("deg", "deg");
        uomAtomMappings.put("sec", "s");
        uomAtomMappings.put("seconds", "s");
        uomAtomMappings.put("minutes", "min");
        uomAtomMappings.put("mn", "min");
        uomAtomMappings.put("hr", "h");
        uomAtomMappings.put("day", "d");
        uomAtomMappings.put("days", "d");
        uomAtomMappings.put("week", "wk");
        uomAtomMappings.put("years", "a");
        uomAtomMappings.put("psi", "[psi]");
        uomAtomMappings.put("ohms", "Ohm");
        uomAtomMappings.put("Btu", "[Btu_60]");
        uomAtomMappings.put("uE", "umol");
        uomAtomMappings.put("uEinst", "umol");
        uomAtomMappings.put("bq", "Bq");
        uomAtomMappings.put("dpm", "60.Bq");
        uomAtomMappings.put("ppth", "[ppth]");
        uomAtomMappings.put("ppm", "[ppm]");
        uomAtomMappings.put("fraction", "1");
        uomAtomMappings.put("ratio", "1");
        uomAtomMappings.put("units", "1");
        uomAtomMappings.put("#", "1");
        uomAtomMappings.put("none", "1");
        uomAtomMappings.put("number", "1");
        uomAtomMappings.put("count", "1");
        uomAtomMappings.put("counts", "1");
        uomAtomMappings.put("cells", "1");
        uomAtomMappings.put("cp", "1");
        
        uomMappings.put("mm/Hg", "mm[Hg]");
        uomMappings.put("in/Hg", "[in_i'Hg]");
        uomMappings.put("in/H2O", "[in_i'H2O]");
        uomMappings.put("ac-ft", "[acr_us].[ft_us]");
        uomMappings.put("Kgal", "10^3.[gal_us]");
        uomMappings.put("mgd", "10^6.[gal_us]");
        uomMappings.put("Mgal", "10^6.[gal_us]");
        uomMappings.put("gal/batch", "[gal_us]");        
        uomMappings.put("cm/sample", "cm");
        uomMappings.put("20C/day", "1/d");
        uomMappings.put("per day", "1/d");
        uomMappings.put("neg bar", "-1.bar");
        uomMappings.put("ohm-meters", "Ohm/m");
        uomMappings.put("cfs-days", "[ft_us]3/s.d");
        uomMappings.put("tons/ac ft", "[ston_av]/[acr_us]/[ft_us]");
        uomMappings.put("alpha/m", "1/m");
        uomMappings.put("pct modern", "%");
        uomMappings.put("per mil", "[ppth]");
        uomMappings.put("cp/L", "1/L");
        uomMappings.put("cp/100 mL", "1/dL");
                        
        uomMappings.put("BU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/BU");
        uomMappings.put("RFU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/RFU");
        uomMappings.put("IVFU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/IVFU");
        uomMappings.put("JTU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/JTU");
        uomMappings.put("FBU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/FBU");
        uomMappings.put("FBRU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/FBRU");
        uomMappings.put("FAU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/FAU");
        uomMappings.put("FNU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/FNU");
        uomMappings.put("FNMU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/FNMU");
        uomMappings.put("FNRU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/FNRU");
        uomMappings.put("NTU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/NTU");
        uomMappings.put("NTMU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/NTMU");
        uomMappings.put("NTRU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/NTRU");
        uomMappings.put("SBU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/SBU");
        uomMappings.put("RU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/RU");
        uomMappings.put("(RU cm)/AU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/RU_cm");
        uomMappings.put("PCU", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/PCU");
        uomMappings.put("PSU", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/PSU");
        uomMappings.put("PSS", SWEConstants.SWE_UOM_URI_PREFIX + "USGS/unit/PSU");
        uomMappings.put("TON", SWEConstants.SML_ONTOLOGY_ROOT + "USGS/unit/TON");
    }
    
    
    public InMemoryParamDb(Logger logger) throws SensorHubException
    {
        this(null, logger);
    }
    
    
    public InMemoryParamDb(File paramTableFile, Logger logger) throws SensorHubException
    {
        this.logger = logger;
        
        // if no DB file provided, unzip included parameter file to temp folder
        if (paramTableFile == null)
        {
            try
            {
                var tmpDir = System.getProperty("java.io.tmpdir");
                var tmpFile = new File(tmpDir, getClass().getPackageName() + ".params.txt");
                tmpFile.deleteOnExit();
                
                try (var is = new GZIPInputStream(getClass().getResourceAsStream("all_parameters.txt.gz"));
                     var os = new FileOutputStream(tmpFile))
                {
                    ByteStreams.copy(is, os);
                }
                
                paramTableFile = tmpFile;
            }
            catch (IOException e)
            {
                throw new SensorHubException("Cannot unzip USGS parameter file");
            }
        }
        
        final var paramTable = this.paramTableFile = paramTableFile;
        this.paramUriMap = new TreeMap<>();
        this.paramCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .build(new CacheLoader<>() {
                public USGSParam load(String key) throws Exception
                {
                    Asserts.checkNotNull(key, "paramId");
                    
                    // TODO implement faster lookup?
                    try (var reader = new BufferedReader(new FileReader(paramTable)))
                    {
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            String[] row = line.split("\t");
                            if (key.equals(row[0]))
                            {
                                var param = createParam(row);
                                return param;
                            }
                        }
                        
                        return null;
                    }
                    catch (Exception e)
                    {
                        logger.error("Error while reading USGS parameter file", e);
                        return null;
                    }                        
                }                
            });
    }
    
    
    @Override
    public USGSParam getParamById(String id)
    {
        try
        {
            return paramCache.get(id);
        }
        catch (ExecutionException e)
        {
            throw new IllegalStateException("Cannot load param " + id, e);
        }
    }


    @Override
    public String getParamCode(String uri)
    {
        return paramUriMap.get(uri);
    }
    
    
    public void preloadParams(Set<String> groupIds, Set<String> paramIds)
    {
        CompletableFuture.runAsync(() -> {
            logger.info("Loading USGS observed parameters...");
            int count = 0;
            try (var reader = new BufferedReader(new FileReader(paramTableFile)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    // skip header lines
                    if (line.isBlank() || line.startsWith("#"))
                        continue;
                    
                    String[] row = line.split("\t");
                    if ((groupIds == null && paramIds == null) ||
                        (groupIds != null && groupIds.contains(row[1])) ||
                        (paramIds != null && paramIds.contains(row[0])) )
                    {
                        var param = createParam(row);
                        paramCache.put(param.code, param);
                        paramUriMap.put(param.uri, param.code);
                        count++;
                        
                        if (logger.isTraceEnabled())
                        {
                            logger.trace(
                                String.format("Added param code=%s, group=%s, name=%s",
                                param.code, param.group, param.name));
                        }
                    }
                }
                
                logger.info(String.format("Loaded %d parameters", count));
            }
            catch (Exception e)
            {
                logger.error("Error loading USGS parameter definitions", e);
            }
        });
    }
    
    
    private USGSParam createParam(String[] row)
    {
        var param = new USGSParam();
        param.code = row[0];
        param.group = row[1];
        param.name = row[2];
        param.uri = USGSWaterDataArchive.UID_PREFIX + "param:" + param.code;
        
        // handle unit or special cases for 'code' and 'count'
        var usgsUnit = row[12].trim();
        
        if ("code".equalsIgnoreCase(usgsUnit)) {
            param.isCategory = true;
        }
        else if ("count".equalsIgnoreCase(usgsUnit)) {
            param.isCount = true;
        }
        else
        {
            // convert to UCUM unit or URI            
            param.unit = convertToUcum(usgsUnit);
            if ("std".equals(param.unit) && param.name.contains("pH"))
                param.unit = "[pH]";
            
            if (!param.unit.startsWith("http") && !UnitParserUCUM.isValidUnit(param.unit))
            {
                logger.error("Unsupported unit: '{}' for param {}: {}", usgsUnit, param.code, param.name);
                param.unit = "urn:usgs:unit:unknown:" + param.unit;
            }
        }
        
        return param;
    }
    
    
    private String convertToUcum(String unit)
    {
        unit = unit.trim();
        
        // try to map the entire string first
        var uom = uomMappings.get(unit);
        if (uom != null)
            return uom;
        
        // if nothing was found, remove everything after space
        // it's usually qualifiers such as chemical species
        var sepIdx = unit.indexOf(' ');
        if (sepIdx > 0)
            unit = unit.substring(0, sepIdx);
        
        // try to map the remaining string
        uom = uomMappings.get(unit);
        if (uom != null)
            return uom;        
        
        // otherwise process each unit atom separately
        var parts = unit.split("/");
        var ucum = new StringBuilder();
        
        for (var s: parts)
        {        
            s = s.trim();
            var ucumAtom = uomAtomMappings.get(s);
            if (ucumAtom != null)
                s = ucumAtom;
            ucum.append(s).append('/');
        }
        
        ucum.setLength(ucum.length()-1); // remove trailing slash
        return ucum.toString();
    }

}
