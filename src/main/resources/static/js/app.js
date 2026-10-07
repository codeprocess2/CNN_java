let currentInspectionId = null;

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    loadUsers();
    setupDropZone();
    setupFormSubmit();
});

function loadDashboardStats() {
    fetch('/api/dashboard/stats').then(r => r.json()).then(d => {
        document.getElementById('stat-total').innerText = d.totalInspections || 0;
        document.getElementById('stat-open').innerText = d.openIncidents || 0;
        document.getElementById('stat-high').innerText = d.highSeverityIncidents || 0;
        document.getElementById('stat-resolved').innerText = d.resolvedIncidents || 0;
        document.getElementById('stat-avg-conf').innerText = (d.averageConfidence ? Math.round(d.averageConfidence * 100) : 0) + '%';
    });
}

function loadUsers() {
    fetch('/api/users').then(r => r.json()).then(users => {
        const s = document.getElementById('user-select');
        s.innerHTML = '<option value="">Select Inspector...</option>';
        users.forEach(u => s.innerHTML += `<option value="${u.id}">${u.name} (${u.role})</option>`);
    });
}

function setupDropZone() {
    const dz = document.getElementById('drop-zone');
    const input = document.getElementById('file-input');
    dz.onclick = () => input.click();
    dz.ondragover = (e) => { e.preventDefault(); dz.classList.add('bg-light'); };
    dz.ondragleave = () => dz.classList.remove('bg-light');
    dz.ondrop = (e) => {
        e.preventDefault();
        dz.classList.remove('bg-light');
        if (e.dataTransfer.files.length) { input.files = e.dataTransfer.files; handleFileSelect(); }
    };
    input.onchange = handleFileSelect;
}

function handleFileSelect() {
    const input = document.getElementById('file-input');
    if (!input.files.length) return;
    const file = input.files[0];
    document.getElementById('file-name').innerText = file.name;
    const reader = new FileReader();
    reader.onload = (e) => {
        document.getElementById('image-preview').src = e.target.result;
        document.getElementById('preview-container').classList.remove('d-none');
    };
    reader.readAsDataURL(file);
}

function setupFormSubmit() {
    document.getElementById('inspection-upload-form').onsubmit = (e) => {
        e.preventDefault();
        const input = document.getElementById('file-input');
        if (!input.files.length) return alert('Select image file first');
        const fd = new FormData();
        fd.append('file', input.files[0]);
        fd.append('description', document.getElementById('upload-description').value);

        fetch('/api/inspections', { method: 'POST', body: fd })
            .then(r => { if (!r.ok) throw new Error('Upload failed'); return r.json(); })
            .then(d => { displayResult(d); loadDashboardStats(); })
            .catch(err => alert(err.message));
    };
}

function displayResult(d) {
    document.getElementById('result-placeholder').classList.add('d-none');
    document.getElementById('result-content').classList.remove('d-none');
    document.getElementById('res-id').innerText = d.inspectionId;
    document.getElementById('res-class').innerText = d.predictedClass;
    document.getElementById('res-confidence').innerText = Math.round(d.confidence * 100) + '%';
    document.getElementById('res-severity-badge').innerHTML = `<span class="badge badge-${d.severity.toLowerCase()}">${d.severity}</span>`;
    document.getElementById('res-status-badge').innerHTML = `<span class="badge badge-${d.status.toLowerCase()}">${d.status}</span>`;
    document.getElementById('res-desc').innerText = d.description;
}

