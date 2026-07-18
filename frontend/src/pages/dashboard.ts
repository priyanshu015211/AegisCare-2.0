import { api, type DashboardSummary } from '../api/client';
import { statTile, panelHeader, severityBadge, severityColor } from '../components/ui';
import { donutChart } from '../components/charts';

export async function renderDashboard(container: HTMLElement) {
  container.innerHTML = `<div class="text-sm text-ink-soft font-mono">Loading overview&hellip;</div>`;

  try {
    const data = await api.get<DashboardSummary>('/dashboard/summary');
    container.innerHTML = buildView(data);
  } catch (e) {
    container.innerHTML = `<div class="panel p-6 text-severity-critical text-sm">Failed to load dashboard: ${(e as Error).message}</div>`;
  }
}

function buildView(data: DashboardSummary): string {
  const donutData = Object.entries(data.risk_distribution).map(([label, value]) => ({
    label,
    value,
    color: severityColor(label),
  }));

  const activityRows = data.recent_activity
    .map(
      (a) => `
      <tr class="border-t border-line">
        <td class="py-2.5 pr-4 font-mono text-sm">${a.patient_id}</td>
        <td class="py-2.5 pr-4">${severityBadge(a.severity)}</td>
        <td class="py-2.5 pr-4 font-mono text-sm">${a.risk_score}/100</td>
        <td class="py-2.5 text-xs text-ink-soft">${new Date(a.started_at).toLocaleString()}</td>
      </tr>`
    )
    .join('');

  return `
    <div class="space-y-6">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        ${statTile('Hospital Load', `${data.hospital_load_pct}%`, `${data.hospitals_tracked} facilities tracked`)}
        ${statTile('Active Emergencies', `${data.active_emergencies}`, 'escalated or critical')}
        ${statTile('Sessions Logged', `${data.total_sessions}`, 'most recent 25')}
        ${statTile('Risk Distribution', `${Object.values(data.risk_distribution).reduce((a, b) => a + b, 0)}`, 'total classified cases')}
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div class="panel p-5 lg:col-span-1 flex flex-col items-center justify-center">
          ${panelHeader('Risk Distribution')}
          ${donutChart(donutData)}
          <div class="grid grid-cols-2 gap-x-4 gap-y-1 mt-4 text-xs w-full">
            ${donutData
              .map(
                (d) => `<div class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full" style="background:${d.color}"></span>${d.label} &middot; ${d.value}</div>`
              )
              .join('')}
          </div>
        </div>

        <div class="panel p-5 lg:col-span-2">
          ${panelHeader('Recent Activity', 'Latest triage sessions across the network')}
          <table class="w-full text-left">
            <thead>
              <tr class="text-[11px] uppercase tracking-wide text-ink-soft font-mono">
                <th class="pb-2 font-normal">Patient</th>
                <th class="pb-2 font-normal">Severity</th>
                <th class="pb-2 font-normal">Risk Score</th>
                <th class="pb-2 font-normal">Time</th>
              </tr>
            </thead>
            <tbody>
              ${activityRows || `<tr><td colspan="4" class="py-6 text-center text-ink-soft text-sm">No activity yet.</td></tr>`}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}
