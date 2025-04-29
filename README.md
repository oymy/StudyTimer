# Study Timer App

A modern Android application designed to help you maintain productive study sessions with regular eye rest breaks.

## Features

- **Customizable Study Sessions**: Default 90-minute study sessions followed by 20-minute breaks
- **Eye Rest Reminders**: Random alarms every 3-5 minutes during study sessions to remind you to rest your eyes
- **10-Second Eye Rest**: Brief eye rest periods after each alarm to reduce eye strain
- **Background Operation**: Continues to run when the app is in the background or the screen is off
- **Modern UI**: Built with Jetpack Compose with a clean, intuitive interface
- **Visual Progress**: Circular progress indicator to visualize remaining time

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
