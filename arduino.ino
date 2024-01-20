#define FASTLED_ALLOW_INTERRUPTS 0
#include <FastLED.h>
#define BAUD_RATE 490800 * 2
#define DATA_PIN 5

int convertToInt(byte* data, int start) {
  int result;
  byte* resultPtr = (byte*)&result;
  resultPtr[0] = data[start + 3];
  resultPtr[1] = data[start + 2];
  resultPtr[2] = data[start + 1];
  resultPtr[3] = data[start];
  return result;
}

void setup() {
  Serial.begin(BAUD_RATE);
}

// color buffers
CRGB* colors;
int len;
CRGB* final_colors;

int readIndex = 0;

// header data
char header_data[16];
int maxBrightness;
int leds = 0;
float r, g, b;
uint8_t lerp;
int upsInMillis;

// loop stuff
unsigned long lastUpdate = 0;
unsigned long lastRead = 0;

void prepare() {
  // read header
  Serial.readBytes(header_data, 16);
  maxBrightness = convertToInt(header_data, 0);
  r = ((float)header_data[8] + 128) / 256.0f;
  g = ((float)header_data[9] + 128) / 256.0f;
  b = ((float)header_data[10] + 128) / 256.0f;
  lerp = (uint8_t)((float)header_data[11] + 128);
  upsInMillis = 1000 / convertToInt(header_data, 12);
  readIndex = 0;

  if (leds) { // skip some steps if previously set up
    Serial.write(1);
    return;
  }

  leds = convertToInt(header_data, 4);
  len = leds * 3;
  colors = (CRGB*)calloc(leds, sizeof(CRGB));
  final_colors = (CRGB*)calloc(leds, sizeof(CRGB));

  FastLED.addLeds<NEOPIXEL, DATA_PIN>(final_colors, leds);
  FastLED.setMaxRefreshRate(0);
  Serial.write(1);
}

void loop() {
  unsigned long time = millis();

  // read header once enough data available
  if (leds == 0 || (time - lastRead) > 2000) {
    if (Serial.available() < 16)
      return;

    prepare();
    lastRead = time;
  }

  // read new colors
  while (Serial.available()) {
    ((char*)colors)[readIndex] = Serial.read();

    if (++readIndex >= len)
      readIndex = 0;

    lastRead = time;
  }

  // check if it is update time
  if (time - lastUpdate < upsInMillis)
    return;
  lastUpdate = time;

  // lerp colors
  uint32_t avg = 0;
  for (int i = 0; i < leds; i++) {
    final_colors[i].red = lerp8by8(final_colors[i].red, colors[i].red * r, lerp);
    final_colors[i].green = lerp8by8(final_colors[i].green, colors[i].green * g, lerp);
    final_colors[i].blue = lerp8by8(final_colors[i].blue, colors[i].blue * b, lerp);
    avg += (uint32_t)final_colors[i].red + (uint32_t)final_colors[i].green + (uint32_t)final_colors[i].blue;
  }
  avg /= leds;

  // update colors
  float reduction = min(1.0, maxBrightness / max(1.0f, avg));
  FastLED.show((char)(reduction * 255));
}
