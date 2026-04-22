#include <Wire.h>
#include <BH1750.h>
#include <Adafruit_PWMServoDriver.h>

Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver(0x40);
BH1750 lightMeter;

bool erkannt(byte addr) {
  Wire.beginTransmission(addr);
  return Wire.endTransmission() == 0;
}

void setup() {
  Serial.begin(115200);
  Wire.begin(21, 22);
  delay(200);

  Serial.println();
  Serial.println("Komponententest");

  // PCA9685
  if (erkannt(0x40)) {
    pwm.begin();
    pwm.setPWMFreq(1000);
    pwm.setPWM(0, 0, 4095);
    delay(300);
    pwm.setPWM(0, 0, 0);
    Serial.println("PCA9685: OK");
  } else {
    Serial.println("PCA9685: NICHT gefunden");
  }

  // BH1750
  if (erkannt(0x23) && lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    delay(200);
    float lux = lightMeter.readLightLevel();
    Serial.print("BH1750: OK (");
    Serial.print(lux);
    Serial.println(" lx)");
  } else {
    Serial.println("BH1750: NICHT gefunden");
  }
}

void loop() {
}
