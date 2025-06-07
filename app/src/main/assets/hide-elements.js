(function () {
    const HIDE_SELECTORS = `
        header,
        nav[class^="md"],
        h2#translation-modes-heading + div > div:first-child,
        div[data-testid="translator"] span:nth-child(3),
        footer,
        div[data-testid="chrome-extension-toast"],
        div[data-testid="firefox-extension-toast"],
        div[data-testid="write-banner"],
        div[id="cookieBanner"],
        div[aria-labelledby="app-stores-banner-description"],
        aside
    `;

    function injectCSS(rules) {
        const style = document.createElement('style');
        style.innerHTML = rules;
        document.head.appendChild(style);
    }

    /* wait for the Head element to be available */
    function waitForHead(callback) {
        if (document.head) {
            callback();
            return;
        }

        const observer = new MutationObserver(function (mutations) {
            if (document.head) {
                observer.disconnect();
                callback();
            }
        });

        observer.observe(document, {
            childList: true,
            subtree: true
        });
    }

    /* initially hide elements with `visibility: hidden` */
    window.hideElementsInitially = function () {
        waitForHead(function () {
            injectCSS(HIDE_SELECTORS + ` { visibility: hidden !important; }`);
        });
    };

    /* finally hide elements with `display: none` */
    window.hideElementsFinal = function () {
        injectCSS(`
            * { -webkit-tap-highlight-color: rgba(0, 0, 0,.1); }
            ${HIDE_SELECTORS} {
                display: none !important;
                height: 0;
                width: 0;
                overflow: hidden;
            }
        `);
    };
})();