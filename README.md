# hubitat-heos
Denon HEOS Integration for Hubitat. This allows you to interact with all of your HEOS devices using the HEOS CLI interface.
 
## Devices
You must install the following device drivers for this to work
* Denon HEOS Speaker

## Apps
The Denon HEOS Integration app configures your HEOS environment. It will create the child devices for all of the individual speakers/AVRs/etc. It may take up to 2 minutes for the app to detect all of your devices. The device list screen will refresh every 15 seconds while devices are detected.

### Configuration
Enter your HEOS account information and select the devices you wish to include. During alpha/beta testing please consider leaving the debug logging on to help with troubleshooting.

### Features
You will be able to control mute/unmute, volume, next/previous track, play/pause/stop, send text to speech, play a URL, play a preset, play an input (valid inputs depend on the device type), or to play a top search result from an online music source (see table below for more details)

|    Service   |           Search Type          |
|:------------:|:------------------------------:|
| Amazon Music | Station, Playlist              |
| Deezer       | Artist, Album, Track           |
| iHeartRadio  | Artist, Track                  |
| Napster      | Artist, Album, Track           |
| Rhapsody     | Artist, Album, Track           |
| Soundcloud   | Artist, Track                  |
| Tidal        | Artist, Album, Track, Playlist |
| TuneIn       | Station                        |

## Donations
If you find this app useful, please consider making a [donation](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url)! 

## Revision History
* 2020.01.26 - Initial Release
* 2020.02.01 - Fixed an issue when passwords contained non-url safe characters
* 2020.02.15 - Added individual attributes for track/artist/album