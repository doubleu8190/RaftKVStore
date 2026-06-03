/**
 * RaftKVStore Admin UI - Client-side logic.
 * Uses the Fetch API to interact with the admin REST endpoints.
 */
(function () {
    'use strict';

    // ---- Status polling ----

    async function checkStatus() {
        try {
            const resp = await fetch('/api/status');
            const json = await resp.json();
            const badge = document.getElementById('status-badge');
            if (json.success && json.data === true) {
                badge.textContent = 'Connected';
                badge.className = 'badge connected';
            } else {
                badge.textContent = 'Disconnected';
                badge.className = 'badge disconnected';
            }
        } catch (e) {
            const badge = document.getElementById('status-badge');
            badge.textContent = 'Disconnected';
            badge.className = 'badge disconnected';
        }
    }

    // ---- Helpers ----

    function showResult(elementId, result) {
        const el = document.getElementById(elementId);
        el.classList.remove('hidden');
        if (result.success) {
            el.classList.remove('error');
        } else {
            el.classList.add('error');
        }
        el.textContent = JSON.stringify(result, null, 2);
    }

    async function postJson(url, body) {
        const resp = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        return resp.json();
    }

    // ---- SET ----

    document.getElementById('set-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const key = document.getElementById('set-key').value.trim();
        const value = document.getElementById('set-value').value.trim();
        const result = await postJson('/api/set', { key: key, value: value });
        showResult('set-result', result);
    });

    // ---- GET ----

    document.getElementById('get-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const key = document.getElementById('get-key').value.trim();
        const result = await postJson('/api/get', { key: key });
        showResult('get-result', result);
    });

    // ---- Cluster Info ----

    document.getElementById('cluster-info-btn').addEventListener('click', async function () {
        const errorEl = document.getElementById('cluster-info-error');
        const tableEl = document.getElementById('cluster-info-result');
        try {
            const resp = await fetch('/api/cluster-info');
            const json = await resp.json();
            if (json.success) {
                const info = json.data;
                document.getElementById('ci-leader').textContent = info.leader || '-';
                document.getElementById('ci-mode').textContent = info.mode || '-';
                document.getElementById('ci-phase').textContent = info.phase || '-';
                document.getElementById('ci-old-config').textContent = info.oldConfig || '-';
                document.getElementById('ci-new-config').textContent = info.newConfig || '-';
                document.getElementById('ci-size').textContent = info.size != null ? info.size : '-';
                tableEl.classList.remove('hidden');
                errorEl.classList.add('hidden');
            } else {
                tableEl.classList.add('hidden');
                errorEl.classList.remove('hidden');
                errorEl.textContent = 'Error: ' + json.error;
            }
        } catch (e) {
            tableEl.classList.add('hidden');
            errorEl.classList.remove('hidden');
            errorEl.textContent = 'Network error: ' + e.message;
        }
    });

    // ---- Add Node ----

    document.getElementById('add-node-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const nodeId = document.getElementById('an-node-id').value.trim();
        const host = document.getElementById('an-host').value.trim();
        const port = parseInt(document.getElementById('an-port').value, 10);
        const connectorPort = parseInt(document.getElementById('an-conn-port').value, 10);
        const result = await postJson('/api/add-node', {
            nodeId: nodeId,
            host: host,
            port: port,
            connectorPort: connectorPort
        });
        showResult('add-node-result', result);
    });

    // ---- Remove Node ----

    document.getElementById('remove-node-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const nodeId = document.getElementById('rn-node-id').value.trim();
        const result = await postJson('/api/remove-node', { nodeId: nodeId });
        showResult('remove-node-result', result);
    });

    // ---- Init ----

    checkStatus();
    setInterval(checkStatus, 5000); // poll status every 5 seconds
})();
