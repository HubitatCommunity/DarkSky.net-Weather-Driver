/*
   DarkSky.net Weather Driver
   Import URL: https://raw.githubusercontent.com/Scottma61/Hubitat/master/DarkSky.net%20Weather%20Driver.groovy
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

   *** PLEASE NOTE: You should download and store these 'Alternative' icons on your own server and
   change the reference to that location in the driver. There is no assurance that those icon files will
   remain in my github repository.    ***
 
   The driver exposes both metric and imperial measurements for you to select from.
 
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
 
   Last Update 09/14/2019
  { Left room below to document version changes...}
 
 
 
 
 
 
 
   V1.0.6   Another optional attribute bug fix.                                               - 09/15/2019 
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
public static String version()      {  return "1.0.7"  }
import groovy.transform.Field

metadata {
    definition (name: "DarkSky.net Weather Driver", namespace: "Matthew", author: "Scottma61", importUrl: "https://raw.githubusercontent.com/Scottma61/Hubitat/master/DarkSky.net%20Weather%20Driver.groovy") {
        capability "Actuator"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
 		capability "Pressure Measurement"
 		capability "Ultraviolet Index"
	
		attributesMap.each
		{
//            k, v -> if (("${k}Publish") == true && v.typeof) {
            k, v -> if (v.typeof)        attribute "${k}" , "${v.typeof}"
		}
//    The following attributes may be needed for dashboards that require these attributes,
//    so they are listed here and shown by default.
        attribute "city", "string"              //SharpTool.io  SmartTiles
        attribute "feelsLike", "number"         //SharpTool.io  SmartTiles
        attribute "forecastIcon", "string"      //SharpTool.io
        attribute "localSunrise", "string"      //SharpTool.io  SmartTiles
        attribute "localSunset", "string"       //SharpTool.io  SmartTiles
        attribute "percentPrecip", "number"     //SharpTool.io  SmartTiles
        attribute "weather", "string"           //SharpTool.io  SmartTiles
        attribute "weatherIcon", "string"       //SharpTool.io  SmartTiles
        attribute "weatherIcons", "string"       //Hubitat  openWeather
        attribute "wind", "number"              //SharpTool.io
        attribute "windDirection", "number"     //Hubitat  OpenWeather
        attribute "windSpeed", "number"         //Hubitat  OpenWeather
        

        command "pollData"         
    }
    def settingDescr = settingEnable ? "<br><i>Hide many of the Preferences to reduce the clutter, if needed, by turning OFF this toggle.</i><br>" : "<br><i>Many Preferences are available to you, if needed, by turning ON this toggle.</i><br>"

    preferences() {
		section("Query Inputs"){
			input "city", "text", required: true, defaultValue: "City or Location name forecast area", title: "City name"
			input "apiKey", "text", required: true, defaultValue: "Type DarkSky.net API Key Here", title: "API Key"
			input "pollIntervalForecast", "enum", title: "External Source Poll Interval (daytime)", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "2 Minutes", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
            input "pollIntervalForecastnight", "enum", title: "External Source Poll Interval (nighttime)", required: true, defaultValue: "3 Hours", options: ["Manual Poll Only", "2 Minutes", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
			input "sourceImg", "bool", required: true, defaultValue: false, title: "Icons from: On = Standard - Off = Alternative"
			input "iconLocation", "text", required: true, defaultValue: "https://raw.githubusercontent.com/HubitatCommunity/DarkSky.net-Weather-Driver/tree/master/WeatherIcons/master/", title: "Alternative Icon Location:"
            input "iconType", "bool", title: "Condition Icon: On = Current - Off = Forecast", required: true, defaultValue: false
	    	input "tempFormat", "enum", required: true, defaultValue: "Fahrenheit (°F)", title: "Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)",  options: ["Fahrenheit (°F)", "Celsius (°C)"]
            input "datetimeFormat", "enum", required: true, defaultValue: "m/d/yyyy 12 hour (am|pm)", title: "Display Unit - Date-Time Format",  options: [1:"m/d/yyyy 12 hour (am|pm)", 2:"m/d/yyyy 24 hour", 3:"mm/dd/yyyy 12 hour (am|pm)", 4:"mm/dd/yyyy 24 hour", 5:"d/m/yyyy 12 hour (am|pm)", 6:"d/m/yyyy 24 hour", 7:"dd/mm/yyyy 12 hour (am|pm)", 8:"dd/mm/yyyy 24 hour", 9:"yyyy/mm/dd 24 hour"]
            input "distanceFormat", "enum", required: true, defaultValue: "Miles (mph)", title: "Display Unit - Distance/Speed: Miles or Kilometres",  options: ["Miles (mph)", "Kilometers (kph)"]
            input "pressureFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Pressure: Inches or Millibar",  options: ["Inches", "Millibar"]
            input "rainFormat", "enum", required: true, defaultValue: "Inches", title: "Display Unit - Precipitation: Inches or Millimetres",  options: ["Inches", "Millimetres"]
            input "summaryType", "bool", title: "Full Weather Summary", required: true, defaultValue: false
            input "logSet", "bool", title: "Create extended Logging", required: true, defaultValue: false
            input "settingEnable", "bool", title: "<b>Display All Preferences</b>", description: "$settingDescr", defaultValue: true
	// build a Selector for each mapped Attribute or group of attributes
	    	attributesMap.each
		    {
	    		keyname, attribute ->
	    		if (settingEnable) input "${keyname}Publish", "bool", title: "${attribute.title}", required: true, defaultValue: "${attribute.default}", description: "<br>${attribute.descr}<br>"
	    	}
        }
    }

    
}

// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
def pollSunRiseSet() {
    currDate = new Date().format("yyyy-MM-dd", TimeZone.getDefault())
    log.info("DarkSky.net Weather Driver - INFO: Polling Sunrise-Sunset.org")
    def requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=" + location.latitude + "&lng=" + location.longitude + "&formatted=0" ]
    if (currDate) {requestParams = [ uri: "https://api.sunrise-sunset.org/json?lat=" + location.latitude + "&lng=" + location.longitude + "&formatted=0&date=$currDate" ]}
    LOGINFO("Poll Sunrise-Sunset: $requestParams")
    asynchttpGet("sunRiseSetHandler", requestParams)
    return
}

def sunRiseSetHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		sunRiseSet = resp.getJson().results
		updateDataValue("sunRiseSet", resp.data)
        LOGINFO("Sunrise-Sunset Data: $sunRiseSet")
        setDateTimeFormats(datetimeFormat)
        updateDataValue("currDate", new Date().format("yyyy-MM-dd", TimeZone.getDefault()))
        updateDataValue("currTime", new Date().format("HH:mm", TimeZone.getDefault()))
		updateDataValue("riseTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format("HH:mm", TimeZone.getDefault()))     
        updateDataValue("noonTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.solar_noon).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("setTime", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_begin", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_begin).format("HH:mm", TimeZone.getDefault()))
        updateDataValue("tw_end", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.civil_twilight_end).format("HH:mm", TimeZone.getDefault()))
		updateDataValue("localSunset",new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
		updateDataValue("localSunrise", new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
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
    } else {
		log.warn "DarkSky.net Weather Driver - Sunrise-Sunset api did not return data"
	}
    return
}
// >>>>>>>>>> End Sunrise-Sunset Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin DarkSky Poll Routines >>>>>>>>>>
def pollDS() {
	def ParamsDS = [ uri: "https://api.darksky.net/forecast/${apiKey}/" + location.latitude + ',' + location.longitude + "?units=us&exclude=minutely,hourly,flags" ]
    LOGINFO("Poll DarkSky: $ParamsDS")
	asynchttpGet("pollDSHandler", ParamsDS)
    return
}

def pollDSHandler(resp, data) {
    log.info "DarkSky.net Weather Driver - INFO: Polling DarkSky.net"
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
        ds = parseJson(resp.data)
        LOGINFO("DarkSky Data: $ds")
		doPollDS()		// parse the data returned by DarkSky
	} else {
		log.error "DarkSky.net Weather Driver - DarkSky weather api did not return data"
	}
}

def doPollDS() {
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
        initialize()
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
    if(ds.currently.windSpeed.toBigDecimal() < 1.0) {
        w_string_bft = "Calm"; w_bft_icon = 'wb0.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 4.0) {
        w_string_bft = "Light air"; w_bft_icon = 'wb1.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 8.0) {
        w_string_bft = "Light breeze"; w_bft_icon = 'wb2.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 13.0) {
        w_string_bft = "Gentle breeze"; w_bft_icon = 'wb3.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 19.0) {
        w_string_bft = "Moderate breeze"; w_bft_icon = 'wb4.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 25.0) {
        w_string_bft = "Fresh breeze"; w_bft_icon = 'wb5.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 32.0) {
        w_string_bft = "Strong breeze"; w_bft_icon = 'wb6.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 39.0) {
        w_string_bft = "High wind, moderate gale, near gale"; w_bft_icon = 'wb7.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 47.0) {
        w_string_bft = "Gale, fresh gale"; w_bft_icon = 'wb8.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 55.0) {
        w_string_bft = "Strong/severe gale"; w_bft_icon = 'wb9.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 64.0) {
        w_string_bft = "Storm, whole gale"; w_bft_icon = 'wb10.png'
    }else if(ds.currently.windSpeed.toBigDecimal() < 73.0) {
        w_string_bft = "Violent storm"; w_bft_icon = 'wb11.png'
    }else if(ds.currently.windSpeed.toBigDecimal() >= 73.0) {
        w_string_bft = "Hurricane force"; w_bft_icon = 'wb12.png'
    }
	updateDataValue("wind_string_bft", w_string_bft)
    updateDataValue("wind_bft_icon", w_bft_icon)
    updateDataValue("wind", (isDistanceMetric ? (Math.round(ds.currently.windSpeed.toBigDecimal() * 1.609344 * 10) / 10) : (Math.round(ds.currently.windSpeed.toBigDecimal() * 10) / 10)).toString())
    updateDataValue("wind_gust", (isDistanceMetric ? (Math.round(ds.currently.windGust.toBigDecimal() * 1.609344 * 10) / 10) : (Math.round(ds.currently.windGust.toBigDecimal() * 10) / 10)).toString())
    updateDataValue("wind_degree", ds.currently.windBearing.toInteger().toString())	
    if(ds.currently.windBearing.toBigDecimal() < 11.25) {
        w_cardinal = 'N'; w_direction = 'North'
    }else if(ds.currently.windBearing.toBigDecimal() < 33.75) {
        w_cardinal = 'NNE'; w_direction = 'North-Northeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 56.25) {
        w_cardinal = 'NE';  w_direction = 'Northeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 56.25) {
        w_cardinal = 'ENE'; w_direction = 'East-Northeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 101.25) {
        w_cardinal = 'E'; w_direction = 'East'
    }else if(ds.currently.windBearing.toBigDecimal() < 123.75) {
        w_cardinal = 'ESE'; w_direction = 'East-Southeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 146.25) {
        w_cardinal = 'SE'; w_direction = 'Southeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 168.75) {
        w_cardinal = 'SSE'; w_direction = 'South-Southeast'
    }else if(ds.currently.windBearing.toBigDecimal() < 191.25) {
        w_cardinal = 'S'; w_direction = 'South'
    }else if(ds.currently.windBearing.toBigDecimal() < 213.75) {
        w_cardinal = 'SSW'; w_direction = 'South-Southwest'
    }else if(ds.currently.windBearing.toBigDecimal() < 236.25) {
        w_cardinal = 'SW'; w_direction = 'Southwest'
    }else if(ds.currently.windBearing.toBigDecimal() < 258.75) {
        w_cardinal = 'WSW'; w_direction = 'West-Southwest'
    }else if(ds.currently.windBearing.toBigDecimal() < 281.25) {
        w_cardinal = 'W'; w_direction = 'West'
    }else if(ds.currently.windBearing.toBigDecimal() < 303.75) {
        w_cardinal = 'WNW'; w_direction = 'West-Northwest'
    }else if(ds.currently.windBearing.toBigDecimal() < 326.25) {
        w_cardinal = 'NW'; w_direction = 'Northwest'
    }else if(ds.currently.windBearing.toBigDecimal() < 348.75) {
        w_cardinal = 'NNW'; w_direction = 'North-Northwest'
    }else if(ds.currently.windBearing.toBigDecimal() >= 348.75) {
        w_cardinal = 'N'; w_direction = 'North'
    }
    updateDataValue("wind_direction", w_direction)
    updateDataValue("wind_cardinal", w_cardinal)	
    updateDataValue("wind_string", w_string_bft + " from the " + getDataValue("wind_direction") + (getDataValue("wind").toBigDecimal() < 1.0 ? '': " at " + getDataValue("wind") + (isDistanceMetric ? " KPH" : " MPH")))
    if(nearestStormPublish) {
        if(!ds.currently.nearestStormBearing){
            updateDataValue("nearestStormBearing", "360")
            s_cardinal = 'U'
            s_direction = 'Unknown'        
        }else{
            updateDataValue("nearestStormBearing", (Math.round(ds.currently.nearestStormBearing * 10) / 10).toString())
            if(ds.currently.nearestStormBearing.toInteger() < 11.25) {
                s_cardinal = 'N'; s_direction = 'North'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 33.75) {
                s_cardinal = 'NNE'; s_direction = 'North-Northeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 56.25) {
                s_cardinal = 'NE';  s_direction = 'Northeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 78.75) {
                s_cardinal = 'ENE'; s_direction = 'East-Northeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 101.25) {
                s_cardinal = 'E'; s_direction = 'East'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 123.75) {
                s_cardinal = 'ESE'; s_direction = 'East-Southeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 146.25) {
                s_cardinal = 'SE'; s_direction = 'Southeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 168.75) {
                s_cardinal = 'SSE'; s_direction = 'South-Southeast'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 191.25) {
                s_cardinal = 'S'; s_direction = 'South'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 213.75) {
                s_cardinal = 'SSW'; s_direction = 'South-Southwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 236.25) {
                s_cardinal = 'SW'; s_direction = 'Southwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 258.75) {
                s_cardinal = 'WSW'; s_direction = 'West-Southwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 281.25) {
                s_cardinal = 'W'; s_direction = 'West'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 303.75) {
                s_cardinal = 'WNW'; s_direction = 'West-Northwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 326.26) {
                s_cardinal = 'NW'; s_direction = 'Northwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() < 348.75) {
                s_cardinal = 'NNW'; s_direction = 'North-Northwest'
            }else if(ds.currently.nearestStormBearing.toBigDecimal() >= 348.75) {
                s_cardinal = 'N'; s_direction = 'North'
            }
        }    
        updateDataValue("nearestStormCardinal", s_cardinal)
        updateDataValue("nearestStormDirection", s_direction)
        updateDataValue("nearestStormDistance", (!ds.currently.nearestStormDistance ? "9999.9" : (isDistanceMetric ? (Math.round(ds.currently.nearestStormDistance.toBigDecimal() * 1.609344 * 10) / 10) : (Math.round(ds.currently.nearestStormDistance.toBigDecimal() * 10) / 10)).toString()))
    }
	updateDataValue("ozone", (Math.round(ds.currently.ozone.toBigDecimal() * 10 ) / 10).toString())

    if(moonPhasePublish){
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 6.25) {mPhase = "New Moon"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 18.75) {mPhase = "Waxing Crescent"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 31.25) {mPhase = "First Quarter"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 43.75) {mPhase = "Waxing Gibbous"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 56.25) {mPhase = "Full Moon"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 68.75) {mPhase = "Waning Gibbous"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 81.25) {mPhase = "Last Quarter"}
        if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 < 93.75) {mPhase = "Waxing Gibbous"}
		if (ds.daily.data[0].moonPhase.toBigDecimal() * 100 >= 93.75) {mPhase = "New Moon"} 
        updateDataValue("moonPhase", mPhase)
    }
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<	
	
    if (!ds.currently.cloudCover) {
        cloudCover = 1
    } else {
        cloudCover = (ds.currently.cloudCover.toBigDecimal() <= 0.01) ? 1 : ds.currently.cloudCover.toBigDecimal() * 100
    }
    updateDataValue("cloud", cloudCover.toInteger().toString())
    if (!ds.alerts){
        updateDataValue("alert", "No current weather alerts for this area")
        updateDataValue("possAlert", "false")
    } else {
        updateDataValue("alert", ds.alerts.title.toString().replaceAll("[{}\\[\\]]", "").split(/,/)[0])
        updateDataValue("possAlert", "true")
    }
    updateDataValue("vis", (isDistanceMetric ? ds.currently.visibility.toBigDecimal() * 1.60934 : ds.currently.visibility.toBigDecimal()).toString())
    updateDataValue("percentPrecip", !ds.daily.data[0].precipProbability ? "1" : (ds.daily.data[0].precipProbability.toBigDecimal() * 100).toInteger().toString())
    switch(ds.currently.icon) {
        case "clear-day": c_code = "sunny"; break;
        case "clear-night": c_code = "nt_clear"; break;
        case "rain": c_code = (getDataValue("is_day")=="1" ? "rain" : "nt_rain"); break;
        case "wind": c_code = (getDataValue("is_day")=="1" ? "breezy" : "nt_breezy"); break;
        case "snow": c_code = (getDataValue("is_day")=="1" ? "snow" : "nt_snow"); break;
        case "sleet": c_code = (getDataValue("is_day")=="1" ? "sleet" : "nt_sleet"); break;
        case "fog": c_code = (getDataValue("is_day")=="1" ? "fog" : "nt_fog"); break;
        case "cloudy": c_code = (getDataValue("is_day")=="1" ? "cloudy" : "nt_cloudy"); break;
        case "partly-cloudy-day": c_code = "partlycloudy"; break;
        case "partly-cloudy-night": c_code = "nt_partlycloudy"; break;
        default: c_code = "unknown"; break;
    }
    updateDataValue("condition_code", c_code)
    updateDataValue("condition_text", ds.currently.summary)
    switch(ds.daily.data[0].icon){
        case "clear-day": f_code =  "sunny"; break;
        case "clear-night": f_code =  "nt_clear"; break;
        case "rain": f_code = (getDataValue("is_day")=="1" ? "rain" : "nt_rain"); break;
        case "wind": f_code = (getDataValue("is_day")=="1" ? "breezy" : "nt_breezy"); break;
        case "snow": f_code = (getDataValue("is_day")=="1" ? "snow" : "nt_snow"); break;
        case "sleet": f_code = (getDataValue("is_day")=="1" ? "sleet" : "nt_sleet"); break;
        case "fog": f_code = (getDataValue("is_day")=="1" ? "fog" : "nt_fog"); break;
        case "cloudy": f_code = (getDataValue("is_day")=="1" ? "cloudy" : "nt_cloudy"); break;
        case "partly-cloudy-day": f_code = "partlycloudy"; break;
        case "partly-cloudy-night": f_code = "nt_partlycloudy"; break;
        default: f_code = "unknown"; break;
    }
    updateDataValue("forecast_code", f_code)
    updateDataValue("forecast_text", ds.daily.data[0].summary)
    updateDataValue("forecastHigh", (isFahrenheit ? (Math.round(ds.daily.data[0].temperatureHigh.toBigDecimal() * 10) / 10) : (Math.round((ds.daily.data[0].temperatureHigh.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    updateDataValue("forecastLow", (isFahrenheit ? (Math.round(ds.daily.data[0].temperatureLow.toBigDecimal() * 10) / 10) : (Math.round((ds.daily.data[0].temperatureLow.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
    if(precipExtendedPublish){
        updateDataValue("rainTomorrow", (ds.daily.data[1].precipProbability.toBigDecimal() * 100).toInteger().toString())
        updateDataValue("rainDayAfterTomorrow", (ds.daily.data[2].precipProbability.toBigDecimal() * 100).toInteger().toString())
    }
	def (lux, bwn) = estimateLux(getDataValue("condition_code"), getDataValue("cloud"))
	updateDataValue("bwn", bwn)
	updateDataValue("illuminance", lux.toString())
	updateDataValue("illuminated", String.format("%,4d", lux).toString())
	updateDataValue("ultravioletIndex", ds.currently.uvIndex.toBigDecimal().toString())
	updateDataValue("feelsLike", (isFahrenheit ? (Math.round(ds.currently.apparentTemperature.toBigDecimal() * 10) / 10) : (Math.round((ds.currently.apparentTemperature.toBigDecimal() - 32) / 1.8 * 10) / 10)).toString())
// >>>>>>>>>> End Setup Forecast Variables <<<<<<<<<<

	// <<<<<<<<<< Begin Icon Processing  >>>>>>>>>>    
    if(sourceImg==false){ // 'Alternative' Icons selected
        imgName = (getDataValue("iconType")== 'true' ? getImgName(getDataValue("condition_code")) : getImgName(getDataValue("forecast_code"))) 
        sendEventPublish(name: "condition_icon", value: '<img src=' + imgName + '>')
        sendEventPublish(name: "condition_iconWithText", value: "<img src=" + imgName + "><br>" + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))
        sendEventPublish(name: "condition_icon_url", value: imgName)
        updateDataValue("condition_icon_url", imgName)
        sendEventPublish(name: "condition_icon_only", value: imgName.split("/")[-1].replaceFirst("\\?raw=true",""))
    } else if(sourceImg==true) { // 'Standard Icons selected
        sendEventPublish(name: "condition_icon", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif>')
        sendEventPublish(name: "condition_iconWithText", value: '<img src=https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) + '.gif><br>' + (getDataValue("iconType")== 'true' ? getDataValue("condition_text") : getDataValue("forecast_text")))
        sendEventPublish(name: "condition_icon_url", value: 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')
        updateDataValue("condition_icon_url", 'https://icons.wxug.com/i/c/a/' + (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')
        sendEventPublish(name: "condition_icon_only", value: (getDataValue("iconType")== 'true' ? getDataValue("condition_code") : getDataValue("forecast_code")) +'.gif')
    }
// >>>>>>>>>> End Icon Processing <<<<<<<<<<    
    if(getDataValue("forecastPoll") == "false"){
        updateDataValue("forecastPoll", "true")
    }
    PostPoll()
}
// >>>>>>>>>> End DarkSky Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin Post-Poll Routines >>>>>>>>>>
def PostPoll() {
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
    sendEvent(name: "localSunset", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards
    sendEvent(name: "localSunrise", value: new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards       
/*  Weather-Display & 'Required for Dashboards' Data Elements */
	sendEvent(name: "humidity", value: getDataValue("humidity").toBigDecimal(), unit: '%')
    sendEvent(name: "illuminance", value: getDataValue("illuminance").toInteger(), unit: 'lx')
	sendEvent(name: "pressure", value: isPressureMetric ? String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) : String.format("%2.2f", getDataValue("pressure").toBigDecimal()), unit: (isPressureMetric ? 'mbar' : 'inHg'))
	sendEvent(name: "temperature", value: String.format("%3.1f", getDataValue("temperature").toBigDecimal()), unit: (isFahrenheit ? '°F' : '°C'))
    sendEvent(name: "ultravioletIndex", value: getDataValue("ultravioletIndex").toBigDecimal(), unit: 'uvi')
    sendEvent(name: "city", value: getDataValue("city"))
    sendEvent(name: "feelsLike", value: getDataValue("feelsLike").toBigDecimal(), unit: (isFahrenheit ? '°F' : '°C'))
    sendEvent(name: "forecastIcon", value: getDataValue("condition_text"))
    sendEvent(name: "percentPrecip", value: getDataValue("percentPrecip"))
    sendEvent(name: "weather", value: getDataValue("condition_code"))
    sendEvent(name: "weatherIcon", value: getDataValue("condition_code"))
    sendEvent(name: "weatherIcons", value: getowmImgName(getDataValue("condition_code")))
    sendEvent(name: "wind", value: getDataValue("wind"), unit: (isDistanceMetric ? 'KPH' : 'MPH'))
    sendEvent(name: "windSpeed", value: getDataValue("wind").toBigDecimal(), unit: (isDistanceMetric ? 'KPH' : 'MPH'))
    sendEvent(name: "windDirection", value: getDataValue("wind_degree").toInteger(), unit: "DEGREE")  

