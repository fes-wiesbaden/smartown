#include <Stepper.h>

const int STEPS_PER_REVOLUTION = 2048;
const int STEP_ANGLE = 512;
const int MOTOR_SPEED_RPM = 10;
const unsigned long SENSOR_TIMEOUT_US = 30000;
const unsigned long LOCK_DURATION_MS = 3000;
const unsigned long SENSOR_SETTLE_DELAY_MS = 30;
const unsigned long LOOP_DELAY_MS = 50;

const int TRIG_1_PIN = 2;
const int ECHO_1_PIN = 3;
const int TRIG_2_PIN = 4;
const int ECHO_2_PIN = 5;

const float MIN_DISTANCE_CM = 5.0;
const float MAX_DISTANCE_CM = 8.0;

Stepper bridgeMotor(STEPS_PER_REVOLUTION, 8, 10, 9, 11);

enum DetectionState {
  IDLE = 0,
  SENSOR_1_FIRST = 1,
  SENSOR_2_FIRST = 2,
};

DetectionState state = IDLE;
unsigned long lockUntilMs = 0;

float readDistanceCm(int trigPin, int echoPin) {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  const long duration = pulseIn(echoPin, HIGH, SENSOR_TIMEOUT_US);
  if (duration == 0) {
    return 999.0;
  }

  return duration * 0.034f / 2.0f;
}

bool isInDetectionRange(float distanceCm) {
  return distanceCm >= MIN_DISTANCE_CM && distanceCm <= MAX_DISTANCE_CM;
}

void moveBridgeForward() {
  bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
  bridgeMotor.step(STEP_ANGLE);
}

void moveBridgeBackward() {
  bridgeMotor.setSpeed(MOTOR_SPEED_RPM);
  bridgeMotor.step(-STEP_ANGLE);
}

void setup() {
  pinMode(TRIG_1_PIN, OUTPUT);
  pinMode(ECHO_1_PIN, INPUT);
  pinMode(TRIG_2_PIN, OUTPUT);
  pinMode(ECHO_2_PIN, INPUT);
  Serial.begin(9600);
}

void loop() {
  if (millis() < lockUntilMs) {
    return;
  }

  const float distance1 = readDistanceCm(TRIG_1_PIN, ECHO_1_PIN);
  delay(SENSOR_SETTLE_DELAY_MS);
  const float distance2 = readDistanceCm(TRIG_2_PIN, ECHO_2_PIN);

  Serial.print("S1: ");
  Serial.print(distance1);
  Serial.print(" | S2: ");
  Serial.println(distance2);

  const bool sensor1Active = isInDetectionRange(distance1);
  const bool sensor2Active = isInDetectionRange(distance2);

  if (state == IDLE) {
    if (sensor1Active) {
      state = SENSOR_1_FIRST;
      Serial.println("Sensor 1 zuerst -> Motor vorwaerts");
      moveBridgeForward();
    } else if (sensor2Active) {
      state = SENSOR_2_FIRST;
      Serial.println("Sensor 2 zuerst -> Motor vorwaerts");
      moveBridgeForward();
    }
  } else if (state == SENSOR_1_FIRST) {
    if (sensor2Active) {
      Serial.println("Sensor 2 danach -> Motor zurueck");
      moveBridgeBackward();
      state = IDLE;
      lockUntilMs = millis() + LOCK_DURATION_MS;
    }
  } else if (state == SENSOR_2_FIRST) {
    if (sensor1Active) {
      Serial.println("Sensor 1 danach -> Motor zurueck");
      moveBridgeBackward();
      state = IDLE;
      lockUntilMs = millis() + LOCK_DURATION_MS;
    }
  }

  delay(LOOP_DELAY_MS);
}
