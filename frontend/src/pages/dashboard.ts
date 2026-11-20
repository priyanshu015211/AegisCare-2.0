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
    container.querySelector('#refresh-overview')?.addEventListener('click', () => {
      void renderDashboard(container);
    });
  } catch (e) {
    container.innerHTML = `
      <section class="panel dashboard-error" role="alert">
        <span class="error-mark">!</span>
        <div><h2>We couldn't load the network overview</h2>
        <p>${escapeHtml((e as Error).message)}</p>
        <button id="retry-overview" class="button-secondary" type="button">Try again</button></div>
      </section>`;
    container.querySelector('#retry-overview')?.addEventListener('click', () => {
      void renderDashboard(container);
    });
  }
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

function buildView(data: DashboardSummary): string {
  const distribution = Object.entries(data.risk_distribution ?? {});
  const totalCases = distribution.reduce((sum, [, value]) => sum + value, 0);
  const donutData = distribution.map(([label, value]) => ({
    label, value, color: severityColor(label),
  }));
  const safeLoad = Math.max(0, Math.min(100, Number(data.hospital_load_pct) || 0));
  const activity = [...(data.recent_activity ?? [])];
  const activityRows = activity.map(a => `
    <tr>
      <td><span class="patient-id">${escapeHtml(a.patient_id)}</span></td>
      <td>${severityBadge(a.severity)}</td>
      <td><span class="risk-score">${Number(a.risk_score)}<small>/100</small></span></td>
      <td><span class="table-date">${escapeHtml(new Date(a.started_at).toLocaleString())}</span></td>
    </tr>
  `).join('');

  const riskBars = distribution.map(([label, value]) => {
    const percent = totalCases ? Math.round((value / totalCases) * 100) : 0;
    return `
      <div class="risk-row">
        <div class="risk-row-meta">
          <span class="risk-key"><i style="background:${severityColor(label)}"></i>${escapeHtml(label)}</span>
          <strong>${value}<small> · ${percent}%</small></strong>
        </div>
        <div class="risk-track"><span style="width:${percent}%;background:${severityColor(label)}"></span></div>
      </div>
    `;
  }).join('');

  const priorityCases = [...activity]
    .sort((a, b) => Number(b.risk_score) - Number(a.risk_score))
    .slice(0, 3);
  const priorityRows = priorityCases.map((item, index) => `
    <a class="priority-row" href="#emergency">
      <span class="priority-rank">${String(index + 1).padStart(2, '0')}</span>
      <span class="priority-patient">
        <strong>${escapeHtml(item.patient_id)}</strong>
        <small>Risk score ${Number(item.risk_score)}/100</small>
      </span>
      ${severityBadge(item.severity)}
      <span class="priority-arrow" aria-hidden="true">↗</span>
    </a>
  `).join('');

  const dateLabel = new Intl.DateTimeFormat(undefined, {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  }).format(new Date());

  return `
    <div class="dashboard-page dashboard-v2">
      <section class="dashboard-hero">
        <div class="hero-copy">
          <div class="eyebrow"><span class="eyebrow-line"></span> AEGISCARE / NETWORK OPERATIONS</div>
          <p class="hero-date">${escapeHtml(dateLabel)}</p>
          <h1>${greeting()}, <span>Operations team.</span></h1>
          <p class="hero-description">A focused view of patient flow, facility capacity and the cases that need attention.</p>
          <div class="hero-actions">
            <a class="button-primary" href="#triage"><span aria-hidden="true">＋</span> Start new triage</a>
            <button id="refresh-overview" class="button-hero-secondary" type="button"><span aria-hidden="true">↻</span> Refresh overview</button>
          </div>
        </div>
        <aside class="hero-status" aria-label="Network status summary">
          <div class="hero-status-top">
            <span class="hero-status-label">NETWORK STATUS</span>
            <span class="hero-live"><i></i> Reporting</span>
          </div>
          <div class="hero-status-main">
            <span class="hero-status-value">${data.hospitals_tracked}</span>
            <span class="hero-status-caption">facilities reporting</span>
          </div>
          <div class="hero-status-divider"></div>
          <div class="hero-status-foot">
            <span>Average occupancy</span>
            <strong>${safeLoad}%</strong>
          </div>
          <div class="hero-status-track"><span style="width:${safeLoad}%"></span></div>
          <p>Based on the latest available network summary</p>
        </aside>
      </section>

      <section class="metrics-grid metrics-grid-v2" aria-label="Network metrics">
        ${statTile('Active emergencies', `${data.active_emergencies}`, 'Cases escalated or marked critical', '＋')}
        ${statTile('Triage sessions', `${data.total_sessions}`, 'Recorded assessments', '◷')}
        ${statTile('Classified cases', `${totalCases}`, 'Across all risk levels', '◈')}
        ${statTile('Hospital capacity', `${safeLoad}%`, `${data.hospitals_tracked} facilities reporting`, '▤')}
      </section>

      <section class="attention-strip" aria-label="Priority overview">
        <div class="attention-icon">!</div>
        <div class="attention-copy">
          <strong>${Number(data.active_emergencies) > 0 ? `${data.active_emergencies} active emergencies require operational attention` : 'No active emergencies reported'}</strong>
          <span>Review escalation status and coordinate with the relevant facility.</span>
        </div>
        <a href="#emergency">Review emergency center <span aria-hidden="true">→</span></a>
      </section>

      <section class="dashboard-grid dashboard-grid-v2">
        <article class="panel analytics-panel analytics-panel-v2">
          ${panelHeader('Patient risk profile', 'Distribution of classified triage cases')}
          <div class="risk-visual">
            <div class="donut-wrap">${donutChart(donutData, { size: 174, thickness: 20 })}</div>
            <div class="risk-legend">${riskBars || '<p class="empty-copy">No classified cases available.</p>'}</div>
          </div>
          <div class="panel-footnote"><span class="footnote-mark">i</span> Risk categories reflect recorded assessments, not a clinical diagnosis.</div>
        </article>

        <article class="panel priority-panel">
          <div class="panel-heading">
            <div><h2>Highest-risk recent assessments</h2><p>Prioritize review using recorded risk scores</p></div>
            <span class="panel-heading-tag">TOP 3</span>
          </div>
          <div class="priority-list">
            ${priorityRows || '<p class="empty-copy priority-empty">No recent assessments are available.</p>'}
          </div>
          <div class="priority-note"><span class="footnote-mark">i</span> Sorted by score. Clinical review is required before action.</div>
          <a href="#triage" class="text-link">Open patient triage <span>→</span></a>
        </article>
      </section>

      <section class="panel capacity-panel capacity-panel-v2">
        <div class="capacity-v2-heading">
          ${panelHeader('Facility capacity', 'Network-level utilization snapshot')}
          <a href="#coordination" class="text-link">View coordination workspace <span>→</span></a>
        </div>
        <div class="capacity-v2-content">
          <div class="capacity-v2-stat">
            <span class="capacity-number">${safeLoad}<small>%</small></span>
            <p>Reported occupancy</p>
          </div>
          <div class="capacity-v2-meter">
            <div class="capacity-track"><span style="width:${safeLoad}%"></span></div>
            <div class="capacity-scale"><span>0% · Available</span><span>100% · Full</span></div>
          </div>
          <div class="capacity-v2-facilities">
            <span class="facility-symbol">▤</span>
            <span><strong>${data.hospitals_tracked}</strong><small>Facilities tracked</small></span>
          </div>
        </div>
      </section>

      <section class="panel activity-panel activity-panel-v2">
        <div class="activity-heading">
          ${panelHeader('Recent triage activity', 'Latest assessments recorded across the network')}
          <a href="#analytics" class="text-link">View analytics <span>→</span></a>
        </div>
        <div class="table-scroll">
          <table class="data-table">
            <thead><tr><th>Patient reference</th><th>Classification</th><th>Risk score</th><th>Recorded at</th></tr></thead>
            <tbody>${activityRows || '<tr><td colspan="4" class="empty-cell">No triage activity has been recorded yet.</td></tr>'}</tbody>
          </table>
        </div>
        <div class="table-footer"><span>Showing ${activity.length} recent records</span><span>Source: AegisCare service</span></div>
      </section>

      <div class="dashboard-disclaimer"><span>ⓘ</span> Operational decision support only. Clinical decisions must be reviewed by qualified healthcare professionals.</div>
    </div>
  `;
}
