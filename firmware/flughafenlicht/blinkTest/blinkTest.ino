int pins[] = {2, 4, 16, 5, 18, 19, 21, 22, 23};

void setup() {
  for (int i = 0; i < 9; i++) {
    pinMode(pins[i], OUTPUT);
  }
}

void loop() {
  for (int i = 0; i < 9; i++) {
    digitalWrite(pins[i], HIGH);
    delay(500);
    digitalWrite(pins[i], HIGH);
  }
}