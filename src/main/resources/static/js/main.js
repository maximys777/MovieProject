const API_BASE_URL = '';
const IMG_500 = 'https://image.tmdb.org/t/p/w500';

let currentLang = 'ru';
let currentTimeWindow = 'day';
let currentMediaType = 'movies';
let currentPage = 1;
let totalPages = 1;
let isLoading = false;

let currentSelectedMovie = null;
let currentMovieBtnElement = null;

// --- УВЕДОМЛЕНИЯ ---
function showToast(message, isError = false) {
    const toast = document.getElementById('custom-toast');
    toast.innerText = message;
    toast.className = 'toast-notification';
    if (isError) toast.classList.add('error');

    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// --- НАВИГАЦИЯ И КАТАЛОГ ---
function updateMediaType(type) {
    currentMediaType = type;
    document.getElementById('type-movies').classList.toggle('active', type === 'movies');
    document.getElementById('type-tv').classList.toggle('active', type === 'tv-shows');
    document.getElementById('catalog-title').innerText = type === 'movies' ? 'Каталог фильмов' : 'Каталог сериалов';
    loadTrending();
    resetCatalog();
}

function updateTimeWindow(window, btnId) {
    currentTimeWindow = window;
    document.getElementById('btn-day').classList.remove('active');
    document.getElementById('btn-week').classList.remove('active');
    document.getElementById(btnId).classList.add('active');
    loadTrending();
    resetCatalog();
}

function changeLanguage(lang) {
    currentLang = lang;
    loadTrending();
    resetCatalog();
}

function resetCatalog() {
    currentPage = 1;
    document.getElementById('movies-grid').innerHTML = '';
    loadCatalog();
}

function scrollSlider(dist) {
    const slider = document.getElementById('trending-slider');
    if (slider) slider.scrollBy({left: dist, behavior: 'smooth'});
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
    return item.title || item.name || 'Без названия';
}

// --- ЗАГРУЗКА ДАННЫХ ---
async function loadTrending() {
    const slider = document.getElementById('trending-slider');
    slider.innerHTML = '<p style="padding: 20px;">Загрузка...</p>';
    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=1&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) return;

        let items = responseData.results.filter(m => m.poster_path).slice(0, 10);

        slider.innerHTML = items.map(item => `
            <div class="slider-item">
                <div class="image-container">
                    <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
                    <img src="${IMG_500}${item.poster_path}" alt="${getDisplayTitle(item)}" onerror="this.parentElement.parentElement.style.display='none'">
                </div>
                <div class="movie-title">${getDisplayTitle(item)}</div>
            </div>
        `).join('');

        setTimeout(updateNavButtons, 300);
        slider.scrollLeft = 0;
    } catch (e) {
        console.error(e);
    }
}

async function loadCatalog() {
    if (isLoading) return;
    isLoading = true;
    document.getElementById('load-status').innerText = 'Загрузка контента...';

    try {
        const url = `${API_BASE_URL}/trending/${currentMediaType}/${currentTimeWindow}?page=${currentPage}&language=${currentLang}`;
        const res = await fetch(url);
        const data = await res.json();
        const responseData = Array.isArray(data) ? data[0] : data;

        if (!responseData || !responseData.results) {
            document.getElementById('load-status').innerText = 'Сталася помилка при завантаженні';
            return;
        }

        totalPages = responseData.total_pages;
        const items = responseData.results.filter(m => m.poster_path);

        appendItems(items);

        if (currentPage >= totalPages) {
            document.getElementById('load-status').innerText = 'Больше ничего нет';
        } else {
            document.getElementById('load-status').innerText = '';
        }
    } catch (e) {
        console.error(e);
        document.getElementById('load-status').innerText = 'Ошибка сети';
    } finally {
        isLoading = false;
    }
}

function appendItems(items) {
    const grid = document.getElementById('movies-grid');
    const html = items.map(item => {
        const encodedItem = encodeURIComponent(JSON.stringify(item));
        return `
        <div class="movie-card">
            <div style="position:relative">
                <img src="${IMG_500}${item.poster_path || item.posterPath}" alt="${getDisplayTitle(item)}" onerror="this.src='https://via.placeholder.com/500x750?text=No+Image'">
                <span class="rating-badge">★ ${item.vote_average ? item.vote_average.toFixed(1) : '0.0'}</span>
            </div>
            <div class="movie-card-body">
                <div class="movie-title" title="${getDisplayTitle(item)}">${getDisplayTitle(item)}</div>
                <button style="width:100%; padding:8px; background:var(--primary-color); border:none; color:white; border-radius:4px; cursor:pointer;"
                        onclick="handleFavoriteClick(this, '${encodedItem}')">
                    Детально
                </button>
            </div>
        </div>
    `
    }).join('');
    grid.insertAdjacentHTML('beforeend', html);
}

