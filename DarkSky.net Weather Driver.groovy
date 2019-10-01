/*
   DarkSky.net Weather Driver
   Import URL: https://raw.githubusercontent.com/HubitatCommunity/DarkSky.net-Weather-Driver/master/DarkSky.net%20Weather%20Driver.groovy
   Copyright 2019 @Matthew (Scottma61)
 
   Many people contributed to the creation of this driver.  Significant contributors include:
   - @Cobra who adapted it from @mattw01's work and I thank them for that!
   - @bangali for his original APIXU.COM base code that much of the early versions of this driver was 
     adapted from. 
   - @bangali for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more
     recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
   - @csteele (and prior versions from @bangali) for the attribute selection code.
   - @csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
   - @bangali also contributed the icon work from
     https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
     of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
    - @storageanarchy for his Dark Sky Icon mapping and some new icons to compliment the Vclouds set.
 
   In addition to all the cloned code from the Hubitat community, I have heavily modified/created new
   code myself @Matthew (Scottma61) with lots of help from the Hubitat community.  If you believe you
   should have been acknowledged or received attribution for a code contribution, I will happily do so.
   While I compiled and orchestrated the driver, very little is actually original work of mine.

   This driver is free to use.  I do not accept donations. Please feel free to contribute to those
   mentioned here if you like this work, as it would not have been possible without them.

   This driver is intended to pull weather data from DarkSky.net (http://darksky.net). You will need your
   DarkSky API key to use the data from that site.
 
   You can select to use a base set of condition icons from the forecast source, or an 'alternative'
   (fancier) set.  The base 'Standard' icon set will be from WeatherUnderground.  You may choose the
   fancier 'Alternative' icon set if you use the Dark Sky.
 
   The driver exposes both metric and imperial measurements for you to select from.
 
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
 
   Last Update 10/01/2019
  { Left room below to document version changes...}
 

   V1.2.1   Added ability to show 'knots' for wind/gust speeds                                - 10/01/2019
   V1.2.0   Eliminated 'Std' Icons.  Reworked condition_code/condition_text.                  - 09/30/2019
   V1.1.9   myTile format tweaking                                                            - 09/29/2019
   V1.1.8   Bug fixes, optimizations, Added 'wind' and lux jitter control                     - 09/28/2019
   V1.1.7   More myTile 'display:inline' corrections                                          - 09/28/2019
   V1.1.6   myTile 'display:inline' correction                                                - 09/27/2019
   V1.1.5   myTile enhancement for excessive length                                           - 09/27/2019
   V1.1.4   Prevent myTile from exceeding 1,024 characters                                    - 09/26/2019
   V1.1.3   Corrected myTile for 'alert' condition                                            - 09/26/2019
   V1.1.2 - Added 'wind_cardinal', more code optimization and cleanup                         - 09/25/2019 
   V1.1.1 - Corrected MoonPhase, optimized lux updates and code optimizations re-organized    - 09/24/2019
            preference order and some goupings of 'optional' attributes.
   V1.1.0 - Randomized schedule start times, Added 'Powered by DarkSky' attribution           - 09/18/2019
   V1.0.9 - Default to 'TinyURL' for icon location, added log when changeing schedule         - 09/16/2019
   V1.0.8 - Changed icon location to prevent duplication - Please update icon file location   - 09/16/2019
   V1.0.7 - Moved driver to the HubitatCommunity github, added 'Nighttime' schedule option    - 09/16/2019
            added upDateCheck() to show if driver is current (thanks @csteele)
   V1.0.6 - Another optional attribute bug fix.                                               - 09/15/2019 
   V1.0.5 - Tweaking and bug fixes.                                                           - 09/14/2019
   V1.0.4 - Added 'weatherIcons' used for OWM icons/dashboard                                 - 09/14/2019
   V1.0.3 - Added windSpeed and windDirection, required for some dashboards.                  - 09/14/2019
   V1.0.2 - Attribute now dislplayed for dashboards ** Read caution below **                  - 09/14/2019 
   V1.0.1 - Bug fixes.                                                                        - 09/13/2019  
   V1.0.0 - Initial release of driver with ApiXU.com completely removed.                      - 09/13/2019 
 =========================================================================================================
**ATTRIBUTES CAUTION**
The way the 'optional' attributes work:
 - Initially, only the optional attributes selected will show under 'Current States' and will be available
   in dashboards.
 - Once an attribute has been selected it too will show under 'Current States' and be available in dashboards.
   <*** HOWEVER ***> If you ever de-select the optional attribute, it will still show under 'Current States' 
   and will still show as an attribute for dashboards **BUT IT'S DATA WEILL NO LONGER BE REFRESHED WITH DATA
   POLLS**.  This means what is shown on the 'Current States' and dashboard tiles for de-selected attributes
   may not be current valid data.
 - To my knowledge, the only way to remove the de-selected attribute from 'Current States' and not show it as
   available in the dashboard is to delete the virtual device and create a new one AND DO NOT SELECT the
   attribute you do not want to show.
*/
public static String version()      {  return "1.2.1"  }
import groovy.transform.Field

metadata {
    definition (name: "DarkSky.net Weather Driver", namespace: "Matthew", author: "Scottma61", importUrl: "https://raw.githubusercontent.com/HubitatCommunity/DarkSky.net-Weather-Driver/master/DarkSky.net%20Weather%20Driver.groovy") {
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
 		capability "Pressure Measurement"
 		capability "Ultraviolet Index"
        
        capability "Refresh"
	
		attributesMap.each
		{
            k, v -> if (v.typeof)        attribute "${k}" , "${v.typeof}"
		}
//    The following attributes may be needed for dashboards that require these attributes,
//    so they are alway available and shown by default.
        attribute "city", "string"              //Hubitat  OpenWeather  SharpTool.io  SmartTiles 
        attribute "feelsLike", "number"         //SharpTool.io  SmartTiles
        attribute "forecastIcon", "string"      //SharpTool.io
        attribute "localSunrise", "string"      //SharpTool.io  SmartTiles
        attribute "localSunset", "string"       //SharpTool.io  SmartTiles
        attribute "percentPrecip", "number"     //SharpTool.io  SmartTiles
        attribute "weather", "string"           //SharpTool.io  SmartTiles
        attribute "weatherIcon", "string"       //SharpTool.io  SmartTiles
        attribute "weatherIcons", "string"      //Hubitat  openWeather
        attribute "wind", "number"              //SharpTool.io
        attribute "windDirection", "number"     //Hubitat  OpenWeather
        attribute "windSpeed", "number"         //Hubitat  OpenWeather

//      The attributes below are sub-groups of optional attributes.  They need to be listed here to be available
//DSAttribution
        attribute "dsIcondarktext", "string"
        attribute "dsIconlighttext", "string"
        
//fcstHighLow
		attribute "forecastHigh", "string"
		attribute "forecastLow", "string"

// controlled with localSunrise
		attribute "tw_begin", "string"
		attribute "sunriseTime", "string"
		attribute "noonTime", "string"
		attribute "sunsetTime", "string"
		attribute "tw_end", "string"

//obspoll   these are the same value...
		attribute "last_poll_Forecast", "string"
		attribute "last_observation_Forecast", "string"

//precipExtended
		attribute "rainDayAfterTomorrow", "number"
		attribute "rainTomorrow", "number"

//nearestStorm
		attribute "nearestStormBearing", "string"
		attribute "nearestStormCardinal", "string"
		attribute "nearestStormDirection", "string"
		attribute "nearestStormDistance", "number"

        command "pollData"         
    }
    def settingDescr = settingEnable ? "<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>" : "<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>"

    preferences() {
		section("Query Inputs"){
			input "apiKey", "text", required: true, title: "Type DarkSky.net API Key Here", defaultValue: null
            input "city", "text", required: true, defaultValue: "City or Location name forecast area", title: "City name"
			input "pollIntervalForecast", "enum", title: "External Source Poll Interval (daytime)", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "2 Minutes", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "pollIntervalForecastnight", "enum", title: "External Source Poll Interval (nighttime)", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "2 Minutes", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "logSet", "bool", title: "Create extended Logging", required: true, defaultValue: false
	    	input "tempFormat", "enum", required: true, defaultValue: "Fahrenheit (°F)", title: "Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)",  options: ["Fahrenheit (°F)", "Celsius (°C)"]
            input "datetimeFormat", "enum", required: true, defaultValue: "1", title: "Display Unit - Date-Time Format",  options: [1:"m/d/yyyy 12 hour (am|pm)", 2:"m/d/yyyy 24 hour", 3:"mm/dd/yyyy 12 hour (am|pm)", 4:"mm/dd/yyyy 24 hour", 5:"d/m/yyyy 12 hour (am|pm)", 6:"d/m/yyyy 24 hour", 7:"dd/mm/yyyy 12 hour (am|pm)", 8:"dd/mm/yyyy 24 hour", 9:"yyyy/mm/dd 24 hour"]
            input "distanceFormat", "enum", required: true, defaultValue: "Miles (mph)", title: "Display Unit - Distance/Speed: Miles, Kilometres or knots",  options: ["Miles (mph)", "Kilometers (kph)", "knots"]
            input "pressureFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Pressure: Inches or Millibar",  options: ["Inches", "Millibar"]
            input "rainFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Precipitation: Inches or Millimetres",  options: ["Inches", "Millimetres"]
            input "luxjitter", "bool", title: "Use lux jitter control (rounding)?", required: true, defaultValue: false
			input "iconLocation", "text", required: true, defaultValue: "https://tinyurl.com/y6xrbhpf/", title: "Alternative Icon Location:"
            input "iconType", "bool", title: "Condition Icon: On = Current - Off = Forecast", required: true, defaultValue: false
            input "dsIconbackgrounddark", "bool", required: true, defaultValue: false, title: "DarkSky logo text color for myTile/weatherSummary: On = Dark  -  Off = Light"                        

            input "settingEnable", "bool", title: "<b>Display All Optional Attributes</b>", description: "$settingDescr", defaultValue: true
	// build a Selector for each mapped Attribute or group of attributes
	    	attributesMap.each
		    {
	    		keyname, attribute ->
                if (settingEnable) {
                    input "${keyname}Publish", "bool", title: "${attribute.title}", required: true, defaultValue: "${attribute.default}", description: "<br>${attribute.descr}<br>"
                    if(keyname == "weatherSummary") input "summaryType", "bool", title: "Full Weather Summary", description: "<br>Full: on or short: off summary?<br>", required: true, defaultValue: false
                }
	    	}
            if (settingEnable) {
                input "windPublish", "bool", title: "Wind Speed", required: true, defaultValue: "false", description: "<br>Display 'wind' speed<br>"
            }
        }
    }    
}

// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
void pollSunRiseSet() {
    currDate = new Date().format("yyyy-MM-dd", TimeZone.getDefault())
    log.info("DarkSky.net Weather Driver - INFO: Polling Sunrise-Sunset.org")
    def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=" + location.latitude + "&lng=" + location.longitude + "&formatted=0" ]
    if (currDate) {requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=" + location.latitude + "&lng=" + location.longitude + "&formatted=0&date=$currDate" ]}
    LOGINFO("Poll Sunrise-Sunset: $requestParams")
    asynchttpGet("sunRiseSetHandler", requestParams)
    return
}

void sunRiseSetHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		sunRiseSet = resp.getJson().results
		updateDataValue("sunRiseSet", resp.data)
        LOGINFO("Sunrise-Sunset Data: $sunRiseSet")
        setDateTimeFormats(datetimeFormat)
		updateDataValue("riseTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format("HH:mm", TimeZone.getDefault()))     
        updateDataValue("noonTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("setTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_begin", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_end", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("localSunset",new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
		updateDataValue("localSunrise", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
    } else {
		log.warn "DarkSky.net Weather Driver - Sunrise-Sunset api did not return data"
	}
    return
}
// >>>>>>>>>> End Sunrise-Sunset Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin DarkSky Poll Routines >>>>>>>>>>
void pollDS() {
    if( apiKey == null ) {
        log.error "DarkSky API Key not found.  Please configure in preferences."
        return
    }
	def ParamsDS = [ uri: "https://api.darksky.net/forecast/${apiKey}/" + location.latitude + ',' + location.longitude + "?units=us&exclude=minutely,hourly,flags" ]
    LOGINFO("Poll DarkSky: $ParamsDS")
	asynchttpGet("pollDSHandler", ParamsDS)
    return
}

void pollDSHandler(resp, data) {
    log.info "DarkSky.net Weather Driver - INFO: Polling DarkSky.net"
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        def ds = parseJson(resp.data)
		doPollDS(ds)		// parse the data returned by DarkSky
	} else {
		log.error "DarkSky.net Weather Driver - DarkSky weather api did not return data"
	}
}

void doPollDS(Map ds) {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
    setDateTimeFormats(datetimeFormat)    
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
    updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))    
    if(getDataValue("riseTime") <= getDataValue("currTime") && getDataValue("setTime") >= getDataValue("currTime")) {
        updateDataValue("is_day", "1")
    } else {
        updateDataValue("is_day", "0")
    }
    if(getDataValue("currTime") < getDataValue("tw_begin") || getDataValue("currTime") > getDataValue("tw_end")) {
        updateDataValue("is_light", "0")
    } else {
        updateDataValue("is_light", "1")
    }
    if(getDataValue("is_light") != getDataValue("is_lightOld")) {
        if(getDataValue("is_light")=="1") {
            log.info("DarkSky.net Weather Driver - INFO: Switching to Daytime schedule.")
        }else{
            log.info("DarkSky.net Weather Driver - INFO: Switching to Nighttime schedule.")
        }
        initialize()
        updateDataValue("is_lightOld", getDataValue("is_light"))
    }    
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<  

// <<<<<<<<<< Begin Setup Forecast Variables >>>>>>>>>>            
    fotime = new Date(ds.currently.time * 1000L)
    updateDataValue("fotime", fotime.toString())
	futime = new Date(ds.currently.time * 1000L)
    updateDataValue("futime", futime.toString())
	
// <<<<<<<<<< Begin Process Standard Weather-Station Variables (Regardless of Forecast Selection)  >>>>>>>>>>    
    updateDataValue("dewpoint", (isFahrenheit ? (Math.round(ds.currently.dewPoint.toBigDecimal() * 10) / 10) : (Math.round((ds.currently.dewPoint.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    updateDataValue("humidity", (Math.round(ds.currently.humidity.toBigDecimal() * 1000) / 10).toString())
    updateDataValue("pressure", (isPressureMetric ? (Math.round(ds.currently.pressure.toBigDecimal() * 10) / 10) : (Math.round(ds.currently.pressure.toBigDecimal() * 0.029529983071445 * 100) / 100)).toString())
    updateDataValue("temperature", (isFahrenheit ? (Math.round(ds.currently.temperature.toBigDecimal() * 10) / 10) : (Math.round((ds.currently.temperature.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    String w_string_bft
    String w_bft_icon
    BigDecimal tws = ds.currently.windSpeed.toBigDecimal()
    if(tws < 1.0) {
        w_string_bft = "Calm"; w_bft_icon = 'wb0.png'
    }else if(tws < 4.0) {
        w_string_bft = "Light air"; w_bft_icon = 'wb1.png'
    }else if(tws < 8.0) {
        w_string_bft = "Light breeze"; w_bft_icon = 'wb2.png'
    }else if(tws < 13.0) {
        w_string_bft = "Gentle breeze"; w_bft_icon = 'wb3.png'
    }else if(tws < 19.0) {
        w_string_bft = "Moderate breeze"; w_bft_icon = 'wb4.png'
    }else if(tws < 25.0) {
        w_string_bft = "Fresh breeze"; w_bft_icon = 'wb5.png'
    }else if(tws < 32.0) {
        w_string_bft = "Strong breeze"; w_bft_icon = 'wb6.png'
    }else if(tws < 39.0) {
        w_string_bft = "High wind, moderate gale, near gale"; w_bft_icon = 'wb7.png'
    }else if(tws < 47.0) {
        w_string_bft = "Gale, fresh gale"; w_bft_icon = 'wb8.png'
    }else if(tws < 55.0) {
        w_string_bft = "Strong/severe gale"; w_bft_icon = 'wb9.png'
    }else if(tws < 64.0) {
        w_string_bft = "Storm, whole gale"; w_bft_icon = 'wb10.png'
    }else if(tws < 73.0) {
        w_string_bft = "Violent storm"; w_bft_icon = 'wb11.png'
    }else if(tws >= 73.0) {
        w_string_bft = "Hurricane force"; w_bft_icon = 'wb12.png'
    }
	updateDataValue("wind_string_bft", w_string_bft)
    updateDataValue("wind_bft_icon", w_bft_icon)
    updateDataValue("wind", (isDistanceMetric ? (isDistanceKnots ? (Math.round(ds.currently.windSpeed.toBigDecimal() * 0.868976 * 10) / 10) : (Math.round(ds.currently.windSpeed.toBigDecimal() * 1.609344 * 10) / 10)) : (Math.round(ds.currently.windSpeed.toBigDecimal() * 10) / 10)).toString())
    updateDataValue("wind_gust", (isDistanceMetric ? (isDistanceKnots ? (Math.round(ds.currently.windGust.toBigDecimal() * 0.868976 * 10) / 10) : (Math.round(ds.currently.windGust.toBigDecimal() * 1.609344 * 10) / 10)) : (Math.round(ds.currently.windGust.toBigDecimal() * 10) / 10)).toString())
    updateDataValue("wind_degree", ds.currently.windBearing.toInteger().toString())	
    String w_cardinal
    String w_direction
    BigDecimal twb = ds.currently.windBearing.toBigDecimal()
    if(twb < 11.25) {
        w_cardinal = 'N'; w_direction = 'North'
    }else if(twb < 33.75) {
        w_cardinal = 'NNE'; w_direction = 'North-Northeast'
    }else if(twb < 56.25) {
        w_cardinal = 'NE';  w_direction = 'Northeast'
    }else if(twb < 56.25) {
        w_cardinal = 'ENE'; w_direction = 'East-Northeast'
    }else if(twb < 101.25) {
        w_cardinal = 'E'; w_direction = 'East'
    }else if(twb < 123.75) {
        w_cardinal = 'ESE'; w_direction = 'East-Southeast'
    }else if(twb < 146.25) {
        w_cardinal = 'SE'; w_direction = 'Southeast'
    }else if(twb < 168.75) {
        w_cardinal = 'SSE'; w_direction = 'South-Southeast'
    }else if(twb < 191.25) {
        w_cardinal = 'S'; w_direction = 'South'
    }else if(twb < 213.75) {
        w_cardinal = 'SSW'; w_direction = 'South-Southwest'
    }else if(twb < 236.25) {
        w_cardinal = 'SW'; w_direction = 'Southwest'
    }else if(twb < 258.75) {
        w_cardinal = 'WSW'; w_direction = 'West-Southwest'
    }else if(twb < 281.25) {
        w_cardinal = 'W'; w_direction = 'West'
    }else if(twb < 303.75) {
        w_cardinal = 'WNW'; w_direction = 'West-Northwest'
    }else if(twb < 326.25) {
        w_cardinal = 'NW'; w_direction = 'Northwest'
    }else if(twb < 348.75) {
        w_cardinal = 'NNW'; w_direction = 'North-Northwest'
    }else if(twb >= 348.75) {
        w_cardinal = 'N'; w_direction = 'North'
    }
    updateDataValue("wind_direction", w_direction)
    updateDataValue("wind_cardinal", w_cardinal)	
    updateDataValue("wind_string", w_string_bft + " from the " + getDataValue("wind_direction") + (getDataValue("wind").toBigDecimal() < 1.0 ? '': " at " + getDataValue("wind") + (isDistanceMetric ? (isDistanceKnots ? " knots" : " KPH") : " MPH")))
    String s_cardinal
    String s_direction 
    if(!ds.currently.nearestStormBearing){
        updateDataValue("nearestStormBearing", "360")
        s_cardinal = 'U'
        s_direction = 'Unknown'        
    }else{
        updateDataValue("nearestStormBearing", (Math.round(ds.currently.nearestStormBearing * 10) / 10).toString())
        BigDecimal tnsb = ds.currently.nearestStormBearing.toBigDecimal()
        if(tnsb < 11.25) {
            s_cardinal = 'N'; s_direction = 'North'
        }else if(tnsb < 33.75) {
            s_cardinal = 'NNE'; s_direction = 'North-Northeast'
        }else if(tnsb < 56.25) {
            s_cardinal = 'NE';  s_direction = 'Northeast'
        }else if(tnsb < 78.75) {
            s_cardinal = 'ENE'; s_direction = 'East-Northeast'
        }else if(tnsb < 101.25) {
            s_cardinal = 'E'; s_direction = 'East'
        }else if(tnsb < 123.75) {
            s_cardinal = 'ESE'; s_direction = 'East-Southeast'
        }else if(tnsb < 146.25) {
            s_cardinal = 'SE'; s_direction = 'Southeast'
        }else if(tnsb < 168.75) {
            s_cardinal = 'SSE'; s_direction = 'South-Southeast'
        }else if(tnsb < 191.25) {
            s_cardinal = 'S'; s_direction = 'South'
        }else if(tnsb < 213.75) {
            s_cardinal = 'SSW'; s_direction = 'South-Southwest'
        }else if(tnsb < 236.25) {
            s_cardinal = 'SW'; s_direction = 'Southwest'
        }else if(tnsb < 258.75) {
            s_cardinal = 'WSW'; s_direction = 'West-Southwest'
        }else if(tnsb < 281.25) {
            s_cardinal = 'W'; s_direction = 'West'
        }else if(tnsb < 303.75) {
            s_cardinal = 'WNW'; s_direction = 'West-Northwest'
        }else if(tnsb < 326.26) {
            s_cardinal = 'NW'; s_direction = 'Northwest'
        }else if(tnsb < 348.75) {
            s_cardinal = 'NNW'; s_direction = 'North-Northwest'
        }else if(tnsb >= 348.75) {
            s_cardinal = 'N'; s_direction = 'North'
        }
    }
    updateDataValue("nearestStormCardinal", s_cardinal)
    updateDataValue("nearestStormDirection", s_direction)
    updateDataValue("nearestStormDistance", (!ds.currently.nearestStormDistance ? "9999.9" : (isDistanceMetric ? (Math.round(ds.currently.nearestStormDistance.toBigDecimal() * 1.609344 * 10) / 10) : (Math.round(ds.currently.nearestStormDistance.toBigDecimal() * 10) / 10)).toString()))
	updateDataValue("ozone", (Math.round(ds.currently.ozone.toBigDecimal() * 10 ) / 10).toString())
    String mPhase
    BigDecimal tmnp = ds.daily.data[0].moonPhase.toBigDecimal() * 100
    if (tmnp < 6.25) {mPhase = "New Moon"}
    else if (tmnp < 18.75) {mPhase = "Waxing Crescent"}
    else if (tmnp < 31.25) {mPhase = "First Quarter"}
    else if (tmnp < 43.75) {mPhase = "Waxing Gibbous"}
    else if (tmnp < 56.25) {mPhase = "Full Moon"}
    else if (tmnp < 68.75) {mPhase = "Waning Gibbous"}
    else if (tmnp < 81.25) {mPhase = "Last Quarter"}
    else if (tmnp < 93.75) {mPhase = "Waxing Gibbous"}
    else if (tmnp >= 93.75) {mPhase = "New Moon"} 
    updateDataValue("moonPhase", mPhase)
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<	
	int cloudCover = 1
    if (!ds.currently.cloudCover) {
        cloudCover = 1
    } else {
        cloudCover = (ds.currently.cloudCover.toBigDecimal() <= 0.01) ? 1 : (ds.currently.cloudCover.toBigDecimal() * 100)
    }
    updateDataValue("cloud", cloudCover.toString())
    if (!ds.alerts){
        updateDataValue("alert", "No current weather alerts for this area")
        updateDataValue("possAlert", "false")
    } else {
        updateDataValue("alert", ds.alerts.title.toString().replaceAll("[{}\\[\\]]", "").split(/,/)[0])
        updateDataValue("possAlert", "true")
    }
    updateDataValue("vis", (isDistanceMetric ? ds.currently.visibility.toBigDecimal() * 1.60934 : ds.currently.visibility.toBigDecimal()).toString())
    updateDataValue("percentPrecip", !ds.daily.data[0].precipProbability ? "1" : (ds.daily.data[0].precipProbability.toBigDecimal() * 100).toInteger().toString())

    String c_code = getdsIconCode(ds?.currently?.icon, ds?.currently?.summary)
    updateDataValue("condition_code", c_code)
    updateDataValue("condition_text", getcondText(c_code))    

    String f_code = getdsIconCode(ds?.daily?.data[0]?.icon, ds?.daily?.data[0]?.summary)
    updateDataValue("forecast_code", f_code)
    updateDataValue("forecast_text", getcondText(f_code))

    updateDataValue("forecastHigh", (isFahrenheit ? (Math.round(ds.daily.data[0].temperatureHigh.toBigDecimal() * 10) / 10) : (Math.round((ds.daily.data[0].temperatureHigh.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    updateDataValue("forecastLow", (isFahrenheit ? (Math.round(ds.daily.data[0].temperatureLow.toBigDecimal() * 10) / 10) : (Math.round((ds.daily.data[0].temperatureLow.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    if(precipExtendedPublish){
        updateDataValue("rainTomorrow", (ds.daily.data[1].precipProbability.toBigDecimal() * 100).toInteger().toString())
        updateDataValue("rainDayAfterTomorrow", (ds.daily.data[2].precipProbability.toBigDecimal() * 100).toInteger().toString())
    }
    updateLux(false)
    updateDataValue("ultravioletIndex", ds.currently.uvIndex.toBigDecimal().toString())
	updateDataValue("feelsLike", (isFahrenheit ? (Math.round(ds.currently.apparentTemperature.toBigDecimal() * 10) / 10) : (Math.round((ds.currently.apparentTemperature.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
// >>>>>>>>>> End Setup Forecast Variables <<<<<<<<<<

	// <<<<<<<<<< Begin Icon Processing  >>>>>>>>>>    
//    if(sourceImg==false){ // 'Alternative' Icons selected
    String imgName = (getDataValue("iconType")== 'true' ? getImgName(getDataValue("condition_code")) : getImgName(getDataValue("forecast_code"))) 
    sendEventPublish(name: "condition_icon", value: '<img src=' + imgName + '>')
    sendEventPublish(name: "condition_iconWithText", value: "<img src=" + imgName + "><br>" + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))
    sendEventPublish(name: "condition_icon_url", value: imgName)
    updateDataValue("condition_icon_url", imgName)
    sendEventPublish(name: "condition_icon_only", value: imgName.split("/")[-1].replaceFirst("\\?raw=true",""))
// >>>>>>>>>> End Icon Processing <<<<<<<<<<    
    PostPoll()
}
// >>>>>>>>>> End DarkSky Poll Routines <<<<<<<<<<

// >>>>>>>>>> Begin Lux Processing <<<<<<<<<<    
void updateLux(boolean pollAgain=true) {
	LOGINFO("UpdateLux $pollAgain")
	if(pollAgain) {
		String curTime = new Date().format("HH:mm", TimeZone.getDefault())
		String newLight
		if(curTime < getDataValue("tw_begin") || curTime > getDataValue("tw_end")) {
			newLight =  "0"
		} else {
			newLight =  "1"
		}
		if(newLight != getDataValue("is_lightOld")) {
			pollDS()
			return
		}
	}
	def (lux, bwn) = estimateLux(getDataValue("condition_code"), getDataValue("cloud").toInteger())
	updateDataValue("bwn", bwn)
	updateDataValue("illuminance", !lux ? "0" : lux.toString())
	updateDataValue("illuminated", String.format("%,4d", !lux ? 0 : lux).toString())
	if(pollAgain) PostPoll()
}
// >>>>>>>>>> End Lux Processing <<<<<<<<<<
// <<<<<<<<<< Begin Icon and condition_code, condition_text processing >>>>>>>>>>
String getdsIconCode(String icon='unknown', String dcs='unknown') {
	switch(icon) {
		case 'rain':
		// rain=[Possible Light Rain, Light Rain, Rain, Heavy Rain, Drizzle, Light Rain and Breezy, Light Rain and Windy, 
		//       Rain and Breezy, Rain and Windy, Heavy Rain and Breezy, Rain and Dangerously Windy, Light Rain and Dangerously Windy],
			if (dcs == 'Drizzle') {
				icon = 'drizzle'
			} else if 	(dcs.startsWith('Light Rain')) { 
				icon = 'lightrain'
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			} else if 	(dcs.startsWith('Heavy Rain')) {
				icon = 'heavyrain'
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			} else if 	(dcs == 'Possible Light Rain') {
				icon = 'chancelightrain'
			} else if 	(dcs.startsWith('Possible')) {
				icon = 'chancerain'
			} else if 	(dcs.startsWith('Rain')) {
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			}
			break;
		case 'snow':
			if      (dcs == 'Light Snow') icon = 'lightsnow'
			else if (dcs == 'Flurries') icon = 'flurries'
			else if (dcs == 'Possible Light Snow') icon = 'chancelightsnow'
			else if (dcs.startsWith('Possible Light Snow')) {
				if      (dcs.contains('Breezy')) icon = 'chancelightsnowbreezy'
				else if (dcs.contains('Windy')) icon = 'chancelightsnowwindy'
			} else if (dcs.startsWith('Possible')) icon = 'chancesnow'
			break;
		case 'sleet':
			if (dcs.startsWith('Possible')) icon = 'chancesleet'
			else if (dcs.startsWith('Light')) icon = 'lightsleet'
			break;
		case 'thunderstorm':
			if (dcs.startsWith('Possible')) icon = 'chancetstorms'
			break;
		case 'partly-cloudy-night':
			if (dcs.contains('Mostly Cloudy')) icon = 'mostlycloudy'
			else icon = 'partlycloudy'
			break;
		case 'partly-cloudy-day':
			if (dcs.contains('Mostly Cloudy')) icon = 'mostlycloudy'
			else icon = 'partlycloudy'
			break;
		case 'cloudy-night':
			icon = 'cloudy'
			break;
		case 'cloudy':
		case 'cloudy-day':
			icon = 'cloudy'
			break;
		case 'clear-night':
			icon = 'clear'
			break;
		case 'clear':
		case 'clear-day':
			icon = 'clear'
			break;
        case 'fog':
		case 'wind':
			// wind=[Windy and Overcast, Windy and Mostly Cloudy, Windy and Partly Cloudy, Breezy and Mostly Cloudy, Breezy and Partly Cloudy, 
			// Breezy and Overcast, Breezy, Windy, Dangerously Windy and Overcast, Windy and Foggy, Dangerously Windy and Partly Cloudy, Breezy and Foggy]}
			if (dcs.contains('Windy')) {
				// icon = 'wind'
				if 		(dcs.contains('Overcast')) 	  icon = 'windovercast'
				else if (dcs.contains('Mostly Cloudy')) icon = 'windmostlycloudy'
				else if (dcs.contains('Partly Cloudy')) icon = 'windpartlycloudy'
				else if (dcs.contains('Foggy'))		  icon = 'windfoggy'
			} else if (dcs.contains('Breezy')) {
				icon = 'breezy'
				if 		(dcs.contains('Overcast')) 	  icon = 'breezyovercast'
				else if (dcs.summary.contains('Mostly Cloudy')) icon = 'breezymostlycloudy'
				else if (dcs.contains('Partly Cloudy')) icon = 'breezypartlycloudy'
				else if (dcs.contains('Foggy')) 		  icon = 'breezyfoggy'
			}
			break;
		case '':
			icon = 'unknown'
			break;
		default:
			icon = 'unknown'
	}
    boolean isNight = getDataValue("is_day")=="false"
	if(isNight) icon = 'nt_' + icon
    return icon
}
// >>>>>>>>>> End Icon and condition_code, condition_text processing <<<<<<<<<<

// <<<<<<<<<< Begin Post-Poll Routines >>>>>>>>>>
void PostPoll() {
    def sunRiseSet = parseJson(getDataValue("sunRiseSet")).results
    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)   
/*  SunriseSunset Data Eements */    
    if(localSunrisePublish){  // don't bother setting these values if it's not enabled
        sendEvent(name: "tw_begin", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "sunriseTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "noonTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "sunsetTime", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "tw_end", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end).format(timeFormat, TimeZone.getDefault()))    
    }
    if(dashSharpToolsPublish || dashSmartTilesPublish || localSunrisePublish) {
        sendEvent(name: "localSunset", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards
        sendEvent(name: "localSunrise", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards       
    }

/*  Weather-Display Data Elements */
	sendEvent(name: "humidity", value: getDataValue("humidity").toBigDecimal(), unit: '%')
    sendEvent(name: "illuminance", value: getDataValue("illuminance").toInteger(), unit: 'lx')
	sendEvent(name: "pressure", value: isPressureMetric ? String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) : String.format("%2.2f", getDataValue("pressure").toBigDecimal()), unit: (isPressureMetric ? 'mbar' : 'inHg'))
	sendEvent(name: "temperature", value: String.format("%3.1f", getDataValue("temperature").toBigDecimal()), unit: (isFahrenheit ? '°F' : '°C'))
    sendEvent(name: "ultravioletIndex", value: getDataValue("ultravioletIndex").toBigDecimal(), unit: 'uvi')

/*  'Required for Dashboards' Data Elements */    
    if(dashHubitatOWMPublish || dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: "city", value: getDataValue("city")) }
    if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: "feelsLike", value: getDataValue("feelsLike").toBigDecimal(), unit: (isFahrenheit ? '°F' : '°C')) }
    if(dashSharpToolsPublish) { sendEvent(name: "forecastIcon", value: getDataValue("condition_code")) }
    if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: "percentPrecip", value: getDataValue("percentPrecip")) }
    if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: "weather", value: getDataValue("condition_text")) }
    if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: "weatherIcon", value: getDataValue("condition_code")) }
    if(dashHubitatOWMPublish) { sendEvent(name: "weatherIcons", value: getowmImgName(getDataValue("condition_code"))) }
    if(dashSharpToolsPublish || windPublish) { sendEvent(name: "wind", value: getDataValue("wind"), unit: (isDistanceMetric ? (isDistanceKnots ? "knots" : 'KPH') : 'MPH')) }
    if(dashHubitatOWMPublish) { sendEvent(name: "windSpeed", value: getDataValue("wind").toBigDecimal(), unit: (isDistanceMetric ? (isDistanceKnots ? "knots" : 'KPH') : 'MPH')) }
    if(dashHubitatOWMPublish) { sendEvent(name: "windDirection", value: getDataValue("wind_degree").toInteger(), unit: "DEGREE")   }

/*  Selected optional Data Elements */   
    sendEventPublish(name: "alert", value: getDataValue("alert"))
    sendEventPublish(name: "betwixt", value: getDataValue("bwn"))
    sendEventPublish(name: "cloud", value: getDataValue("cloud").toInteger(), unit: '%')
    sendEventPublish(name: "condition_code", value: getDataValue("condition_code"))
    sendEventPublish(name: "condition_text", value: getDataValue("condition_text"))
    sendEventPublish(name: "dewpoint", value: getDataValue("dewpoint").toBigDecimal(), unit: (isFahrenheit ? '°F' : '°C'))
    if(dsAttributionPublish){
        sendEvent(name: "dsIconlighttext", value: '<a href=\"https://darksky.net/poweredby/\"><img src=' + getDataValue("iconLocation") + 'dsL.png' + ' style=\"height:2em\";></a>')
        sendEvent(name: "dsIcondarktext", value: '<a href=\"https://darksky.net/poweredby/\"><img src=' + getDataValue("iconLocation") + 'dsD.png' + ' style=\"height:2em\";></a>')
    }
    sendEventPublish(name: "forecast_code", value: getDataValue("forecast_code"))
    sendEventPublish(name: "forecast_text", value: getDataValue("forecast_text"))
    if(fcstHighLowPublish){ // don't bother setting these values if it's not enabled
        sendEvent(name: "forecastHigh", value: String.format("%3.1f", getDataValue("forecastHigh").toBigDecimal()), unit: (isFahrenheit ? '°F' : '°C'))
    	sendEvent(name: "forecastLow", value: String.format("%3.1f", getDataValue("forecastLow").toBigDecimal()), unit: (isFahrenheit ? '°F' : '°C'))
    }
    sendEventPublish(name: "illuminated", value: getDataValue("illuminated") + ' lx')
    sendEventPublish(name: "is_day", value: getDataValue("is_day"))
    sendEventPublish(name: "moonPhase", value: getDataValue("moonPhase"))
    if(obspollPublish){  // don't bother setting these values if it's not enabled
    	sendEvent(name: "last_poll_Forecast", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: "last_observation_Forecast", value: new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("fotime")).format(dateFormat, TimeZone.getDefault()) + ", " + new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("fotime")).format(timeFormat, TimeZone.getDefault()))
    }
    sendEventPublish(name: "ozone", value: Math.round(getDataValue("ozone").toBigDecimal() * 10) / 10)
    if(precipExtendedPublish){ // don't bother setting these values if it's not enabled
        sendEvent(name: "rainDayAfterTomorrow", value: getDataValue("rainDayAfterTomorrow").toBigDecimal(), unit: '%')	
    	sendEvent(name: "rainTomorrow", value: getDataValue("rainTomorrow").toBigDecimal(), unit: '%')
    }  
    sendEventPublish(name: "vis", value: Math.round(getDataValue("vis").toBigDecimal() * 10) / 10, unit: (isDistanceMetric ? "kilometers" : "miles"))
    sendEventPublish(name: "wind_degree", value: getDataValue("wind_degree").toInteger(), unit: "DEGREE")
    sendEventPublish(name: "wind_direction", value: getDataValue("wind_direction"))    
    sendEventPublish(name: "wind_cardinal", value: getDataValue("wind_cardinal"))
    sendEventPublish(name: "wind_gust", value: getDataValue("wind_gust").toBigDecimal(), unit: (isDistanceMetric ? (isDistanceKnots ? 'knots' : 'KPH') : 'MPH'))
    sendEventPublish(name: "wind_string", value: getDataValue("wind_string"))
    if(nearestStormPublish) {
        sendEvent(name: "nearestStormBearing", value: getDataValue("nearestStormBearing"), unit: "DEGREE")
        sendEvent(name: "nearestStormCardinal", value: getDataValue("nearestStormCardinal"))    
        sendEvent(name: "nearestStormDirection", value: getDataValue("nearestStormDirection"))    	
        sendEvent(name: "nearestStormDistance", value: String.format("%,5.1f", getDataValue("nearestStormDistance").toBigDecimal()), unit: (isDistanceMetric ? "kilometers" : "miles"))	
    }
	
//  <<<<<<<<<< Begin Built Weather Summary text >>>>>>>>>> 
    String Summary_last_poll_time = new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(timeFormat, TimeZone.getDefault())
    String Summary_last_poll_date = new Date().parse("EEE MMM dd HH:mm:ss z yyyy", getDataValue("futime")).format(dateFormat, TimeZone.getDefault())
    String mtprecip = getDataValue("percentPrecip") + '%'
    if(weatherSummaryPublish){ // don't bother setting these values if it's not enabled
		String Summary_forecastTemp = " with a high of " + String.format("%3.1f", getDataValue("forecastHigh").toBigDecimal()) + (isFahrenheit ? '°F' : '°C') + " and a low of " + String.format("%3.1f", getDataValue("forecastLow").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
		String Summary_precip = "There is a " + getDataValue("percentPrecip") + "% chance of precipitation. "
		String Summary_vis = "Visibility is around " + String.format("%3.1f", getDataValue("vis").toBigDecimal()) + (isDistanceMetric ? " kilometers." : " miles.")
        SummaryMessage(summaryType, Summary_last_poll_date, Summary_last_poll_time, Summary_forecastTemp, Summary_precip, Summary_vis)
    }
//  >>>>>>>>>> End Built Weather Summary text <<<<<<<<<<    
    
//  <<<<<<<<<< Begin Built mytext >>>>>>>>>> 
    if(myTilePublish){ // don't bother setting these values if it's not enabled
        boolean gitclose = (getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))
        String iconClose = (gitclose ? "?raw=true" : "")
        String iconCloseStyled = iconClose + ' style=\"display:inline;height:3em;\">'
        boolean noAlert = (!getDataValue("possAlert") || getDataValue("possAlert")=="" || getDataValue("possAlert")=="false")
        String alertStyleOpen = (noAlert ? '' :  '<span style=\"font-style:italic;\">')
        String alertStyleClose = (noAlert ? '<br>' : '</span><br>')
        String dsIcon = '<a href=\"https://darksky.net/poweredby/\"><img src=' + getDataValue("iconLocation") + (dsIconbackgrounddark ? 'dsD.png' : 'dsL.png') + ' style=\"height:2em;display:inline;\"></a>'
        String dsText = '<a href=\"https://darksky.net/poweredby/\">Powered by Dark Sky</a>'        
        BigDecimal wgust
        if(getDataValue("wind_gust").toBigDecimal() < 1.0 ) {
            wgust = 0.0g
        } else {
            wgust = getDataValue("wind_gust").toBigDecimal()
        }        
        String mytext = '<span>' + getDataValue("city") + '<br>'        
        mytext+= getDataValue("condition_text") + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue("alert")) + alertStyleClose
        mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + '<img src=' + getDataValue("condition_icon_url") + iconClose + ' style=\"height:2.2em;display:inline;\">'
        mytext+= ' Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C') + '<br></span>'
        mytext+= '<span style=\"font-size:.8em;\"><img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconCloseStyled + getDataValue("wind_direction") + " "
        mytext+= getDataValue("wind").toBigDecimal() < 1.0 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')
        mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  "@ " + wgust.toString() + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')) + '<br>'
        mytext+= '<img src=' + getDataValue("iconLocation") + 'wb.png' + iconCloseStyled + String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) + (isPressureMetric ? ' mbar' : ' inHg') + '     <img src=' + getDataValue("iconLocation") + 'wh.png' + iconCloseStyled
        mytext+= getDataValue("humidity") + '%     ' + '<img src=' + getDataValue("iconLocation") + 'wu.png' + iconCloseStyled + getDataValue("percentPrecip") + '%<br>'
        mytext+= '<img src=' + getDataValue("iconLocation") + 'wsr.png' + iconCloseStyled + getDataValue("localSunrise") + '     <img src=' + getDataValue("iconLocation") + 'wss.png' + iconCloseStyled
        mytext+= getDataValue("localSunset") + '     Updated: ' + Summary_last_poll_time
        if((mytext.length() + dsIcon.length() + 10) < 1025) {
            mytext+= '<br>' + dsIcon + '</span>'
        }else{
            if((mytext.length() + dsText.length() + 10) < 1025) {
                mytext+= '<br>' + dsText + '</span>'
            }else{
                mytext+= '<br>Powered by Dark Sky</span>'
            }
        }
        if(mytext.length() > 1024) {
            int iconfilepath = ('<img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconCloseStyled).length()
            int excess = (mytext.length() - 1024)
            int removeicons = 0
            int ics = iconfilepath + iconCloseStyled.length()
            if((excess - ics + 11) < 0) {
                removeicons = 1  //Remove sunset
            }else if((excess - (ics * 2) + 20) < 0) {
                removeicons = 2 //remove sunset and sunrise
            }else if((excess - (ics * 3) + 31) < 0) {
                removeicons = 3 //remove sunset, sunrise, PercentPrecip
            }else if((excess - (ics * 4) + 38) < 0) {
                removeicons = 4 //remove sunset, sunrise, PercentPrecip, Humidity
            }else if((excess - (ics * 5) + 42) < 0) {
                removeicons = 5 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure
            }else if((excess - (ics * 6) + 42) < 0) {
                removeicons = 6 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure, Wind
            }else if((excess - (ics * 7) + 42) < 0) {
                removeicons = 7 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure, Wind, condition
            }else{
                removeicons = 8 // still need to remove html formatting
            }
            if(removeicons < 8) {
                LOGDEBUG("myTile exceeds 1,024 characters (" + mytext.length() + ") ... removing last " + (removeicons + 1).toString() + " icons.")
                mytext = '<span>' + getDataValue("city") + '<br>'
                mytext+= getDataValue("condition_text") + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue("alert")) + alertStyleClose + '<br>'
                mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + (removeicons < 7 ? '<img src=' + getDataValue("condition_icon_url") + iconClose + ' style=\"height:2.0em;display:inline;\">' : '') 
                mytext+= ' Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C') + '<br></span>'
                mytext+= '<span style=\"font-size:.8em;\">' + (removeicons < (raintoday ? 7 : 6) ? '<img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconCloseStyled : '') + getDataValue("wind_direction") + " "
                mytext+= (removeicons < 6 ? '<img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconCloseStyled : '') + getDataValue("wind_direction") + " "
                mytext+= getDataValue("wind").toBigDecimal() < 1.0 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')
                mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  "@ " + wgust.toString() + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')) + '<br>'
                mytext+= (removeicons < 5 ? '<img src=' + getDataValue("iconLocation") + 'wb.png' + iconCloseStyled : 'Bar: ') + (isPressureMetric ? String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) : String.format("%2.2f", getDataValue("pressure").toBigDecimal())) + (isPressureMetric ? ' mbar' : ' inHg') + '  '
                mytext+= (removeicons < 4 ? '<img src=' + getDataValue("iconLocation") + 'wh.png' + iconCloseStyled : ' | Hum: ') + getDataValue("humidity") + '%  ' 
                mytext+= (removeicons < 3 ? '<img src=' + getDataValue("iconLocation") + 'wu.png' + iconCloseStyled : ' | Precip%: ') + getDataValue("percentPrecip") + '%<br>'
                mytext+= (removeicons < 2 ? '<img src=' + getDataValue("iconLocation") + 'wsr.png' + iconCloseStyled : 'Sunrise: ') + getDataValue("localSunrise") + '  '
                mytext+= (removeicons < 1 ? '<img src=' + getDataValue("iconLocation") + 'wss.png' + iconCloseStyled : ' | Sunset: ') + getDataValue("localSunset")
                mytext+= '     Updated ' + Summary_last_poll_time + '</span>'
            }else{
                LOGINFO("myTile still exceeds 1,024 characters (" + mytext.length() + ") ... removing all formatting.")
                mytext = getDataValue("city") + '<br>'
                mytext+= getDataValue("condition_text") + (noAlert ? '' : ' | ') + (noAlert ? '' : getDataValue("alert")) + '<br>'
                mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + 'Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C') + '<br>'
                mytext+= getDataValue("wind_direction") + " "
                mytext+= getDataValue("wind").toBigDecimal() < 1.0 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')
                mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  "@ " + wgust.toString() + (isDistanceMetric ? (isDistanceKnots ? " knots" : ' KPH') : ' MPH')) + '<br>'
                mytext+= 'Bar: ' + (isPressureMetric ? String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) : String.format("%2.2f", getDataValue("pressure").toBigDecimal())) + (isPressureMetric ? ' mbar' : ' inHg')
                mytext+= ' | Hum: ' + getDataValue("humidity") + '%  ' + ' | Precip%: ' + getDataValue("percentPrecip") + '%<br>'
                mytext+= 'Sunrise: ' + getDataValue("localSunrise") + ' | Sunset:' + getDataValue("localSunset") + ' |  Updated:' + Summary_last_poll_time
                if(mytext.length() > 1024) {
                    LOGINFO("myTile even still exceeds 1,024 characters (" + mytext.length() + ") ... truncating.")
                }
            }
        }
        LOGINFO("mytext: ${mytext}")
        sendEvent(name: "myTile", value: mytext.take(1024))
    }
//  >>>>>>>>>> End Built mytext <<<<<<<<<<
}
// >>>>>>>>>> End Post-Poll Routines <<<<<<<<<<

public void refresh() {
	pollDS()
}

void updated()   {
	unschedule()
	updateCheck()
	initialize()
	runEvery5Minutes(updateLux)
	Random rand = new Random(now())
	int ssseconds = rand.nextInt(60)
	schedule("${ssseconds} 20 0/8 ? * * *", pollSunRiseSet)
	runIn(5, pollDS)
	if(settingEnable) runIn(2100,settingsOff)// "roll up" (hide) the condition selectors after 35 min
	if(settings.logSet) runIn(1800,logsOff)
	int r_minutes = rand.nextInt(60)
	schedule("0 ${r_minutes} 8 ? * FRI *", updateCheck)
}
void initialize() {
    unschedule("pollDS")
    boolean logSet = (settings?.logSet ?: false)
    String city = (settings?.city ?: "")
    updateDataValue("city", city)
    String pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours")
    String pollIntervalForecastnight = (settings?.pollIntervalForecastnight ?: "3 Hours")
    boolean dsIconbackgrounddark = (settings?.dsIconbackgrounddark ?: true)    
    String datetimeFormat = (settings?.datetimeFormat ?: "1")
    String distanceFormat = (settings?.distanceFormat ?: "Miles (mph)")
    String pressureFormat = (settings?.pressureFormat ?: "Inches")
    String rainFormat = (settings?.rainFormat ?: "Inches")
    String tempFormat = (settings?.tempFormat ?: "Fahrenheit (°F)")
    boolean luxjitter = (settings?.luxjitter ?: false)
	boolean iconType = (settings?.iconType ?: false)
    updateDataValue("iconType", iconType ? 'true' : 'false')
    boolean summaryType = (settings?.summaryType ?: false)
    String iconLocation = (settings?.iconLocation ?: "https://tinyurl.com/y6xrbhpf/")
    updateDataValue("iconLocation", iconLocation)
    state.DarkSky = '<a href=\"https://darksky.net/poweredby/\"><img src=' + getDataValue("iconLocation") + 'dsD.png style=\"height:2em\";></a>'
    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    
    pollSunRiseSet()

	Random rand = new Random(now())
	int ssseconds = rand.nextInt(60)
	int minutes2 = rand.nextInt(2)
	int minutes5 = rand.nextInt(5)
	int minutes10 = rand.nextInt(10)
	int minutes15 = rand.nextInt(15)
	int minutes30 = rand.nextInt(30)
	int minutes60 = rand.nextInt(60)
	int hours3 = rand.nextInt(3)
	int dsseconds
	if(ssseconds < 56 ){
		dsseconds = ssseconds + 4
	}else{
		dsseconds = ssseconds - 60 + 4
	}
	if(getDataValue("is_light")=="1") {
		if(pollIntervalForecast == "Manual Poll Only"){
			LOGINFO("MANUAL FORECAST POLLING ONLY")
		} else {
			pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours").replace(" ", "")
			if(pollIntervalForecast=='2Minutes'){
				schedule("${dsseconds} ${minutes2}/2 * * * ? *", pollDS)
			}else if(pollIntervalForecast=='5Minutes'){
				schedule("${dsseconds} ${minutes5}/5 * * * ? *", pollDS)
			}else if(pollIntervalForecast=='10Minutes'){
				schedule("${dsseconds} ${minutes10}/10 * * * ? *", pollDS)
			}else if(pollIntervalForecast=='15Minutes'){
				schedule("${dsseconds} ${minutes15}/15 * * * ? *", pollDS)
			}else if(pollIntervalForecast=='30Minutes'){
				schedule("${dsseconds} ${minutes30}/30 * * * ? *", pollDS)
			}else if(pollIntervalForecast=='1Hour'){
				schedule("${dsseconds} ${minutes60} * * * ? *", pollDS)
			}else if(pollIntervalForecast=='3Hours'){
				schedule("${dsseconds} ${minutes60} ${hours3}/3 * * ? *", pollDS)
			}
		}
	}else{
		if(pollIntervalForecastnight == "Manual Poll Only"){
			LOGINFO("MANUAL FORECAST POLLING ONLY")
		} else {
			pollIntervalForecastnight = (settings?.pollIntervalForecastnight ?: "3 Hours").replace(" ", "")
			if(pollIntervalForecastnight=='2Minutes'){
				schedule("${dsseconds} ${minutes2}/2 * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='5Minutes'){
				schedule("${dsseconds} ${minutes5}/5 * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='10Minutes'){
				schedule("${dsseconds} ${minutes10}/10 * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='15Minutes'){
				schedule("${dsseconds} ${minutes15}/15 * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='30Minutes'){
				schedule("${dsseconds} ${minutes30}/30 * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='1Hour'){
				schedule("${dsseconds} ${minutes60} * * * ? *", pollDS)
			}else if(pollIntervalForecastnight=='3Hours'){
				schedule("${dsseconds} ${minutes60} ${hours3}/3 * * ? *", pollDS)
			}
		}
	}
	return
}

public void pollData() {
	pollDS()
    return
}
// ************************************************************************************************

public void setDateTimeFormats(String formatselector){
    switch(formatselector) {
        case "1": DTFormat = "M/d/yyyy h:mm a";   dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
        case "2": DTFormat = "M/d/yyyy HH:mm";    dateFormat = "M/d/yyyy";   timeFormat = "HH:mm";  break;
    	case "3": DTFormat = "MM/dd/yyyy h:mm a"; dateFormat = "MM/dd/yyyy"; timeFormat = "h:mm a"; break;
    	case "4": DTFormat = "MM/dd/yyyy HH:mm";  dateFormat = "MM/dd/yyyy"; timeFormat = "HH:mm";  break;
		case "5": DTFormat = "d/M/yyyy h:mm a";   dateFormat = "d/M/yyyy";   timeFormat = "h:mm a"; break;
    	case "6": DTFormat = "d/M/yyyy HH:mm";    dateFormat = "d/M/yyyy";   timeFormat = "HH:mm";  break;
    	case "7": DTFormat = "dd/MM/yyyy h:mm a"; dateFormat = "dd/MM/yyyy"; timeFormat = "h:mm a"; break;
        case "8": DTFormat = "dd/MM/yyyy HH:mm";  dateFormat = "dd/MM/yyyy"; timeFormat = "HH:mm";  break;
    	case "9": DTFormat = "yyyy/MM/dd HH:mm";  dateFormat = "yyyy/MM/dd"; timeFormat = "HH:mm";  break;
    	default: DTFormat = "M/d/yyyy h:mm a";  dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
	}
    return
}

public void setMeasurementMetrics(distFormat, pressFormat, precipFormat, temptFormat){
    isDistanceMetric = (distFormat == "Miles (mph)") ? false : true
    isDistanceKnots =  isDistanceMetric ? ((distFormat == "knots") ? true : false) : false
    isPressureMetric = (pressFormat == "Millibar") ? true : false
    isRainMetric = (precipFormat == "Millimetres") ? true : false
    isFahrenheit = (temptFormat == "Fahrenheit (°F)") ? true : false
    return
}

def estimateLux(String condition_code, int cloud)     {	
	long lux = 0l
	boolean aFCC = true
	double l
	String bwn
	def sunRiseSet            = parseJson(getDataValue("sunRiseSet")).results
	def tZ                    = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	String lT                 = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
	long localeMillis         = getEpoch(lT)
	long twilight_beginMillis = getEpoch(sunRiseSet.civil_twilight_begin)
	long sunriseTimeMillis    = getEpoch(sunRiseSet.sunrise)
	long noonTimeMillis       = getEpoch(sunRiseSet.solar_noon)
	long sunsetTimeMillis     = getEpoch(sunRiseSet.sunset)
	long twilight_endMillis   = getEpoch(sunRiseSet.civil_twilight_end)
	long twiStartNextMillis   = twilight_beginMillis + 86400000 // = 24*60*60*1000 --> one day in milliseconds
	long sunriseNextMillis    = sunriseTimeMillis + 86400000 
	long noonTimeNextMillis   = noonTimeMillis + 86400000 
	long sunsetNextMillis     = sunsetTimeMillis + 86400000
	long twiEndNextMillis     = twilight_endMillis + 86400000

	switch(localeMillis) { 
		case { it < twilight_beginMillis}: 
			bwn = "Fully Night Time" 
			lux = 5l
			break
		case { it < sunriseTimeMillis}:
			bwn = "between twilight and sunrise" 
			l = (((localeMillis - twilight_beginMillis) * 50f) / (sunriseTimeMillis - twilight_beginMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < noonTimeMillis}:
			bwn = "between sunrise and noon" 
			l = (((localeMillis - sunriseTimeMillis) * 10000f) / (noonTimeMillis - sunriseTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < sunsetTimeMillis}:
			bwn = "between noon and sunset" 
			l = (((sunsetTimeMillis - localeMillis) * 10000f) / (sunsetTimeMillis - noonTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < twilight_endMillis}:
			bwn = "between sunset and twilight" 
			l = (((twilight_endMillis - localeMillis) * 50f) / (twilight_endMillis - sunsetTimeMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < twiStartNextMillis}:
			bwn = "Fully Night Time" 
			lux = 5l
			break
		case { it < sunriseNextMillis}:
			bwn = "between twilight and sunrise" 
			l = (((localeMillis - twiStartNextMillis) * 50f) / (sunriseNextMillis - twiStartNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		case { it < noonTimeNextMillis}:
			bwn = "between sunrise and noon" 
			l = (((localeMillis - sunriseNextMillis) * 10000f) / (noonTimeNextMillis - sunriseNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < sunsetNextMillis}:
			bwn = "between noon and sunset" 
			l = (((sunsetNextMillis - localeMillis) * 10000f) / (sunsetNextMillis - noonTimeNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as long)
			break
		case { it < twiEndNextMillis}:
			bwn = "between sunset and twilight" 
			l = (((twiEndNextMillis - localeMillis) * 50f) / (twiEndNextMillis - sunsetNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as long)
			break
		default:
			bwn = "Fully Night Time" 
			lux = 5l
			aFCC = false
			break
	}
    String cC = condition_code
	String cCT = "not set"
	double cCF = (!cloud || cloud=="") ? 0.998d : ((100 - (cloud / 3d)) / 100)

    if(aFCC){
        if(cloud){
			LUitem = LUTable.find{ it.ccode == condition_code }            
			if (LUitem && (condition_code != "unknown"))    {
				cCF = (LUitem ? LUitem.luxpercent : 0)
				cCT = (LUitem ? LUitem.ctext : 'unknown') + ' using cloud cover.'
            } else    {
                cCF = 1.0
		        cCT = 'cloud not available now.'
			}
        } else {
		    cCF = 1.0
		    cCT = 'cloud not available now.'
        }
    }
	lux = (lux * cCF) as long
    if(luxjitter){
        // reduce event variability  code from @nh.schottfam
        if(lux > 1100) {
            long t0 = (lux/800)
            lux = t0 * 800
        } else if(lux <= 1100 && lux > 400) {
            long t0 = (lux/400)
            lux = t0 * 400
        } else {
            lux = 5
        }
    }
	LOGDEBUG("condition: $cC | condition factor: $cCF | condition text: $cCT| lux: $lux")
	return [lux, bwn]
}

private long getEpoch (String aTime) {
	def tZ = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	def localeTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", aTime, tZ)
	long localeMillis = localeTime.getTime()
	return (localeMillis)
}

void SummaryMessage(boolean SType, String Slast_poll_date, String Slast_poll_time, String SforecastTemp, String Sprecip, String Svis){   
    BigDecimal windgust
    if(getDataValue("wind_gust") == "" || getDataValue("wind_gust").toBigDecimal() < 1.0 || getDataValue("wind_gust")==null) {
        windgust = 0.00g
    } else {
        windgust = getDataValue("wind_gust").toBigDecimal()
    }
    String dsIcon = '<a href=\"https://darksky.net/poweredby/\"><img src=' + getDataValue("iconLocation") + (dsIconbackgrounddark ? 'dsD.png' : 'dsL.png') + ' style=\"height:1.5em;display:inline;\"></a>'    
    String wSum = (String)null
    if(SType == true){
        wSum = "Weather summary for " + getDataValue("city") + " updated at ${Slast_poll_time} on ${Slast_poll_date}. "
        wSum+= getDataValue("condition_text")
        wSum+= (!SforecastTemp || SforecastTemp=="") ? ". " : "${SforecastTemp}"
        wSum+= "Humidity is " + getDataValue("humidity") + "% and the temperature is " + String.format("%3.1f", getDataValue("temperature").toBigDecimal()) +  (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "The temperature feels like it is " + String.format("%3.1f", getDataValue("feelsLike").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "Wind: " + getDataValue("wind_string") + ", gusts: " + ((windgust < 1.00) ? "calm. " : "up to " + windgust.toString() + (isDistanceMetric ? (isDistanceKnots ? ' knots. ' : ' KPH. ') : ' MPH. '))
        wSum+= Sprecip
        wSum+= Svis
        wSum+= ((!getDataValue("alert") || getDataValue("alert")==null) ? "" : " " + getDataValue("alert") + '. ')
    } else {
        wSum = getDataValue("condition_text") + " "
        wSum+= ((!SforecastTemp || SforecastTemp=="") ? ". " : "${SforecastTemp}")
        wSum+= " Humidity: " + getDataValue("humidity") + "%. Temperature: " + String.format("%3.1f", getDataValue("temperature").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= getDataValue("wind_string") + ", gusts: " + ((windgust == 0.00) ? "calm. " : "up to " + windgust + (isDistanceMetric ? (isDistanceKnots ? ' knots. ' : ' KPH. ') : ' MPH. '))
	}
    wSum = wSum.take(1024)
    sendEvent(name: "weatherSummary", value: wSum)
	return
}

String getImgName(String wCode){
    LOGINFO("getImgName Input: wCode: " + wCode)
    LUitem = LUTable.find{ it.ccode == wCode }
	LOGINFO("getImgName Result: image url: " + getDataValue("iconLocation") + (LUitem ? LUitem.altIcon : 'na.png') + "?raw=true")
    return (getDataValue("iconLocation") + (LUitem ? LUitem.altIcon : 'na.png') + (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : ""))    
}
String getowmImgName(String wCode){
    LOGINFO("getImgName Input: wCode: " + wCode + " iconLocation: " + getDataValue("iconLocation"))
    LUitem = LUTable.find{ it.ccode == wCode }
    return (LUitem ? LUitem.owmIcon : '')   
}
String getcondText(String wCode){
    LOGINFO("getImgName Input: wCode: " + wCode)
    LUitem = LUTable.find{ it.ccode == wCode }
    return (LUitem ? LUitem.ctext : '')   
}

void logCheck(){
    if(settings?.logSet == true){
        log.info "DarkSky.net Weather Driver - INFO:  All Logging Enabled"
    } else {
        log.info "DarkSky.net Weather Driver - INFO:  Further Logging Disabled"
    }
    return
}

void LOGDEBUG(txt){
    if(settings?.logSet == true){ log.debug("DarkSky.net Weather Driver - DEBUG:  ${txt}") }
    return
}

void LOGINFO(txt){
    if(settings?.logSet == true){log.info("DarkSky.net Weather Driver - INFO:  ${txt}") }
    return
}

void logsOff(){
	log.warn "${device?.displayName} debug logging disabled..."
	device.updateSetting("logSet",[value:"false",type:"bool"])
}

void settingsOff(){
	log.warn "Settings disabled..."
	device.updateSetting("settingEnable",[value:"false",type:"bool"])
}

void sendEventPublish(evt)	{
// 	Purpose: Attribute sent to DB if selected	
    if (settings."${evt.name + "Publish"}") {
		sendEvent(name: evt.name, value: evt.value, descriptionText: evt.descriptionText, unit: evt.unit, displayed: evt.displayed);
		LOGDEBUG("$evt.name") //: $evt.name, $evt.value $evt.unit"
    }
}

@Field final List    LUTable =     [
[ ccode: 'breezy', altIcon: '23.png', ctext: 'Breezy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'breezyfoggy', altIcon: '48.png', ctext: 'Breezy and Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'breezymostlycloudy', altIcon: '51.png', ctext: 'Breezy and Mostly Cloudy', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'breezyovercast', altIcon: '49.png', ctext: 'Breezy and Overcast', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'breezypartlycloudy', altIcon: '53.png', ctext: 'Breezy and Partly Cloudy', owmIcon: '03d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'chancelightrain', altIcon: '39.png', ctext: 'Chance of Light Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'chancelightsnow', altIcon: '41.png', ctext: 'Possible Light Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'chancelightsnowbreezy', altIcon: '54.png', ctext: 'Possible Light Snow and Breezy', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'chancerain', altIcon: '39.png', ctext: 'Chance of Rain', owmIcon: '10d', stdIcon: 'chancerain', luxpercent: 0.7 ],
[ ccode: 'chancesleet', altIcon: '41.png', ctext: 'Chance of Sleet', owmIcon: '13d', stdIcon: 'chancesleet', luxpercent: 0.7 ],
[ ccode: 'chancesnow', altIcon: '41.png', ctext: 'Chance of Snow', owmIcon: '13d', stdIcon: 'chancesnow', luxpercent: 0.3 ],
[ ccode: 'chancetstorms', altIcon: '38.png', ctext: 'Chance of Thunderstorms', owmIcon: '11d', stdIcon: 'chancetstorms', luxpercent: 0.2 ],
[ ccode: 'chancelightsnowwindy', altIcon: '54.png', ctext: 'Possible Light Snow and Windy', owmIcon: '13d', stdIcon: 'chancesnow', luxpercent: 0.3 ],
[ ccode: 'clear', altIcon: '32.png', ctext: 'Clear', owmIcon: '01d', stdIcon: 'clear', luxpercent: 1 ],
[ ccode: 'cloudy', altIcon: '26.png', ctext: 'Overcast', owmIcon: '04d', stdIcon: 'cloudy', luxpercent: 0.6 ],
[ ccode: 'drizzle', altIcon: '9.png', ctext: 'Drizzle', owmIcon: '09d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'flurries', altIcon: '13.png', ctext: 'Snow Flurries', owmIcon: '13d', stdIcon: 'flurries', luxpercent: 0.4 ],
[ ccode: 'fog', altIcon: '19.png', ctext: 'Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'heavyrain', altIcon: '12.png', ctext: 'Heavy Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'heavyrainbreezy', altIcon: '1.png', ctext: 'Heavy Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'heavyrainwindy', altIcon: '1.png', ctext: 'Heavy Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrain', altIcon: '11.png', ctext: 'Light Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrainbreezy', altIcon: '2.png', ctext: 'Light Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrainwindy', altIcon: '2.png', ctext: 'Light Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightsleet', altIcon: '8.png', ctext: 'Light Sleet', owmIcon: '13d', stdIcon: 'sleet', luxpercent: 0.5 ],
[ ccode: 'lightsnow', altIcon: '14.png', ctext: 'Light Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'mostlycloudy', altIcon: '28.png', ctext: 'Mostly Cloudy', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'partlycloudy', altIcon: '30.png', ctext: 'Partly Cloudy', owmIcon: '03d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'rain', altIcon: '12.png', ctext: 'Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'rainbreezy', altIcon: '1.png', ctext: 'Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'rainwindy', altIcon: '1.png', ctext: 'Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'sleet', altIcon: '10.png', ctext: 'Sleet', owmIcon: '13d', stdIcon: 'sleet', luxpercent: 0.5 ],
[ ccode: 'snow', altIcon: '15.png', ctext: 'Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'sunny', altIcon: '36.png', ctext: 'Sunny', owmIcon: '01d', stdIcon: 'clear', luxpercent: 1 ],
[ ccode: 'thunderstorm', altIcon: '0.png', ctext: 'Thunderstorm', owmIcon: '11d', stdIcon: 'tstorms', luxpercent: 0.3 ],
[ ccode: 'wind', altIcon: '23.png', ctext: 'Windy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'windfoggy', altIcon: '23.png', ctext: 'Windy and Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'windmostlycloudy', altIcon: '51.png', ctext: 'Windy and Mostly Cloudy', owmIcon: '50d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'windovercast', altIcon: '49.png', ctext: 'Windy and Overcast', owmIcon: '50d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'windpartlycloudy', altIcon: '53.png', ctext: 'Windy and Partly Cloudy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'nt_breezy', altIcon: '23.png', ctext: 'Breezy', owmIcon: '50n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezyfoggy', altIcon: '48.png', ctext: 'Breezy and Foggy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_breezymostlycloudy', altIcon: '50.png', ctext: 'Breezy and Mostly Cloudy', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezyovercast', altIcon: '49.png', ctext: 'Breezy and Overcast', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezypartlycloudy', altIcon: '52.png', ctext: 'Breezy and Partly Cloudy', owmIcon: '03n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_chancelightrain', altIcon: '45.png', ctext: 'Chance of Light Rain', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnow', altIcon: '46.png', ctext: 'Possible Light Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnowbreezy', altIcon: '55.png', ctext: 'Possible Light Snow and Breezy', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_chancerain', altIcon: '39.png', ctext: 'Chance of Rain', owmIcon: '09n', stdIcon: 'nt_chancerain', luxpercent: 0 ],
[ ccode: 'nt_chancesleet', altIcon: '46.png', ctext: 'Chance of Sleet', owmIcon: '13n', stdIcon: 'nt_chancesleet', luxpercent: 0 ],
[ ccode: 'nt_chancesnow', altIcon: '46.png', ctext: 'Chance of Snow', owmIcon: '13n', stdIcon: 'nt_chancesnow', luxpercent: 0 ],
[ ccode: 'nt_chancetstorms', altIcon: '47.png', ctext: 'Chance of Thunderstorms', owmIcon: '11n', stdIcon: 'nt_chancetstorms', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnowwindy', altIcon: '55.png', ctext: 'Possible Light Snow and Windy', owmIcon: '13n', stdIcon: 'nt_chancesnow', luxpercent: 0 ],
[ ccode: 'nt_clear', altIcon: '31.png', ctext: 'Clear', owmIcon: '01n', stdIcon: 'nt_clear', luxpercent: 0 ],
[ ccode: 'nt_cloudy', altIcon: '26.png', ctext: 'Overcast', owmIcon: '04n', stdIcon: 'nt_cloudy', luxpercent: 0 ],
[ ccode: 'nt_drizzle', altIcon: '9.png', ctext: 'Drizzle', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_flurries', altIcon: '13.png', ctext: 'Flurries', owmIcon: '13n', stdIcon: 'nt_flurries', luxpercent: 0 ],
[ ccode: 'nt_fog', altIcon: '22.png', ctext: 'Foggy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_heavyrain', altIcon: '12.png', ctext: 'Heavy Rain', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_heavyrainbreezy', altIcon: '1.png', ctext: 'Heavy Rain and Breezy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_heavyrainwindy', altIcon: '1.png', ctext: 'Heavy Rain and Windy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrain', altIcon: '11.png', ctext: 'Light Rain', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrainbreezy', altIcon: '11.png', ctext: 'Light Rain and Breezy', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrainwindy', altIcon: '11.png', ctext: 'Light Rain and Windy', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightsleet', altIcon: '46.png', ctext: 'Sleet', owmIcon: '13n', stdIcon: 'nt_sleet', luxpercent: 0 ],
[ ccode: 'nt_lightsnow', altIcon: '14.png', ctext: 'Light Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_mostlycloudy', altIcon: '27.png', ctext: 'Mostly Cloudy', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_partlycloudy', altIcon: '29.png', ctext: 'Partly Cloudy', owmIcon: '03n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_rain', altIcon: '11.png', ctext: 'Rain', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_rainbreezy', altIcon: '2.png', ctext: 'Rain and Breezy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_rainwindy', altIcon: '2.png', ctext: 'Rain and Windy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_sleet', altIcon: '46.png', ctext: 'Sleet', owmIcon: '13n', stdIcon: 'nt_sleet', luxpercent: 0 ],
[ ccode: 'nt_snow', altIcon: '46.png', ctext: 'Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_thunderstorm', altIcon: '0.png', ctext: 'Thunderstorm', owmIcon: '11n', stdIcon: 'nt_clear', luxpercent: 0 ],
[ ccode: 'nt_wind', altIcon: '23.png', ctext: 'Windy', owmIcon: '50n', stdIcon: 'nt_tstorms', luxpercent: 0 ],
[ ccode: 'nt_windfoggy', altIcon: '48.png', ctext: 'Windy and Foggy', owmIcon: '50n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_windmostlycloudy', altIcon: '50.png', ctext: 'Windy and Mostly Cloudy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_windovercast', altIcon: '49.png', ctext: 'Windy and Overcast', owmIcon: '50n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_windpartlycloudy', altIcon: '52.png', ctext: 'Windy and Partly Cloudy', owmIcon: '50n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
]    

@Field static attributesMap = [
	"alert":				    [title: "Weather Alert", descr: "Display any weather alert?", typeof: "string", default: "false"],
    "betwixt":				    [title: "Slice of Day", descr: "Display the 'slice-of-day'?", typeof: "string", default: "false"],
	"cloud":			    	[title: "Cloud", descr: "Display cloud coverage %?", typeof: "number", default: "false"],
	"condition_code":			[title: "Condition Code", descr: "Display 'condition_code'?", typeof: "string", default: "false"],
	"condition_icon_only":		[title: "Condition Icon Only", descr: "Display 'condition_code_only'?", typeof: "string", default: "false"],
	"condition_icon_url":		[title: "Condition Icon URL", descr: "Display 'condition_code_url'?", typeof: "string", default: "false"],
	"condition_icon":			[title: "Condition Icon", descr: "Dislay 'condition_icon'?", typeof: "string", default: "false"],
    "condition_iconWithText":   [title: "Condition Icon With Text", descr: "Display 'condition_iconWithText'?", typeof: "string", default: "false"],    
	"condition_text":			[title: "Condition Text", descr: "Display 'condition_text'?", typeof: "string", default: "false"],
    "dashHubitatOWM":           [title: "Dash - Hubitat and OpenWeatherMap", descr: "Display attributes required by Hubitat and OpenWeatherMap dashboards?", typeof: false, default: "false"],
    "dashSmartTiles":           [title: "Dash - SmartTiles", descr: "Display attributes required by SmartTiles dashboards?", typeof: false, default: "false"],
    "dashSharpTools":           [title: "Dash - SharpTools.io", descr: "Display attributes required by SharpTools.io?", typeof: false, default: "false"],
    "dewpoint":                 [title: "Dewpoint (in default unit)", descr: "Display the dewpoint?", typeof: "number", default: "false"],
	"dsAttribution":    		[title: "Dark Sky Attribution", descr: "Display the 'Dark Sky attribution'?", typeof: false, default: "false"],    
    "fcstHighLow":              [title: "Forecast High/Low Temperatures:", descr: "Display forecast High/Low temperatures?", typeof: false, default: "false"],
	"forecast_code":		    [title: "Forecast Code", descr: "Display 'forecast_code'?", typeof: "string", default: "false"],
	"forecast_text":		    [title: "Forecast Text", descr: "Display 'forecast_text'?", typeof: "string", default: "false"],
	"illuminated":			    [title: "Illuminated", descr: "Display 'illuminated' (with 'lux' added for use on a Dashboard)?", typeof: "string", default: "false"],
	"is_day":				    [title: "Is daytime", descr: "Display 'is_day'?", typeof: "number", default: "false"],
	"localSunrise":			    [title: "Local SunRise and SunSet", descr: "Display the Group of 'Time of Local Sunrise and Sunset,' with and without Dashboard text?", typeof: false, default: "false"],
	"myTile":				    [title: "myTile for dashboard", descr: "Display 'mytile'?", typeof: "string", default: "false"],
	"moonPhase":			    [title: "Moon Phase", descr: "Display 'moonPhase'?", typeof: "string", default: "false"],    
	"nearestStorm":          	[title: "Nearest Storm Info", descr: "Display nearest storm data'?", typeof: false, default: "false"],
	"ozone":			    	[title: "Ozone", descr: "Display 'ozone'?", typeof: "number", default: "false"],    	
	"percentPrecip":			[title: "Percent Precipitation", descr: "Display the Chance of Rain, in percent?", typeof: "number", default: "false"],
	"precipExtended":			[title: "Precipitation Forecast", descr: "Display precipitation forecast?", typeof: false, default: "false"],
    "obspoll":			        [title: "Observation time", descr: "Display Observation and Poll times?", typeof: false, default: "false"], 
	"vis":				        [title: "Visibility (in default unit)", descr: "Display visibility distance?", typeof: "number", default: "false"],
    "weatherSummary":			[title: "Weather Summary Message", descr: "Display the Weather Summary?", typeof: "string", default: "false"],    
	"wind_cardinal":		    [title: "Wind Cardinal", descr: "Display the Wind Direction (text initials)?", typeof: "number", default: "false"],	
	"wind_degree":			    [title: "Wind Degree", descr: "Display the Wind Direction (number)?", typeof: "number", default: "false"],
	"wind_direction":			[title: "Wind direction", descr: "Display the Wind Direction (text words)?", typeof: "string", default: "false"],
	"wind_gust":				[title: "Wind gust (in default unit)", descr: "Display the Wind Gust?", typeof: "number", default: "false"],
	"wind_string":			    [title: "Wind string", descr: "Display the wind string?", typeof: "string", default: "false"],
]

// Check Version   ***** with great thanks and acknowledgment to Cobra (CobraVmax) for his original code ****
def updateCheck()
{    
	def paramsUD = [uri: "https://raw.githubusercontent.com/Scottma61/Hubitat/master/docs/version2.json"] //https://hubitatcommunity.github.io/???/version2.json"]
	
 	asynchttpGet("updateCheckHandler", paramsUD) 
}

void updateCheckHandler(resp, data) {

	state.InternalName = "DarkSky.net Weather Driver"
    boolean descTextEnable = settings?.logSet ?: false

	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		def respUD = parseJson(resp.data)
		// log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = respUD.copyright
		// uses reformattted 'version2.json' 
		String newVer = padVer(respUD.driver.(state.InternalName).ver)
		String currentVer = padVer(version())               
		state.UpdateInfo = (respUD.driver.(state.InternalName).updated)
            // log.debug "updateCheck: ${respUD.driver.(state.InternalName).ver}, $state.UpdateInfo, ${respUD.author}"

		switch(newVer) {
			case { it == "NLS"}:
                state.Status = "<b>** This Driver is no longer supported by ${respUD.author}  **</b>"       
                if (descTextEnable) log.warn "** This Driver is no longer supported by ${respUD.author} **"      
				break
			case { it > currentVer}:
			      state.Status = "<b>New Version Available (Version: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "** There is a newer version of this Driver available  (Version: ${respUD.driver.(state.InternalName).ver}) **"
			      if (descTextEnable) log.warn "** $state.UpdateInfo **"
				break
			case { it < currentVer}:
			      state.Status = "<b>You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})"
				break
			default:
				state.Status = "Current Version: ${respUD.driver.(state.InternalName).ver}"
				if (descTextEnable) log.info "You are using the current version of this driver"
				break
        }

    } else {
        log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI"
    }
}

/*
	padVer

	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each duple is padded first.

*/ 
String padVer(String ver) {
	String pad = ""
	ver.replaceAll( "[vV]", "" ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}

String getThisCopyright(){"&copy; 2019 Matthew (scottma61) "}
