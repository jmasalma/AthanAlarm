/**
 * Qibla Compass - Device orientation and compass functionality
 */

class QiblaCompass {
    constructor() {
        this.canvas = null;
        this.ctx = null;
        this.isActive = false;
        this.currentHeading = 0;
        this.qiblaDirection = 0;
        this.animationId = null;
        this.permissionGranted = false;
        
        // Compass colors
        this.colors = {
            background: '#ffffff',
            border: '#e0e0e0',
            north: '#ff4444',
            qibla: '#2E7D32',
            text: '#333333',
            degrees: '#666666'
        };
    }

    /**
     * Initialize the compass
     */
    init(canvasId) {
        this.canvas = document.getElementById(canvasId);
        if (!this.canvas) {
            console.error('Canvas element not found');
            return false;
        }

        this.ctx = this.canvas.getContext('2d');
        
        // Setup canvas immediately
        this.setupCanvas();
        this.drawCompass();
        
        // Also setup with a delay to ensure proper sizing
        setTimeout(() => {
            this.setupCanvas();
            this.drawCompass();
        }, 100);
        
        // Add click handler to request permissions
        this.canvas.addEventListener('click', () => this.requestPermissions());
        
        // Add status click handler
        const statusElement = document.getElementById('compass-status');
        if (statusElement) {
            statusElement.addEventListener('click', () => this.requestPermissions());
        }
        
        return true;
    }

    /**
     * Setup canvas properties
     */
    setupCanvas() {
        const rect = this.canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;
        
        this.canvas.width = rect.width * dpr;
        this.canvas.height = rect.height * dpr;
        
        this.ctx.scale(dpr, dpr);
        this.canvas.style.width = rect.width + 'px';
        this.canvas.style.height = rect.height + 'px';
        
        this.centerX = rect.width / 2;
        this.centerY = rect.height / 2;
        this.radius = Math.max(Math.min(this.centerX, this.centerY) - 20, 50);
    }

    /**
     * Request device orientation permissions
     */
    async requestPermissions() {
        if (!this.isOrientationSupported()) {
            this.updateStatus('Device orientation not supported', 'error');
            return false;
        }

        try {
            // For iOS 13+ devices
            if (typeof DeviceOrientationEvent.requestPermission === 'function') {
                const permission = await DeviceOrientationEvent.requestPermission();
                if (permission === 'granted') {
                    this.startCompass();
                    return true;
                } else {
                    this.updateStatus('Permission denied', 'error');
                    return false;
                }
            } else {
                // For other devices
                this.startCompass();
                return true;
            }
        } catch (error) {
            console.error('Error requesting permissions:', error);
            this.updateStatus('Permission request failed', 'error');
            return false;
        }
    }

    /**
     * Check if device orientation is supported
     */
    isOrientationSupported() {
        return 'DeviceOrientationEvent' in window;
    }

    /**
     * Start compass functionality
     */
    startCompass() {
        if (this.isActive) return;

        this.isActive = true;
        this.permissionGranted = true;
        this.updateStatus('Compass active', 'active');

        // Add orientation event listener
        window.addEventListener('deviceorientation', (event) => {
            this.handleOrientation(event);
        });

        // Start animation loop
        this.animate();
    }

    /**
     * Stop compass functionality
     */
    stopCompass() {
        this.isActive = false;
        this.permissionGranted = false;
        
        window.removeEventListener('deviceorientation', this.handleOrientation);
        
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
        
        this.updateStatus('Tap to enable compass');
    }

    /**
     * Handle device orientation event
     */
    handleOrientation(event) {
        if (!this.isActive) return;

        // Get compass heading (alpha)
        let heading = event.alpha;
        
        if (heading !== null) {
            // Normalize heading to 0-360 degrees
            heading = (360 - heading) % 360;
            this.currentHeading = heading;
        }

        // Handle compass calibration for different browsers
        if (event.webkitCompassHeading) {
            // iOS Safari
            this.currentHeading = event.webkitCompassHeading;
        }
    }

    /**
     * Set Qibla direction
     */
    setQiblaDirection(direction) {
        this.qiblaDirection = direction;
        if (!this.isActive) {
            this.drawCompass();
        }
    }

    /**
     * Animation loop
     */
    animate() {
        if (!this.isActive) return;
        
        this.drawCompass();
        this.animationId = requestAnimationFrame(() => this.animate());
    }