function handleFavoriteClick(btnElement, encodedItemStr) {
    const item = JSON.parse(decodeURIComponent(encodedItemStr));
    if (currentMediaType === 'tv-shows') {
        openTvModal(item);
    } else {
        openMovieModal(item, btnElement);
    }
}

// ==========================================
// ЛОГИКА МОДАЛЬНОГО ОКНА ФИЛЬМОВ
// ==========================================

async function openMovieModal(movie, btnElement) {
    currentSelectedMovie = movie;
    currentMovieBtnElement = btnElement;

    document.getElementById('movie-modal-title').innerText = getDisplayTitle(movie);
    document.getElementById('movie-modal-poster').src = movie.poster_path ? `${IMG_500}${movie.poster_path}` : 'https://via.placeholder.com/500x750?text=No+Image';

    const releaseDate = movie.release_date || movie.releaseDate;
    document.getElementById('movie-modal-year').innerText = `Рік: ${releaseDate ? releaseDate.substring(0, 4) : '...'}`;
    document.getElementById('movie-modal-rating').innerText = `★ ${movie.vote_average ? movie.vote_average.toFixed(1) : 'NR'}`;

    document.getElementById('movie-modal-country').innerText = `Країна: Завантаження...`;
    document.getElementById('movie-modal-runtime').innerText = `... хв`;
    document.getElementById('movie-modal-genres').innerHTML = '';
    setupOverview('movie-modal-overview', 'movie-read-more', movie.overview || 'Завантаження опису...');

    document.getElementById('movie-modal').classList.add('active');
    document.body.classList.add('modal-open');

    try {
        const detailsRes = await fetch(`${API_BASE_URL}/details/movies/${movie.id}?language=${currentLang}`);
        if (detailsRes.ok) {
            const details = await detailsRes.json();
            currentSelectedMovie = { ...currentSelectedMovie, ...details };

            const countries = details.production_countries || details.productionCountries || [];
            document.getElementById('movie-modal-country').innerText = `Країна: ${formatList(countries)}`;
            document.getElementById('movie-modal-runtime').innerText = details.runtime ? `${details.runtime} хв` : '...';
            document.getElementById('movie-modal-genres').innerHTML = formatGenres(details.genres);

            if (details.overview) {
                setupOverview('movie-modal-overview', 'movie-read-more', details.overview);
            }
        } else {
            document.getElementById('movie-modal-country').innerText = `Країна: Невідомо`;
            document.getElementById('movie-modal-runtime').innerText = `...`;
        }
    } catch (e) {
        console.error("Ошибка загрузки деталей фильма", e);
        document.getElementById('movie-modal-country').innerText = `Країна: Невідомо`;
        document.getElementById('movie-modal-runtime').innerText = `...`;
    }
}

function closeMovieModal() {
    document.getElementById('movie-modal').classList.remove('active');
    document.body.classList.remove('modal-open');
    currentSelectedMovie = null;
    currentMovieBtnElement = null;
}

