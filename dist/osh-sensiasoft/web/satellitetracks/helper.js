/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Initial Developer is Terravolv, Inc. Portions created by the Initial
 * Developer are Copyright (C) 2015 the Initial Developer. All Rights Reserved.
 *
 * Date: 2015-04-07T00:00Z
 */
$('#playSatTrack').click(function (e) {
  e.preventDefault();
  var polyline = L.polyline([]).addTo(map);
  var satSocket = getSatelliteTrackFeed(SAT_FEED);
});

function log(msg) {
  if (DEBUG_MODE) 
    $("<p style='padding:0;margin:0;'>" + msg + "</p>").appendTo("#dbg");
    var d = $('#dbg');
    d.scrollTop(d.prop("scrollHeight"));    
} // log

function getSatelliteTrackFeed(feedSource) {
  // Query SOS Satellite Track Stream
  try {
    var ws = new WebSocket(feedSource);
    ws.onmessage = function (event) {
      processSocketOnMessage(event);
    }
    ws.onerror = function (event) {
      ws.close();
    }
    ws.onclose = function (event) {
      log("Number of data points received: " + --counter);
      counter=0;
      satSocket = null;
    }
    return ws;    
  } catch (e) {
    alert (e);
  }
} // getSatelliteTrackFeed

function processSocketOnMessage(e) {
  var reader = new FileReader();
  reader.readAsBinaryString(e.data);
  reader.onload = function () {
    var rec = reader.result;
    if (null !== rec) {
      processWebSocketFeed(rec);
      var lla = ecef2lla(ecef);
      buildSatMarker(lla);
      counter++;
    }
  }
} //processSocketOnMessage()

function removeMarker(thisMarker) {
  map.removeLayer(thisMarker);
  thisMarker.update(thisMarker);
} //removeMarker

function processWebSocketFeed(rec) {
  response = interpretFeed(rec, ',');
  ecef[0] = response.x;
  ecef[1] = response.y;
  ecef[2] = response.z;
  ecef[3] = response.time;
  } // processWebSocketFeed

function interpretFeed(data, delimiter) {
  var vals = data.trim().split(delimiter);
  var s_time = vals[0],
      s_ecef_x = vals[1],
      s_ecef_y = vals[2],
      s_ecef_z = vals[3];
  return { time: s_time, x: s_ecef_x, y: s_ecef_y, z: s_ecef_z };
} // interpretFeed

function buildSatMarker(data) {
  var s_lat = data[0], s_long = data[1], s_alt = data[2], s_time = data[3];
  if (typeof s_lat === "undefined" || typeof s_long === "undefined") {
    throw new Error ("Latitude and/or Longitude is unavailable.");
    return;
  } /*Latitude or Longitude empty */ else {
    if (!isNaN(s_lat) && !isNaN(s_long)) {
      /*
      var   satMarker = L.marker([s_lat, s_long], 
                                  {icon: L.icon({ iconUrl: 'satellite.png',
                                                  iconSize: [16,16],})}).addTo(map)
                                .bindPopup('<div id="pop-satMarker' + counter + '">Time: ' + formatDateTime(s_time) + '<br/>Latitude: ' + s_lat + '<br />Longitude: ' + s_long + '</div>', { className: 'marker-popup' , closeButton: true});
      polyline.addLatLng(L.latLng(s_lat, s_long));
      */
      if (satMarker === null) {
        satMarker = L.marker([s_lat, s_long], 
                              {icon: L.icon({ iconUrl: 'satellite.png',
                                              iconSize: [16,16],})}).addTo(map)
                            .bindPopup('<div id="pop-satMarker">Time: ' + formatDateTime(s_time) + '<br/>Latitude: ' + s_lat + '<br />Longitude: ' + s_long + '</div>', { className: 'marker-popup' , closeButton: true});
      } else {
        satMarker.setLatLng([s_lat, s_long]);
        $('#pop-satMarker').html('Time: ' + formatDateTime(s_time) + '<br/>Latitude: ' + s_lat + '<br />Longitude: ' + s_long);
      }      
    }
  }  // Got Latitude or Longitude 
} // buildSatMarker

function addZero(x,n) {
  if (x.toString().length < n) {
    x = "0" + x;
  }
  return x;
}

function formatDateTime(_timestamp) {
  
  var weekday = new Array(7);
  weekday[0]=  "Sunday";
  weekday[1] = "Monday";
  weekday[2] = "Tuesday";
  weekday[3] = "Wednesday";
  weekday[4] = "Thursday";
  weekday[5] = "Friday";
  weekday[6] = "Saturday";

  var month = new Array(12);
  month[0] = "January";
  month[1] = "February";
  month[2] = "March";
  month[3] = "April";
  month[4] = "May";
  month[5] = "June";
  month[6] = "July";
  month[7] = "August";
  month[8] = "September";
  month[9] = "October";
  month[10] = "November";
  month[11] = "December";
  
  var d = new Date(_timestamp);
  var mo = month[d.getMonth()];
  var yyyy = d.getFullYear();
  var day = d.getDate().toString();
  var h = addZero(d.getHours(), 2);
  var m = addZero(d.getMinutes(), 2);
  var s = addZero(d.getSeconds(), 2);
  
  var ret = mo + " " + day + "," + yyyy + " " + h + ":" + m + ":" + s;
    return ret;
}
