# NMEA AIS Message Decoder Driver
The purpose of this driver is to decode standard NMEA AIS Messages in the standard format:

```!AIVDM,1,1,,A,H52lOI@<4pU>0lTpu:000000000,2*55 ```

NMEA Message structure looks like this:

Field | Value | Meaning
-- | -- | --
Sentence type | !AIVDM | AIS VHF Data-link Message
Fragment count | 1 | Total number of fragments in this message
Fragment number | 1 | This is fragment 1
Sequential ID | `` (blank) | Used to link multipart messages
Channel | A | AIS channel A (161.975 MHz) or B (162.025 MHz)
Payload | H52lOI@<4pU>0lTpu:000000000 | Encoded AIS payload
Fill bits | 2 | Padding bits added to final payload
Checksum | 55 | NMEA checksum

This driver outputs the AIS Messages into Class A Reports, Class B reports, ATON Reports, AIS Addressed Binary Messages, and AIS Binary Broadcast Messsages as identified
https://www.navcen.uscg.gov/ais-messages.

Setup
This driver was tested using the following pre-requestites:
## Hardware
- [ShipXplorer AIS Dongle](https://www.shipxplorer.com/ais-dongle)
- [ShipXplorer AIS Antenna](https://www.shipxplorer.com/ais-antenna)

## Software
- Install [AIS Catcher](https://jvde-github.github.io/AIS-catcher-docs/)

Local Setup (AIS-Catcher is running on local machine)
- 