/*  Selected optional Data Elements */   
    sendEventPublish(name: "alert", value: getDataValue("alert"))
    sendEventPublish(name: "betwixt", value: getDataValue("bwn"))
    sendEventPublish(name: "cloud", value: getDataValue("cloud").toInteger(), unit: '%')
    sendEventPublish(name: "condition_code", value: getDataValue("condition_code"))
    sendEventPublish(name: "condition_text", value: getDataValue("condition_text"))
    sendEventPublish(name: "dewpoint", value: getDataValue("dewpoint").toBigDecimal(), unit: (isFahrenheit ? '°F' : '°C'))
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
    sendEventPublish(name: "wind_gust", value: getDataValue("wind_gust").toBigDecimal(), unit: (isDistanceMetric ? 'KPH' : 'MPH'))
    sendEventPublish(name: "wind_string", value: getDataValue("wind_string"))
    if(nearestStormPublish) {
        sendEvent(name: "nearestStormBearing", value: getDataValue("nearestStormBearing"), unit: "DEGREE")
        sendEvent(name: "nearestStormCardinal", value: getDataValue("nearestStormCardinal"))    
        sendEvent(name: "nearestStormDirection", value: getDataValue("nearestStormDirection"))    	
        sendEvent(name: "nearestStormDistance", value: String.format("%,5.1f", getDataValue("nearestStormDistance").toBigDecimal()), unit: (isDistanceMetric ? "kilometers" : "miles"))	
    }
	
