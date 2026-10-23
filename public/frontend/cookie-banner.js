(function () {
  if (localStorage.getItem('fixmycity_cookie_consent')) return;

  const banner = document.createElement('div');
  banner.id = 'cookie-banner';
  banner.innerHTML = `
    <div class="cookie-banner-inner">
      <div class="cookie-banner-text">
        <strong>Wir nutzen Cookies</strong>
        <p>Diese Seite verwendet Cookies, um die Nutzererfahrung zu verbessern und den Betrieb der App sicherzustellen. Mit „Alle zulassen" stimmst du der Nutzung aller Cookies zu.</p>
      </div>
      <div class="cookie-banner-actions">
        <button class="cookie-btn cookie-btn--accept" id="cookie-accept">Alle zulassen</button>
        <button class="cookie-btn cookie-btn--necessary" id="cookie-necessary">Nur notwendige</button>
      </div>
    </div>
  `;
  document.body.appendChild(banner);

  function dismiss(choice) {
    localStorage.setItem('fixmycity_cookie_consent', choice);
    banner.classList.add('cookie-banner--hidden');
    setTimeout(() => banner.remove(), 350);
  }

  document.getElementById('cookie-accept').addEventListener('click', () => dismiss('all'));
  document.getElementById('cookie-necessary').addEventListener('click', () => dismiss('necessary'));
})();
