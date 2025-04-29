# Study Timer App

A modern Android application designed to help you maintain productive study sessions with regular eye rest breaks.

## Features

*   **Random Alarms:** During study sessions, receive random alarms every 3-5 minutes (configurable).
*   **Eye Rest Periods:** After each alarm, a mandatory 10-second eye rest period is initiated.
*   **Configurable Settings:** Adjust study duration, minimum/maximum alarm intervals via the settings screen.
*   **Optional Next Alarm Countdown:** Choose whether to display the countdown timer for the next random alarm (default is off).
*   **Progress Visualization:** A circular progress indicator visually represents the remaining time in the current session (study or break).
*   **Background Operation:** The timer continues to run accurately even when the app is in the background or the screen is off, thanks to a foreground service.
*   **Notifications:** Receive notifications for alarms, eye rest periods, and session transitions.

## How It Works

1. **Study Session**: The app starts a 90-minute study timer
2. **Eye Rest Alarms**: During the study session, alarms will sound at random intervals (between 3-5 minutes)
3. **Eye Rest Period**: When an alarm sounds, take a 10-second break to rest your eyes
4. **Break Time**: After the 90-minute study session, take a 20-minute break
5. **Repeat**: The cycle automatically repeats until you stop the timer

## Technical Details

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Background Processing**: Foreground Service with Wake Lock
- **Notifications**: Persistent notification with timer status
- **Minimum SDK**: Android 6.0 (API level 23)

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on your device or emulator

## Usage

1. **Start Timer**: Press the "Start" button to begin a study session
2. **Stop Timer**: Press the "Stop" button to end the current session
3. **Eye Rest**: When you hear the alarm, rest your eyes for 10 seconds
4. **Break**: After 90 minutes, take a 20-minute break

## Why Use Study Timer?

The Study Timer app is based on research-backed productivity techniques:

- **Focused Study**: Dedicated study periods help maintain concentration
- **Regular Breaks**: Taking breaks prevents mental fatigue
- **Eye Rest**: Regular eye rest reduces digital eye strain
- **Structured Approach**: The structured cycle helps build consistent study habits

## Permissions

- **Foreground Service**: Required to run the timer in the background
- **Vibration**: Used for alarm notifications
- **Wake Lock**: Prevents the device from sleeping during timer operation
- **Notifications**: Displays timer status and alerts

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Inspired by productivity techniques and eye health research
- Built with modern Android development practices
