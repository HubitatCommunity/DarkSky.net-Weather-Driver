DarkSky.net Weather Driver
	Import URL: https://raw.githubusercontent.com/HubitatCommunity/DarkSky.net-Weather-Driver/master/DarkSky.net%20Weather%20Driver.groovy
	Copyright 2020 @Matthew (Scottma61)

Hubitat Community page: https://community.hubitat.com/t/release-darksky-net-weather-driver-no-pws-required/22699

	Please see extensive attributions and contribution at the end of this page.

	Some Notes:
Latitude and Longitude are pulled from the Hub by default. Make sure your Latitude and Longitude are set properly in your Hub settings. Optionally, you may override the hub's coordnates and input an alternative latitude and longitude.  The coordinates are used both for the Sunrise/Sunset times and the DarkSky forecast location.

The driver exposes a default set of attributes for weather capabilities:
humidity
illuminance
pressure
temperature
ultravioletIndex

Plus a small set of 'Required for Dashboard' attributes (used by SmartTiles/SharpTool.io, and maybe some others)
city
feelsLike
forecastIcon
localSunrise
localSunset
percentPrecip
weather
weatherIcon
weatherIcons
wind
WindDirection
windSpeed

ATTRIBUTES CAUTION
The way the 'optional' attributes work:

Initially, only the optional attributes selected will show under 'Current States' and will be available in dashboards.
Once an attribute has been selected it too will show under 'Current States' and be available in dashboards.
<*** HOWEVER ***> If you ever de-select the optional attribute, it will still show under 'Current States' and will still show as an attribute for dashboards BUT IT'S DATA WILL NO LONGER BE REFRESHED WITH DATA POLLS. This means what is shown on the 'Current States' and dashboard tiles for de-selected attributes may not be current valid data.
To my knowledge, the only way to remove the de-selected attribute from 'Current States' and not show it as available in the dashboard is to delete the virtual device and create a new one AND DO NOT SELECT the attribute you do not want to show.

There are many other 'optional' attributes that can be selected if you need those exposed. It is best to keep those 'off' if you do not require them as they will increase the Hub database size if selected. You can turn 'on' the 'Display All Preferences' then hit 'Save Preferences' to expose the optional attributes. Turn 'on' those you want, turn 'off' those you no longer want, then 'Save Preferences' again. You can turn off 'Display All Preferences' then 'Save Preferences' to hide those options and reduce the clutter on the driver display. NOTE: You do NOT have to select the optional attribute to allow those specific attributes to show in either the 'myTile' or 'weatherSummary' attributes (e.g. 'alert' will show in myTile and weatherSummary even if it is not selected). Optional attributes include:
alert -- Display any weather alert?
betwixt -- Display the 'slice-of-day'?
cloud -- Display cloud coverage %?
condition_code -- Display 'condition_code'?
condition_icon_only -- Display 'condition_code_only'?
condition_icon_url -- Display 'condition_code_url'?
condition_icon -- Dislay 'condition_icon'?
condition_iconWithText -- Display 'condition_iconWithText'?
condition_text -- Display 'condition_text'?
dewpoint -- Display the dewpoint?
fcstHighLow -- Display forecast High/Low temperatures?
forecast_code -- Displays 'forecast_code'?
forecast_text -- Displays 'forecast_text'?
illuminated -- Display 'illuminated' (with 'lux' added for use on a Dashboard)?
is_day -- Display 'is_day'?
localSunrise -- Display the Group of 'Time of Local Sunrise and Sunset,' with and without Dashboard text?
myTile -- Display 'mytile'?
moonPhase -- Display 'moonPhase'?
nearestStorm -- Display the neastestStorm data?
nearestStormBearing -- Display 'nearestStormBearing'?
nearestStormCardinal -- Display 'nearestStormCardinal'?
nearestStormDirection -- Display 'nearestStormDirection'?
nearestStormDistance -- Display 'nearestStormDistance'?
ozone -- Display 'ozone'?
percentPrecip -- Display the Chance of Rain, in percent?
summarymessage -- Display the Weather Summary?
precipExtended -- Display precipitation forecast?
obspoll -- Display Observation and Poll times?
vis -- Display visibility distance?
wind_cardinal -- Display the Wind Direction (text initials)?
wind_degree -- Display the Wind Direction (number)?
wind_direction -- Display the Wind Direction (text words)?
wind_gust -- Display the Wind Gust?
wind_string -- Display the wind string?

You may select the number of decimals shown. There are two groups i) Temperature, Wind Speed & Distance, ii) Pressure. Each can be set indepently of the other. You may choose to display from none (0) to four (4) decimal places, although I am not certain any data sources provide that level of detail.

There are three 'Dash' groups for attributes that are required by some dashboard weather tiles (see details below). If your dashboards are not showing/updating information from this driver you think it should be, check these and turn on for the dashboard in question. You should only turn on what you need to minimize database utilization.

