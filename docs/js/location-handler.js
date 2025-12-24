/**
 * Location Handler - Manages geolocation and location settings
 */

class LocationHandler {
    constructor() {
        this.currentLocation = null;
        this.watchId = null;
        this.settings = this.loadSettings();
        
        // Default location (Mecca) if no location available
        this.defaultLocation = {
            latitude: 21.4225,
            longitude: 39.8262,
            name: 'Mecca, Saudi Arabia'
        };
    }

    /**
     * Initialize location services
     */
    init() {
        // Try to get saved location first
        if (this.settings.location) {
            this.currentLocation = this.settings.location;
            this.updateLocationDisplay();
            this.calculateAndUpdateTimes();
        } else {
            // Try to get current location
            this.getCurrentLocation();
        }
    }

    /**
     * Get current location using browser geolocation
     */
    getCurrentLocation() {
        if (!navigator.geolocation) {
            console.error('Geolocation is not supported by this browser');
            this.useDefaultLocation();
            return;
        }

        this.showLoading(true);

        const options = {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000 // 5 minutes
        };

        navigator.geolocation.getCurrentPosition(
            (position) => this.onLocationSuccess(position),
            (error) => this.onLocationError(error),
            options
        );
    }

    /**
     * Handle successful location retrieval
     */
    onLocationSuccess(position) {
        this.currentLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            timestamp: Date.now()
        };

        // Get location name from coordinates
        this.getLocationName(this.currentLocation.latitude, this.currentLocation.longitude)
            .then(name => {
                this.currentLocation.name = name;
                this.updateLocationDisplay();
            })
            .catch(() => {
                this.currentLocation.name = `${this.currentLocation.latitude.toFixed(4)}, ${this.currentLocation.longitude.toFixed(4)}`;
                this.updateLocationDisplay();
            });

