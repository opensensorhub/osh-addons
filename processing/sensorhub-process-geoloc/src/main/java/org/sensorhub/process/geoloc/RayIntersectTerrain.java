/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.geoloc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Text;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.geoloc.SRTMUtil;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Computes intersection of a 3D ray with terrain given as a
 * rectangular grid of altitude posts in EPSG 4979 projection.
 * It also allows height adjustment.
 * </p>
 *
 * @author Alex Robin
 * @since Nov 13, 2015
 */
public class RayIntersectTerrain extends RayIntersectEllipsoid
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:RayIntersectTerrain", "Ray Terrain Intersection", "Compute 3D intersection between a ray and a terrain model", RayIntersectSphere.class);
    
    protected Text srtmDataPath;
    
    protected GeoTransforms transforms;
    protected SRTMUtil util;
    protected Vect3d lla;
    protected double initAlti;
    

    public RayIntersectTerrain()
    {
        super(INFO);
                
        // change parameters
        srtmDataPath = new SWEHelper().createText()
            .label("SRTM Data Path")
            .description("Local absolute path to SRTM data folder")
            .build();
        paramData.add("srtmDataPath", srtmDataPath);        
    }
    
    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        this.lla = new Vect3d();
        this.transforms = new GeoTransforms(Ellipsoid.WGS84);
        
        // init SRTM data set
        String dataPath = srtmDataPath.getData().getStringValue();
        var path = Path.of(dataPath);
        if (dataPath == null || !Files.isDirectory(path) || !Files.isReadable(path))
            reportError("Missing or inaccessible SRTM data directory: " + dataPath);
        util = new SRTMUtil(dataPath);
        
        // set init altitude
        initAlti = heightAdjustment.getData().getDoubleValue();
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        double altitude = initAlti;
        double error = 0.0;
        double maxError = 15; // 15m
                
        // get ray origin input
        DataBlock originData = rayOrigin.getData();
        origin.x = originData.getDoubleValue(0);
        origin.y = originData.getDoubleValue(1);
        origin.z = originData.getDoubleValue(2);
        
        // get ray direction input
        DataBlock dirData = rayDirection.getData();
        dir.x = dirData.getDoubleValue(0);
        dir.y = dirData.getDoubleValue(1);
        dir.z = dirData.getDoubleValue(2);
        
        try
        {
            do
            {
                // set height adjustment to DEM altitude
                rie.setHeightAdjustment(altitude);
            	
                boolean ok = rie.computeIntersection(origin, dir, intersect);
                if (!ok)
                {
                    getLogger().debug("No intersection found");
                    break;
                }
                
                // get altitude at this position
                transforms.ECEFtoLLA(intersect, lla);
                altitude = getAltitude(lla.x, lla.y);
                error = Math.abs(altitude - lla.z);
            }
            while (error > maxError);
        }
        catch (IOException e)
        {
            throw new ProcessException("Error while looking up altitude from SRTM DEM data", e);
        }
    	
        // assign new values to intersection point output
        DataBlock intersectData = intersection.getData();
        intersectData.setDoubleValue(0, intersect.x);
        intersectData.setDoubleValue(1, intersect.y);
        intersectData.setDoubleValue(2, intersect.z);
    }
    
    
    private final double getAltitude(double lon, double lat) throws IOException
    {
        return util.getInterpolatedElevation(Math.toDegrees(lat), Math.toDegrees(lon));
    }
}
