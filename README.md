# smartthings-marantz-avr-8802
## SmartThings Device Handler for Marantz AVR 8802 Processor

A device handler for SmartThings which provides a basic interface to a Marantz AVR 8802
Network Processor (or similar Marantz Receiver/Processor - your mileage may vary). Leverages
the processor's local web API to scan for status and issue commands, so your Marantz unit
needs to be accessible on the same LAN (or from the LAN segment) used by your SmartThings Hub 
in order for this to work.

This code is based on previous work by Kristopher Kubicki and Sean Buckley 
(see [seanb-uk/marantz-avr](https://github.com/seanb-uk/marantz-avr)) , but has been
modified to the degree that it really demanded a new repo rather than a fork. My groovy
skills in general, and SmartThings developer skills in particular, are sub-par, so I'm 
really standing on the shoulders of giants here.

Key differences with the source project [seanb-uk/marantz-avr](https://github.com/seanb-uk/marantz-avr) 
as of 09/16/2019:
* Tweaked and tested to work with specifically with an AVR 8802 processor
* Added logic to dynamically parse & use input metadata from AVR status XML (see below)
* Added additional preferences allowing the user to specify inputs for three direct input
tiles (note, once you see the pattern, it should be easy to add/remove additional direct 
input tiles as you see fit)
* Changed the volume level logic to use the "native" dB scale (i.e. zero is very loud)

### Installation
Basic instructions to get you up & running:

#### Install the Device Handler
- Log into the SmartThings Web IDE at https://graph.api.smartthings.com/ & go
to My Device Handlers (note: you may need to go to the My Locations tab first and
select your location before your hub's data appears).
- Click the Settings button & add
a new repository with the following settings:

| Owner | Name | Branch |
| ------- | ------- | ------- |
| mpeskin-r7 | smartthings-marantz-avr-8802 | master |

- Save your settings, then click the Update from Reop button and select the smartthings-marantz-avr-8802
repository. 
- You should see the Device Handler File marantz-avr-8802.groovy in the New
section. Select it, as well as the Publish option, and click Execute Update.
- When you return
to your My Device Handlers page you should see the new handler listed with a Status of 
Published.

#### Create a Device Instance
Unfortunately, the handler does not come bundled with a companion SmartApp, so the only
way to create a device that uses it is manually via the SmartThings IDE as follows:

- Go to the My Devices page in the SmartThings Web IDE
- Click the New Device button
- Fill out the form for the new device. Under Type, select Marantz AVR 8802 (it will
probably appear near the bottom of the list)
- You need to supply a Device Network Id, but it can be any string (the handler will
automatically update it.)
- Click Create
- Select your newly-created Marantz device from the device list
- Click the edit link next to preferences, and enter the local ip and port information
for your Marantz processor/receiver.
- You can (optionally) enter the names of up to three inputs that you want to appear in
direct-access tiles in the SmartThings UI. If you have created aliases/friendly names
for any of the default inputs, use the alias.
- Save your preferences
- Go check out your new device in the SmartThings App!

### About Marantz Input Naming
The device handler attempts to reconcile the three (!!!) sets of input names associated
the preprocessor. These are:

1. The "Standard" or default names associated with each input
2. The "Canonical" input names used by the web API, which differ
(in sometimes-puzzling ways) from the standard names that are generally presented in the
processor UI.
3. The "Friendly" custom input names or aliases that may have been assigned to one or
more inputs by the user.

The handler logic will attempt to display the friendly or standard names in the SmartThings
app UI, while using the canonical names under the covers to issue commands. The handler
also scans for hidden/disabled inputs and will only rotate through inputs that have not been
hidden/disabled. There may
be some kinks in this logic and it has only been tested with my 8802, so caveat emptor.