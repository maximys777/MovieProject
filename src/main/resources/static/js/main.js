// --- ГЛОБАЛЬНЫЕ НАСТРОЙКИ ---
// Язык (currentLang, t, tf, changeLanguage) живет в i18n.js, он подключается раньше main.js
const API_BASE_URL = '';
const IMG_500 = 'https://image.tmdb.org/t/p/w500';
const POSTER_FALLBACK = 'https://placehold.co/500x750/333333/ffffff?text=No+Image';
const POSTER_FALLBACK_SMALL = 'https://placehold.co/50x75/333333/ffffff?text=No+Image';

let currentTimeWindow = 'day';
let currentMediaType = 'movies';
let currentPage = 1;
let totalPages = 1;
let isLoading = false;

let currentSelectedMovie = null;

// Данные карточек храним в JS, а не в HTML-атрибутах:
// раньше объект сериализовался прямо в onclick="..." и любой апостроф в названии
// ломал разметку с ошибкой "missing ) after argument list"
let trendingItems = [];
let catalogItems = [];

// --- УВЕДОМЛЕНИЯ ---
function showToast(message, isError = false) {
    const toast = document.getElementById('custom-toast');
    if (!toast) return;
    toast.innerText = message;
    toast.className = 'toast-notification';
    if (isError) toast.classList.add('error');

    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// --- АВТОРИЗАЦИЯ И ОТЛОЖЕННЫЕ ДЕЙСТВИЯ ---
const PENDING_ACTION_KEY = 'pendingAction';

// Все запросы к watchlist помечаем как AJAX: бэкенд отдает на них 401,
// а не 302 на /login.html с потерей тела запроса
function watchlistFetch(path, options = {}) {
    const headers = {'X-Requested-With': 'XMLHttpRequest', ...(options.headers || {})};
    if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
    return fetch(`${API_BASE_URL}${path}`, {...options, headers});
}

// Намерение живет только в текущей вкладке — sessionStorage, не localStorage
function savePendingAction(action) {
    try {
        sessionStorage.setItem(PENDING_ACTION_KEY, JSON.stringify(action));
    } catch (e) {
        console.error(e);
    }
}

function redirectToLogin() {
    window.location.href = '/login.html';
}

// Доигрываем действие, которое не прошло из-за 401, после успешного логина
async function replayPendingAction() {
    const raw = sessionStorage.getItem(PENDING_ACTION_KEY);
    if (!raw) return;

    // Снимаем намерение ДО запроса: даже при ошибке или падении
    // оно не должно повториться при следующей загрузке страницы
    sessionStorage.removeItem(PENDING_ACTION_KEY);

    let pending;
    try {
        pending = JSON.parse(raw);
    } catch (e) {
        console.error(e);
        return;
    }
    if (!pending || !pending.url || !pending.method) return;

    try {
        const response = await watchlistFetch(pending.url, {
            method: pending.method,
            body: pending.body ? JSON.stringify(pending.body) : undefined
        });

        // Повторный 401 НЕ уводит на логин и НЕ сохраняется заново —
        // иначе получится цикл логин → повтор → логин
        if (response.ok) {
            showToast(t('toast-success'));
            // На профиле обновляем карусели и счетчики
            if (typeof loadDashboard === 'function') loadDashboard();
        } else {
            showToast(t('toast-error'), true);
        }
    } catch (e) {
        showToast(t('toast-error'), true);
    }
}

// --- КАРТИНКИ ---
// data-fallback="hide" прячет карточку, любое другое значение — это URL заглушки
function bindImageFallbacks(root) {
    if (!root) return;
    root.querySelectorAll('img[data-fallback]').forEach(img => {
        img.addEventListener('error', () => {
            const mode = img.dataset.fallback;
            if (mode === 'hide') {
                const card = img.closest('.slider-item') || img.parentElement;
                if (card) card.style.display = 'none';
            } else if (img.src !== mode) {
                img.src = mode;
            }
        }, {once: true});
    });
}

// --- НАВИГАЦИЯ И КАТАЛОГ ---
function updateMediaType(type) {
    currentMediaType = type;
    const btnMovies = document.getElementById('type-movies');
    const btnTv = document.getElementById('type-tv');
    if (btnMovies) btnMovies.classList.toggle('active', type === 'movies');
    if (btnTv) btnTv.classList.toggle('active', type === 'tv-shows');

    const titleEl = document.getElementById('catalog-title');
    if (titleEl) titleEl.innerText = type === 'movies' ? t('catalog-movies') : t('catalog-tv');

    loadTrending();
    resetCatalog();
}

function updateTimeWindow(timeWindow, btnId) {
    currentTimeWindow = timeWindow;
    const btnDay = document.getElementById('btn-day');
    const btnWeek = document.getElementById('btn-week');
    if (btnDay) btnDay.classList.remove('active');
    if (btnWeek) btnWeek.classList.remove('active');
    const activeBtn = document.getElementById(btnId);
    if (activeBtn) activeBtn.classList.add('active');
    loadTrending();
    resetCatalog();
}

function resetCatalog() {
    const grid = document.getElementById('movies-grid');
    if (!grid) return;
    currentPage = 1;
    catalogItems = [];
    grid.innerHTML = '';
    loadCatalog();
}

function scrollSlider(sliderId, distance) {
    const slider = document.getElementById(sliderId);
    if (slider) slider.scrollBy({left: distance, behavior: 'smooth'});
}

function updateNavButtons() {
    const slider = document.getElementById('trending-slider');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    if (!slider || !prevBtn || !nextBtn) return;
    prevBtn.style.display = slider.scrollLeft > 10 ? 'flex' : 'none';
    const isEnd = slider.scrollLeft + slider.clientWidth >= slider.scrollWidth - 15;
    nextBtn.style.display = isEnd ? 'none' : 'flex';
}

function getDisplayTitle(item) {
    return item.title || item.name || t('no-title');
}

// --- ЗАГРУЗКА ДАННЫХ ---
async function loadTrending() {
    const slider = document.getElementById('trending-slider');
    if (!slider) return;
    slider.innerHTML = `<p style="padding: 20px;">${t('loading')}</p>`;
    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=1&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) return;

        trendingItems = responseData.results.filter(m => m.poster_path).slice(0, 10);

        slider.innerHTML = trendingItems.map((item, index) => `
            <div class="slider-item" data-trending-index="${index}">
                <div class="image-container">
                    <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
                    <img src="${IMG_500}${item.poster_path}" alt="${getDisplayTitle(item)}" data-fallback="hide">
                </div>
                <div class="movie-title">${getDisplayTitle(item)}</div>
            </div>
        `).join('');

        bindImageFallbacks(slider);
        setTimeout(updateNavButtons, 300);
        slider.scrollLeft = 0;
    } catch (e) {
        console.error(e);
    }
}