function loadInspections() {
    const status = document.getElementById('filter-status').value;
    const severity = document.getElementById('filter-severity').value;
    let url = '/api/inspections?';
    if (status) url += `status=${status}&`;
    if (severity) url += `severity=${severity}`;

    fetch(url).then(r => r.json()).then(data => {
        const tbody = document.getElementById('inspection-table-body');
        tbody.innerHTML = '';
        if (!data.length) { tbody.innerHTML = '<tr><td colspan="9" class="text-center py-3">No records</td></tr>'; return; }
        data.forEach(i => {
            const assigned = i.assignedTo ? i.assignedTo.name : '<span class="text-muted">Unassigned</span>';
            const dateStr = i.createdAt ? new Date(i.createdAt).toLocaleString() : '';
            tbody.innerHTML += `<tr>
                <td>#${i.inspectionId}</td>
                <td><img src="/${i.imagePath}" class="rounded border" style="width:40px; height:40px; object-fit:cover;"></td>
                <td><strong>${i.predictedClass}</strong></td>
                <td>${Math.round(i.confidence * 100)}%</td>
                <td><span class="badge badge-${i.severity.toLowerCase()}">${i.severity}</span></td>
                <td><span class="badge badge-${i.status.toLowerCase()}">${i.status}</span></td>
                <td>${assigned}</td>
                <td class="small text-muted">${dateStr}</td>
                <td><button class="btn btn-sm btn-outline-primary" onclick="openDetailModal(${i.inspectionId})">Details</button></td>
            </tr>`;
        });
    });
}

function loadAnalytics() {
    fetch('/api/dashboard/stats').then(r => r.json()).then(d => {
        const c = document.getElementById('damage-distribution-container');
        c.innerHTML = '';
        const dist = d.damageDistribution || {};
        Object.keys(dist).forEach(k => {
            c.innerHTML += `<div class="col-md-4"><div class="card p-3 border shadow-sm"><div class="text-muted small">${k}</div><div class="fs-4 fw-bold mt-1">${dist[k]}</div></div></div>`;
        });
    });
}

function openDetailModal(id) {
    currentInspectionId = id;
    fetch(`/api/inspections/${id}`).then(r => r.json()).then(d => {
        document.getElementById('modal-id').innerText = d.inspectionId;
        document.getElementById('modal-img').src = '/' + d.imagePath;
        document.getElementById('modal-class').innerText = d.predictedClass;
        document.getElementById('modal-conf').innerText = Math.round(d.confidence * 100) + '%';
        document.getElementById('modal-sev').innerHTML = `<span class="badge badge-${d.severity.toLowerCase()}">${d.severity}</span>`;
        document.getElementById('modal-status').innerHTML = `<span class="badge badge-${d.status.toLowerCase()}">${d.status}</span>`;
        document.getElementById('modal-assignee').innerText = d.assignedTo ? d.assignedTo.name : 'Unassigned';

        fetch(`/api/inspections/${id}/history`).then(r => r.json()).then(h => {
            const tl = document.getElementById('modal-timeline');
            tl.innerHTML = '';
            h.forEach(i => {
                tl.innerHTML += `<div class="list-group-item px-0"><div class="d-flex justify-content-between"><strong>${i.action}</strong><span class="text-muted">${new Date(i.timestamp).toLocaleString()}</span></div><div>${i.description}</div><div class="text-muted">By: ${i.performedBy}</div></div>`;
            });
        });
        new bootstrap.Modal(document.getElementById('detailModal')).show();
    });
}

function assignUser() {
    const uid = document.getElementById('user-select').value;
    if (!uid || !currentInspectionId) return;
    fetch(`/api/inspections/${currentInspectionId}/assign/${uid}`, { method: 'PUT' })
        .then(r => r.json()).then(() => { openDetailModal(currentInspectionId); loadInspections(); loadDashboardStats(); });
}

function escalateCurrent() {
    if (!currentInspectionId) return;
    fetch(`/api/inspections/${currentInspectionId}/escalate`, { method: 'PUT' })
        .then(r => r.json()).then(() => { openDetailModal(currentInspectionId); loadInspections(); loadDashboardStats(); });
}

function resolveCurrent() {
    if (!currentInspectionId) return;
    fetch(`/api/inspections/${currentInspectionId}/resolve`, { method: 'PUT' })
        .then(r => r.json()).then(() => { openDetailModal(currentInspectionId); loadInspections(); loadDashboardStats(); });
}
