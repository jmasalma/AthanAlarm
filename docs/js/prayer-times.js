/**
 * Prayer Times Calculator - JavaScript port of JITL library
 * Based on the Java Islamic Tools and Libraries (JITL)
 */

class PrayerTimes {
    constructor() {
        // Constants
        this.KAABA_LAT = 21.423333;
        this.KAABA_LONG = 39.823333;
        this.DEG_TO_RAD = Math.PI / 180.0;
        this.RAD_TO_DEG = 180.0 / Math.PI;
        this.CENTER_OF_SUN_ANGLE = -0.8333;
        this.ALTITUDE_REFRACTION = 0.0347;
        
        // Prayer indices
        this.FAJR = 0;
        this.SUNRISE = 1;
        this.DHUHR = 2;
        this.ASR = 3;
        this.MAGHRIB = 4;
        this.ISHAA = 5;
        this.NEXT_FAJR = 6;
        
        // Calculation methods
        this.CALCULATION_METHODS = [
            { // Jafari
                fajrAngle: 16,
                ishaaAngle: 14,
                asrMethod: 0, // Shafi
                name: 'Jafari (Ithna Ashari)'
            },
            { // ISNA
                fajrAngle: 15,
                ishaaAngle: 15,
                asrMethod: 0, // Shafi
                name: 'Islamic Society of North America'
            },
            { // Muslim World League
                fajrAngle: 18,
                ishaaAngle: 17,
                asrMethod: 0, // Shafi
                name: 'Muslim World League'
            },
            { // Umm Al-Qura
                fajrAngle: 18.5,
                ishaaAngle: 0,
                ishaaInterval: 90, // 90 minutes after Maghrib
                asrMethod: 0, // Shafi
                name: 'Umm Al-Qurra, Saudi Arabia'
            },
            { // Egyptian Survey
                fajrAngle: 19.5,
                ishaaAngle: 17.5,
                asrMethod: 0, // Shafi
                name: 'Egyptian General Authority of Survey'
            },
            { // Karachi Hanafi
                fajrAngle: 18,
                ishaaAngle: 18,
                asrMethod: 1, // Hanafi
                name: 'Hanafi - Karachi University'
            },
            { // Dubai
                fajrAngle: 18.2,
                ishaaAngle: 18.2,
                asrMethod: 0, // Shafi
                name: 'Dubai'
            }
        ];
    }

    /**
     * Calculate prayer times for a given date and location
     */
    calculatePrayerTimes(date, latitude, longitude, timezone, methodIndex = 1) {
        const method = this.CALCULATION_METHODS[methodIndex];
        const julianDay = this.getJulianDay(date);
        
        // Get astronomical data
        const astro = this.getAstronomicalData(julianDay, latitude, longitude);
        
        // Calculate prayer times
        const times = this.computePrayerTimes(astro, latitude, longitude, timezone, method);
        
        // Format times
        const formattedTimes = this.formatPrayerTimes(times);
        
        // Find next prayer
        const nextPrayer = this.findNextPrayer(formattedTimes, new Date());
        
        return {
            times: formattedTimes,
            nextPrayer: nextPrayer,
            method: method.name,
            location: { latitude, longitude }
        };
    }

