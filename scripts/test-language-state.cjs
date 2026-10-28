const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const script = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/persist-language-state.js'), 'utf8');
const listeners = {};
const timers = [];
const savedPairs = [];
const languages = { source: 'en', target: 'de' };
let mutationCallback;
const context = {
    document: {
        documentElement: {},
        querySelector(selector) {
            const language = selector.includes('translator-source-lang') ? 'source' : 'target';
            return { getAttribute() { return languages[language]; } };
        },
        addEventListener(event, callback) { listeners[event] = callback; }
    },
    MutationObserver: class {
        constructor(callback) { mutationCallback = callback; }
        observe() {}
    },
    window: {
        DeepLState: {
            postMessage(data) {
                const message = JSON.parse(data);
                if (message.type === 'languages') savedPairs.push([message.source, message.target]);
                else assert.deepEqual(message, { type: 'url' });
            }
        },
        location: { href: 'https://www.deepl.com/en/translator/l/en/de', pathname: '/en/translator/l/en/de' },
        history: { pushState() {}, replaceState() {} },
        addEventListener() {},
        setTimeout(callback) { timers.push(callback); },
        setInterval() {}
    }
};
context.window.top = context.window;
vm.createContext(context);
vm.runInContext(script, context);

function click(testId) {
    const element = {
        getAttribute() { return testId; },
        closest(selector) {
            return selector.includes(testId) ||
                (testId.startsWith('translator-lang-option-') && selector.includes('translator-lang-option-'))
                ? element : null;
        }
    };
    listeners.click({ target: element });
}

assert.equal(savedPairs.length, 0, 'initial URL pair is preserved');
timers.splice(0).forEach(callback => callback());
click('translator-source-lang-btn');
click('translator-lang-option-ja');
assert.equal(savedPairs.length, 0, 'source option must not be saved as a target');
languages.source = 'ja';
mutationCallback();
assert.deepEqual(savedPairs.at(-1), ['ja', 'de'], 'source change preserves target');
click('translator-target-lang-btn');
click('translator-lang-option-fr');
assert.equal(savedPairs.length, 1, 'wait for actual selected target');
languages.target = 'fr';
mutationCallback();
assert.deepEqual(savedPairs.at(-1), ['ja', 'fr'], 'target change preserves source');
timers.splice(0).forEach(callback => callback());
assert.equal(savedPairs.length, 2, 'unchanged pair is not saved again');
languages.source = 'fr';
languages.target = 'ja';
mutationCallback();
assert.deepEqual(savedPairs.at(-1), ['fr', 'ja'], 'swapping languages saves both');
vm.runInContext(script, context);
assert.equal(savedPairs.length, 3, 'installation is idempotent');
console.log('Language persistence regressions passed.');

// Unsupported WebViews and child frames must not install observers or timers.
for (const window of [{}, { DeepLState: {}, top: {} }]) {
    vm.runInNewContext(script, { window });
    assert.equal(window.__deeplViewerLanguageStateSaverInstalled, undefined);
}
