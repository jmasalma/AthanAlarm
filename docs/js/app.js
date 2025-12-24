/**
 * Main Application - Coordinates all components and handles UI interactions
 */

class AthanAlarmApp {
    constructor() {
        this.currentScreen = 'today';
        this.settingsOpen = false;
        this.updateInterval = null;
    }

    /**
     * Initialize the application
     */
    init() {
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.onDOMReady());
        } else {
            this.onDOMReady();
        }
    }

    /**
     * Handle DOM ready event
     */
    onDOMReady() {
        console.log('Athan Alarm Web App initializing...');
        
        // Initialize components
        this.initializeComponents();
        
        // Setup event listeners
        this.setupEventListeners();
        
        // Load saved settings
        this.loadSettings();
        
        // Start periodic updates
        this.startPeriodicUpdates();
        
        console.log('Athan Alarm Web App initialized successfully');
    }

    /**
     * Initialize all components
     */
    initializeComponents() {
        // Initialize location handler
        if (window.locationHandler) {
            window.locationHandler.init();
        }

        // Initialize compass
        if (window.qiblaCompass) {
            window.qiblaCompass.init('compass-canvas');
        }

        // Set initial screen
        this.showScreen('today');
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        // Navigation buttons
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const screen = e.target.getAttribute('data-screen');
                if (screen) {
                    this.showScreen(screen);
                }
            });
        });

        // Settings panel
        const settingsBtn = document.querySelector('.settings-btn');
        if (settingsBtn) {
            settingsBtn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.toggleSettings();
            });
        }

        // Settings panel close (click outside)
        const settingsPanel = document.getElementById('settings-panel');
        if (settingsPanel) {
            settingsPanel.addEventListener('click', (e) => {
                if (e.target === settingsPanel) {
                    this.toggleSettings();
                }
            });
        }

        // Calculation method change
        const calculationSelect = document.getElementById('calculation-method');
        if (calculationSelect) {
            calculationSelect.addEventListener('change', (e) => {
                this.updateCalculationMethod(e.target.value);
            });
        }

        // Manual location button
        const manualLocationBtn = document.querySelector('.location-inputs button');
        if (manualLocationBtn) {
            manualLocationBtn.addEventListener('click', () => this.setManualLocation());
        }

        // Get current location button
        const getCurrentLocationBtn = document.querySelector('button[onclick="getCurrentLocation()"]');
        if (getCurrentLocationBtn) {
            getCurrentLocationBtn.onclick = () => this.getCurrentLocation();
        }

        // Handle Enter key in manual location inputs
        document.getElementById('manual-lat')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.setManualLocation();
        });
        document.getElementById('manual-lng')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.setManualLocation();
        });

        // Handle visibility change (tab switching)
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                this.onAppVisible();
            }
        });

        // Handle orientation change
        window.addEventListener('orientationchange', () => {
            setTimeout(() => this.onOrientationChange(), 100);
        });

        // Handle resize
        window.addEventListener('resize', () => this.onResize());
    }

    /**
     * Show specific screen
     */
    showScreen(screenName) {
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
            this.currentScreen = screenName;
        }

        // Handle screen-specific actions
        this.onScreenChange(screenName);
    }

    /**
     * Handle screen change
     */
    onScreenChange(screenName) {
        if (screenName === 'qibla') {
            // Refresh compass when switching to qibla screen
            if (window.qiblaCompass) {
                setTimeout(() => {
                    window.qiblaCompass.setupCanvas();
                    window.qiblaCompass.drawCompass();
                }, 100);
                // Also try immediate draw
                window.qiblaCompass.setupCanvas();
                window.qiblaCompass.drawCompass();
            }
        }
    }

    /**
     * Toggle settings panel
     */
    toggleSettings() {
        const settingsPanel = document.getElementById('settings-panel');
        if (settingsPanel) {
            this.settingsOpen = !this.settingsOpen;
            settingsPanel.classList.toggle('active', this.settingsOpen);
        }
    }

    /**
     * Update calculation method
     */
    updateCalculationMethod(methodIndex) {
        if (window.locationHandler) {
            window.locationHandler.updateCalculationMethod(methodIndex);
        }
    }

    /**
     * Set manual location
     */
    setManualLocation() {
        const latInput = document.getElementById('manual-lat');
        const lngInput = document.getElementById('manual-lng');
        
        if (!latInput || !lngInput) return;

        const latitude = latInput.value.trim();
        const longitude = lngInput.value.trim();

        if (!latitude || !longitude) {
            alert('Please enter both latitude and longitude');
            return;
        }

        if (window.locationHandler) {
            const success = window.locationHandler.setManualLocation(latitude, longitude);
            if (success) {
                this.toggleSettings(); // Close settings panel
            }
        }
    }

    /**
     * Get current location
     */
    getCurrentLocation() {
        if (window.locationHandler) {
            window.locationHandler.getCurrentLocation();
        }
    }

    /**
     * Load saved settings
     */
    loadSettings() {
        try {
            const saved = localStorage.getItem('athanAlarmSettings');
            if (saved) {
                const settings = JSON.parse(saved);
                
                // Set calculation method
                const calculationSelect = document.getElementById('calculation-method');
                if (calculationSelect && settings.calculationMethod) {
                    calculationSelect.value = settings.calculationMethod;
                }
            }
        } catch (error) {
            console.error('Error loading settings:', error);
        }
    }

    /**
     * Start periodic updates
     */
    startPeriodicUpdates() {
        // Update every minute
        this.updateInterval = setInterval(() => {
            this.periodicUpdate();
        }, 60000);

        // Initial update
        setTimeout(() => this.periodicUpdate(), 1000);
    }

    /**
     * Periodic update function
     */
    periodicUpdate() {
        // Update prayer times if location is available
        if (window.locationHandler && window.locationHandler.getCurrentLocationData()) {
            window.locationHandler.calculateAndUpdateTimes();
        }

        // Refresh location if needed (not manual and old)
        if (window.locationHandler) {
            window.locationHandler.refreshLocationIfNeeded();
        }
    }

    /**
     * Handle app becoming visible (tab focus)
     */
    onAppVisible() {
        // Refresh data when app becomes visible
        this.periodicUpdate();
        
        // Restart compass if on qibla screen
        if (this.currentScreen === 'qibla' && window.qiblaCompass) {
            window.qiblaCompass.drawCompass();
        }
    }

    /**
     * Handle orientation change
     */
    onOrientationChange() {
        // Redraw compass after orientation change
        if (this.currentScreen === 'qibla' && window.qiblaCompass) {
            window.qiblaCompass.setupCanvas();
            window.qiblaCompass.drawCompass();
        }
    }

    /**
     * Handle window resize
     */
    onResize() {
        // Redraw compass on resize
        if (this.currentScreen === 'qibla' && window.qiblaCompass) {
            setTimeout(() => {
                window.qiblaCompass.setupCanvas();
                window.qiblaCompass.drawCompass();
            }, 100);
        }
    }

    /**
     * Cleanup when app is destroyed
     */
    destroy() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }

        if (window.qiblaCompass) {
            window.qiblaCompass.stopCompass();
        }
    }
}