//  <<<<<<<<<< Begin Built Weather Summary text >>>>>>>>>> 
    Summary_last_poll_time = new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${futime}").format(timeFormat, TimeZone.getDefault())
    Summary_last_poll_date = new Date().parse("EEE MMM dd HH:mm:ss z yyyy", "${futime}").format(dateFormat, TimeZone.getDefault())
    mtprecip = getDataValue("percentPrecip") + '%'
    if(weatherSummaryPublish){ // don't bother setting these values if it's not enabled
		Summary_forecastTemp = " with a high of " + String.format("%3.1f", getDataValue("forecastHigh").toBigDecimal()) + (isFahrenheit ? '°F' : '°C') + " and a low of " + String.format("%3.1f", getDataValue("forecastLow").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
		Summary_precip = "There is a " + getDataValue("percentPrecip") + "% chance of precipitation. "
		Summary_vis = "Visibility is around " + String.format("%3.1f", getDataValue("vis").toBigDecimal()) + (isDistanceMetric ? " kilometers." : " miles. ")
        SummaryMessage(summaryType, Summary_last_poll_date, Summary_last_poll_time, Summary_forecastTemp, Summary_precip, Summary_vis)
    }
//  >>>>>>>>>> End Built Weather Summary text <<<<<<<<<<    
    
//  <<<<<<<<<< Begin Built mytext >>>>>>>>>> 
    if(myTilePublish){ // don't bother setting these values if it's not enabled
    	iconClose = (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : "")
        alertStyleOpen = ((!getDataValue("possAlert") || getDataValue("possAlert")=="" || getDataValue("possAlert")=="false")  ?  '' : '<span style=\"font-size:0.75em;line-height=75%;\">')
        alertStyleClose = ((!getDataValue("possAlert") || getDataValue("possAlert")=="" || getDataValue("possAlert")=="false") ? '' : ' | <span style=\"font-style:italic;\">' + getDataValue("alert") + "</span></span>" )
        if(getDataValue("wind_gust").toBigDecimal() < 1.0 ) {
            wgust = 0.0g
        } else {
            wgust = getDataValue("wind_gust").toBigDecimal()
        }
        mytext = '<div style=\"text-align:center;display:inline;margin-top:0em;margin-bottom:0em;\">' + getDataValue("city") + '</div><br>'
        mytext+= alertStyleOpen + getDataValue("condition_text") + alertStyleClose + '<br>'
        mytext+= getDataValue("temperature") + (isFahrenheit ? '°F ' : '°C ') + '<img style=\"height:2.0em\" src=' + getDataValue("condition_icon_url") + '>' + '<span style= \"font-size:.75em;\"> Feels like ' + getDataValue("feelsLike") + (isFahrenheit ? '°F' : '°C') + '</span><br>'
        mytext+= '<div style=\"font-size:0.75em;line-height=50%;\">' + '<img src=' + getDataValue("iconLocation") + getDataValue("wind_bft_icon") + iconClose + '>' + getDataValue("wind_direction") + " "
        mytext+= getDataValue("wind").toBigDecimal() < 1.0 ? 'calm' : "@ " + getDataValue("wind") + (isDistanceMetric ? ' KPH' : ' MPH')
        mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  "@ " + wgust.toString() + (isDistanceMetric ? ' KPH' : ' MPH')) + '<br>'
        mytext+= '<img src=' + getDataValue("iconLocation") + 'wb.png' + iconClose + '>' + (isPressureMetric ? String.format("%,4.1f", getDataValue("pressure").toBigDecimal()) : String.format("%2.2f", getDataValue("pressure").toBigDecimal())) + (isPressureMetric ? ' mbar' : ' inHg') + '  <img src=' + getDataValue("iconLocation") + 'wh.png' + iconClose + '>'
        mytext+= getDataValue("humidity") + '%  ' + '<img src=' + getDataValue("iconLocation") + 'wu.png' + iconClose + '>' + getDataValue("percentPrecip") + '%<br>'
        mytext+= '<img src=' + getDataValue("iconLocation") + 'wsr.png' + iconClose + '>' + getDataValue("localSunrise") + '     <img src=' + getDataValue("iconLocation") + 'wss.png' + iconClose + '>' + getDataValue("localSunset") + '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Updated:&nbsp;' + Summary_last_poll_time + '</div>'
        LOGINFO("mytext: ${mytext}")
        sendEvent(name: "myTile", value: mytext)
    }
