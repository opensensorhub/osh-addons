# Rules Based Engine

A simple expert system based on predicate rules used to identify data streams supporting specific properties
as given by ontological definition URIs mapped to a term.

## Example Rules File

The rule file can be a simple text file containing a key word followed by a list of ontological definition URI strings 
joined logically by & (AND) and | (OR).  The logical operators can be symbolic or verbatim.

Rule files can contain comments, a comment line begins with the # symbol

    # This is a comment line, there can be unlimited comment lines but each must begin with #
    location http://www.opengis.net/def/property/OGC/0/SensorLocation | http://www.opengis.net/def/property/OGC/0/PlatformLocation & http://sensorml.com/ont/swe/property/GeodeticLatitude & http://sensorml.com/ont/swe/property/Longitude
    orientation http://www.opengis.net/def/property/OGC/0/PlatformOrientation
    sensor_orientation http://www.opengis.net/def/property/OGC/0/SensorOrientation
    position http://www.opengis.net/def/property/OGC/0/PlatformLocation AND http://www.opengis.net/def/property/OGC/0/PlatformOrientation

## Structure and Meaning of Rules

    location http://www.opengis.net/def/property/OGC/0/SensorLocation | http://www.opengis.net/def/property/OGC/0/PlatformLocation & http://sensorml.com/ont/swe/property/GeodeticLatitude & http://sensorml.com/ont/swe/property/Longitude

This rule will return a result set containing the system id, corresponding data stream ids and information regarding 
the path to the fields for the given data structure each observation produced by the data stream will generate. 
The location is given by a definition for _SensorLocation_ or _PlatformLocation_ and is further restricted by location 
being given as _GeodeticLatitude_ and _Longitude_

The result set is given below:

      {
        "resultSet": [{
          "systemId": "41s2wfnr",
          "location": [{
              "dataStreamId": "8wl0jfwp",
              "paths": [{
                  "name": "location",
                  "definition": "http://www.opengis.net/def/property/OGC/0/SensorLocation",
                  "path": "location"
                },
                {
                  "name": "lat",
                  "definition": "http://sensorml.com/ont/swe/property/GeodeticLatitude",
                  "path": "location.lat"
                },
                {
                  "name": "lon",
                  "definition": "http://sensorml.com/ont/swe/property/Longitude",
                  "path": "location.lon"
                }]
            }]
        }]
      }

## Usage

        try {

            RuleManager.loadRules(new File([PATH TO RULE DEFINITIONS), rules);

            rulesEngine = new RulesEngine(getParentHub());

            rulesEngine.setRules(rules);

            rulesEngine.setTargetRuleIds(List.of([RULE ID A], [RULE ID B], ...);

            rulesEngine.fire();

            DataStreamResults results = rulesEngine.getResultSet();

        } catch (IOException e) {

            throw new SensorHubException("Failed to load rules", e);
        }


