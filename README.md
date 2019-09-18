DarkSky.net-Weather-Driver
Driver to use the DarkSky.net weather API data

Hubitat Community Forum discussion is here: https://community.hubitat.com/t/release-darksky-net-weather-driver-no-pws-required/22699

DarkSky.net Weather Driver Copyright 2019 @Matthew (Scottma61)

Many people contributed to the creation of this driver. Significant contributors include:

@Cobra who adapted it from @mattw01's work and I thank them for that!
@bangali for his original APIXU.COM base code that much of the early versions of this driver was adapted from.
@bangali for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
@csteele (and prior versions from @bangali) for the attribute selection code.
@csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
@bangali also contributed the icon work from https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
In addition to all the cloned code from the Hubitat community, I have heavily modified/created new code myself @Matthew (Scottma61) with lots of help from the Hubitat community. If you believe you should have been acknowledged or received attribution for a code contribution, I will happily do so. While I compiled and orchestrated the driver, very little is actually original work of mine.

This driver is free to use. I do not accept donations. Please feel free to contribute to those mentioned here if you like this work, as it would not have been possible without them.

This driver is intended to pull weather data from DarkSky.net (http://darksky.net). You will need your DarkSky API key to use the data from that site.

You can select to use a base set of condition icons from the forecast source, or an 'alternative' (fancier) set. The base 'Standard' icon set will be from WeatherUnderground. You may choose the fancier 'Alternative' icon set if you use the Dark Sky.

The driver exposes both metric and imperial measurements for you to select from.

ATTRIBUTES CAUTION The way the 'optional' attributes work:

Initially, only the optional attributes selected will show under 'Current States' and will be available in dashboards.

Once an attribute has been selected it too will show under 'Current States' and be available in dashboards. <*** HOWEVER ***> If you ever de-select the optional attribute, it will still show under 'Current States' and will still show as an attribute for dashboards BUT IT'S DATA WILL NO LONGER BE REFRESHED WITH DATA POLLS. This means what is shown on the 'Current States' and dashboard tiles for de-selected attributes may not be current valid data.

To my knowledge, the only way to remove the de-selected attribute from 'Current States' and not show it as available in the dashboard is to delete the virtual device and create a new one AND DO NOT SELECT the attribute you do not want to show.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
