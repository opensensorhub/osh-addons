package org.sensorhub.process.geoloc;

import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Vector;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Vect3d;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.process.vecmath.VecMathHelper;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.helper.GeoPosHelper;

public class RayIntersection extends ExecutableProcessImpl {

    public static final OSHProcessInfo INFO = new OSHProcessInfo(
            "geoloc:RayIntersection",
            "Azimuth Intersection",
            "Compute 2D intersection between 2 or more locations with heading",
            RayIntersection.class);

    protected GeoTransforms transforms;

    protected Vector llaOrigin1;
    protected Quantity azimuth1;
    protected Vector llaOrigin2;
    protected Quantity azimuth2;

    protected Vect3d lla1 = new Vect3d();
    protected Vect3d p1 = new Vect3d();
    protected Vect3d lla2 = new Vect3d();
    protected Vect3d p2 = new Vect3d();

    protected Vector intersection;

    public RayIntersection() {
        super(INFO);

        transforms = new GeoTransforms();
        var fac = new GeoPosHelper();

        inputData.add("llaOrigin1", llaOrigin1 = fac.createLocationVectorLLA().build());

        azimuth1 = fac.createQuantity()
                .definition(GeoPosHelper.DEF_AZIMUTH_ANGLE)
                .label("Azimuth Angle")
                .description("Line-of-sight azimuth from true north, measured clockwise")
                .uomCode("deg")
                .axisId("Z")
                .build();
        inputData.add("azimuth1", azimuth1);

        inputData.add("llaOrigin2", llaOrigin2 = (Vector) llaOrigin1.clone());
        azimuth2 = (Quantity) azimuth1.clone();
        inputData.add("azimuth2", azimuth2);

        outputData.add("intersection", intersection = fac.newLocationVectorLLA("Intersection"));
    }

    @Override
    public void execute() throws ProcessException {
        VecMathHelper.toVect3d(llaOrigin1.getData(), lla1);
        VecMathHelper.toVect3d(llaOrigin2.getData(), lla2);
        double az1 = azimuth1.getData().getDoubleValue();
        double az2 = azimuth2.getData().getDoubleValue();

        try {
            double[] i = computeIntersection(lla1.y, lla1.x, az1, lla2.y, lla2.x, az2);

            var data = intersection.createDataBlock();
            data.setDoubleValue(0, i[1]); // latitude
            data.setDoubleValue(1, i[0]); // longitude
            data.setDoubleValue(2, lla1.z); // altitude
            intersection.setData(data);
            getLogger().info("Found intersection at {} {} {}", data.getDoubleValue(0), data.getDoubleValue(1), data.getDoubleValue(2));
        } catch (Exception e) {
            getLogger().info("Intersection not found!", e);
        }
    }

    public static double[] computeIntersection(
            double lat1, double lon1, double az1,
            double lat2, double lon2, double az2) {

        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);
        az1 = Math.toRadians(az1);
        az2 = Math.toRadians(az2);

        // Earth radius (meters)
        double R = Ellipsoid.WGS84.getEquatorRadius();

        // ENU coordinates of point B relative to point A
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double east = R * dLon * Math.cos(lat1);
        double north = R * dLat;

        // Directions (unit vectors in ENU)
        double dx1 = Math.sin(az1);
        double dy1 = Math.cos(az1);
        double dx2 = Math.sin(az2);
        double dy2 = Math.cos(az2);

        // 2D intersection
        double denom = dx1 * dy2 - dy1 * dx2;
        if (Math.abs(denom) < 1e-12)
            throw new IllegalArgumentException("Lines are parallel or nearly parallel");

        double t1 = (east * dy2 - north * dx2) / denom;

        // Intersection point (ENU)
        double interEast = t1 * dx1;
        double interNorth = t1 * dy1;

        // Convert ENU back to LLA
        double interLat = lat1 + interNorth / R;
        double interLon = lon1 + interEast / (R * Math.cos(lat1));

        return new double[] { Math.toDegrees(interLat), Math.toDegrees(interLon) };
    }

}