    /**
     * Get Julian Day number
     */
    getJulianDay(date) {
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();
        
        let a = Math.floor((14 - month) / 12);
        let y = year - a;
        let m = month + 12 * a - 3;
        
        return day + Math.floor((153 * m + 2) / 5) + 365 * y + 
               Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) + 1721119;
    }

    /**
     * Get astronomical data for the given Julian day
     */
    getAstronomicalData(julianDay, latitude, longitude) {
        const n = julianDay - 2451545.0;
        const L = (280.460 + 0.9856474 * n) % 360;
        const g = this.DEG_TO_RAD * ((357.528 + 0.9856003 * n) % 360);
        const lambda = this.DEG_TO_RAD * (L + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g));
        
        const R = 1.00014 - 0.01671 * Math.cos(g) - 0.00014 * Math.cos(2 * g);
        const alpha = Math.atan2(Math.cos(23.439 * this.DEG_TO_RAD) * Math.sin(lambda), Math.cos(lambda));
        const delta = Math.asin(Math.sin(23.439 * this.DEG_TO_RAD) * Math.sin(lambda));
        
        const equation = 4 * (L - alpha * this.RAD_TO_DEG);
        
        return {
            declination: delta,
            equation: equation,
            R: R
        };
    }

    /**
     * Compute prayer times
     */
    computePrayerTimes(astro, latitude, longitude, timezone, method) {
        const lat = latitude * this.DEG_TO_RAD;
        const dec = astro.declination;
        
        // Dhuhr (Solar noon)
        const dhuhr = 12 - astro.equation / 60 - longitude / 15 + timezone;
        
        // Sunrise and Maghrib
        const sunAngle = this.CENTER_OF_SUN_ANGLE * this.DEG_TO_RAD;
        const cosH = (Math.sin(sunAngle) - Math.sin(lat) * Math.sin(dec)) / 
                     (Math.cos(lat) * Math.cos(dec));
        
        let sunrise = 99, maghrib = 99;
        if (Math.abs(cosH) <= 1) {
            const H = Math.acos(cosH) * this.RAD_TO_DEG / 15;
            sunrise = dhuhr - H;
            maghrib = dhuhr + H;
        }
        
        // Fajr
        const fajrAngle = -method.fajrAngle * this.DEG_TO_RAD;
        const cosFajr = (Math.sin(fajrAngle) - Math.sin(lat) * Math.sin(dec)) / 
                        (Math.cos(lat) * Math.cos(dec));
        let fajr = 99;
        if (Math.abs(cosFajr) <= 1) {
            const HFajr = Math.acos(cosFajr) * this.RAD_TO_DEG / 15;
            fajr = dhuhr - HFajr;
        }
        
        // Asr
        const asrFactor = method.asrMethod === 1 ? 2 : 1; // Hanafi vs Shafi
        const tanAsr = asrFactor + Math.tan(Math.abs(lat - dec));
        const asrAngle = Math.atan(1 / tanAsr);
        const cosAsr = (Math.sin(asrAngle) - Math.sin(lat) * Math.sin(dec)) / 
                       (Math.cos(lat) * Math.cos(dec));
        let asr = 99;
        if (Math.abs(cosAsr) <= 1) {
            const HAsr = Math.acos(cosAsr) * this.RAD_TO_DEG / 15;
            asr = dhuhr + HAsr;
        }
        
        // Ishaa
        let ishaa = 99;
        if (method.ishaaInterval) {
            // Use interval after Maghrib
            if (maghrib !== 99) {
                ishaa = maghrib + method.ishaaInterval / 60;
            }
        } else {
            // Use angle
            const ishaaAngle = -method.ishaaAngle * this.DEG_TO_RAD;
            const cosIshaa = (Math.sin(ishaaAngle) - Math.sin(lat) * Math.sin(dec)) / 
                             (Math.cos(lat) * Math.cos(dec));
            if (Math.abs(cosIshaa) <= 1) {
                const HIshaa = Math.acos(cosIshaa) * this.RAD_TO_DEG / 15;
                ishaa = dhuhr + HIshaa;
            }
        }
        
        return [fajr, sunrise, dhuhr, asr, maghrib, ishaa];
    }

    /**
     * Format prayer times to HH:MM format
     */
    formatPrayerTimes(times) {
        const prayerNames = ['Fajr', 'Sunrise', 'Dhuhr', 'Asr', 'Maghrib', 'Ishaa'];
        const formatted = {};
        
        for (let i = 0; i < times.length; i++) {
            if (times[i] === 99) {
                formatted[prayerNames[i].toLowerCase()] = '--:--';
            } else {
                let time = times[i];
                
                // Handle times that go past midnight
                if (time < 0) time += 24;
                if (time >= 24) time -= 24;
                
                const hours = Math.floor(time);
                const minutes = Math.round((time - hours) * 60);
                
                formatted[prayerNames[i].toLowerCase()] = 
                    `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
            }
        }
        
        // Calculate next day Fajr (avoid recursion)
        if (this.lastLatitude !== undefined && this.lastLongitude !== undefined && this.lastTimezone !== undefined) {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            const julianDay = this.getJulianDay(tomorrow);
            const astro = this.getAstronomicalData(julianDay, this.lastLatitude, this.lastLongitude);
            const method = this.CALCULATION_METHODS[1]; // Default to ISNA
            const tomorrowTimes = this.computePrayerTimes(astro, this.lastLatitude, this.lastLongitude, this.lastTimezone, method);
            
            if (tomorrowTimes[0] !== 99) {
                let time = tomorrowTimes[0];
                if (time < 0) time += 24;
                if (time >= 24) time -= 24;
                const hours = Math.floor(time);
                const minutes = Math.round((time - hours) * 60);
                formatted['next-fajr'] = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
            } else {
                formatted['next-fajr'] = '--:--';
            }
        } else {
            formatted['next-fajr'] = '--:--';
        }
        
        return formatted;
    }

    /**
     * Find the next prayer time
     */
    findNextPrayer(times, currentTime) {
        const now = currentTime.getHours() + currentTime.getMinutes() / 60;
        const prayerOrder = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'ishaa'];
        
        for (let prayer of prayerOrder) {
            if (times[prayer] !== '--:--') {
                const [hours, minutes] = times[prayer].split(':').map(Number);
                const prayerTime = hours + minutes / 60;
                
                if (prayerTime > now) {
                    return prayer;
                }
            }
        }
        
        // If no prayer found for today, next prayer is tomorrow's Fajr
        return 'next-fajr';
    }

    /**
     * Calculate Qibla direction
     */
    calculateQiblaDirection(latitude, longitude) {
        const lat = latitude * this.DEG_TO_RAD;
        const lng = longitude * this.DEG_TO_RAD;
        const kaabaLat = this.KAABA_LAT * this.DEG_TO_RAD;
        const kaabaLng = this.KAABA_LONG * this.DEG_TO_RAD;
        
        const dLng = kaabaLng - lng;
        
        const y = Math.sin(dLng);
        const x = Math.cos(lat) * Math.tan(kaabaLat) - Math.sin(lat) * Math.cos(dLng);
        
        let qibla = Math.atan2(y, x) * this.RAD_TO_DEG;
        
        // Normalize to 0-360 degrees
        if (qibla < 0) qibla += 360;
        
        return qibla;
    }

    /**
     * Get Hijri date
     */
    getHijriDate(gregorianDate) {
        // Simple Hijri conversion (approximate)
        const gregorianYear = gregorianDate.getFullYear();
        const gregorianMonth = gregorianDate.getMonth() + 1;
        const gregorianDay = gregorianDate.getDate();
        
        // Convert to Julian Day
        const jd = this.getJulianDay(gregorianDate);
        
        // Convert Julian Day to Hijri (simplified algorithm)
        const hijriJD = jd - 1948439.5; // Hijri epoch
        const hijriYear = Math.floor(hijriJD / 354.367) + 1;
        const remainingDays = hijriJD - (hijriYear - 1) * 354.367;
        const hijriMonth = Math.floor(remainingDays / 29.531) + 1;
        const hijriDay = Math.floor(remainingDays - (hijriMonth - 1) * 29.531) + 1;
        
        const hijriMonths = [
            'Muharram', 'Safar', 'Rabi\' al-awwal', 'Rabi\' al-thani',
            'Jumada al-awwal', 'Jumada al-thani', 'Rajab', 'Sha\'ban',
            'Ramadan', 'Shawwal', 'Dhu al-Qi\'dah', 'Dhu al-Hijjah'
        ];
        
        return `${hijriDay} ${hijriMonths[Math.min(hijriMonth - 1, 11)]} ${hijriYear} AH`;
    }

    /**
     * Store last used coordinates for next day calculation
     */
    setLastLocation(latitude, longitude, timezone) {
        this.lastLatitude = latitude;
        this.lastLongitude = longitude;
        this.lastTimezone = timezone;
    }
}

// Create global instance
window.prayerTimes = new PrayerTimes();
