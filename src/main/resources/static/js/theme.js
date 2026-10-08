/*
 * Auto Spare Pro — shared UI behaviour, loaded in <head> of every page.
 *  1. Applies the saved light/dark theme immediately (no flash on load).
 *  2. Injects the theme toggle button into the nav (or floats it on login/signup).
 *  3. Highlights the current page's nav link.
 *  4. Marks images that fail to load so the CSS can show a placeholder instead.
 */
(function () {
    var KEY = 'asp-theme';
    var root = document.documentElement;

    function stored() {
        try { return localStorage.getItem(KEY); } catch (e) { return null; }
    }

    function save(theme) {
        try { localStorage.setItem(KEY, theme); } catch (e) { /* private mode — theme just won't persist */ }
    }

    function apply(theme) {
        root.setAttribute('data-theme', theme);
    }

    apply(stored() === 'dark' ? 'dark' : 'light');

    // Broken <img> -> add .is-broken (capture phase: 'error' doesn't bubble)
    document.addEventListener('error', function (e) {
        var t = e.target;
        if (t && t.tagName === 'IMG') t.classList.add('is-broken');
    }, true);

    var SUN = '<svg class="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>';
    var MOON = '<svg class="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';

    function buildToggle() {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'theme-toggle';
        btn.innerHTML = SUN + MOON;

        function sync() {
            var dark = root.getAttribute('data-theme') === 'dark';
            btn.setAttribute('aria-pressed', dark ? 'true' : 'false');
            btn.setAttribute('aria-label', dark ? 'Switch to light theme' : 'Switch to dark theme');
            btn.title = dark ? 'Switch to light theme' : 'Switch to dark theme';
        }

        btn.addEventListener('click', function () {
            var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            apply(next);
            save(next);
            sync();
        });

        sync();
        return btn;
    }

    function highlightNav() {
        var path = location.pathname.replace(/\/+$/, '') || '/';
        var best = null, bestLen = -1;
        document.querySelectorAll('nav a[href]').forEach(function (a) {
            var p = a.pathname.replace(/\/+$/, '') || '/';
            var match = p === path || (p !== '/' && path.indexOf(p + '/') === 0);
            if (match && p.length > bestLen) { best = a; bestLen = p.length; }
        });
        if (best) best.classList.add('active');
    }

    document.addEventListener('DOMContentLoaded', function () {
        var toggle = buildToggle();
        var navRight = document.querySelector('nav .nav-right');
        if (navRight) {
            navRight.insertBefore(toggle, navRight.firstChild);
        } else {
            toggle.classList.add('theme-toggle-floating');
            document.body.appendChild(toggle);
        }
        highlightNav();
    });
})();