//  >>>>>>>>>> End Built mytext <<<<<<<<<<
}
// >>>>>>>>>> End Post-Poll Routines <<<<<<<<<<

def updated()   {
	initialize()  // includes an unsubscribe()
    updateDataValue("forecastPoll", "false")
    if (settingEnable) runIn(2100,settingsOff)  // "roll up" (hide) the condition selectors after 35 min
    runIn(5, pollDS)
    updateCheck()
    schedule("0 0 8 ? * FRI *", updateCheck)

}
def initialize() {
    state.clear()
    unschedule()
    logSet = (settings?.logSet ?: false)
    city = (settings?.city ?: "")
    updateDataValue("city", !city ? "" : city)
    pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours")
    pollIntervalForecastnight = (settings?.pollIntervalForecastnight ?: "3 Hours")    
    datetimeFormat = (settings?.datetimeFormat ?: 1).toInteger()
    distanceFormat = (settings?.distanceFormat ?: "Miles (mph)")
    pressureFormat = (settings?.pressureFormat ?: "Inches")
    rainFormat = (settings?.rainFormat ?: "Inches")
    tempFormat = (settings?.tempFormat ?: "Fahrenheit (°F)")
	iconType = (settings?.iconType ?: false)
    updateDataValue("iconType", iconType ? 'true' : 'false')
    sourceImg = (settings?.sourceImg ?: false)
    summaryType = (settings?.summaryType ?: false)
    iconLocation = (settings?.iconLocation ?: "https://raw.githubusercontent.com/HubitatCommunity/DarkSky.net-Weather-Driver/tree/master/WeatherIcons/master/")
    updateDataValue("iconLocation", iconLocation)

    setDateTimeFormats(datetimeFormat)
    setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
    pollSunRiseSet()
    Random rand = new Random(now())
    ssseconds = rand.nextInt(60)
    if(ssseconds < 56 ){
        dsseconds = ssseconds + 4
    }else{
        dsseconds = ssseconds - 60 + 4
    }   
    schedule("${ssseconds} 20 0/8 ? * * *", pollSunRiseSet)
	if(getDataValue("is_light")=="1") {
		if(pollIntervalForecast == "Manual Poll Only"){
			LOGINFO("MANUAL FORECAST POLLING ONLY")
		} else {
			pollIntervalForecast = (settings?.pollIntervalForecast ?: "3 Hours").replace(" ", "")
			if (extSource.toInteger() == 2) {
				if(pollIntervalForecast=='2Minutes'){
					schedule("${dsseconds} 1/2 * * * ? *", pollDS)
				}else if(pollIntervalForecast=='5Minutes'){
    				schedule("${dsseconds} 2/5 * * * ? *", pollDS)                
				}else if(pollIntervalForecast=='10Minutes'){
					schedule("${dsseconds} 3/10 * * * ? *", pollDS)                
				}else if(pollIntervalForecast=='15Minutes'){
					schedule("${dsseconds} 4/15 * * * ? *", pollDS)                
				}else if(pollIntervalForecast=='30Minutes'){
					schedule("${dsseconds} 5/30 * * * ? *", pollDS)                
				}else if(pollIntervalForecast=='1Hour'){
					schedule("${dsseconds} 6 * * * ? *", pollDS)                
				}else if(pollIntervalForecast=='3Hours'){
					schedule("${dsseconds} 7 0/3 * * ? *", pollDS)                
				}
			}
		}
	}else{
		if(pollIntervalForecastnight == "Manual Poll Only"){
			LOGINFO("MANUAL FORECAST POLLING ONLY")
		} else {
			pollIntervalForecastnight = (settings?.pollIntervalForecastnight ?: "3 Hours").replace(" ", "")
			if (extSource.toInteger() == 2) {
				if(pollIntervalForecastnight=='2Minutes'){
					schedule("${dsseconds} 1/2 * * * ? *", pollDS)
				}else if(pollIntervalForecastnight=='5Minutes'){
				    schedule("${dsseconds} 2/5 * * * ? *", pollDS)                
				}else if(pollIntervalForecastnight=='10Minutes'){
					schedule("${dsseconds} 3/10 * * * ? *", pollDS)                
				}else if(pollIntervalForecastnight=='15Minutes'){
					schedule("${dsseconds} 4/15 * * * ? *", pollDS)                
				}else if(pollIntervalForecastnight=='30Minutes'){
					schedule("${dsseconds} 5/30 * * * ? *", pollDS)                
				}else if(pollIntervalForecastnight=='1Hour'){
					schedule("${dsseconds} 6 * * * ? *", pollDS)                
				}else if(pollIntervalForecastnight=='3Hours'){
					schedule("${dsseconds} 7 0/3 * * ? *", pollDS)                
				}
			}
		}
	}
	updateDataValue("is_lightOld", getDataValue("is_light"))
    return
}

