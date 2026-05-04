#include <HCSR04.h>

UltraSonicDistanceSensor distanceSensor(5, 4);
int pins[] = {21, 19, 32, 33, 25, 26, 27, 14, 12, 13};

void setup() {
  Serial.begin(9600);
  for (int i = 0; i < 10; i++) {
    pinMode(pins[i], OUTPUT);
  }
}

void loop() {
  digitalWrite(pins[0], LOW);
  for (int i = 9; i > 0; i--) {
    if (2 < distanceSensor.measureDistanceCm() && distanceSensor.measureDistanceCm() < 40) {
      for (int i = 0; i < 10; i++) {
        digitalWrite(pins[i], LOW);
      }
      i = 0;
      flugzeugKommt();
    }
    digitalWrite(pins[i], HIGH);
    Serial.println(String("Pin: ") + pins[i]);
    delay(100);
    if (i == 9) {
      digitalWrite(pins[1], LOW);
    }
    else {
      digitalWrite(pins[i + 1], LOW);
    }
  }
}

void flugzeugKommt() {
  while (distanceSensor.measureDistanceCm() < 40) {
    if (distanceSensor.measureDistanceCm() < 6) {
      digitalWrite(pins[0], HIGH);
    }
    else {
       Serial.println(distanceSensor.measureDistanceCm());
    }
  }
  digitalWrite(pins[0], LOW);
}