async function loadCatalog() {
    const statusEl = document.getElementById('load-status');
    const grid = document.getElementById('movies-grid');
    if (!statusEl || !grid) return;

    if (isLoading) return;
    isLoading = true;
    statusEl.innerText = t('loading');

    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=${currentPage}&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) {
            statusEl.innerText = t('toast-error');
            return;
        }

        totalPages = responseData.total_pages;
        appendItems(responseData.results.filter(m => m.poster_path));

        statusEl.innerText = currentPage >= totalPages ? t('no-more-results') : '';
    } catch (e) {
        console.error(e);
        statusEl.innerText = t('toast-error');
    } finally {
        isLoading = false;
    }
}

function appendItems(items) {
    const grid = document.getElementById('movies-grid');
    if (!grid) return;
    const detailsLabel = t('more-details');

    const html = items.map(item => {
        const index = catalogItems.push(item) - 1;
        return `
        <div class="movie-card" data-item-index="${index}">
            <div style="position:relative">
                <img src="${IMG_500}${item.poster_path || item.posterPath}" alt="${getDisplayTitle(item)}" data-fallback="${POSTER_FALLBACK}">
                <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
            </div>
            <div class="movie-card-body">
                <div class="movie-title" title="${getDisplayTitle(item)}">${getDisplayTitle(item)}</div>
                <button class="btn-detail-card" style="width:100%; padding:8px; background:var(--primary-color); border:none; color:white; border-radius:4px; cursor:pointer;">
                    ${detailsLabel}
                </button>
            </div>
        </div>
    `;
    }).join('');

    grid.insertAdjacentHTML('beforeend', html);
    bindImageFallbacks(grid);
}

