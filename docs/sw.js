/**
 * Service Worker for Athan Alarm
 * Provides offline support and caching
 */

const CACHE_NAME = 'athan-alarm-v1.0.0';
const urlsToCache = [
    './',
    './index.html',
    './css/style.css',
    './css/compass.css',
    './js/prayer-times.js',
    './js/qibla-compass.js',
    './js/location-handler.js',
    './js/app.js',
    './manifest.json'
];

// Install event - cache resources
self.addEventListener('install', (event) => {
    console.log('Service Worker installing...');
    
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('Caching app shell');
                return cache.addAll(urlsToCache);
            })
            .then(() => {
                // Force the waiting service worker to become the active service worker
                return self.skipWaiting();
            })
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    console.log('Service Worker activating...');
    
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cacheName) => {
                    if (cacheName !== CACHE_NAME) {
                        console.log('Deleting old cache:', cacheName);
                        return caches.delete(cacheName);
                    }
                })
            );
        }).then(() => {
            // Ensure the new service worker takes control immediately
            return self.clients.claim();
        })
    );
});

// Fetch event - serve from cache, fallback to network
self.addEventListener('fetch', (event) => {
    // Skip non-GET requests
    if (event.request.method !== 'GET') {
        return;
    }

    // Skip external requests (geocoding API, etc.)
    if (!event.request.url.startsWith(self.location.origin)) {
        return;
    }

    event.respondWith(
        caches.match(event.request)
            .then((response) => {
                // Return cached version if available
                if (response) {
                    return response;
                }

                // Clone the request because it's a stream
                const fetchRequest = event.request.clone();

                return fetch(fetchRequest).then((response) => {
                    // Check if we received a valid response
                    if (!response || response.status !== 200 || response.type !== 'basic') {
                        return response;
                    }

                    // Clone the response because it's a stream
                    const responseToCache = response.clone();

                    caches.open(CACHE_NAME)
                        .then((cache) => {
                            cache.put(event.request, responseToCache);
                        });

                    return response;
                }).catch(() => {
                    // If both cache and network fail, return a custom offline page
                    if (event.request.destination === 'document') {
                        return caches.match('./index.html');
                    }
                });
            })
    );
});

// Background sync for prayer time updates
self.addEventListener('sync', (event) => {
    if (event.tag === 'prayer-times-sync') {
        event.waitUntil(
            // Update prayer times when back online
            updatePrayerTimes()
        );
    }
});

// Push notifications (for future prayer time alerts)
self.addEventListener('push', (event) => {
    if (event.data) {
        const data = event.data.json();
        const options = {
            body: data.body,
            icon: './assets/icon-192.png',
            badge: './assets/icon-192.png',
            vibrate: [200, 100, 200],
            data: data.data,
            actions: [
                {
                    action: 'view',
                    title: 'View Prayer Times'
                },
                {
                    action: 'dismiss',
                    title: 'Dismiss'
                }
            ]
        };

        event.waitUntil(
            self.registration.showNotification(data.title, options)
        );
    }
});

// Handle notification clicks
self.addEventListener('notificationclick', (event) => {
    event.notification.close();

    if (event.action === 'view') {
        event.waitUntil(
            clients.openWindow('./')
        );
    }
});

// Message handling for communication with main app
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

// Helper function to update prayer times
async function updatePrayerTimes() {
    try {
        // This would typically sync with a server or update local calculations
        console.log('Updating prayer times in background...');
        
        // For now, just log that we would update
        // In a full implementation, you might:
        // 1. Get stored location from IndexedDB
        // 2. Calculate new prayer times
        // 3. Store updated times
        // 4. Send notification if prayer time is approaching
        
        return Promise.resolve();
    } catch (error) {
        console.error('Failed to update prayer times:', error);
        return Promise.reject(error);
    }
}

// Periodic background sync (if supported)
self.addEventListener('periodicsync', (event) => {
    if (event.tag === 'prayer-times-update') {
        event.waitUntil(updatePrayerTimes());
    }
});

// Handle errors
self.addEventListener('error', (event) => {
    console.error('Service Worker error:', event.error);
});

self.addEventListener('unhandledrejection', (event) => {
    console.error('Service Worker unhandled rejection:', event.reason);
});

console.log('Service Worker loaded');
