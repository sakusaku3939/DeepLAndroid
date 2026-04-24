(function () {
    const HIDE_SELECTORS = [
        'header',
        'nav[class^="md"]',
        'h2#translation-modes-heading + div > div:first-child',
        'div[data-testid="translator"] > div:not(:first-child)',
        'footer',
        'div[data-testid="chrome-extension-toast"]',
        'div[data-testid="firefox-extension-toast"]',
        'div[data-testid="write-banner"]',
        'div[id="cookieBanner"]',
        'div[aria-labelledby="app-stores-banner-description"]',
        'aside',
    ].join(',');

    const style = document.createElement('style');
    style.innerHTML = `
        * { -webkit-tap-highlight-color: rgba(0, 0, 0, .1); }
        ${HIDE_SELECTORS} { display: none !important; height: 0; width: 0; overflow: hidden; }
    `;
    (document.head || document.documentElement).appendChild(style);
})();
