export function severityColor(severity: string): string {
  switch (severity.toLowerCase()) {
    case 'critical': return 'var(--color-severity-critical)';
    case 'high': return 'var(--color-severity-high)';
    case 'medium': return 'var(--color-severity-medium)';
    default: return 'var(--color-severity-low)';
  }
}

export function severityBadge(severity: string): string {
  const safe = escapeHtml(severity);
  const color = severityColor(severity);
  return `<span class="severity-badge" style="--severity-color:${color}"><span class="severity-dot"></span>${safe}</span>`;
}

export function statTile(label: string, value: string, sub?: string, icon = '◈'): string {
  return `
    <article class="metric-card">
      <div class="metric-topline">
        <span class="metric-label">${escapeHtml(label)}</span>
        <span class="metric-icon" aria-hidden="true">${icon}</span>
      </div>
      <div class="metric-value">${escapeHtml(value)}</div>
      ${sub ? `<div class="metric-caption">${escapeHtml(sub)}</div>` : ''}
    </article>
  `;
}

export function panelHeader(title: string, caption?: string): string {
  return `
    <div class="panel-heading">
      <div><h2>${escapeHtml(title)}</h2>${caption ? `<p>${escapeHtml(caption)}</p>` : ''}</div>
    </div>
  `;
}

export function escapeHtml(value: unknown): string {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[char] ?? char);
}