function handleItemClick(item) {
    if (!item) return;
    const type = item.media_type || item.mediaType || (currentMediaType === 'tv-shows' ? 'tv' : 'movie');

    if (type === 'tv' || type === 'tv-shows') {
        openTvModal(item);
    } else {
        openMovieModal(item);
    }
}

// --- МОДАЛКА ФИЛЬМОВ ---
async function openMovieModal(movie) {
    currentSelectedMovie = movie;

    const modalEl = document.getElementById('movie-modal');
    if (!modalEl) {
        console.error('Модалка фильма не найдена!');
        return;
    }

    document.getElementById('movie-modal-title').innerText = getDisplayTitle(movie);
    document.getElementById('movie-modal-poster').src = movie.poster_path ? `${IMG_500}${movie.poster_path}` : POSTER_FALLBACK;

    const date = movie.release_date || movie.releaseDate;
    document.getElementById('movie-modal-year').innerText = `${t('year-label')}: ${date ? date.substring(0, 4) : '...'}`;
    document.getElementById('movie-modal-rating').innerText = `★ ${movie.vote_average ? movie.vote_average.toFixed(1) : 'NR'}`;

    const countryEl = document.getElementById('movie-modal-country');
    if (countryEl) countryEl.innerText = `${t('country-label')}: ${t('loading')}`;

    // Сбрасываем описание сразу, чтобы не показывать текст предыдущего фильма, пока грузятся детали
    setupOverview('movie-modal-overview', 'movie-read-more', movie.overview);

    // Добавляем и active (для главной) и show (для профиля)
    modalEl.classList.add('active', 'show');
    document.body.classList.add('modal-open');

    try {
        const res = await fetch(`${API_BASE_URL}/details/movies/${movie.id}?language=${currentLang}`);
        const details = await res.json();

        const originalId = currentSelectedMovie.id;
        currentSelectedMovie = {...currentSelectedMovie, ...details};
        currentSelectedMovie.id = originalId || details.id;

        if (countryEl) countryEl.innerText = `${t('country-label')}: ${formatList(details.production_countries || details.productionCountries)}`;

        const runtimeEl = document.getElementById('movie-modal-runtime');
        if (runtimeEl) runtimeEl.innerText = `${details.runtime || '...'} ${t('runtime-label')}`;

        const genresCont = document.getElementById('movie-modal-genres');
        if (genresCont) genresCont.innerHTML = formatGenres(details.genres);

        // Берем точную оценку из полных деталей и обновляем бейдж
        const finalRating = details.vote_average || details.voteAverage;
        const ratingEl = document.getElementById('movie-modal-rating');
        if (ratingEl) {
            ratingEl.innerText = `★ ${(finalRating && finalRating > 0) ? finalRating.toFixed(1) : 'NR'}`;
        }

        setupOverview('movie-modal-overview', 'movie-read-more', details.overview);
    } catch (e) {
        console.error(e);
    }
}

function closeMovieModal() {
    closeModal('movie-modal');
}

