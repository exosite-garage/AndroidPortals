## Portals for Android

This Android app demonstrates logging in as an Exosite [Portals](https://portals.exosite.com) user and accessing device data via the [RPC API](https://docs.exosite.com/rpc) and [Portals API](https://docs.exosite.com/portals).

### Usage

To log in using the app, you need a [Portals](https://portals.exosite.com) account username (i.e. your email) and password. Enter these on the first screen.

![screenshot of login](/images/screenshot-login.png)
![screenshot of signing in](/images/screenshot-signingin.png)
![screenshot of device list](/images/screenshot-devicelist.png)

You'll see a list of devices in your default domain, along with what portal owns them. If you have an account on multiple Portals domains and want to select a different domain, slide from left to right to open the list of your domains.Click on one to load device list for it. 

![screenshot of device list](/images/screenshot-domains.png)

If you click on one of the devices in the device list, you'll see a list of its dataports and datarules, and the latest value for each one.

![screenshot of device list](/images/screenshot-device.png)


### Build 

This application was built using [Android Studio](http://developer.android.com/sdk/index.html) version 1.0.2. Building in Eclipse requires some manual steps.

1.) Clone the source

```
$ git clone git@github.com:exosite-garage/AndroidPortals.git AndroidPortals 
```

2.) Set up an Android device for development over USB (enable developer options, enable USB debugging)

3.) Set up PC for debugging (this varies by platform, see http://developer.android.com/tools/device.html)

4.) Open the project with Android Studio

5.) In Android Studio, select Build->Make Project

6.) Select Run->Run Demo

7.) Select your Android device. If you don't see your device, you may need to unplug it and plug it back in.

### Known Issues


### Release Guide

1.) Test (incl. in airplane mode)

2.) Update version number in preferences.xml and AndroidManifest.xml

3.) Build -> Generate Signed APK 

5.) Commit and push in git

6.) Tag with version number and push that too

### TODOs

- add timestamp to resource last value display. Maybe something like "n weeks/days/hours/minutes ago"
