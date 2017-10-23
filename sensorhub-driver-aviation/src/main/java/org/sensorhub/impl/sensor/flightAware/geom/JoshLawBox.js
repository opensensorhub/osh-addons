/**
 * 
 */
const RADIUS_OF_EARTH = 6371e3;
const NAUTICAL_MILE_IN_METERS = 1852;

let lat = locationObj.location.lat;
let lon = locationObj.location.lon;
let alt = locationObj.location.alt;
let heading = locationObj.heading;
let airspeed = locationObj.airspeed;

let distance = 0;

// convert lat/lon to radians
lat = lat * Math.PI / 180;
lon = lon * Math.PI / 180;

distance = 10 * NAUTICAL_MILE_IN_METERS;
let brHeading = (heading + 90) * Math.PI / 180;
let brLat = Math.asin(Math.sin(lat) * Math.cos(distance / RADIUS_OF_EARTH) +
            Math.cos(lat) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(brHeading));
let brLon = lon + Math.atan2(Math.sin(brHeading) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(lat),
            Math.cos(distance / RADIUS_OF_EARTH) - Math.sin(lat) * Math.sin(brLat));

distance = 250 * NAUTICAL_MILE_IN_METERS;
let trHeading = heading * Math.PI / 180;
let trLat = Math.asin(Math.sin(brLat) * Math.cos(distance / RADIUS_OF_EARTH) +
            Math.cos(brLat) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(trHeading));
let trLon = brLon + Math.atan2(Math.sin(trHeading) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(brLat),
            Math.cos(distance / RADIUS_OF_EARTH) - Math.sin(brLat) * Math.sin(trLat));

distance = 10 * NAUTICAL_MILE_IN_METERS;
let blHeading = (heading - 90) * Math.PI / 180;
let blLat = Math.asin(Math.sin(lat) * Math.cos(distance / RADIUS_OF_EARTH) +
            Math.cos(lat) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(blHeading));
let blLon = lon + Math.atan2(Math.sin(blHeading) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(lat),
            Math.cos(distance / RADIUS_OF_EARTH) - Math.sin(lat) * Math.sin(blLat));

distance = 250 * NAUTICAL_MILE_IN_METERS;
let tlHeading = heading * Math.PI / 180;
let tlLat = Math.asin(Math.sin(blLat) * Math.cos(distance / RADIUS_OF_EARTH) +
            Math.cos(blLat) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(tlHeading));
let tlLon = blLon + Math.atan2(Math.sin(tlHeading) * Math.sin(distance / RADIUS_OF_EARTH) * Math.cos(blLat),
            Math.cos(distance / RADIUS_OF_EARTH) - Math.sin(blLat) * Math.sin(tlLat));

// convert LAW box coords to degrees
brLat = brLat * 180 / Math.PI;
brLon = brLon * 180 / Math.PI;
trLat = trLat * 180 / Math.PI;
trLon = trLon * 180 / Math.PI;
tlLat = tlLat * 180 / Math.PI;
tlLon = tlLon * 180 / Math.PI;
blLat = blLat * 180 / Math.PI;
blLon = blLon * 180 / Math.PI;

// convert lat/lon back to degrees
lat = lat * 180 / Math.PI;
lon = lon * 180 / Math.PI;

locationLayer.entities.removeAll();

let lawBoxEntity = locationLayer.entities.add({
            id: 'currentLawBox_' + locationObj.time,
            position: Cesium.Cartesian3.fromDegrees(lon, lat),
            polygon: {
                hierarchy: {
                    positions: Cesium.Cartesian3.fromDegreesArray(
                        [trLon, trLat, // Top Right
                         tlLon, tlLat, // Top Left
                         blLon, blLat, // Bottom Left
                         brLon, brLat] // Bottom Right
                    ),
                },
                material: new Cesium.Color(255 / 255, 255 / 255, 255 / 255, 0),
                height: 1,
                outline: true,
                outlineColor: new Cesium.Color(255 / 255, 0 / 255, 0 / 255, 1),
                outlineWidth: 2,
            }
});