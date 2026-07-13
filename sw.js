const CACHE = 'court-tracker-v9';
const HTML = './tennis_tracker.html';

self.addEventListener('install', function(e) {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE).then(function(c) {
      return c.addAll([HTML, './manifest.json', './icon.svg']);
    })
  );
});

self.addEventListener('activate', function(e) {
  e.waitUntil(
    caches.keys().then(function(keys) {
      return Promise.all(keys.filter(function(k) { return k !== CACHE; }).map(function(k) { return caches.delete(k); }));
    }).then(function() { return clients.claim(); })
  );
});

self.addEventListener('fetch', function(e) {
  var url = e.request.url;

  // Never intercept Firebase Auth or Google OAuth URLs — they carry redirect tokens
  // that must reach the network unchanged for signInWithRedirect to work.
  if (
    url.indexOf('/__/auth/') !== -1 ||
    url.indexOf('firebaseapp.com') !== -1 ||
    url.indexOf('accounts.google.com') !== -1 ||
    url.indexOf('googleapis.com') !== -1
  ) {
    return;
  }

  // Network-first for the HTML — always get the latest, fall back to cache
  if (url.indexOf('tennis_tracker.html') !== -1 || url === self.location.origin + '/tennis-score-analytics/') {
    e.respondWith(
      fetch(e.request).then(function(res) {
        var clone = res.clone();
        caches.open(CACHE).then(function(c) { c.put(e.request, clone); });
        return res;
      }).catch(function() {
        return caches.match(e.request);
      })
    );
    return;
  }

  // Cache-first for everything else
  e.respondWith(
    caches.match(e.request).then(function(r) {
      return r || fetch(e.request);
    })
  );
});
