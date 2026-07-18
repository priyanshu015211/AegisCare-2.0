import { api, type HighRiskPatient } from '../api/client';
import { panelHeader, severityBadge } from '../components/ui';

export async function renderEmergency(container: HTMLElement) {
  container.innerHTML = `<div class="text-sm text-ink-soft font-mono">Loading emergency queue&hellip;</div>`;

  try {
    const patients = await api.get<HighRiskPatient[]>('/emergency/high-risk');
    container.innerHTML = buildView(patients);
  } catch (e) {
    container.innerHTML = `<div class="panel p-6 text-severity-critical text-sm">Failed to load emergency data: ${(e as Error).message}</div>`;
  }
}

function buildView(patients: HighRiskPatient[]): string {
  const rows = patients
    .map(
      (p) => `
      <tr class="border-t border-line">
        <td class="py-3 pr-4 font-mono text-sm">${p.patient_id}</td>
        <td class="py-3 pr-4 font-mono text-sm">${p.risk_score}</td>
        <td class="py-3 pr-4">${severityBadge(p.severity)}</td>
        <td class="py-3 pr-4 text-sm">${p.status}</td>
        <td class="py-3 text-xs text-ink-soft">${p.last_update}</td>
      </tr>`
    )
    .join('');

  return `
    <div class="space-y-6">
      <div class="panel p-5">
        ${panelHeader('High Risk Patients', 'Sessions with risk score 70 or above')}
        <table class="w-full text-left">
          <thead>
            <tr class="text-[11px] uppercase tracking-wide text-ink-soft font-mono">
              <th class="pb-2 font-normal">Patient ID</th>
              <th class="pb-2 font-normal">Risk Score</th>
              <th class="pb-2 font-normal">Severity</th>
              <th class="pb-2 font-normal">Status</th>
              <th class="pb-2 font-normal">Last Update</th>
            </tr>
          </thead>
          <tbody>
            ${rows || `<tr><td colspan="5" class="py-8 text-center text-ink-soft text-sm">No high-risk patients right now.</td></tr>`}
          </tbody>
        </table>
      </div>

      <div class="panel p-5 border-dashed">
        ${panelHeader('Escalation Queue')}
        <p class="text-sm text-ink-soft">Patients requiring immediate attention will surface here as escalations are triggered from Patient Triage.</p>
      </div>
    </div>
  `;
}
