# ambient-led
I used to have this led strip on my piano and barely ever used it, so I decided to put it to better use.

## Hardware
This repository is more of a personal project, but if you want to replicate it, here's what you'll need:
- Arduino **and/or** Raspberry Pi
- WS2812B led strip with individual addressable leds (I have 2x 144/1m and 1x 2m/288)

## Setup
0. *Clone this repository and open it in your favorite IDE*
1. Configure your screen coordinates in the grabber classes
2. Configure the led strip information in the led classes
3. *Remove either the arduino or raspberry pi code if you don't have one of them*
4. Run `gradle shadowJar` to build the jar file and create a shortcut in your startup folder

### Arduino
0. *Download Arduino IDE and install the FastLED library*
1. Flash the arduino with the `arduino.ino` file
2. Connect led strip to 5v, ground and pin 5
### Raspberry Pi
0. *Install Raspbian and Java 17*
1. Configure build.gradle to upload to your pi (setup systemd service, ssh, etc. on raspberry pi)
2. Run `gradle build uploadRpi restartRpi` to upload and test the program
