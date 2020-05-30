/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Initial Developer is Terravolv, Inc. Portions created by the Initial
 * Developer are Copyright (C) 2014-2015 the Initial Developer. All Rights Reserved.
 */

var DEBUG_MODE = true;

var counter = 0;

var SENSORHUB_SERVER = 'sensiasoft.net:8181';
    
var SAT_FEED_DESCRIPTOR_BASE_URL              =  'http://' + SENSORHUB_SERVER + '/sensorhub/sos?service=SOS&version=2.0',
    SAT_FEED_DESCRIPTOR_REQUEST               =  '&request=GetResultTemplate',
    SAT_FEED_DESCRIPTOR_DESCRIPTOR_OFFERING   =  '&offering=urn:mysos:offering:predictedState',
    SAT_FEED_DESCRIPTOR_OBSERVED_PROPERTY     =  '&observedProperty=http://www.opengis.net/def/property/OGC/0/PlatformLocation';
    
var SAT_FEED_DESCRIPTOR =  SAT_FEED_DESCRIPTOR_BASE_URL + SAT_FEED_DESCRIPTOR_REQUEST + SAT_FEED_DESCRIPTOR_DESCRIPTOR_OFFERING + SAT_FEED_DESCRIPTOR_OBSERVED_PROPERTY;

var SAT_FEED_BASE_URL            = 'ws://' + SENSORHUB_SERVER + '/sensorhub/sos?service=SOS&version=2.0&request=GetResult',
    SAT_FEED_OFFERING            = '&offering=urn:mysos:offering:predictedState',
    SAT_FEED_OBSERVED_PROPERTY   = '&observedProperty=http://www.opengis.net/def/property/OGC/0/PlatformState',
    SAT_FEED_TEMPORAL_FILTER     = '&temporalFilter=phenomenonTime,now/2055-01-01&replaySpeed=100';

var SAT_FEED = SAT_FEED_BASE_URL + SAT_FEED_OFFERING + SAT_FEED_OBSERVED_PROPERTY + SAT_FEED_TEMPORAL_FILTER;
    
var ecef = [];

var satMarker = null;
          
var osm_StreetMapURL        = 'http://{s}.tile.osm.org/{z}/{x}/{y}.png',
    osm_StreetMapAttrib     = '',
    osm_SatelliteMapAttrib  = '',
    osm_StreetMap           = L.tileLayer(osm_StreetMapURL, {maxZoom: 18, attribution: osm_StreetMapAttrib, id: 'swe.map-street', noWrap: true});
    ggl_SatelliteHybridMap  = new L.Google('HYBRID'),
    ggl_SatelliteMap        = new L.Google('SATELLITE'),
    ggl_RoadMap             = new L.Google('ROADMAP'),
    map                     = new L.Map('map').setView(new L.LatLng(10.0, 0.0), 3);