// СОХРАНЕНИЕ ФИЛЬМА
async function saveMovieToWatchlist() {
    if (!currentSelectedMovie) return;

    const btnSave = document.querySelector('#movie-modal .btn-save');
    const originalText = btnSave.innerText;
    btnSave.innerText = '⏳ Збереження...';
    btnSave.style.pointerEvents = 'none';

    const item = currentSelectedMovie;

    // БЕЗОПАСНЫЙ ПАРСИНГ ДАТЫ (Чтобы бэкенд не падал на nullT00:00:00)
    const releaseDate = item.release_date || item.releaseDate;
    let formattedDate = null;
    if (releaseDate && releaseDate.length >= 10) {
        formattedDate = releaseDate.substring(0, 10) + "T00:00:00";
    }

    const payload = {
        poster_path: item.poster_path || item.posterPath,
        title: getDisplayTitle(item),
        movieId: item.id,
        release_date: formattedDate,
        popularity: item.popularity || 0.0 // Дефолтное значение, если бэк не прислал
    };

    try {
        const response = await fetch(`${API_BASE_URL}/watchlist-movies`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        // ПРОВЕРКА НА АВТОРИЗАЦИЮ
        if (response.status === 401 || response.status === 403 || (response.redirected && response.url.includes('/login'))) {
            window.location.href = '/login.html';
            return;
        }

        if (response.ok || response.status === 201) {
            showToast("Фільм успішно додано до списку!");
            closeMovieModal();

            if (currentMovieBtnElement) {
                currentMovieBtnElement.innerText = '✅ Збережено';
                currentMovieBtnElement.style.background = '#28a745';
                currentMovieBtnElement.style.pointerEvents = 'none';
            }
        } else {
            showToast("Помилка при збереженні фільму!", true);
        }
    } catch (e) {
        console.error(e);
        showToast("Помилка мережі", true);
    } finally {
        btnSave.innerText = originalText;
        btnSave.style.pointerEvents = 'auto';
    }
}

// ==========================================
// ЛОГИКА МОДАЛЬНОГО ОКНА СЕРИАЛОВ
// ==========================================
let currentShowData = null;
let currentShowSeasons = [];
let currentSelectedSeason = 1;
let currentSelectedEpisode = 0;

async function openTvModal(show) {
    currentShowData = show;

    document.getElementById('modal-title').innerText = getDisplayTitle(show);
    document.getElementById('modal-poster').src = `${IMG_500}${show.poster_path || show.posterPath}`;

    const airDate = show.first_air_date || show.firstAirDate;
    document.getElementById('modal-year').innerText = `Рік: ${airDate ? airDate.substring(0, 4) : '...'}`;

    const countries = show.origin_country || show.originCountry;
    document.getElementById('modal-country').innerText = `Країна: ${countries && countries.length > 0 ? countries[0] : '...'}`;

    document.getElementById('modal-overview').innerText = show.overview || 'Опис відсутній.';

    document.getElementById('tv-modal').classList.add('active');
    document.body.classList.add('modal-open');

    try {
        const detailsRes = await fetch(`${API_BASE_URL}/details/tv-shows/${show.id}?language=${currentLang}`);
        const details = await detailsRes.json();

        const seasonsCount = details.number_of_seasons || details.numberOfSeasons;
        document.getElementById('modal-seasons-count').innerText = `Усього сезонів: ${seasonsCount || '...'}`;

        currentShowSeasons = details.seasons || [];
        renderSeasonTabs();

        if (currentShowSeasons.length > 0) {
            loadEpisodes(0);
        } else {
            document.getElementById('episodes-grid').innerHTML = '<span style="color:#aaa;">Немає інформації про серії</span>';
        }
    } catch (e) {
        console.error("Ошибка загрузки деталей", e);
    }
}

function renderSeasonTabs() {
    const tabsContainer = document.getElementById('season-tabs');
    tabsContainer.innerHTML = currentShowSeasons.map((s, index) => `
        <button class="season-tab ${index === 0 ? 'active' : ''}"
                onclick="loadEpisodes(${index}, this)">
            Сезон ${s.season_number || s.seasonNumber}
        </button>
    `).join('');
}

function loadEpisodes(seasonIndex, tabElement = null) {
    const seasonObj = currentShowSeasons[seasonIndex];

    currentSelectedSeason = seasonObj.season_number || seasonObj.seasonNumber;
    currentSelectedEpisode = 0;
    document.getElementById('check-all-season').checked = false;

    if (tabElement) {
        document.querySelectorAll('.season-tab').forEach(t => t.classList.remove('active'));
        tabElement.classList.add('active');
    }

    const grid = document.getElementById('episodes-grid');
    const epCount = seasonObj.episode_count || seasonObj.episodeCount;

    if (epCount && epCount > 0) {
        let buttonsHtml = '';
        for (let i = 1; i <= epCount; i++) {
            buttonsHtml += `<button class="episode-btn" id="ep-btn-${i}" onclick="selectEpisode(${i})">${i}</button>`;
        }
        grid.innerHTML = buttonsHtml;
    } else {
        grid.innerHTML = '<span style="color:#aaa;">Серії не знайдені</span>';
    }
}

function selectEpisode(epNum) {
    currentSelectedEpisode = epNum;
    const allBtns = document.querySelectorAll('.episode-btn');

    allBtns.forEach(btn => {
        const currentNum = parseInt(btn.innerText);
        if (currentNum <= epNum) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    document.getElementById('check-all-season').checked = (epNum === allBtns.length && allBtns.length > 0);
}

function toggleAllEpisodes(checkbox) {
    const allBtns = document.querySelectorAll('.episode-btn');
    if (checkbox.checked) {
        selectEpisode(allBtns.length);
    } else {
        selectEpisode(0);
    }
}

function closeModal() {
    document.getElementById('tv-modal').classList.remove('active');
    document.body.classList.remove('modal-open');
}

// СОХРАНЕНИЕ СЕРИАЛА
async function saveTvShowProgress() {
    if (currentSelectedEpisode === 0) {
        showToast("Оберіть хоча б одну серію!", true);
        return;
    }

    const countries = currentShowData.origin_country || currentShowData.originCountry;
    const originCountryStr = (countries && countries.length > 0) ? countries[0] : "US";

    const payload = {
        tvShowId: currentShowData.id,
        name: currentShowData.name || currentShowData.title,
        posterUrl: currentShowData.poster_path || currentShowData.posterPath,
        firstAirDate: currentShowData.first_air_date || currentShowData.firstAirDate,
        originCountry: originCountryStr,
        currentSeason: currentSelectedSeason,
        currentEpisode: currentSelectedEpisode
    };

    const btnSave = document.querySelector('#tv-modal .btn-save');
    const originalText = btnSave.innerText;
    btnSave.innerText = '⏳ Збереження...';

    try {
        const response = await fetch(`${API_BASE_URL}/watchlist-tv-shows`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        // ПРОВЕРКА НА АВТОРИЗАЦИЮ
        if (response.status === 401 || response.status === 403 || (response.redirected && response.url.includes('/login'))) {
            window.location.href = '/login.html';
            return;
        }

        if (response.ok) {
            closeModal();
            showToast("Прогрес успішно збережено!");
        } else {
            showToast("Помилка при збереженні", true);
        }
    } catch (e) {
        showToast("Помилка підключення до сервера", true);
    } finally {
        btnSave.innerText = originalText;
    }
}

// ==========================================
// ГЛОБАЛЬНЫЕ СЛУШАТЕЛИ И УТИЛИТЫ
// ==========================================

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
        document.body.classList.remove('modal-open');
        currentSelectedMovie = null;
    }
});

window.addEventListener('scroll', () => {
    if (isLoading || currentPage >= totalPages) return;
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 600) {
        currentPage++;
        loadCatalog();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const slider = document.getElementById('trending-slider');
    if (slider) slider.addEventListener('scroll', updateNavButtons);

    const langSelect = document.getElementById('langSelect');
    if (langSelect) currentLang = langSelect.value;

    if (document.getElementById('trending-slider')) {
        loadTrending();
        loadCatalog();
    }
});

function formatList(list) {
    if (!list || !Array.isArray(list) || list.length === 0) return '...';
    return list.map(item => typeof item === 'object' ? item.name : item).join(', ');
}

function formatGenres(genres) {
    if (!genres || !Array.isArray(genres) || genres.length === 0) return '';
    return genres.map(g => {
        const name = typeof g === 'object' ? g.name : g;
        return `<span class="genre-tag">${name}</span>`;
    }).join('');
}

function toggleOverview(textId, btn) {
    const textEl = document.getElementById(textId);
    if (textEl.classList.contains('collapsed')) {
        textEl.classList.remove('collapsed');
        textEl.classList.add('expanded');
        btn.innerText = 'Згорнути';
    } else {
        textEl.classList.remove('expanded');
        textEl.classList.add('collapsed');
        btn.innerText = 'Читати далі';
    }
}

function setupOverview(textId, btnId, text) {
    const textEl = document.getElementById(textId);
    const btnEl = document.getElementById(btnId);
    textEl.innerText = text || 'Опис відсутній.';
    textEl.classList.remove('expanded');
    textEl.classList.add('collapsed');
    btnEl.innerText = 'Читати далі';
    btnEl.style.display = 'none';

    setTimeout(() => {
        if (textEl.scrollHeight > textEl.clientHeight) btnEl.style.display = 'inline-block';
    }, 10);
}