    /**
     * Draw the compass
     */
    drawCompass() {
        if (!this.ctx || !this.canvas) return;

        // Ensure canvas is properly sized
        if (this.canvas.width === 0 || this.canvas.height === 0) {
            this.setupCanvas();
        }

        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw compass background
        this.drawCompassBackground();
        
        // Draw degree markers
        this.drawDegreeMarkers();
        
        // Draw cardinal directions
        this.drawCardinalDirections();
        
        // Draw Qibla indicator
        this.drawQiblaIndicator();
        
        // Draw north needle
        this.drawNorthNeedle();
        
        // Draw center dot
        this.drawCenter();
    }

    /**
     * Draw compass background
     */
    drawCompassBackground() {
        // Outer circle
        this.ctx.beginPath();
        this.ctx.arc(this.centerX, this.centerY, this.radius, 0, 2 * Math.PI);
        this.ctx.fillStyle = this.colors.background;
        this.ctx.fill();
        this.ctx.strokeStyle = this.colors.border;
        this.ctx.lineWidth = 3;
        this.ctx.stroke();

        // Inner circle
        this.ctx.beginPath();
        this.ctx.arc(this.centerX, this.centerY, this.radius - 30, 0, 2 * Math.PI);
        this.ctx.strokeStyle = this.colors.border;
        this.ctx.lineWidth = 1;
        this.ctx.stroke();
    }

    /**
     * Draw degree markers
     */
    drawDegreeMarkers() {
        this.ctx.strokeStyle = this.colors.degrees;
        this.ctx.lineWidth = 1;

        for (let i = 0; i < 360; i += 10) {
            const angle = (i - 90) * Math.PI / 180;
            const isMainMarker = i % 30 === 0;
            
            const outerRadius = this.radius - 5;
            const innerRadius = this.radius - (isMainMarker ? 20 : 10);
            
            const x1 = this.centerX + Math.cos(angle) * outerRadius;
            const y1 = this.centerY + Math.sin(angle) * outerRadius;
            const x2 = this.centerX + Math.cos(angle) * innerRadius;
            const y2 = this.centerY + Math.sin(angle) * innerRadius;
            
            this.ctx.beginPath();
            this.ctx.moveTo(x1, y1);
            this.ctx.lineTo(x2, y2);
            this.ctx.stroke();
            
            // Draw degree numbers for main markers
            if (isMainMarker) {
                const textRadius = this.radius - 35;
                const textX = this.centerX + Math.cos(angle) * textRadius;
                const textY = this.centerY + Math.sin(angle) * textRadius;
                
                this.ctx.fillStyle = this.colors.degrees;
                this.ctx.font = '12px monospace';
                this.ctx.textAlign = 'center';
                this.ctx.textBaseline = 'middle';
                this.ctx.fillText(i.toString(), textX, textY);
            }
        }
    }

    /**
     * Draw cardinal directions
     */
    drawCardinalDirections() {
        const directions = [
            { angle: 0, text: 'N', color: this.colors.north },
            { angle: 90, text: 'E', color: this.colors.text },
            { angle: 180, text: 'S', color: this.colors.text },
            { angle: 270, text: 'W', color: this.colors.text }
        ];

        directions.forEach(dir => {
            const angle = (dir.angle - 90 - this.currentHeading) * Math.PI / 180;
            const textRadius = this.radius - 50;
            const x = this.centerX + Math.cos(angle) * textRadius;
            const y = this.centerY + Math.sin(angle) * textRadius;
            
            this.ctx.fillStyle = dir.color;
            this.ctx.font = 'bold 16px sans-serif';
            this.ctx.textAlign = 'center';
            this.ctx.textBaseline = 'middle';
            this.ctx.fillText(dir.text, x, y);
        });
    }