There are two independent 'polling' intervals. One will run the poll interval selected during 'Daytime' hours and the second will run the interval selected during 'Nighttime'. They can be set the same or differently. I did this because I use the illuminance calculation so I wanted the 'Daytime' calculation to run frequently. However, at night I do not need it to run frequently so I reduce the frequency. 'Nighttime' is defined as between Twilight end and Twlight begin.

There are four 'Tile' attributes, built to be shown on dashboards.  They are 'myTile', 'threedayfcstTile', 'alertTile', and 'weatherSummary'.  In a dashboard select the driver, then select a template of 'attribute', then those will be listed with the other indiviaual attributes the driver provides.  Remember to turn 'on' any optional attributesif you want them to be available in dashbaord or in Rule Machine.

On 'myTile', if there is a weather alert, it will show in red italics. That weather alert will also be a hyperlink to the weather.gov 1 alert details page. Those weather.gov 1 links can be quite long, but the reduced formatting mentioned above should prevent exceeding the 1,024 character limit in most cases. If it does exceed the limit, the hyperlink will first revert to the Hub location's weather details page on Darsky.net where you can get details on the alert, and if it still exceeds the character limit, the hyperlink will be omitted.
On the 'threedayfcstTile' there is a hyperlink on the 'Today' text that will take you to the hub location's weather details page on Darksky.net
On the 'alertTile' that will just list weather alerts (or 'No current weather alerts for this area.'). Hyperlinks will take you to the hub location's weather details page on Darksky.net for no alerts, or the weather.gov alert details page if there is an alert.

Dashboard tiles are limited to a maximum of 1,024 characters.  If an attrbute's value ewxceeds that if will display 'Please select and attribute' instead of the attributes's value.  The driver does a lot of 'behind the scenes' adjustments to prevent ths message from occuring.  That means the tile's appearance may change based on the weather conditions.  For example, if the character limt is eceeds the driver will start to eliminate hyperlinks and/or icons to shorten the number of characters.  As a last resort, all formatting and icons are removed, and a the text is truncated to 1,024 characters.  That prcess is described here:
'myTile' handling of greater than 1,024 characters:
-> Will show default 'with all icons' and 'with html formatting', if possible. If not:
--> Will show 'myTile' with as many icons as fill fit with html formatting, if possible. If not:
----> Will show 'myTile' with all icons removed but with html formatting, if possible. If not:
------> Will show 'myTile' with no icons and no html formatting, if possible. If not:
---------> Will show 'myTile' with no icons, no html formatting and text truncated to 1,024 characters.

All required icons are contained in (https://raw.githubusercontent.com/HubitatCommunity/WeatherIcons/master/), I have created a TinyURL that reduces the number characters in the path to about 40% of the direct path. This is important as the 'myTile' has seven icons and the full path to those adds to the character limit of a dashboard tile. The TinyURL is: https://tinyurl.com/y6xrbhpf/ 

Lux
An estimated Lux is calculated by the driver using sunrise/sunset/twilight begins/twilight ends/cloud cover/ and/or current conditions.  The calculation assumes a maximum Lux of 10,000 for a clear sky at noon.  It adjusts proportionally for time time from twilight ends to Nonn in the montning, and Noon to twilight begins in the afternoon (the closer to noon, the higher the number).  It also factors in cloud cover.  If cloud cover is not available, it will estimate from the weather condition test.  The update lux routine checks at least every five minutes to see if a daytime/nighttime schedule change was needed (without polling) even if long polling intervals were selected.  It also provides an updated Lux calculation (again, without Polling) due to the time change.  The maximum lux is set by time of day as listed below:
from Twilight begins until Sunrise: 50
from Sunrise until Sunset 10,000
from Sunset to Twilight ends: 50
between Twilight ends and Twilight begins: 5
There is an option for 'lux smoothing/jitter control.' This basically rounds the lux calculation to reduce the variability (code/recommendation from @nh.schottfam) . This is optional and must be selected to enable it.  The lux smoothing/jitter control works as follows:
If lux estimation is greater than 1,100 --> round to the nearest '800'
else If lux estimation is greater than 400 --> round to the nearest '400'
else set lux = 5

Attributions:
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
	- @nh.schottfam for lots of code clean up and optimizations.

	In addition to all the cloned code from the Hubitat community, I have heavily modified/created new
	code myself @Matthew (Scottma61) with lots of help from the Hubitat community.  If you believe you
	should have been acknowledged or received attribution for a code contribution, I will happily do so.
	While I compiled and orchestrated the driver, very little is actually original work of mine.
	This driver is free to use.  I do not accept donations. Please feel free to contribute to those
	mentioned here if you like this work, as it would not have been possible without them.
	This driver is intended to pull weather data from DarkSky.net (http://darksky.net). You will need your
	DarkSky API key to use the data from that site.
	
