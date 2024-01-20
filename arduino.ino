#include <FastLED.h>
#define NUM_LEDS 180
#define BAUD_RATE 460800
#define DATA_PIN 5

// led array
CRGB leds[NUM_LEDS];

// serial buffer
char buffer[NUM_LEDS * 3 + 1];
int bufferIndex = 0;

// reset sequence
char resetSeq[] = {'R', 'E', 'S', 'E', 'T', '!', '!', '!'};
int resetSeqLength = 8;
int resetSeqIndex = 0;

void setup() {
    // setup serial and leds
    Serial.begin(BAUD_RATE);
    FastLED.addLeds<NEOPIXEL, DATA_PIN>(leds, NUM_LEDS);
}

void loop() {
    while (Serial.available()) {
        // read serial data into buffer
        buffer[bufferIndex] = Serial.read();

        // progress reset sequence
        if (buffer[bufferIndex] == resetSeq[resetSeqIndex])
            resetSeqIndex++;
        else
            resetSeqIndex = 0;

        // trigger reset
        if (resetSeqIndex >= resetSeqLength) {
            resetSeqIndex = 0;
            bufferIndex = 0;
            Serial.print(0xFF);
            continue;
        }

        // update leds once buffer is full
        bufferIndex++;
        if (bufferIndex >= NUM_LEDS * 3) {
            bufferIndex = 0;

            for (int i = 0; i < NUM_LEDS; i++) {
                leds[i].red = buffer[i*3];
                leds[i].green = buffer[i*3+1];
                leds[i].blue = buffer[i*3+2];
            }

            FastLED.show();
        }

    }
}
