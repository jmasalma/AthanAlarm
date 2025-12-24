/**
 * Simplified Athan Alarm App - Fixed version
 */

// Global state
let currentLocation = null;
let settingsOpen = false;

// Default location (Mecca)
const DEFAULT_LOCATION = {
    latitude: 21.4225,
    longitude: 39.8262,
    name: 'Mecca, Saudi Arabia'
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('Athan Alarm initializing...');
    
    // Hide loading overlay immediately
    hideLoading();
    
    // Initialize with default location
    initializeApp();
    
    // Setup event listeners
    setupEventListeners();
    
    console.log('Athan Alarm initialized successfully');
});

function initializeApp() {
    // Use default location immediately
    currentLocation = { ...DEFAULT_LOCATION };
    
    // Calculate and display prayer times
    calculateAndDisplayTimes();
    
    // Update location display
    updateLocationDisplay();
    
    // Initialize compass
    if (window.qiblaCompass) {
        window.qiblaCompass.init('compass-canvas');
    }
}

function setupEventListeners() {
    // Settings button
    const settingsBtn = document.querySelector('.settings-btn');
    if (settingsBtn) {
        settingsBtn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            toggleSettings();
        });
    }
    
    // Navigation buttons
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            const screen = e.target.getAttribute('data-screen');
            if (screen) {
                showScreen(screen);
            }
        });
    });
    
    // Settings panel close
    const settingsPanel = document.getElementById('settings-panel');
    if (settingsPanel) {
        settingsPanel.addEventListener('click', function(e) {
            if (e.target === settingsPanel) {
                toggleSettings();
            }
        });
    }
    
    // Manual location button
    const manualLocationBtn = document.querySelector('.location-inputs button');
    if (manualLocationBtn) {
        manualLocationBtn.addEventListener('click', setManualLocation);
    }
    
    // Get current location button
    const getCurrentLocationBtn = document.querySelector('button[onclick="getCurrentLocation()"]');
    if (getCurrentLocationBtn) {
        getCurrentLocationBtn.addEventListener('click', getCurrentLocation);
    }
}

function toggleSettings() {
    const settingsPanel = document.getElementById('settings-panel');
    if (settingsPanel) {
        settingsOpen = !settingsOpen;
        settingsPanel.classList.toggle('active', settingsOpen);
    }
}

function showScreen(screenName) {
    // Update navigation
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.getAttribute('data-screen') === screenName) {
            btn.classList.add('active');
        }
    });

    // Update screens
    document.querySelectorAll('.screen').forEach(screen => {
        screen.classList.remove('active');
    });

    const targetScreen = document.getElementById(`${screenName}-screen`);
    if (targetScreen) {
        targetScreen.classList.add('active');
    }
    
    // Redraw compass if switching to qibla screen
    if (screenName === 'qibla' && window.qiblaCompass) {
        setTimeout(() => {
            window.qiblaCompass.setupCanvas();
            window.qiblaCompass.drawCompass();
        }, 100);
    }
}

function calculateAndDisplayTimes() {
    if (!currentLocation || !window.prayerTimes) return;
    
    const { latitude, longitude } = currentLocation;
    const timezone = -new Date().getTimezoneOffset() / 60;
    
    // Store location for prayer calculations
    window.prayerTimes.setLastLocation(latitude, longitude, timezone);
    
    // Calculate prayer times
    const result = window.prayerTimes.calculatePrayerTimes(
        new Date(),
        latitude,
        longitude,
        timezone,
        1 // ISNA method
    );
    
    // Update prayer times display
    updatePrayerTimesDisplay(result);
    
    // Update Hijri date
    updateHijriDate();
}

function updatePrayerTimesDisplay(result) {
    const { times, nextPrayer } = result;
    
    // Update each prayer time
    Object.keys(times).forEach(prayer => {
        const element = document.querySelector(`[data-prayer="${prayer}"] .prayer-time`);
        if (element) {
            element.textContent = times[prayer];
        }
    });

    // Highlight next prayer
    document.querySelectorAll('.prayer-row').forEach(row => {
        row.classList.remove('next-prayer');
    });
    
    const nextPrayerRow = document.querySelector(`[data-prayer="${nextPrayer}"]`);
    if (nextPrayerRow) {
        nextPrayerRow.classList.add('next-prayer');
    }
}

function updateHijriDate() {
    if (!window.prayerTimes) return;
    
    const hijriDate = window.prayerTimes.getHijriDate(new Date());
    const hijriElement = document.getElementById('hijri-date');
    if (hijriElement) {
        hijriElement.textContent = hijriDate;
    }
}

function updateLocationDisplay() {
    if (!currentLocation) return;

    const { latitude, longitude } = currentLocation;
    
    // Update coordinate displays
    document.getElementById('latitude').textContent = formatDMS(latitude, 'lat');
    document.getElementById('longitude').textContent = formatDMS(longitude, 'lng');
    
    // Calculate and display Qibla direction
    const qiblaDirection = window.prayerTimes.calculateQiblaDirection(latitude, longitude);
    document.getElementById('qibla-direction').textContent = formatDMS(qiblaDirection);
    
    // Update compass
    if (window.qiblaCompass) {
        window.qiblaCompass.setQiblaDirection(qiblaDirection);
    }
}

function formatDMS(degrees, type = '') {
    const abs = Math.abs(degrees);
    const d = Math.floor(abs);
    const m = Math.floor((abs - d) * 60);
    const s = ((abs - d - m / 60) * 3600).toFixed(1);
    
    let direction = '';
    if (type === 'lat') {
        direction = degrees >= 0 ? 'N' : 'S';
    } else if (type === 'lng') {
        direction = degrees >= 0 ? 'E' : 'W';
    }
    
    return `${d}° ${m}' ${s}'' ${direction}`.trim();
}

function setManualLocation() {
    const latInput = document.getElementById('manual-lat');
    const lngInput = document.getElementById('manual-lng');
    
    if (!latInput || !lngInput) return;

    const latitude = parseFloat(latInput.value.trim());
    const longitude = parseFloat(lngInput.value.trim());

    if (isNaN(latitude) || isNaN(longitude)) {
        alert('Please enter valid numbers for latitude and longitude');
        return;
    }
    
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
        alert('Please enter valid coordinates\nLatitude: -90 to 90\nLongitude: -180 to 180');
        return;
    }

    currentLocation = {
        latitude: latitude,
        longitude: longitude,
        manual: true,
        name: `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
    };

    calculateAndDisplayTimes();
    updateLocationDisplay();
    
    // Clear input fields
    latInput.value = '';
    lngInput.value = '';
    
    // Close settings
    toggleSettings();
}

function getCurrentLocation() {
    if (!navigator.geolocation) {
        alert('Geolocation is not supported by this browser');
        return;
    }

    showLoading();

    navigator.geolocation.getCurrentPosition(
        function(position) {
            currentLocation = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude,
                name: `${position.coords.latitude.toFixed(4)}, ${position.coords.longitude.toFixed(4)}`
            };
            
            calculateAndDisplayTimes();
            updateLocationDisplay();
            hideLoading();
        },
        function(error) {
            console.error('Location error:', error);
            hideLoading();
            alert('Could not get your location. Using default location (Mecca).');
        },
        {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000
        }
    );
}

function showLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) {
        overlay.classList.add('active');
    }
}

function hideLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) {
        overlay.classList.remove('active');
    }
}

// Global functions for HTML onclick handlers
window.toggleSettings = toggleSettings;
window.showScreen = showScreen;
window.setManualLocation = setManualLocation;
window.getCurrentLocation = getCurrentLocation;
