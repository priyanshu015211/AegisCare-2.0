export function severityColor(severity: string): string {
  switch (severity.toLowerCase()) {
    case 'critical':
      return 'var(--color-severity-critical)';
    case 'high':
      return 'var(--color-severity-high)';
    case 'medium':
      return 'var(--color-severity-medium)';
    default:
      return 'var(--color-severity-low)';
  }
}

export function severityBadge(severity: string): string {
  const color = severityColor(severity);
  return `<span class="inline-flex items-center gap-1.5 px-2 py-0.5 text-xs font-mono uppercase tracking-wide border" style="color:${color};border-color:${color};background:${color}14;">
    <span class="w-1.5 h-1.5 rounded-full" style="background:${color}"></span>${severity}
  </span>`;
}

export function statTile(label: string, value: string, sub?: string): string {
  return `
    <div class="panel p-5">
      <div class="text-[11px] uppercase tracking-[0.12em] text-ink-soft font-mono">${label}</div>
      <div class="text-3xl font-semibold mt-2 font-tabular">${value}</div>
      ${sub ? `<div class="text-xs text-ink-soft mt-1">${sub}</div>` : ''}
    </div>
  `;
}

export function panelHeader(title: string, caption?: string): string {
  return `
    <div class="mb-4">
      <h2 class="text-base font-semibold">${title}</h2>
      ${caption ? `<p class="text-xs text-ink-soft mt-0.5">${caption}</p>` : ''}
    </div>
  `;
}

export function escapeHtml(str: string): string {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
