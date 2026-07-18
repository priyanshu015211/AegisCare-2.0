import { api, type CoordinationSummary, type QueueItem, type Doctor } from '../api/client';
import { statTile, panelHeader } from '../components/ui';

export async function renderCoordination(container: HTMLElement) {
  container.innerHTML = `<div class="text-sm text-ink-soft font-mono">Loading coordination dashboard&hellip;</div>`;

  try {
    const [summary, queue, doctors] = await Promise.all([
      api.get<CoordinationSummary>('/coordination/summary'),
      api.get<QueueItem[]>('/coordination/queue'),
      api.get<Doctor[]>('/coordination/doctors'),
    ]);
    container.innerHTML = buildView(summary, queue, doctors);
  } catch (e) {
    container.innerHTML = `<div class="panel p-6 text-severity-critical text-sm">Failed to load coordination data: ${(e as Error).message}</div>`;
  }
}

function buildView(summary: CoordinationSummary, queue: QueueItem[], doctors: Doctor[]): string {
  const queueRows = queue
    .map(
      (q) => `
      <tr class="border-t border-line">
        <td class="py-2.5 pr-4 font-mono text-sm">${q.patient_id}</td>
        <td class="py-2.5 pr-4 font-mono text-sm">${q.risk_score}</td>
        <td class="py-2.5 text-sm">${q.action}</td>
      </tr>`
    )
    .join('');

  const doctorRows = doctors
    .map((d) => {
      const dotColor =
        d.status === 'available' ? 'bg-severity-low' : d.status === 'busy' ? 'bg-severity-medium' : 'bg-line-strong';
      return `
      <div class="flex items-center justify-between py-2 border-t border-line first:border-t-0">
        <div>
          <div class="text-sm font-medium">${d.full_name}</div>
          <div class="text-xs text-ink-soft">${d.specialty}</div>
        </div>
        <div class="flex items-center gap-1.5 text-xs font-mono text-ink-soft">
          <span class="w-1.5 h-1.5 rounded-full ${dotColor}"></span>${d.status}
        </div>
      </div>`;
    })
    .join('');

  return `
    <div class="space-y-6">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        ${statTile('Active Emergencies', `${summary.active_emergencies}`)}
        ${statTile('Pending Appointments', `${summary.pending_appointments}`)}
        ${statTile('Doctors Available', `${summary.doctors_available}/${summary.doctors_total}`)}
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div class="lg:col-span-2 panel p-5">
          ${panelHeader('High Priority Queue', 'Cases suggested for immediate coordination action')}
          <table class="w-full text-left">
            <thead>
              <tr class="text-[11px] uppercase tracking-wide text-ink-soft font-mono">
                <th class="pb-2 font-normal">Patient</th>
                <th class="pb-2 font-normal">Risk</th>
                <th class="pb-2 font-normal">Action</th>
              </tr>
            </thead>
            <tbody>
              ${queueRows || `<tr><td colspan="3" class="py-6 text-center text-ink-soft text-sm">Queue is empty.</td></tr>`}
            </tbody>
          </table>
        </div>

        <div class="panel p-5">
          ${panelHeader('Care Team Status')}
          <div>${doctorRows}</div>
        </div>
      </div>
    </div>
  `;
}
