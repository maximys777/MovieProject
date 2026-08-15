// --- ЯЗЫК ИНТЕРФЕЙСА ---
// Выбор языка хранится в cookie браузера. Язык по умолчанию — английский.
const LANG_COOKIE = 'app_language';
const SUPPORTED_LANGS = ['en', 'ru', 'uk'];
const DEFAULT_LANG = 'en';
const LANG_COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 год

function readLangCookie() {
    const raw = document.cookie.split('; ').find(row => row.startsWith(`${LANG_COOKIE}=`));
    if (!raw) return null;
    const value = decodeURIComponent(raw.substring(LANG_COOKIE.length + 1));
    return SUPPORTED_LANGS.includes(value) ? value : null;
}

function writeLangCookie(lang) {
    document.cookie = `${LANG_COOKIE}=${encodeURIComponent(lang)}; path=/; max-age=${LANG_COOKIE_MAX_AGE}; SameSite=Lax`;
}

let currentLang = readLangCookie() || DEFAULT_LANG;

// Перевод по ключу. Если ключа нет — возвращаем сам ключ, а не undefined
function t(key) {
    const dictionary = translations[currentLang] || translations[DEFAULT_LANG];
    const fallback = translations[DEFAULT_LANG];
    return dictionary[key] ?? fallback[key] ?? key;
}

// Перевод с подстановкой: tf('episodes-of-season', { n: 3 })
function tf(key, params = {}) {
    return Object.keys(params).reduce(
        (text, name) => text.replaceAll(`{${name}}`, params[name]),
        t(key)
    );
}

function applyTranslations() {
    document.documentElement.lang = currentLang;

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        const dictionary = translations[currentLang] || translations[DEFAULT_LANG];
        if (dictionary[key] === undefined) return;
        const value = dictionary[key];

        const attr = el.getAttribute('data-i18n-attr');
        if (attr) {
            el.setAttribute(attr, value);
        } else if (el.tagName === 'TITLE') {
            document.title = value;
        } else if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
            el.placeholder = value;
        } else {
            el.innerText = value;
        }
    });
}

function syncLangSelectors() {
    document.querySelectorAll('.lang-select').forEach(select => select.value = currentLang);
}

function changeLanguage(lang) {
    if (!SUPPORTED_LANGS.includes(lang)) return;
    currentLang = lang;
    writeLangCookie(lang);

    applyTranslations();
    syncLangSelectors();

    // Каждая страница сама решает, какие данные надо перезагрузить
    if (typeof window.onLanguageChanged === 'function') window.onLanguageChanged();
}

document.addEventListener('DOMContentLoaded', () => {
    syncLangSelectors();
    applyTranslations();

    document.querySelectorAll('.lang-select').forEach(select => {
        select.addEventListener('change', e => changeLanguage(e.target.value));
    });
});
