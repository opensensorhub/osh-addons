package org.sensorhub.impl.sensor.openhab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.sensorhub.api.common.SensorHubException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class OpenHabHandler
{	
	public OpenHabItems getItemsFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonL = null;
		try
		{
			jsonL = getOpenHabInfo(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gsonL = new Gson();
		OpenHabItems habItems = gsonL.fromJson(jsonL, OpenHabItems.class);
		return habItems;
	}
	
	public OpenHabThings[] getThingsFromJSON(String jsonURLfromDriver) throws SensorHubException
	{
		String jsonL = null;
		try
		{
			jsonL = getOpenHabInfo(jsonURLfromDriver);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Gson gsonL = new Gson();
		OpenHabThings[] habThings = gsonL.fromJson(jsonL, OpenHabThings[].class);
		return habThings;
	}
	
	public String getOpenHabInfo(String jsonURL)  throws IOException
	{
    	URL urlGetDevices = new URL(jsonURL);
//    	System.out.println();
//    	System.out.println("Issuing request: " + urlGetDevices);
//    	System.out.println();
    	InputStream isGetDevices = urlGetDevices.openStream();
    	BufferedReader reader = null;
    	try
    	{
    		reader = new BufferedReader(new InputStreamReader(isGetDevices));
    		StringBuffer response = new StringBuffer();
	    	String line;
	    	while ((line = reader.readLine()) != null)
	    	{
	    		response.append(line);
	    	}
	    	return response.toString();
    	}
    	catch (IOException e)
        {
            throw new IOException("Cannot read server response", e);
        }
    	finally
    	{
    		if (reader != null)
    			reader.close();
    		if (isGetDevices != null)
    			isGetDevices.close();
    	}
	}
    
	/******************** Items Class *********************/
    static public class OpenHabItems
    {
    	String link;
    	String state;
    	StateDesc stateDescription;
    	String type;
    	String name;
    	String label;
    	String category;
    	String[] tags;
    	String[] groupNames;
    	
		public String getLink() {
			return link;
		}
		public void setLink(String link) {
			this.link = link;
		}
		public String getState() {
			return state;
		}
		public void setState(String state) {
			this.state = state;
		}
		public StateDesc getStateDescription() {
			return stateDescription;
		}
		public void setStateDescription(StateDesc stateDescription) {
			this.stateDescription = stateDescription;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		public String[] getTags() {
			return tags;
		}
		public void setTags(String[] tags) {
			this.tags = tags;
		}
		public String[] getGroupNames() {
			return groupNames;
		}
		public void setGroupNames(String[] groupNames) {
			this.groupNames = groupNames;
		}
    }
    
    static public class StateDesc
    {
    	String pattern;
    	boolean readOnly;
    	List<OP> options;
    	
		public String getPattern() {
			return pattern;
		}
		public void setPattern(String pattern) {
			this.pattern = pattern;
		}
		public boolean isReadOnly() {
			return readOnly;
		}
		public void setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
		}
		public List<OP> getOptions() {
			return options;
		}
		public void setOptions(List<OP> options) {
			this.options = options;
		}
    }
    
    static public class OP
    {
    	String value;
    	String label;
    	
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
    }
    /******************************************************/
    
    /******************** Things Class *********************/
    static public class OpenHabThings
    {
    	StatusInfo statusInfo;
    	String label;
    	String bridgeUID;
    	ThingConfig configuration;
    	ThingProps properties;
    	String UID;
    	String thingTypeUID;
    	ThingChannels[] channels;
    	String location;
    	
		public StatusInfo getStatusInfo() {
			return statusInfo;
		}
		public void setStatusInfo(StatusInfo statusInfo) {
			this.statusInfo = statusInfo;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getBridgeUID() {
			return bridgeUID;
		}
		public void setBridgeUID(String bridgeUID) {
			this.bridgeUID = bridgeUID;
		}
		public ThingConfig getConfiguration() {
			return configuration;
		}
		public void setConfiguration(ThingConfig configuration) {
			this.configuration = configuration;
		}
		public ThingProps getProperties() {
			return properties;
		}
		public void setProperties(ThingProps properties) {
			this.properties = properties;
		}
		public String getUID() {
			return UID;
		}
		public void setUID(String uID) {
			UID = uID;
		}
		public String getThingTypeUID() {
			return thingTypeUID;
		}
		public void setThingTypeUID(String thingTypeUID) {
			this.thingTypeUID = thingTypeUID;
		}
		public ThingChannels[] getChannels() {
			return channels;
		}
		public void setChannels(ThingChannels[] channels) {
			this.channels = channels;
		}
		public String getLocation() {
			return location;
		}
		public void setLocation(String location) {
			this.location = location;
		}	
    }
    
    static public class StatusInfo
    {
    	String status;
    	String statusDetail;
    	
    	public void setStatus(String status) {
    		this.status = status;
    	}
    	public String getStatus() {
    		return status;
    	}
    	public void setStatusDetail(String statusDetail) {
    		this.statusDetail = statusDetail;
    	}
    	public String getStatusDetail() {
    		return statusDetail;
    	}
    }
    
    static public class ThingConfig
    {
		String switchall_mode;
    	String nodename_location;
    	int powerlevel_timeout;
    	int powerlevel_level;
    	String nodename_name;
    	
    	public String getSwitchall_mode() {
			return switchall_mode;
		}
		public void setSwitchall_mode(String switchall_mode) {
			this.switchall_mode = switchall_mode;
		}
		public String getNodename_location() {
			return nodename_location;
		}
		public void setNodename_location(String nodename_location) {
			this.nodename_location = nodename_location;
		}
		public int getPowerlevel_timeout() {
			return powerlevel_timeout;
		}
		public void setPowerlevel_timeout(int powerlevel_timeout) {
			this.powerlevel_timeout = powerlevel_timeout;
		}
		public int getPowerlevel_level() {
			return powerlevel_level;
		}
		public void setPowerlevel_level(int powerlevel_level) {
			this.powerlevel_level = powerlevel_level;
		}
		public String getNodename_name() {
			return nodename_name;
		}
		public void setNodename_name(String nodename_name) {
			this.nodename_name = nodename_name;
		}
    }
    
    static public class ThingProps
    {
    	String zwave_class_basic;
    	String zwave_class_generic;
    	boolean zwave_frequent;
    	String zwave_neighbours;
    	String zwave_version;
    	boolean zwave_listening;
    	int zwave_deviceid;
    	int zwave_nodeid;
    	boolean zwave_routing;
    	boolean zwave_beaming;
    	String zwave_class_specific;
    	int zwave_manufacturer;
    	int zwave_devicetype;
    	
		public String getZwave_class_basic() {
			return zwave_class_basic;
		}
		public void setZwave_class_basic(String zwave_class_basic) {
			this.zwave_class_basic = zwave_class_basic;
		}
		public String getZwave_class_generic() {
			return zwave_class_generic;
		}
		public void setZwave_class_generic(String zwave_class_generic) {
			this.zwave_class_generic = zwave_class_generic;
		}
		public boolean isZwave_frequent() {
			return zwave_frequent;
		}
		public void setZwave_frequent(boolean zwave_frequent) {
			this.zwave_frequent = zwave_frequent;
		}
		public String getZwave_neighbours() {
			return zwave_neighbours;
		}
		public void setZwave_neighbours(String zwave_neighbours) {
			this.zwave_neighbours = zwave_neighbours;
		}
		public String getZwave_version() {
			return zwave_version;
		}
		public void setZwave_version(String zwave_version) {
			this.zwave_version = zwave_version;
		}
		public boolean isZwave_listening() {
			return zwave_listening;
		}
		public void setZwave_listening(boolean zwave_listening) {
			this.zwave_listening = zwave_listening;
		}
		public int getZwave_deviceid() {
			return zwave_deviceid;
		}
		public void setZwave_deviceid(int zwave_deviceid) {
			this.zwave_deviceid = zwave_deviceid;
		}
		public int getZwave_nodeid() {
			return zwave_nodeid;
		}
		public void setZwave_nodeid(int zwave_nodeid) {
			this.zwave_nodeid = zwave_nodeid;
		}
		public boolean isZwave_routing() {
			return zwave_routing;
		}
		public void setZwave_routing(boolean zwave_routing) {
			this.zwave_routing = zwave_routing;
		}
		public boolean isZwave_beaming() {
			return zwave_beaming;
		}
		public void setZwave_beaming(boolean zwave_beaming) {
			this.zwave_beaming = zwave_beaming;
		}
		public String getZwave_class_specific() {
			return zwave_class_specific;
		}
		public void setZwave_class_specific(String zwave_class_specific) {
			this.zwave_class_specific = zwave_class_specific;
		}
		public int getZwave_manufacturer() {
			return zwave_manufacturer;
		}
		public void setZwave_manufacturer(int zwave_manufacturer) {
			this.zwave_manufacturer = zwave_manufacturer;
		}
		public int getZwave_devicetype() {
			return zwave_devicetype;
		}
		public void setZwave_devicetype(int zwave_devicetype) {
			this.zwave_devicetype = zwave_devicetype;
		}
    }
    
    static public class ThingChannels
    {
    	String[] linkedItems;
    	String uid;
    	String id;
    	String channelTypeUID;
    	String itemType;
    	String kind;
    	String label;
    	String[] defaultTags;
    	ChannelProps properties;
    	ChannelConfig configuration;
    	
		public String[] getLinkedItems() {
			return linkedItems;
		}
		public void setLinkedItems(String[] linkedItems) {
			this.linkedItems = linkedItems;
		}
		public String getUid() {
			return uid;
		}
		public void setUid(String uid) {
			this.uid = uid;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getChannelTypeUID() {
			return channelTypeUID;
		}
		public void setChannelTypeUID(String channelTypeUID) {
			this.channelTypeUID = channelTypeUID;
		}
		public String getItemType() {
			return itemType;
		}
		public void setItemType(String itemType) {
			this.itemType = itemType;
		}
		public String getKind() {
			return kind;
		}
		public void setKind(String kind) {
			this.kind = kind;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String[] getDefaultTags() {
			return defaultTags;
		}
		public void setDefaultTags(String[] defaultTags) {
			this.defaultTags = defaultTags;
		}
		public ChannelProps getProperties() {
			return properties;
		}
		public void setProperties(ChannelProps properties) {
			this.properties = properties;
		}
		public ChannelConfig getConfiguration() {
			return configuration;
		}
		public void setConfiguration(ChannelConfig configuration) {
			this.configuration = configuration;
		}
    }
    
    static public class ChannelProps
    {
    	@SerializedName("binding:*:OnOffType")
    	String bindingOnOff;
    	
    	@SerializedName("binding:*:DecimalType")
    	String bindingDec;
    	
    	@SerializedName("binding:*:PercentType")
    	String bindingPerc;

		public String getBindingOnOff() {
			return bindingOnOff;
		}
		public void setBindingOnOff(String bindingOnOff) {
			this.bindingOnOff = bindingOnOff;
		}
		public String getBindingDec() {
			return bindingDec;
		}
		public void setBindingDec(String bindingDec) {
			this.bindingDec = bindingDec;
		}
		public String getBindingPerc() {
			return bindingPerc;
		}
		public void setBindingPerc(String bindingPerc) {
			this.bindingPerc = bindingPerc;
		}
    }
    
    static public class ChannelConfig
    {
    	int config_scale;

		public int getConfig_scale() {
			return config_scale;
		}

		public void setConfig_scale(int config_scale) {
			this.config_scale = config_scale;
		}
    }
    
    /*******************************************************/
}
