# ambient-led
*I used to have an led strip on my piano - never used it. So I decided to repurpose it... by buying another 2... and two raspberry pi's... and two power supplies*

ambient-led is a simple c program that turns your led strip into a monitor backlight. Originally written in java, I've rewritten it in c to make it more efficient. As of right now it only supports NvFBC on linux, because any other screen capture method on linux is too slow. It's currently not compatible with windows, but I'm working on it (no i'm not).

## Hardware
While this repository is more of a personal project, I'll try to explain how to replicate it. Here's what you'll need:
- Arduino **and/or** Raspberry Pi
- WS2812B led strip (at least I use these)

## Building
First you'll need to patch your nvidia driver to enable nvfbc.
Don't worry, it's not as hard as it sounds. You can find the instructions [here](https://github.com/keylase/nvidia-patch).

Then you'll need to install the following libraries:
- [cJSON (>=1.7.17-1)](https://github.com/DaveGamble/cJSON)

(that's it, just make sure you have build tools installed)

## Installation
1. Clone the repository
2. Create a build/ folder
3. Run `make PROD=1` in the root directory
4. Create a config.json in the build folder (see below)
5. Run `./ambientled` from the build folder

## Configuration
The config.json is fairly simple, so a small example should be all you need to understand it:
```json
{
    "ups": 60, // amount of times the led strip is updated per second
    "fps": 30, // amount of times the screen is captured per second
    "lerp": 0.5, // how much the led strip should lerp to the new color
    "strips": [
        {
            "type": "arduino", // or "rpi"
            "addr": "/dev/ttyACM0", // or the ip address of the rpi
            "leds": 180, // amount of leds on the strip
            "segments": [
                {
                    "offset": 0, // led offset of this segment of the strip
                    "length": 47, // length of this segment
                    "display": "DP-0", // display to capture
                    "x": 3540, // x position of the capture
                    "y": 0, // y position of the capture
                    "width": 300, // width of the capture
                    "height": 2160, // height of the capture
                    "orientation": false, // false = vertical, true = horizontal
                    "flip": true // flip the capture
                },
                {
                    ... // another segment
                }
            ],
            "max_brightness": 145,
            "r_mult": 1.0,
            "g_mult": 0.7,
            "b_mult": 1.0
        },
        {
            ... // another strip
        }
    ]
}
```