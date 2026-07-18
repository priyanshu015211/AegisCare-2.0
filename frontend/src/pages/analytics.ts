import { api, type VolumePoint } from '../api/client';
import { panelHeader, severityColor } from '../components/ui';
import { lineChart, barChart } from '../components/charts';

export async function renderAnalytics(container: HTMLElement) {
  container.innerHTML = `<div class="text-sm text-ink-soft font-mono">Loading analytics&hellip;</div>`;

  try {
    const [volume, escalation] = await Promise.all([
      api.get<VolumePoint[]>('/analytics/volume-trend'),
      api.get<Record<string, number>>('/analytics/escalation-frequency'),
    ]);
    container.innerHTML = buildView(volume, escalation);
  } catch (e) {
    container.innerHTML = `<div class="panel p-6 text-severity-critical text-sm">Failed to load analytics: ${(e as Error).message}</div>`;
  }
}

function buildView(volume: VolumePoint[], escalation: Record<string, number>): string {
  const volumeData = volume.map((v) => ({ label: v.hour, value: v.patients }));
  const escalationData = Object.entries(escalation).map(([label, value]) => ({ label, value }));

  return `
    <div class="space-y-6">
      <div class="panel p-5">
        ${panelHeader('Patient Volume Trend', 'Triage sessions logged, grouped by hour')}
        ${volumeData.length ? lineChart(volumeData) : emptyState()}
      </div>

      <div class="panel p-5">
        ${panelHeader('Escalation Frequency', 'Session count by severity classification')}
        ${escalationData.some((d) => d.value > 0) ? barChart(escalationData, { color: severityColor('high') }) : emptyState()}
      </div>
    </div>
  `;
}

function emptyState(): string {
  return `<div class="text-sm text-ink-soft py-10 text-center">Not enough data yet &mdash; run a few triage sessions first.</div>`;
}
