package org.sensorhub.impl.sensor.twitter;

public final class TweetsModel 
{
    public final String created_at;

    public final long id;
    public final String id_str;

    public final String text;

    public final long in_reply_to_status_id;
    public final String in_reply_to_status_id_str;
    public final long in_reply_to_user_id;
    public final String in_reply_to_user_id_str;
    public final String in_reply_to_screen_name;

    public final Users user;
    public final Coordinates coordinates;
    public final Places place;

    public final boolean favorited;
    public final long favorite_count;
    public final boolean retweeted;
    public final long retweet_count;
    public final TweetsModel retweeted_status;
    public final boolean is_quote_status;

    public final Entities entities;
    public final String lang;
    public final String timestamp_ms;

    public TweetsModel(String created_at, long id, String id_str, String text, 
    		long in_reply_to_status_id, String in_reply_to_status_id_str, long in_reply_to_user_id, String in_reply_to_user_id_str, String in_reply_to_screen_name, 
    		Users user, Coordinates coordinates, Places place, 
    		boolean favorited, long favorite_count, boolean retweeted, long retweet_count, TweetsModel retweeted_status, boolean is_quote_status, 
    		Entities entities, String lang, String timestamp_ms) 
    {
        this.created_at = created_at;
        this.id = id;
        this.id_str = id_str;
        this.text = text;
        this.in_reply_to_status_id = in_reply_to_status_id;
        this.in_reply_to_status_id_str = in_reply_to_status_id_str;
        this.in_reply_to_user_id = in_reply_to_user_id;
        this.in_reply_to_user_id_str = in_reply_to_user_id_str;
        this.in_reply_to_screen_name = in_reply_to_screen_name;
        this.user = user;
        this.coordinates = coordinates;
        this.place = place;
        this.retweeted_status = retweeted_status;
        this.is_quote_status = is_quote_status;
        this.retweet_count = retweet_count;
        this.favorite_count = favorite_count;
        this.entities = entities;
        this.favorited = favorited;
        this.retweeted = retweeted;
        this.lang = lang;
        this.timestamp_ms = timestamp_ms;
    }

    public static final class Users 
    {
        public final long id;
        public final String id_str;

        public final String name;
        public final String screen_name;

        public final String location;
        public final String url;
        public final String description;

        public final long followers_count;
        public final long friends_count;
        public final long listed_count;
        public final long favourites_count;
        public final long statuses_count;

        public final String created_at;
        public final int utc_offset;
        public final String time_zone;

        public final boolean geo_enabled;
        public final String lang;

        public final boolean is_translator;

        public Users(long id, String id_str, 
        		String name, String screen_name, 
        		String location, String url, String description, 
        		long followers_count, long friends_count, long listed_count, long favourites_count, long statuses_count, 
        		String created_at, int utc_offset, String time_zone, 
        		boolean geo_enabled, String lang, 
        		boolean is_translator) 
        {
            this.id = id;
            this.id_str = id_str;

            this.name = name;
            this.screen_name = screen_name;

            this.location = location;
            this.url = url;
            this.description = description;

            this.followers_count = followers_count;
            this.friends_count = friends_count;
            this.listed_count = listed_count;
            this.favourites_count = favourites_count;
            this.statuses_count = statuses_count;

            this.created_at = created_at;
            this.utc_offset = utc_offset;
            this.time_zone = time_zone;

            this.geo_enabled = geo_enabled;
            this.lang = lang;

            this.is_translator = is_translator;
        }
    }

    public static final class Coordinates 
    {
    	public final float[] coordinates;
    	public final String type;
    	
        public Coordinates(float[] coordinates, String type) {
        	this.coordinates = coordinates;
        	this.type = type;
        }
    }

    public static final class Places 
    {
    	public final String id;
    	public final String place_type;

    	public final String country;
    	public final String country_code;
    	public final String name;
    	public final String full_name;

    	public final Geometry geometry;
    	public final BoundingBox bounding_box;

    	public final String url;

        public Places(String id, String place_type, 
        		String country, String country_code, String name, String full_name, 
        		Geometry geometry, BoundingBox bounding_box,
        		String url) 
        {
        	this.id = id;
        	this.place_type = place_type;

        	this.name = name;
        	this.full_name = full_name;
        	this.country = country;
        	this.country_code = country_code;
        	
        	this.geometry = geometry;
        	this.bounding_box = bounding_box;

        	this.url = url;
        }
        
        public static final class Geometry
        {
			public final float[] coordinates;
			public final String type;
			
			public Geometry(float[] coordinates, String type)
			{
				this.coordinates = coordinates;
				this.type = type;
			}
        }

        public static final class BoundingBox
        {
			public final float[][][] coordinates;
			public final String type;
			
			public BoundingBox(float[][][] coordinates, String type)
			{
				this.coordinates= coordinates;
				this.type = type;
			}
        }
    }

    public static final class Entities 
    {
        public final Hashtags[] hashtags;
        public final Urls[] urls;
        public final User_Mentions[] user_mentions;
        public final Media[] media;

        public Entities(Hashtags[] hashtags, Urls[] urls, User_Mentions[] user_mentions, Media[] media) 
        {
            this.hashtags = hashtags;
            this.urls = urls;
            this.user_mentions = user_mentions;
            this.media = media;
        }

        public static final class Hashtags 
        {
        	public final int[] indices;
        	public final String text;

            public Hashtags(int[] indices, String text) 
            {
            	this.indices = indices;
            	this.text = text;
            }
        }

        public static final class Urls 
        {
            public final int[] indices;
            public final String url;
            public final String display_url;
            public final String expanded_url;
    
            public Urls(int[] indices, String url, String display_url, String expanded_url) 
            {
                this.indices = indices;
                this.url = url;
                this.display_url = display_url;
                this.expanded_url = expanded_url;
            }
        }

        public static final class User_Mentions 
        {
            public final String screen_name;
            public final String name;
            public final long id;
            public final String id_str;
            public final int[] indices;
    
            public User_Mentions(String screen_name, String name, long id, String id_str, int[] indices) {
                this.screen_name = screen_name;
                this.name = name;
                this.id = id;
                this.id_str = id_str;
                this.indices = indices;
            }
        }
        
        public static final class Media
        {
        	public Media()
        	{
        	}
        }
    }
}