// Global functions for HTML onclick handlers
window.toggleSettings = function() {
    if (window.app) {
        window.app.toggleSettings();
    }
};

window.showScreen = function(screenName) {
    if (window.app) {
        window.app.showScreen(screenName);
    }
};

window.setManualLocation = function() {
    if (window.app) {
        window.app.setManualLocation();
    }
};

window.getCurrentLocation = function() {
    if (window.app) {
        window.app.getCurrentLocation();
    }
};

// Initialize app when script loads
window.app = new AthanAlarmApp();
window.app.init();

// Handle page unload
window.addEventListener('beforeunload', () => {
    if (window.app) {
        window.app.destroy();
    }
});

// Service Worker registration for offline support (if available)
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js')
            .then(registration => {
                console.log('SW registered: ', registration);
            })
            .catch(registrationError => {
                console.log('SW registration failed: ', registrationError);
            });
    });
}

// Handle install prompt for PWA
let deferredPrompt;
window.addEventListener('beforeinstallprompt', (e) => {
    // Prevent Chrome 67 and earlier from automatically showing the prompt
    e.preventDefault();
    // Stash the event so it can be triggered later
    deferredPrompt = e;
    
    // You could show an install button here
    console.log('App can be installed');
});

// Handle app installation
window.addEventListener('appinstalled', (evt) => {
    console.log('App was installed');
});
