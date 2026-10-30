const COOKIE_NOTICE_KEY = 'fixmycity-cookie-notice-dismissed';

document.addEventListener('DOMContentLoaded', () => {
  if (localStorage.getItem(COOKIE_NOTICE_KEY) === 'true') return;

  const banner = document.createElement('aside');
  banner.className = 'cookie-banner';
  banner.setAttribute('role', 'region');
  banner.setAttribute('aria-label', 'Cookie-Hinweis');

  const message = document.createElement('p');
  message.textContent =
    'Diese Website verwendet nur technisch notwendige Cookies für Anmeldung, Sitzung und Sicherheit. Es werden keine Tracking-Cookies eingesetzt.';

  const dismissButton = document.createElement('button');
  dismissButton.type = 'button';
  dismissButton.className = 'cookie-banner-dismiss';
  dismissButton.textContent = 'Verstanden';
  dismissButton.addEventListener('click', () => {
    localStorage.setItem(COOKIE_NOTICE_KEY, 'true');
    banner.remove();
  });

  banner.append(message, dismissButton);
  document.body.append(banner);
});