def pollData() {
	pollDS()
    return
}
// ************************************************************************************************

public setDateTimeFormats(formatselector){
    switch(formatselector) {
        case 1: DTFormat = "M/d/yyyy h:mm a";   dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
        case 2: DTFormat = "M/d/yyyy HH:mm";    dateFormat = "M/d/yyyy";   timeFormat = "HH:mm";  break;
    	case 3: DTFormat = "MM/dd/yyyy h:mm a"; dateFormat = "MM/dd/yyyy"; timeFormat = "h:mm a"; break;
    	case 4: DTFormat = "MM/dd/yyyy HH:mm";  dateFormat = "MM/dd/yyyy"; timeFormat = "HH:mm";  break;
		case 5: DTFormat = "d/M/yyyy h:mm a";   dateFormat = "d/M/yyyy";   timeFormat = "h:mm a"; break;
    	case 6: DTFormat = "d/M/yyyy HH:mm";    dateFormat = "d/M/yyyy";   timeFormat = "HH:mm";  break;
    	case 7: DTFormat = "dd/MM/yyyy h:mm a"; dateFormat = "dd/MM/yyyy"; timeFormat = "h:mm a"; break;
        case 8: DTFormat = "dd/MM/yyyy HH:mm";  dateFormat = "dd/MM/yyyy"; timeFormat = "HH:mm";  break;
    	case 9: DTFormat = "yyyy/MM/dd HH:mm";  dateFormat = "yyyy/MM/dd"; timeFormat = "HH:mm";  break;
    	default: DTFormat = "M/d/yyyy h:mm a";  dateFormat = "M/d/yyyy";   timeFormat = "h:mm a"; break;
	}
    return
}

