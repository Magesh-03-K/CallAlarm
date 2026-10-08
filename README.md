# Call Alarm (Android)

## Build and install
1. Install Android Studio (Koala or newer).
2. File > Open > select this CallAlarm folder. Let Gradle sync finish.
3. On your phone: Settings > About phone > tap Build number 7 times, then enable
   Developer options > USB debugging. Plug the phone in.
4. Press the green Run button in Android Studio.
   (Or Build > Build APK(s), then copy app-debug.apk to your phone and install it.)

## First run
- Allow Calls and Notifications when asked.
- On Android 14+, tap "Open setting" on the yellow banner and allow full-screen alarms.
- Enter a name and number, tap "Test in 10 seconds", then lock the phone.

## How it works
- AlarmManager.setAlarmClock: exact time, works when idle or the app is closed.
- Full-screen notification opens the Call / Cancel screen over the lock screen.
- Call places the call directly (CALL_PHONE). Cancel stops the alarm.
- Ignored alarm: it stops after 1 minute and rings again every 2 minutes, up to 3 times.
- Alarms are restored after reboot.
