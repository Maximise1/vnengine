document.addEventListener('click', function(e) {
    const button = e.target.closest('[data-action]');

    if (button) {
        const action = button.getAttribute('data-action');
        const argsStr = button.getAttribute('data-args');

        if (argsStr) {
            try {
                const args = JSON.parse(argsStr);
                window.bridge.callLua(action, ...args);
            } catch (err) {
                console.error('Failed to parse data-args:', err);
            }
        } else {
            window.bridge.callLua(action);
        }
    }
});

console.log('Bridge.js loaded');