// СОХРАНЕНИЕ ФИЛЬМА
async function saveMovieToWatchlist() {
    if (!currentSelectedMovie || !currentSelectedMovie.id) {
        showToast(t('toast-movie-id-error'), true);
        return;
    }

    const btnSave = document.querySelector('#movie-modal .btn-save');
    const originalText = btnSave ? btnSave.innerText : '';
    if (btnSave) {
        btnSave.innerText = '⏳...';
        btnSave.style.pointerEvents = 'none';
    }

    const item = currentSelectedMovie;
    const releaseDate = item.release_date || item.releaseDate;
    let formattedDate = null;
    if (releaseDate && releaseDate.length >= 10) {
        formattedDate = releaseDate.substring(0, 10) + 'T00:00:00';
    }

    // Шлем ID в двух форматах, чтобы Spring Boot 100% его съел
    const payload = {
        poster_path: item.poster_path || item.posterPath,
        title: getDisplayTitle(item),
        movieId: item.id,      // Для camelCase
        movie_id: item.id,     // Для snake_case
        release_date: formattedDate,
        popularity: item.popularity || 0.0
    };

    try {
        const response = await watchlistFetch('/watchlist-movies', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        // Не залогинен: запоминаем, что хотел сделать, и сразу уводим на логин
        if (response.status === 401) {
            savePendingAction({
                action: 'add-to-watchlist-movie',
                url: '/watchlist-movies',
                method: 'POST',
                body: payload,
                tmdbId: item.id,
                title: payload.title
            });
            redirectToLogin();
            return;
        }

        if (response.ok) {
            showToast(t('toast-success'));
            closeMovieModal();
        } else {
            showToast(t('toast-error'), true);
        }
    } catch (e) {
        showToast(t('toast-error'), true);
    } finally {
        if (btnSave) {
            btnSave.innerText = originalText;
            btnSave.style.pointerEvents = 'auto';
        }
    }
}

// --- МОДАЛКА СЕРИАЛОВ ---
let currentShowData = null;
let currentShowSeasons = [];
let currentSelectedSeason = 1;
let currentSelectedEpisode = 0;

async function openTvModal(show) {
    currentShowData = show;

    // УМНЫЙ ПОИСК: Ищем tv-modal (на главной) ИЛИ progress-modal (в профиле)
    const modalEl = document.getElementById('tv-modal') || document.getElementById('progress-modal');
    if (!modalEl) {
        console.error('Модальное окно сериала не найдено на этой странице!');
        return;
    }

    const titleEl = document.getElementById('modal-title');
    if (titleEl) titleEl.innerText = getDisplayTitle(show);

    const posterEl = document.getElementById('modal-poster');
    if (posterEl) posterEl.src = show.poster_path ? `${IMG_500}${show.poster_path}` : POSTER_FALLBACK;

    const date = show.first_air_date || show.firstAirDate;
    const yearEl = document.getElementById('modal-year');
    if (yearEl) yearEl.innerText = `${t('year-label')}: ${date ? date.substring(0, 4) : '...'}`;

    const countryEl = document.getElementById('modal-country');
    if (countryEl) countryEl.innerText = `${t('country-label')}: ${t('loading')}`;

    const overviewEl = document.getElementById('modal-overview');
    if (overviewEl) overviewEl.innerText = show.overview || t('no-description');

    // Добавляем и active (для главной) и show (для профиля)
    modalEl.classList.add('active', 'show');
    document.body.classList.add('modal-open');

    try {
        const res = await fetch(`${API_BASE_URL}/details/tv-shows/${show.id}?language=${currentLang}`);
        const details = await res.json();

        // 1. ЗАЩИТА ID: сохраняем TMDB ID, чтобы null с бэкенда его не убил
        const originalId = currentShowData.id;
        currentShowData = {...currentShowData, ...details};
        currentShowData.id = originalId || details.id;

        // 2. ОБНОВЛЕНИЕ СТРАНЫ
        if (countryEl) {
            const countries = details.production_countries || details.productionCountries || [];
            countryEl.innerText = `${t('country-label')}: ${formatList(countries)}`;
        }

        // 3. ОБНОВЛЕНИЕ ОЦЕНКИ
        const finalRating = details.vote_average || details.voteAverage;
        const ratingEl = document.getElementById('modal-rating');
        if (ratingEl) {
            ratingEl.innerText = `★ ${(finalRating && finalRating > 0) ? finalRating.toFixed(1) : 'NR'}`;
        }

        const seasonsCountEl = document.getElementById('modal-seasons-count');
        if (seasonsCountEl) seasonsCountEl.innerText = `${t('seasons-label')}: ${details.number_of_seasons || '...'}`;

        const genresCont = document.getElementById('tv-modal-genres') || document.getElementById('modal-genres');
        if (genresCont) genresCont.innerHTML = formatGenres(details.genres);

        setupOverview('modal-overview', 'tv-read-more', details.overview);

        currentShowSeasons = details.seasons || [];
        renderSeasonTabs();
        if (currentShowSeasons.length > 0) loadEpisodes(0);
    } catch (e) {
        console.error(e);
    }
}

function renderSeasonTabs() {
    const tabsContainer = document.getElementById('season-tabs');
    if (!tabsContainer) return;
    const seasonLabel = t('season-label');
    tabsContainer.innerHTML = currentShowSeasons.map((s, index) => `
        <button class="season-tab ${index === 0 ? 'active' : ''}" data-season-index="${index}">
            ${seasonLabel} ${s.season_number || s.seasonNumber}
        </button>
    `).join('');
}

function loadEpisodes(seasonIndex, tabElement = null) {
    const seasonObj = currentShowSeasons[seasonIndex];
    if (!seasonObj) return;
    currentSelectedSeason = seasonObj.season_number || seasonObj.seasonNumber;
    currentSelectedEpisode = 0;
    const checkAll = document.getElementById('check-all-season');
    if (checkAll) checkAll.checked = false;

    if (tabElement) {
        document.querySelectorAll('.season-tab').forEach(tab => tab.classList.remove('active'));
        tabElement.classList.add('active');
    }

    const grid = document.getElementById('episodes-grid');
    if (!grid) return;
    const epCount = seasonObj.episode_count || seasonObj.episodeCount;

    if (epCount && epCount > 0) {
        let html = '';
        for (let i = 1; i <= epCount; i++) {
            html += `<button class="episode-btn" id="ep-btn-${i}" data-episode="${i}">${i}</button>`;
        }
        grid.innerHTML = html;
    }
}

function selectEpisode(epNum) {
    currentSelectedEpisode = epNum;
    const btns = document.querySelectorAll('.episode-btn');
    btns.forEach(btn => {
        const n = parseInt(btn.innerText);
        btn.classList.toggle('active', n <= epNum);
    });
    const checkAll = document.getElementById('check-all-season');
    if (checkAll) checkAll.checked = (epNum === btns.length && btns.length > 0);
}

function toggleAllEpisodes(checkbox) {
    const btns = document.querySelectorAll('.episode-btn');
    selectEpisode(checkbox.checked ? btns.length : 0);
}

// modalId не задан — закрываем все модалки, какие есть на странице
function closeModal(modalId) {
    const ids = modalId ? [modalId] : ['tv-modal', 'movie-modal', 'progress-modal', 'full-grid-modal'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.remove('active', 'show');
    });
    checkBodyScroll();
}

