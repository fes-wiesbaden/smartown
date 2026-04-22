#include <Wire.h>
#include <BH1750.h>
#include <Adafruit_PWMServoDriver.h>

Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver(0x40);
BH1750 lightMeter;

float schwellwert = 50.0;

void setup() {
  Serial.begin(115200);
  Wire.begin(21, 22);

  pwm.begin();
  pwm.setPWMFreq(1000);

  lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE);

  Serial.println("Laternen-Test gestartet (Kanal 0-15)");
  Serial.print("Schwellwert: ");
  Serial.print(schwellwert);
  Serial.println(" lx");
}

void laternenAn() {
  for (int i = 0; i <= 15; i++) {
    pwm.setPWM(i, 0, 4095);
  }
}

void laternenAus() {
  for (int i = 0; i <= 15; i++) {
    pwm.setPWM(i, 0, 0);
  }
}

void loop() {
  float lux = lightMeter.readLightLevel();

  Serial.print("Helligkeit: ");
  Serial.print(lux);
  Serial.print(" lx -> Laternen: ");

  if (lux < schwellwert) {
    laternenAn();
    Serial.println("AN");
  } else {
    laternenAus();
    Serial.println("AUS");
  }

  delay(500);
}