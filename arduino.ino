#include <FastLED.h>
#define NUM_LEDS 180
#define DATA_PIN 5

CRGB leds[NUM_LEDS];
int should_update = 0;

void setup() {
  Serial.begin(38400);
  FastLED.addLeds<NEOPIXEL, DATA_PIN>(leds, NUM_LEDS);
}

void loop() {
  // read serial if available
  while(Serial.available() >= 5) {
    // parse index
    int led = Serial.read() << 8 | Serial.read();

    // parse colors
    leds[led].red = Serial.read();
    leds[led].green = Serial.read();
    leds[led].blue = Serial.read();

    // increase update counter
    should_update++;
  }

  // update leds if every one was updated
  if (should_update >= NUM_LEDS) {
    FastLED.show();
    // why the fuck am I documenting this in so much detail
    should_update -= NUM_LEDS;
  }
}