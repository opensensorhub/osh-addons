/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.process.cam;

import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Matrix;
import net.opengis.swe.v20.Quantity;
import java.util.Arrays;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;


/**
 * <p>
 * Transforms pixel coordinates to a geographic location on the ground taking
 * into account the full camera model and intersecting with the earth ellipsoid.
 * </p>
 *
 * @author Alex Robin
 * @since Jun 11, 2021
 */
public class FovToCamMatrix extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("geoloc:FovToCamMatrix", "FOV to Camera Matrix", "Compute the pinhole camera matrix knowing FOV and image dimensions", FovToCamMatrix.class);
    
    enum FovTypeEnum {HFOV, VFOV, DFOV}
    
    protected Quantity fovIn;
    protected Matrix camMatrixOut;
    protected Category fovTypeParam;
    protected Count imageWidthParam;
    protected Count imageHeightParam;
    
    protected FovTypeEnum fovType;
    

    public FovToCamMatrix()
    {
        this(INFO);
    }
    
    
    @Override
    public void init() throws ProcessException
    {
        super.init();
        
        // read FOV type
        try
        {
            fovType = FovTypeEnum.valueOf(fovTypeParam.getData().getStringValue());
        }
        catch (IllegalArgumentException e)
        {
            reportError("Unsupported FOV type. Must be one of " + Arrays.toString(FovTypeEnum.values()));
        }
    }
    
    
    public FovToCamMatrix(ProcessInfo info)
    {
        super(info);
        var swe = new RasterHelper();
        
        // inputs
        inputData.add("fov", fovIn = swe.createQuantity()
            .definition(SWEHelper.getPropertyUri("FieldOfView"))
            .label("Field of View")
            .uomCode("deg")
            .build());
        
        // outputs
        outputData.add("camMatrix", camMatrixOut = swe.createMatrix()
            .definition(SWEHelper.getPropertyUri("CameraMatrix"))
            .label("Camera Matrix")
            .size(3, 3, true)
            .withElement("param", swe.createQuantity()
                .uomCode("1"))
            .build());
        
        // parameters
        paramData.add("imageWidth", imageWidthParam = swe.createCount()
            .definition(SWEHelper.getPropertyUri("GridWidth"))
            .label("Image Width")
            .build());
        
        paramData.add("imageHeight", imageHeightParam = swe.createCount()
            .definition(SWEHelper.getPropertyUri("GridHeight"))
            .label("Image Height")
            .build());
        
        paramData.add("fovType", fovTypeParam = swe.createCategory()
            .definition(SWEConstants.DEF_FLAG)
            .label("FOV Type")
            .addAllowedValues(FovTypeEnum.class)
            .build());
    }
    
    
    @Override
    public void execute() throws ProcessException
    {                    
        var fov = fovIn.getData().getDoubleValue();
        var w = imageWidthParam.getData().getDoubleValue();
        var h = imageHeightParam.getData().getDoubleValue();
        
        // compute focal length (f) in pixel units
        // using reference dimension corresponding to FOV type
        var refSize = fovType == FovTypeEnum.HFOV ? w :
                      fovType == FovTypeEnum.VFOV ? h :
                      Math.sqrt(w*w + h*h);
        double f = refSize / Math.tan(Math.toRadians(fov)/2) / 2.;
        
        // compute full pinhole camera matrix of the form
        // | f 0 cx |
        // | 0 f cy |
        // | 0 0  1 |
        DataBlock camMatrix = camMatrixOut.getData();
        camMatrix.setDoubleValue(0, f);
        camMatrix.setDoubleValue(2, w/2);
        camMatrix.setDoubleValue(4, f);
        camMatrix.setDoubleValue(5, h/2);
        camMatrix.setDoubleValue(8, 1.0);
    }
}
