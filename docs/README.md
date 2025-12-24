# Athan Alarm Web App

A web-based Islamic prayer times calculator and Qibla direction finder, ported from the Android AthanAlarm app.

## Features

- **Prayer Times Calculation**: Accurate prayer times using multiple calculation methods
- **Qibla Compass**: Interactive compass showing direction to Mecca with device orientation support
- **Location Services**: Automatic location detection or manual coordinate input
- **Multiple Calculation Methods**: Support for 7 different Islamic calculation methods
- **Hijri Calendar**: Display of Islamic date
- **Responsive Design**: Optimized for mobile and desktop devices
- **Offline Support**: Progressive Web App with service worker caching
- **Dark Mode**: Automatic dark mode support based on system preferences

## Calculation Methods

1. **Jafari (Ithna Ashari)** - Fajr: 16°, Ishaa: 14°
2. **Islamic Society of North America (ISNA)** - Fajr: 15°, Ishaa: 15°
3. **Muslim World League** - Fajr: 18°, Ishaa: 17°
4. **Umm Al-Qurra, Saudi Arabia** - Fajr: 18.5°, Ishaa: 90 min after Maghrib
5. **Egyptian General Authority of Survey** - Fajr: 19.5°, Ishaa: 17.5°
6. **Hanafi - Karachi University** - Fajr: 18°, Ishaa: 18° (Hanafi Asr)
7. **Dubai** - Fajr: 18.2°, Ishaa: 18.2°

## Technology Stack

- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Prayer Calculations**: JavaScript port of JITL (Java Islamic Tools and Libraries)
- **Compass**: Canvas API with DeviceOrientationEvent
- **Location**: Geolocation API with reverse geocoding
- **PWA**: Service Worker, Web App Manifest
- **Hosting**: GitHub Pages compatible

## Usage

### Online
Visit the live application at: `https://[username].github.io/AthanAlarm/`

### Local Development
1. Clone the repository
2. Navigate to the `docs` folder
3. Serve the files using a local web server:
   ```bash
   # Using Python
   python -m http.server 8000
   
   # Using Node.js
   npx serve .
   
   # Using PHP
   php -S localhost:8000
   ```
4. Open `http://localhost:8000` in your browser

## Browser Support

- **Modern Browsers**: Chrome 60+, Firefox 55+, Safari 11+, Edge 79+
- **Mobile**: iOS Safari 11+, Chrome Mobile 60+
- **Features**:
  - Geolocation API
  - Canvas API
  - DeviceOrientationEvent (for compass)
  - Service Workers (for offline support)
  - Web App Manifest (for PWA features)

## Permissions

The app may request the following permissions:
- **Location**: For automatic prayer time calculation based on your location
- **Device Orientation**: For compass functionality on mobile devices
- **Notifications**: For prayer time alerts (future feature)

## Privacy

- Location data is stored locally in your browser
- No personal data is sent to external servers
- Reverse geocoding uses a privacy-friendly service
- All calculations are performed locally

## Accuracy Disclaimer

Prayer times are calculated using astronomical algorithms and may not be 100% accurate. Please verify with your local mosque or Islamic authority for the most accurate prayer times in your area.

## Credits

- **Original Android App**: [AthanAlarm](https://github.com/jmasalma/AthanAlarm)
- **Prayer Time Library**: Based on JITL (Java Islamic Tools and Libraries)
- **Calculation Methods**: Various Islamic organizations and authorities
- **Geocoding**: BigDataCloud API

## License

This project follows the same license as the original AthanAlarm Android application.

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## Changelog

### v1.0.0
- Initial web version release
- Prayer times calculation with 7 methods
- Qibla compass with device orientation
- Location services and manual input
- Responsive design and PWA support
- Offline functionality with service worker
