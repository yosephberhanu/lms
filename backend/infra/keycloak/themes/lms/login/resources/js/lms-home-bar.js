/**
 * Injects a top bar with "Back to home" so the Keycloak login page matches the gateway landing UX.
 * Same-origin "/" is served by nginx as the LMS homepage.
 */
(function () {
  function inject() {
    if (document.getElementById('lms-back-home')) {
      return;
    }
    var bar = document.createElement('header');
    bar.id = 'lms-back-home';
    bar.setAttribute('aria-label', 'Site navigation');

    var inner = document.createElement('div');
    inner.className = 'lms-back-home__inner';

    var link = document.createElement('a');
    link.className = 'lms-back-home__link';
    link.href = '/';
    link.setAttribute('rel', 'home');
    link.textContent = '← Back to home';

    var brand = document.createElement('span');
    brand.className = 'lms-back-home__brand';
    brand.textContent = 'LMS · Lease Management';

    inner.appendChild(link);
    inner.appendChild(brand);
    bar.appendChild(inner);

    if (document.body) {
      document.body.insertBefore(bar, document.body.firstChild);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inject);
  } else {
    inject();
  }
})();