async function saveTvShowProgress() {
    if (currentSelectedEpisode === 0) {
        showToast(t('toast-select-episode'), true);
        return;
    }

    // Ищем кнопку сохранения (она может быть в разных модалках)
    const btn = document.querySelector('#tv-modal .btn-save') || document.querySelector('#progress-modal .btn-save');
    const oldText = btn ? btn.innerText : '...';
    if (btn) {
        btn.innerText = '⏳...';
        btn.style.pointerEvents = 'none';
    }

    // Достаем страну (из новых данных TMDB)
    let originCountry = 'US';
    if (currentShowData.production_countries && currentShowData.production_countries.length > 0) {
        originCountry = currentShowData.production_countries[0].name || currentShowData.production_countries[0].iso_3166_1;
    } else if (currentShowData.origin_country && currentShowData.origin_country.length > 0) {
        originCountry = currentShowData.origin_country[0];
    }

    const payload = {
        // Шлем ID в двух форматах (как делали с фильмами), чтобы Spring 100% его съел
        tvShowId: currentShowData.id,
        tv_show_id: currentShowData.id,
        name: currentShowData.name || currentShowData.title,
        posterUrl: currentShowData.poster_path || currentShowData.posterPath,
        firstAirDate: currentShowData.first_air_date || currentShowData.firstAirDate,
        originCountry: originCountry,
        currentSeason: currentSelectedSeason,
        currentEpisode: currentSelectedEpisode
    };

    try {
        const res = await watchlistFetch('/watchlist-tv-shows', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        // Не залогинен: запоминаем прогресс по сериалу и сразу уводим на логин
        if (res.status === 401) {
            savePendingAction({
                action: 'add-to-watchlist-tv',
                url: '/watchlist-tv-shows',
                method: 'POST',
                body: payload,
                tmdbId: currentShowData.id,
                title: payload.name
            });
            redirectToLogin();
            return;
        }

        if (res.ok) {
            closeModal();
            showToast(t('toast-success'));
        } else {
            showToast(t('toast-error'), true);
        }
    } catch (e) {
        showToast(t('toast-error'), true);
    } finally {
        if (btn) {
            btn.innerText = oldText;
            btn.style.pointerEvents = 'auto';
        }
    }
}

// --- ОБРАБОТЧИКИ КНОПОК ---
// Один делегированный слушатель вместо inline onclick="..." в HTML
function handleAction(action, el) {
    switch (action) {
        case 'search-all':
            goToSearchPage();
            break;
        case 'set-media-type':
            updateMediaType(el.dataset.mediaType);
            break;
        case 'set-time-window':
            updateTimeWindow(el.dataset.timeWindow, el.id);
            break;
        case 'scroll-slider':
            scrollSlider(el.dataset.slider, Number(el.dataset.distance));
            break;
        case 'toggle-overview':
            toggleOverview(el.dataset.target, el);
            break;
        case 'close-movie-modal':
            closeMovieModal();
            break;
        case 'save-movie':
            saveMovieToWatchlist();
            break;
        case 'save-tv':
            saveTvShowProgress();
            break;
        case 'close-modal':
            closeModal(el.dataset.target);
            break;
        case 'open-full-grid':
            // Определена только на странице профиля
            if (typeof openFullGrid === 'function') openFullGrid(el.dataset.gridType);
            break;
        default:
            break;
    }
}

document.addEventListener('click', e => {
    const el = e.target.closest('[data-action]');
    if (el) handleAction(el.dataset.action, el);
});

document.addEventListener('change', e => {
    if (e.target.dataset.action === 'toggle-all-episodes') toggleAllEpisodes(e.target);
});

// --- ГЛОБАЛЬНЫЕ СЛУШАТЕЛИ ---
window.addEventListener('click', e => {
    if (e.target.classList.contains('modal-overlay')) closeModal(e.target.id);
});

window.addEventListener('scroll', () => {
    // Защита: бесконечный скролл только на главной
    if (!document.getElementById('load-status')) return;
    if (isLoading || currentPage >= totalPages) return;
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 600) {
        currentPage++;
        loadCatalog();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const slider = document.getElementById('trending-slider');
    if (slider) {
        slider.addEventListener('scroll', updateNavButtons);
        slider.addEventListener('click', e => {
            const card = e.target.closest('[data-trending-index]');
            if (card) handleItemClick(trendingItems[Number(card.dataset.trendingIndex)]);
        });

        // Перезагружаем данные при смене языка (главная страница)
        window.onLanguageChanged = () => {
            loadTrending();
            resetCatalog();
        };

        loadTrending();
        loadCatalog();
    }

    // Каталог с бесконечным скроллом есть только на главной (#load-status)
    const grid = document.getElementById('movies-grid');
    if (grid && document.getElementById('load-status')) {
        grid.addEventListener('click', e => {
            const card = e.target.closest('[data-item-index]');
            if (card) handleItemClick(catalogItems[Number(card.dataset.itemIndex)]);
        });
    }

    const seasonTabs = document.getElementById('season-tabs');
    if (seasonTabs) {
        seasonTabs.addEventListener('click', e => {
            const tab = e.target.closest('[data-season-index]');
            if (tab) loadEpisodes(Number(tab.dataset.seasonIndex), tab);
        });
    }

    const episodesGrid = document.getElementById('episodes-grid');
    if (episodesGrid) {
        episodesGrid.addEventListener('click', e => {
            const btn = e.target.closest('[data-episode]');
            if (btn) selectEpisode(Number(btn.dataset.episode));
        });
    }
});

// main.js подключен на index/search/profile, поэтому одного слушателя хватает на все страницы
document.addEventListener('DOMContentLoaded', replayPendingAction);

function formatList(list) {
    if (!list || !Array.isArray(list) || list.length === 0) return '...';
    return list.map(item => typeof item === 'object' ? item.name : item).join(', ');
}

function formatGenres(genres) {
    if (!genres || !Array.isArray(genres) || genres.length === 0) return '';
    return genres.map(g => `<span class="genre-tag">${g.name || g}</span>`).join('');
}

function toggleOverview(textId, btn) {
    const el = document.getElementById(textId);
    if (!el) return;
    const isCollapsed = el.classList.contains('collapsed');
    el.classList.toggle('collapsed', !isCollapsed);
    el.classList.toggle('expanded', isCollapsed);
    btn.innerText = isCollapsed ? t('read-less') : t('modal-read-more');
}

function setupOverview(textId, btnId, text) {
    const el = document.getElementById(textId);
    if (!el) return;
    el.innerText = text || t('no-description');
    el.className = 'modal-overview-text collapsed';

    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.innerText = t('modal-read-more');
    btn.style.display = 'none';
    setTimeout(() => {
        if (el.scrollHeight > el.clientHeight) btn.style.display = 'inline-block';
    }, 10);
}

// --- ЖИВОЙ ПОИСК ---
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('search-input');
    const dropdown = document.getElementById('search-dropdown');
    const results = document.getElementById('search-results');
    const spinner = document.getElementById('search-spinner');
    const btnAll = document.getElementById('btn-view-all-search');

    let timeout = null;
    if (input) {
        input.addEventListener('input', e => {
            const q = e.target.value.trim();
            clearTimeout(timeout);
            if (q.length < 2) {
                dropdown.classList.add('hidden');
                spinner.classList.add('hidden');
                return;
            }
            spinner.classList.remove('hidden');
            timeout = setTimeout(async () => {
                try {
                    const res = await fetch(`/search/live?query=${encodeURIComponent(q)}&language=${currentLang}`);
                    const data = await res.json();
                    renderDropdown(data, q);
                } catch (e) {
                    console.error(e);
                } finally {
                    spinner.classList.add('hidden');
                }
            }, 500);
        });
    }

    function renderDropdown(items, query) {
        if (!results || !dropdown || !btnAll) return;
        results.innerHTML = '';
        if (!items || items.length === 0) {
            results.innerHTML = `<div class="search-no-results">${t('no-results')}</div>`;
            btnAll.classList.add('hidden');
        } else {
            items.forEach(item => {
                const type = item.media_type || item.mediaType;
                const poster = item.poster_url || item.posterUrl;
                const year = item.release_year || item.releaseYear;
                const title = item.title || item.name || t('no-title');
                const img = poster ? `https://image.tmdb.org/t/p/w92${poster}` : POSTER_FALLBACK_SMALL;

                const div = document.createElement('div');
                div.className = 'search-item';
                div.addEventListener('click', () => {
                    dropdown.classList.add('hidden');
                    const comp = {
                        id: item.id, title, name: title,
                        poster_path: poster ? poster.replace('https://image.tmdb.org/t/p/w92', '') : null,
                        release_date: year ? `${year}-01-01` : null,
                        first_air_date: year ? `${year}-01-01` : null,
                        vote_average: 0, overview: t('loading')
                    };
                    type === 'movie' ? openMovieModal(comp) : openTvModal(comp);
                });
                div.innerHTML = `
                    <img src="${img}" alt="${title}" class="search-item-img" data-fallback="${POSTER_FALLBACK_SMALL}">
                    <div class="search-item-info">
                        <div class="search-item-title">${title}</div>
                        <div class="search-item-meta">${type === 'movie' ? t('film') : t('tv-show')} • ${year || 'N/A'}</div>
                        <div class="search-item-genres">${(item.genres || []).join(', ')}</div>
                    </div>`;
                bindImageFallbacks(div);
                results.appendChild(div);
            });
            btnAll.classList.remove('hidden');
            btnAll.dataset.query = query;
        }
        dropdown.classList.remove('hidden');
    }
});

function goToSearchPage() {
    const btn = document.getElementById('btn-view-all-search');
    if (btn && btn.dataset.query) window.location.href = `/search.html?query=${encodeURIComponent(btn.dataset.query)}`;
}

function checkBodyScroll() {
    if (document.querySelectorAll('.modal-overlay.active, .modal-overlay.show').length === 0) {
        document.body.classList.remove('modal-open');
    }
}