        this.saveSettings();
        this.calculateAndUpdateTimes();
        this.showLoading(false);
    }

    /**
     * Handle location retrieval error
     */
    onLocationError(error) {
        console.error('Location error:', error);
        
        let errorMessage = 'Location access denied';
        switch (error.code) {
            case error.PERMISSION_DENIED:
                errorMessage = 'Location access denied by user';
                break;
            case error.POSITION_UNAVAILABLE:
                errorMessage = 'Location information unavailable';
                break;
            case error.TIMEOUT:
                errorMessage = 'Location request timed out';
                break;
        }

        // Use saved location if available, otherwise use default
        if (this.settings.location) {
            this.currentLocation = this.settings.location;
            this.updateLocationDisplay();
            this.calculateAndUpdateTimes();
        } else {
            this.useDefaultLocation();
        }

        this.showLoading(false);
        this.showLocationError(errorMessage);
    }

    /**
     * Use default location (Mecca)
     */
    useDefaultLocation() {
        this.currentLocation = { ...this.defaultLocation };
        this.updateLocationDisplay();
        this.calculateAndUpdateTimes();
        this.saveSettings();
    }

    /**
     * Set manual location
     */
    setManualLocation(latitude, longitude) {
        if (!this.isValidCoordinate(latitude, longitude)) {
            alert('Please enter valid coordinates\nLatitude: -90 to 90\nLongitude: -180 to 180');
            return false;
        }

        this.showLoading(true);

        this.currentLocation = {
            latitude: parseFloat(latitude),
            longitude: parseFloat(longitude),
            manual: true,
            timestamp: Date.now()
        };

        // Get location name
        this.getLocationName(latitude, longitude)
            .then(name => {
                this.currentLocation.name = name;
                this.updateLocationDisplay();
                this.showLoading(false);
            })
            .catch(() => {
                this.currentLocation.name = `${latitude}, ${longitude}`;
                this.updateLocationDisplay();
                this.showLoading(false);
            });

        this.saveSettings();
        this.calculateAndUpdateTimes();
        
        // Clear manual input fields
        document.getElementById('manual-lat').value = '';
        document.getElementById('manual-lng').value = '';

        return true;
    }

    /**
     * Validate coordinates
     */
    isValidCoordinate(lat, lng) {
        const latitude = parseFloat(lat);
        const longitude = parseFloat(lng);
        
        return !isNaN(latitude) && !isNaN(longitude) &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180;
    }

    /**
     * Get location name from coordinates using reverse geocoding
     */
    async getLocationName(lat, lng) {
        try {
            // Using a free geocoding service
            const response = await fetch(
                `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=en`
            );
            
            if (response.ok) {
                const data = await response.json();
                if (data.city && data.countryName) {
                    return `${data.city}, ${data.countryName}`;
                } else if (data.locality && data.countryName) {
                    return `${data.locality}, ${data.countryName}`;
                } else if (data.countryName) {
                    return data.countryName;
                }
            }
        } catch (error) {
            console.error('Geocoding error:', error);
        }
        
        // Fallback to coordinates
        return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    }

    /**
     * Update location display in UI
     */
    updateLocationDisplay() {
        if (!this.currentLocation) return;

        const { latitude, longitude } = this.currentLocation;
        
        // Update coordinate displays
        document.getElementById('latitude').textContent = this.formatDMS(latitude, 'lat');
        document.getElementById('longitude').textContent = this.formatDMS(longitude, 'lng');
        
        // Calculate and display Qibla direction
        const qiblaDirection = window.prayerTimes.calculateQiblaDirection(latitude, longitude);
        document.getElementById('qibla-direction').textContent = this.formatDMS(qiblaDirection);
        
        // Update compass
        if (window.qiblaCompass) {
            window.qiblaCompass.setQiblaDirection(qiblaDirection);
        }
    }

    /**
     * Format coordinates to DMS (Degrees, Minutes, Seconds)
     */
    formatDMS(degrees, type = '') {
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

    /**
     * Calculate and update prayer times
     */
    calculateAndUpdateTimes() {
        if (!this.currentLocation) return;

        const { latitude, longitude } = this.currentLocation;
        const timezone = this.getTimezoneOffset();
        const methodIndex = parseInt(this.settings.calculationMethod || '1');
        
        // Store location for prayer calculations
        window.prayerTimes.setLastLocation(latitude, longitude, timezone);
        
        // Calculate prayer times
        const result = window.prayerTimes.calculatePrayerTimes(
            new Date(),
            latitude,
            longitude,
            timezone,
            methodIndex
        );

        // Update prayer times display
        this.updatePrayerTimesDisplay(result);
        
        // Update Hijri date
        this.updateHijriDate();
    }

    /**
     * Update prayer times in the UI
     */
    updatePrayerTimesDisplay(result) {
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

    /**
     * Update Hijri date display
     */
    updateHijriDate() {
        const hijriDate = window.prayerTimes.getHijriDate(new Date());
        const hijriElement = document.getElementById('hijri-date');
        if (hijriElement) {
            hijriElement.textContent = hijriDate;
        }
    }

    /**
     * Get timezone offset in hours
     */
    getTimezoneOffset() {
        return -new Date().getTimezoneOffset() / 60;
    }

    /**
     * Show/hide loading overlay
     */
    showLoading(show) {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.classList.toggle('active', show);
        }
    }

    /**
     * Show location error message
     */
    showLocationError(message) {
        // You could implement a toast notification here
        console.warn('Location error:', message);
    }

    /**
     * Load settings from localStorage
     */
    loadSettings() {
        try {
            const saved = localStorage.getItem('athanAlarmSettings');
            return saved ? JSON.parse(saved) : {};
        } catch (error) {
            console.error('Error loading settings:', error);
            return {};
        }
    }

    /**
     * Save settings to localStorage
     */
    saveSettings() {
        try {
            this.settings.location = this.currentLocation;
            this.settings.lastUpdated = Date.now();
            localStorage.setItem('athanAlarmSettings', JSON.stringify(this.settings));
        } catch (error) {
            console.error('Error saving settings:', error);
        }
    }

    /**
     * Update calculation method
     */
    updateCalculationMethod(methodIndex) {
        this.settings.calculationMethod = methodIndex;
        this.saveSettings();
        this.calculateAndUpdateTimes();
    }

    /**
     * Get current location data
     */
    getCurrentLocationData() {
        return this.currentLocation;
    }

    /**
     * Check if location is recent (within 1 hour)
     */
    isLocationRecent() {
        if (!this.currentLocation || !this.currentLocation.timestamp) {
            return false;
        }
        
        const oneHour = 60 * 60 * 1000;
        return (Date.now() - this.currentLocation.timestamp) < oneHour;
    }

    /**
     * Refresh location if needed
     */
    refreshLocationIfNeeded() {
        if (!this.isLocationRecent() && !this.currentLocation?.manual) {
            this.getCurrentLocation();
        }
    }
}

// Create global instance
window.locationHandler = new LocationHandler();
