/**
 * Toast Notification Utility
 * 앱 전역 알림 시스템
 */
(function(global) {
    'use strict';

    var ICONS = {
        success: '✓',
        error: '✕',
        warning: '⚠',
        info: 'ℹ'
    };

    var DEFAULT_DURATION = 4000;
    var container = null;

    function ensureContainer() {
        if (container) return container;
        container = document.createElement('div');
        container.className = 'toast-container';
        container.setAttribute('role', 'alert');
        container.setAttribute('aria-live', 'polite');
        document.body.appendChild(container);
        return container;
    }

    function createToast(options) {
        var type = options.type || 'info';
        var title = options.title || '';
        var message = options.message || '';
        var duration = options.duration !== undefined ? options.duration : DEFAULT_DURATION;

        var toast = document.createElement('div');
        toast.className = 'toast toast-' + type;

        var icon = document.createElement('span');
        icon.className = 'toast-icon';
        icon.textContent = ICONS[type] || ICONS.info;
        icon.setAttribute('aria-hidden', 'true');
        toast.appendChild(icon);

        var content = document.createElement('div');
        content.className = 'toast-content';
        if (title) {
            var titleEl = document.createElement('div');
            titleEl.className = 'toast-title';
            titleEl.textContent = title;
            content.appendChild(titleEl);
        }
        if (message) {
            var messageEl = document.createElement('div');
            messageEl.className = 'toast-message';
            messageEl.textContent = message;
            content.appendChild(messageEl);
        }
        toast.appendChild(content);

        var closeBtn = document.createElement('button');
        closeBtn.className = 'toast-close';
        closeBtn.type = 'button';
        closeBtn.innerHTML = '&times;';
        closeBtn.setAttribute('aria-label', '닫기');
        closeBtn.addEventListener('click', function() {
            dismissToast(toast);
        });
        toast.appendChild(closeBtn);

        return { element: toast, duration: duration };
    }

    function dismissToast(toast) {
        toast.classList.add('toast-out');
        setTimeout(function() {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }

    function show(options) {
        var toastContainer = ensureContainer();
        var toastData = createToast(options);
        var toast = toastData.element;

        toastContainer.appendChild(toast);

        if (toastData.duration > 0) {
            setTimeout(function() {
                dismissToast(toast);
            }, toastData.duration);
        }

        return {
            dismiss: function() {
                dismissToast(toast);
            }
        };
    }

    var Toast = {
        show: show,
        success: function(title, message, duration) {
            return show({ type: 'success', title: title, message: message, duration: duration });
        },
        error: function(title, message, duration) {
            return show({ type: 'error', title: title, message: message, duration: duration });
        },
        warning: function(title, message, duration) {
            return show({ type: 'warning', title: title, message: message, duration: duration });
        },
        info: function(title, message, duration) {
            return show({ type: 'info', title: title, message: message, duration: duration });
        }
    };

    global.Toast = Toast;

})(window);
