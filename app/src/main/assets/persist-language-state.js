(function () {
    if (!window.DeepLState || window !== window.top) return;
    if (window.__deeplViewerLanguageStateSaverInstalled === 4) return;
    window.__deeplViewerLanguageStateSaverInstalled = 4;

    let lastSavedUrl = '';
    let lastSavedLanguagePair = '';
    let lastObservedSourceLanguage = '';
    let lastObservedTargetLanguage = '';
    let lastLanguageInteractionAt = 0;

    function saveCurrentUrl() {
        const url = window.location.href;
        if (url === lastSavedUrl) return;
        lastSavedUrl = url;

        try {
            window.DeepLState.postMessage(JSON.stringify({ type: 'url' }));
        } catch (e) {
            // Android bridge may not be ready during very early document execution.
        }
    }

    function getLanguagePairFromPath() {
        const match = window.location.pathname.match(/\/translator\/l\/([^/]+)\/([^/?#]+)/);
        return match ? { source: match[1], target: match[2] } : null;
    }

    function getSelectedLanguage(selector) {
        const element = document.querySelector(selector);
        return element ? (element.getAttribute('dl-selected-lang') || '').trim() : '';
    }

    function recordSelectedLanguagesFromDom() {
        const sourceLanguage = getSelectedLanguage('[data-testid="translator-source-lang"]');
        const targetLanguage = getSelectedLanguage('[data-testid="translator-target-lang"]');
        if (!targetLanguage) return;

        lastObservedSourceLanguage = sourceLanguage;
        lastObservedTargetLanguage = targetLanguage;
    }

    function saveSelectedLanguagesFromDom() {
        const recentlyInteracted = Date.now() - lastLanguageInteractionAt < 10000;
        const pathLanguagePair = getLanguagePairFromPath();

        const sourceLanguage = getSelectedLanguage('[data-testid="translator-source-lang"]');
        const targetLanguage = getSelectedLanguage('[data-testid="translator-target-lang"]');
        if (!targetLanguage) return;

        const sourceLanguageChanged =
            lastObservedSourceLanguage !== '' && sourceLanguage !== lastObservedSourceLanguage;
        const targetLanguageChanged =
            lastObservedTargetLanguage !== '' && targetLanguage !== lastObservedTargetLanguage;
        lastObservedSourceLanguage = sourceLanguage;
        lastObservedTargetLanguage = targetLanguage;

        if (pathLanguagePair && !recentlyInteracted && !sourceLanguageChanged && !targetLanguageChanged) {
            if (
                targetLanguage.toLowerCase() === pathLanguagePair.target.toLowerCase() ||
                targetLanguage.toLowerCase() === 'en-us'
            ) {
                return;
            }
        }
        if (
            pathLanguagePair &&
            sourceLanguage.toLowerCase() === pathLanguagePair.source.toLowerCase() &&
            targetLanguage.toLowerCase() === pathLanguagePair.target.toLowerCase()
        ) {
            return;
        }

        const observedLanguagePair = sourceLanguage + '|' + targetLanguage;
        if (observedLanguagePair === lastSavedLanguagePair) return;
        lastSavedLanguagePair = observedLanguagePair;

        try {
            window.DeepLState.postMessage(JSON.stringify({ type: 'languages', source: sourceLanguage, target: targetLanguage }));
        } catch (e) {
            // Android bridge may not be ready during very early document execution.
        }
    }

    function saveCurrentState() {
        saveCurrentUrl();
        saveSelectedLanguagesFromDom();
    }

    function saveAfterNavigation() {
        window.setTimeout(saveCurrentState, 0);
    }

    ['pushState', 'replaceState'].forEach(function (methodName) {
        try {
            const originalMethod = window.history[methodName];
            window.history[methodName] = function () {
                const result = originalMethod.apply(this, arguments);
                saveAfterNavigation();
                return result;
            };
        } catch (e) {
            // Some WebView/DeepL contexts may reject history monkey-patching.
        }
    });

    window.addEventListener('popstate', saveAfterNavigation);
    window.addEventListener('hashchange', saveAfterNavigation);
    document.addEventListener('visibilitychange', saveCurrentState);
    document.addEventListener('click', function (event) {
        if (event.target.closest('[data-testid="translator-source-lang-btn"], [data-testid="translator-target-lang-btn"], [data-testid*="translator-source-lang"], [data-testid*="translator-target-lang"], [data-testid^="translator-lang-option-"]')) {
            lastLanguageInteractionAt = Date.now();
            window.setTimeout(saveSelectedLanguagesFromDom, 500);
            window.setTimeout(saveSelectedLanguagesFromDom, 1500);
        }
    }, true);

    const observer = new MutationObserver(saveSelectedLanguagesFromDom);
    observer.observe(document.documentElement, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['dl-selected-lang']
    });

    saveCurrentState();
    window.setTimeout(recordSelectedLanguagesFromDom, 1000);
    window.setTimeout(recordSelectedLanguagesFromDom, 3000);
    window.setTimeout(recordSelectedLanguagesFromDom, 7000);
    window.setInterval(saveSelectedLanguagesFromDom, 1000);
})();
