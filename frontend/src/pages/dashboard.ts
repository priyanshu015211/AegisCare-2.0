import { api, type DashboardSummary } from '../api/client';
import { statTile, panelHeader, severityBadge, severityColor, escapeHtml } from '../components/ui';
import { donutChart } from '../components/charts';

export async function renderDashboard(container: HTMLElement) {
  container.innerHTML = `
    <div class="page-loading"><span class="loading-spinner"></span> Loading network overview</div>
  `;

  try {
    const data = await api.get<DashboardSummary>('/dashboard/summary');
    container.innerHTML = buildView(data);
    const refresh = container.querySelector('#refresh-overview');
    refresh?.addEventListener('click', () => void renderDashboard(container));
  } catch (e) {
    container.innerHTML = `<div class="panel p-6 text-severity-critical text-sm">Unable to load overview: ${escapeHtml((e as Error).message)}</div>`;
  }
}

function buildView(data: DashboardSummary): string {
  const distribution = Object.entries(data.risk_distribution);
  const totalCases = distribution.reduce((sum, [, value]) => sum + value, 0);
  const donutData = distribution.map(([label, value]) => ({ label, value, color: severityColor(label) }));

  const activityRows = data.recent_activity.map(a => `
    <tr>
      <td><span class="patient-id">${escapeHtml(a.patient_id)}</span></td>
      <td>${severityBadge(a.severity)}</td>
      <td><span class="risk-score">${a.risk_score}<small>/100</small></span></td>
      <td><span class="table-date">${escapeHtml(new Date(a.started_at).toLocaleString())}</span></td>
    </tr>
  `).join('');

  const riskBars = distribution.map(([label, value]) => {
    const percent = totalCases ? Math.round((value / totalCases) * 100) : 0;
    return `
      <div class="risk-row">
        <div class="risk-row-meta"><span class="risk-key"><i style="background:${severityColor(label)}"></i>${escapeHtml(label)}</span><strong>${value}<small> · ${percent}%</small></strong></div>
        <div class="risk-track"><span style="width:${percent}%;background:${severityColor(label)}"></span></div>
      </div>
    `;
  }).join('');

  return `
    <div class="dashboard-page">
      <section class="page-intro">
        <div>
          <div class="eyebrow"><span class="eyebrow-line"></span> NETWORK OVERVIEW</div>
          <h1>Good evening, <span>Operations team</span></h1>
          <p>A clear view of patient flow, facility capacity and care coordination.</p>
        </div>
        <div class="intro-actions">
          <span class="updated-label"><span class="status-dot status-dot-teal"></span> Live network snapshot</span>
          <button id="refresh-overview" class="button-secondary" type="button"><span aria-hidden="true">↻</span> Refresh data</button>
          <a class="button-primary" href="#triage"><span aria-hidden="true">＋</span> New triage</a>
        </div>
      </section>

      <section class="metrics-grid" aria-label="Network metrics">
        ${statTile('Hospital capacity', `${data.hospital_load_pct}%`, `${data.hospitals_tracked} facilities reporting`, '▤')}
        ${statTile('Active emergencies', `${data.active_emergencies}`, 'Critical or escalated cases', '＋')}
        ${statTile('Triage sessions', `${data.total_sessions}`, 'Recent recorded assessments', '◷')}
        ${statTile('Classified cases', `${totalCases}`, 'Across all risk levels', '◈')}
      </section>

      <section class="dashboard-grid dashboard-grid-primary">
        <article class="panel analytics-panel">
          ${panelHeader('Patient risk profile', 'Distribution of classified triage cases')}
          <div class="risk-visual">
            <div class="donut-wrap">${donutChart(donutData, { size: 184, thickness: 18 })}</div>
            <div class="risk-legend">${riskBars || '<p class="empty-copy">No classified cases available.</p>'}</div>
          </div>
          <div class="panel-footnote"><span class="footnote-mark">i</span> Risk categories reflect the latest recorded assessments.</div>
        </article>

        <article class="panel capacity-panel">
          ${panelHeader('Network capacity', 'Current facility utilization')}
          <div class="capacity-highlight">
            <div><span class="capacity-number">${data.hospital_load_pct}<small>%</small></span><p>Overall reported occupancy</p></div>
            <span class="capacity-icon">▤</span>
          </div>
          <div class="capacity-track"><span style="width:${Math.max(0, Math.min(100, data.hospital_load_pct))}%"></span></div>
          <div class="capacity-scale"><span>0% · Available</span><span>100% · Full</span></div>
          <div class="capacity-divider"></div>
          <div class="capacity-detail">
            <div><span class="detail-indicator teal"></span><span>Facilities tracked</span><strong>${data.hospitals_tracked}</strong></div>
            <div><span class="detail-indicator amber"></span><span>Active emergencies</span><strong>${data.active_emergencies}</strong></div>
          </div>
          <a href="#coordination" class="text-link">View coordination workspace <span>→</span></a>
        </article>
      </section>

      <section class="panel activity-panel">
        <div class="activity-heading">
          ${panelHeader('Recent triage activity', 'Latest assessments recorded across the network')}
          <a href="#emergency" class="text-link">Open emergency center <span>→</span></a>
        </div>
        <div class="table-scroll">
          <table class="data-table">
            <thead><tr><th>Patient reference</th><th>Classification</th><th>Risk score</th><th>Recorded at</th></tr></thead>
            <tbody>${activityRows || '<tr><td colspan="4" class="empty-cell">No triage activity has been recorded yet.</td></tr>'}</tbody>
          </table>
        </div>
        <div class="table-footer"><span>Showing ${data.recent_activity.length} recent records</span><span>Updated from the AegisCare service</span></div>
      </section>

      <div class="dashboard-disclaimer"><span>ⓘ</span> Operational overview only. Clinical decisions must be reviewed by qualified healthcare professionals.</div>
    </div>
  `;
}
