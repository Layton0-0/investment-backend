/**
 * Mobile Navigation Toggle
 * 모바일 햄버거 메뉴 처리
 */
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        var nav = document.querySelector('.app-nav');
        var navInner = document.querySelector('.app-nav .nav-inner');
        
        if (!nav || !navInner) return;

        var toggle = document.createElement('button');
        toggle.className = 'mobile-menu-toggle';
        toggle.type = 'button';
        toggle.innerHTML = '&#9776;';
        toggle.setAttribute('aria-label', '메뉴 열기');
        toggle.setAttribute('aria-expanded', 'false');

        nav.insertBefore(toggle, nav.firstChild);

        toggle.addEventListener('click', function() {
            var isOpen = navInner.classList.toggle('nav-open');
            toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
            toggle.setAttribute('aria-label', isOpen ? '메뉴 닫기' : '메뉴 열기');
            toggle.innerHTML = isOpen ? '&times;' : '&#9776;';
        });

        document.addEventListener('click', function(e) {
            if (!nav.contains(e.target) && navInner.classList.contains('nav-open')) {
                navInner.classList.remove('nav-open');
                toggle.setAttribute('aria-expanded', 'false');
                toggle.setAttribute('aria-label', '메뉴 열기');
                toggle.innerHTML = '&#9776;';
            }
        });

        var mediaQuery = window.matchMedia('(min-width: 769px)');
        function handleResize(e) {
            if (e.matches && navInner.classList.contains('nav-open')) {
                navInner.classList.remove('nav-open');
                toggle.setAttribute('aria-expanded', 'false');
                toggle.innerHTML = '&#9776;';
            }
        }
        if (mediaQuery.addEventListener) {
            mediaQuery.addEventListener('change', handleResize);
        } else if (mediaQuery.addListener) {
            mediaQuery.addListener(handleResize);
        }
    });
})();