    /**
     * Draw Qibla indicator
     */
    drawQiblaIndicator() {
        const qiblaAngle = (this.qiblaDirection - 90 - this.currentHeading) * Math.PI / 180;
        
        // Draw Qibla line
        this.ctx.strokeStyle = this.colors.qibla;
        this.ctx.lineWidth = 3;
        this.ctx.beginPath();
        this.ctx.moveTo(this.centerX, this.centerY);
        this.ctx.lineTo(
            this.centerX + Math.cos(qiblaAngle) * (this.radius - 30),
            this.centerY + Math.sin(qiblaAngle) * (this.radius - 30)
        );
        this.ctx.stroke();

        // Draw Qibla arrow
        const arrowSize = 15;
        const arrowX = this.centerX + Math.cos(qiblaAngle) * (this.radius - 15);
        const arrowY = this.centerY + Math.sin(qiblaAngle) * (this.radius - 15);
        
        this.ctx.fillStyle = this.colors.qibla;
        this.ctx.beginPath();
        this.ctx.moveTo(arrowX, arrowY);
        this.ctx.lineTo(
            arrowX - Math.cos(qiblaAngle - 0.5) * arrowSize,
            arrowY - Math.sin(qiblaAngle - 0.5) * arrowSize
        );
        this.ctx.lineTo(
            arrowX - Math.cos(qiblaAngle + 0.5) * arrowSize,
            arrowY - Math.sin(qiblaAngle + 0.5) * arrowSize
        );
        this.ctx.closePath();
        this.ctx.fill();

        // Draw Qibla label
        const labelRadius = this.radius - 70;
        const labelX = this.centerX + Math.cos(qiblaAngle) * labelRadius;
        const labelY = this.centerY + Math.sin(qiblaAngle) * labelRadius;
        
        this.ctx.fillStyle = this.colors.qibla;
        this.ctx.font = 'bold 12px sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.textBaseline = 'middle';
        this.ctx.fillText('QIBLA', labelX, labelY);
    }

    /**
     * Draw north needle
     */
    drawNorthNeedle() {
        const northAngle = (-90 - this.currentHeading) * Math.PI / 180;
        
        // Draw north needle
        this.ctx.strokeStyle = this.colors.north;
        this.ctx.fillStyle = this.colors.north;
        this.ctx.lineWidth = 2;
        
        const needleLength = 40;
        const needleX = this.centerX + Math.cos(northAngle) * needleLength;
        const needleY = this.centerY + Math.sin(northAngle) * needleLength;
        
        // Needle shaft
        this.ctx.beginPath();
        this.ctx.moveTo(this.centerX, this.centerY);
        this.ctx.lineTo(needleX, needleY);
        this.ctx.stroke();
        
        // Needle head
        const headSize = 8;
        this.ctx.beginPath();
        this.ctx.moveTo(needleX, needleY);
        this.ctx.lineTo(
            needleX - Math.cos(northAngle - 0.5) * headSize,
            needleY - Math.sin(northAngle - 0.5) * headSize
        );
        this.ctx.lineTo(
            needleX - Math.cos(northAngle + 0.5) * headSize,
            needleY - Math.sin(northAngle + 0.5) * headSize
        );
        this.ctx.closePath();
        this.ctx.fill();
    }

    /**
     * Draw center dot
     */
    drawCenter() {
        this.ctx.beginPath();
        this.ctx.arc(this.centerX, this.centerY, 5, 0, 2 * Math.PI);
        this.ctx.fillStyle = this.colors.text;
        this.ctx.fill();
    }

    /**
     * Update compass status
     */
    updateStatus(message, type = '') {
        const statusElement = document.getElementById('compass-status');
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.className = 'compass-status ' + type;
        }
    }

    /**
     * Format degrees to DMS (Degrees, Minutes, Seconds)
     */
    formatDMS(degrees) {
        const d = Math.floor(degrees);
        const m = Math.floor((degrees - d) * 60);
        const s = ((degrees - d - m / 60) * 3600).toFixed(1);
        return `${d}° ${m}' ${s}''`;
    }

    /**
     * Update dark mode colors
     */
    updateColors(isDark) {
        if (isDark) {
            this.colors = {
                background: '#2d2d2d',
                border: '#444444',
                north: '#ff6666',
                qibla: '#4CAF50',
                text: '#ffffff',
                degrees: '#aaaaaa'
            };
        } else {
            this.colors = {
                background: '#ffffff',
                border: '#e0e0e0',
                north: '#ff4444',
                qibla: '#2E7D32',
                text: '#333333',
                degrees: '#666666'
            };
        }
        
        if (!this.isActive) {
            this.drawCompass();
        }
    }
}

// Create global instance
window.qiblaCompass = new QiblaCompass();
