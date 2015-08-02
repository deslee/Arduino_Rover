#define pin_lf 5
#define pin_lr 6
#define pin_rf 3
#define pin_rr 4

#define cmd_lf 1
#define cmd_lr 2
#define cmd_rf 4
#define cmd_rr 8

int inByte = 0;

void setup()
{
	pinMode(pin_lf, OUTPUT);
	pinMode(pin_lr, OUTPUT);
	pinMode(pin_rf, OUTPUT);
	pinMode(pin_rr, OUTPUT);
	Serial.begin(9600);
}

void loop()
{
	if (Serial.available()) {
		inByte = Serial.read();
		Serial.write(inByte);

		digitalWrite(pin_lf, LOW);
		digitalWrite(pin_lr, LOW);
		digitalWrite(pin_rf, LOW);
		digitalWrite(pin_rr, LOW);

		if (check_has(cmd_lf)) {
			digitalWrite(pin_lf, HIGH);
		}
		if (check_has(cmd_lr)) {
			digitalWrite(pin_lr, HIGH);
		}
		if (check_has(cmd_rf)) {
			digitalWrite(pin_rf, HIGH);
		}
		if (check_has(cmd_rr)) {
			digitalWrite(cmd_rr, HIGH);
		}
	}
}

bool check_has(int cmd) {
	return ((inByte & cmd) == cmd);
}