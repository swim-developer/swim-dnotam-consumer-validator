var consoleEventSource = null;
var consoleEnabled = true;

function toggleConsole() {
    const consoleEl = document.getElementById('server-console');
    const content = document.querySelector('.content');
    consoleEl.classList.toggle('collapsed');
    consoleEl.classList.toggle('expanded');
    content.classList.toggle('with-console');
}

function addConsoleEntry(type, message) {
    const body = document.getElementById('console-body');
    if (!body) return;
    const empty = body.querySelector('.console-empty');
    if (empty) empty.remove();
    
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = 'console-entry';
    entry.innerHTML = '<span class="console-time">' + time + '</span>' +
                      '<span class="console-type ' + type + '">' + type + '</span>' +
                      '<span class="console-message">' + message + '</span>';
    body.insertBefore(entry, body.firstChild);
    
    const count = document.getElementById('console-count');
    if (count) {
        count.textContent = parseInt(count.textContent || 0) + 1;
        count.style.display = 'inline-block';
    }
    
    if (body.children.length > 100) {
        body.removeChild(body.lastChild);
    }
}

function closeConsole() {
    consoleEnabled = false;
    if (consoleEventSource) {
        consoleEventSource.close();
        consoleEventSource = null;
    }
}

function initConsole() {
    if (!consoleEnabled) return;
    if (consoleEventSource) return;
    
    consoleEventSource = new EventSource('/admin/console/stream');
    
    consoleEventSource.onmessage = function(event) {
        if (!event.data || event.data.startsWith(':')) return;
        try {
            var data = JSON.parse(event.data);
            if (data.type === 'heartbeat') return;
            addConsoleEntry(data.type, data.message);
        } catch (e) {}
    };
    
    consoleEventSource.onerror = function() {
        if (consoleEventSource) {
            consoleEventSource.close();
            consoleEventSource = null;
        }
        if (consoleEnabled) {
            setTimeout(initConsole, 5000);
        }
    };
}

window.addEventListener('beforeunload', closeConsole);
window.addEventListener('pagehide', closeConsole);
document.addEventListener('DOMContentLoaded', initConsole);