public setMeasurementMetrics(distFormat, pressFormat, precipFormat, temptFormat){
    isDistanceMetric = (distFormat == "Kilometers (kph)") ? true : false
    isPressureMetric = (pressFormat == "Millibar") ? true : false
    isRainMetric = (precipFormat == "Millimetres") ? true : false
    isFahrenheit = (temptFormat == "Fahrenheit (°F)") ? true : false
    return
}

def estimateLux(condition_code, cloud)     {	
	def lux = 0l
	def aFCC = true
	def l
	def bwn
	def sunRiseSet           = parseJson(getDataValue("sunRiseSet")).results
	def tZ                   = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	def lT                   = new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX", tZ)
	def localeMillis         = getEpoch(lT)
	def twilight_beginMillis = getEpoch(sunRiseSet.civil_twilight_begin)
	def sunriseTimeMillis    = getEpoch(sunRiseSet.sunrise)
	def noonTimeMillis       = getEpoch(sunRiseSet.solar_noon)
	def sunsetTimeMillis     = getEpoch(sunRiseSet.sunset)
	def twilight_endMillis   = getEpoch(sunRiseSet.civil_twilight_end)
	def twiStartNextMillis   = twilight_beginMillis + 86400000 // = 24*60*60*1000 --> one day in milliseconds
	def sunriseNextMillis    = sunriseTimeMillis + 86400000 
	def noonTimeNextMillis   = noonTimeMillis + 86400000 
	def sunsetNextMillis     = sunsetTimeMillis + 86400000
	def twiEndNextMillis     = twilight_endMillis + 86400000

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

	def cC = condition_code
	def cCF = (!cloud || cloud=="") ? 0.998d : ((100 - (cloud.toInteger() / 3d)) / 100)

    if(aFCC){
        if(cloud !="" && cloud != null){
			LUitem = LUTable.find{ it.wucode == condition_code && it.day == 1 }            
			if (LUitem && (condition_code != "unknown"))    {
				cCF = (LUitem ? LUitem.luxpercent : 0)
				cCT = (LUitem ? LUitem.wuphrase : 'unknown') + ' using cloud cover.'
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
	LOGDEBUG("condition: $cC | condition factor: $cCF | condition text: $cCT| lux: $lux")
	return [lux, bwn]
}

def getEpoch (aTime) {
	def tZ = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	def localeTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ssXXX", aTime, tZ)
	long localeMillis = localeTime.getTime()
	return (localeMillis)
}

public SummaryMessage(SType, Slast_poll_date, Slast_poll_time, SforecastTemp, Sprecip, Svis){   
    if(getDataValue("wind_gust") == "" || getDataValue("wind_gust").toBigDecimal() < 1.0 || getDataValue("wind_gust")==null) {
        wgust = 0.00g
    } else {
        wgust = getDataValue("wind_gust").toBigDecimal()
    }
    if(SType == true){
        wSum = "Weather summary for " + getDataValue("city") + " updated at ${Slast_poll_time} on ${Slast_poll_date}. "
        wSum+= getDataValue("condition_text")
        wSum+= (!SforecastTemp || SforecastTemp=="") ? ". " : "${SforecastTemp}"
        wSum+= "Humidity is " + getDataValue("humidity") + "% and the temperature is " + String.format("%3.1f", getDataValue("temperature").toBigDecimal()) +  (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "The temperature feels like it is " + String.format("%3.1f", getDataValue("feelsLike").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= "Wind: " + getDataValue("wind_string") + ", gusts: " + ((wgust < 1.00) ? "calm. " : "up to " + wgust.toString() + (isDistanceMetric ? ' KPH. ' : ' MPH. '))
        wSum+= Sprecip
        wSum+= Svis
        wSum+= (!getDataValue("alert") || getDataValue("alert")==null) ? "" : getDataValue("alert") + '.'
    } else {
        wSum = getDataValue("condition_text") + " "
        wSum+= ((!SforecastTemp || SforecastTemp=="") ? ". " : "${SforecastTemp}")
        wSum+= " Humidity: " + getDataValue("humidity") + "%. Temperature: " + String.format("%3.1f", getDataValue("temperature").toBigDecimal()) + (isFahrenheit ? '°F. ' : '°C. ')
        wSum+= getDataValue("wind_string") + ", gusts: " + ((wgust == 0.00) ? "calm. " : "up to " + wgust + (isDistanceMetric ? ' KPH. ' : ' MPH. '))
	}
    sendEvent(name: "weatherSummary", value: wSum)    
	return
}

public getImgName(wCode){
    LOGINFO("getImgName Input: wCode: " + wCode + "  state.is_day: " + getDataValue("is_day") + " iconLocation: " + getDataValue("iconLocation"))
    LUitem = LUTable.find{ it.wucode == wCode } //&& it.day.toString() == getDataValue("is_day") }    
	LOGINFO("getImgName Result: image url: " + getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + "?raw=true")
    return (getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + (((getDataValue("iconLocation").toLowerCase().contains('://github.com/')) && (getDataValue("iconLocation").toLowerCase().contains('/blob/master/'))) ? "?raw=true" : ""))    
}
public getowmImgName(wCode){
    LOGINFO("getImgName Input: wCode: " + wCode + "  state.is_day: " + getDataValue("is_day") + " iconLocation: " + getDataValue("iconLocation"))
    LUitem = LUTable.find{ it.wucode == wCode } //&& it.day.toString() == getDataValue("is_day") }    
	LOGINFO("getImgName Result: image url: " + getDataValue("iconLocation") + (LUitem ? LUitem.img : 'na.png') + "?raw=true")
    return (LUitem ? LUitem.owm : '')   
}
def logCheck(){
    if(logSet == true){
        log.info "DarkSky.net Weather Driver - INFO:  All Logging Enabled"
	} else {
        log.info "DarkSky.net Weather Driver - INFO:  Further Logging Disabled"
    }
    return
}

def LOGDEBUG(txt){
    try {
    	if(logSet == true){ log.debug("DarkSky.net Weather Driver - DEBUG:  ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG DarkSky.net Weather Driver - unable to output requested data!")
    }
    return
}

def LOGINFO(txt){
    try {
    	if(logSet == true){log.info("DarkSky.net Weather Driver - INFO:  ${txt}") }
    } catch(ex) {
    	log.error("LOGINFO DarkSky.net Weather Driver - unable to output requested data!")
    }
    return
}

def settingsOff(){
	log.warn "Settings disabled..."
	device.updateSetting("settingEnable",[value:"false",type:"bool"])
}

def sendEventPublish(evt)	{
// 	Purpose: Attribute sent to DB if selected	
	if (this[evt.name + "Publish"]) {
		sendEvent(name: evt.name, value: evt.value, descriptionText: evt.descriptionText, unit: evt.unit, displayed: evt.displayed);
		LOGDEBUG("$evt.name") //: $evt.name, $evt.value $evt.unit"
    }
}

@Field final List    LUTable =     [
[wucode: 'breezy', day: 1, img: '23.png', luxpercent: 1, owm: '02d'],
[wucode: 'chancesnow', day: 1, img: '41.png', luxpercent: 0.3, owm: '13d'],
[wucode: 'chancetstorms', day: 1, img: '37.png', luxpercent: 0.2, owm: '11d'],
[wucode: 'clear', day: 1, img: '32.png', luxpercent: 1, owm: '01d'],
[wucode: 'cloudy', day: 1, img: '28.png', luxpercent: 0.6, owm: '04d'],
[wucode: 'fog', day: 1, img: '19.png', luxpercent: 0.2, owm: '50d'],
[wucode: 'hazy', day: 1, img: '20.png', luxpercent: 0.2, owm: '50d'],
[wucode: 'partlycloudy', day: 1, img: '30.png', luxpercent: 0.8, owm: '02d'],
[wucode: 'rain', day: 1, img: '39.png', luxpercent: 0.5, owm: '10d'],
[wucode: 'sleet', day: 1, img: '8.png', luxpercent: 0.4, owm: '13d'],
[wucode: 'snow', day: 1, img: '16.png', luxpercent: 0.3, owm: '13d'],
[wucode: 'sunny', day: 1, img: '36.png', luxpercent: 1, owm: '01d'],
[wucode: 'tstorms', day: 1, img: '3.png', luxpercent: 0.3, owm: '11d'],
[wucode: 'nt_breezy', day: 0, img: '24.png', luxpercent: 0, owm: '02n'],
[wucode: 'nt_chancesnow', day: 0, img: '46.png', luxpercent: 0, owm: '13n'],
[wucode: 'nt_chancetstorms', day: 0, img: '47.png', luxpercent: 0, owm: '11n'],
[wucode: 'nt_clear', day: 0, img: '31.png', luxpercent: 0, owm: '01n'],
[wucode: 'nt_cloudy', day: 0, img: '27.png', luxpercent: 0, owm: '04n'],
[wucode: 'nt_fog', day: 0, img: '22.png', luxpercent: 0, owm: '50n'],
[wucode: 'nt_hazy', day: 0, img: '21.png', luxpercent: 0, owm: '50n'],
[wucode: 'nt_partlycloudy', day: 0, img: '29.png', luxpercent: 0, owm: '02n'],
[wucode: 'nt_rain', day: 0, img: '45.png', luxpercent: 0, owm: '10n'],
[wucode: 'nt_sleet', day: 0, img: '18.png', luxpercent: 0, owm: '13n'],
[wucode: 'nt_snow', day: 0, img: '7.png', luxpercent: 0, owm: '13n'],
[wucode: 'nt_tstorms', day: 0, img: '38.png', luxpercent: 0, owm: '11n'],
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
    "dewpoint":                 [title: "Dewpoint (in default unit)", descr: "Display the dewpoint?", typeof: "number", default: "false"],
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

def updateCheckHandler(resp, data) {

	state.InternalName = "DarkSky.net Weather Driver"

	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		respUD = parseJson(resp.data)
		// log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = "${thisCopyright}"
		// uses reformattted 'version2.json' 
		def newVer = padVer(respUD.driver.(state.InternalName).ver)
		def currentVer = padVer(version())               
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
				state.Status = "Current"
				if (descTextEnable) log.info "You are using the current version of this driver"
				break
		}

 	sendEvent(name: "verUpdate", value: state.UpdateInfo)
	sendEvent(name: "verStatus", value: state.Status)
      }
      else
      {
           log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI"
      }
}

/*
	padVer

	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each duple is padded first.

*/ 
def padVer(ver) {
	def pad = ""
	ver.replaceAll( "[vV]", "" ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}

def getThisCopyright(){"&copy; 2019 Matthew (scottma61) "}
