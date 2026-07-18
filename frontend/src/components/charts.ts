export function barChart(
  data: Array<{ label: string; value: number }>,
  opts: { width?: number; height?: number; color?: string } = {}
): string {
  const width = opts.width ?? 640;
  const height = opts.height ?? 220;
  const color = opts.color ?? 'var(--color-brand)';
  const padding = { top: 12, bottom: 28, left: 8, right: 8 };
  const max = Math.max(1, ...data.map((d) => d.value));
  const chartH = height - padding.top - padding.bottom;
  const barW = (width - padding.left - padding.right) / data.length;

  const bars = data
    .map((d, i) => {
      const h = (d.value / max) * chartH;
      const x = padding.left + i * barW + barW * 0.2;
      const y = padding.top + (chartH - h);
      const w = barW * 0.6;
      return `
        <rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${color}" opacity="0.85">
          <title>${d.label}: ${d.value}</title>
        </rect>
        <text x="${x + w / 2}" y="${height - 8}" text-anchor="middle" font-size="10" font-family="var(--font-mono)" fill="var(--color-ink-soft)">${d.label}</text>
        <text x="${x + w / 2}" y="${y - 4}" text-anchor="middle" font-size="10" font-family="var(--font-mono)" fill="var(--color-ink)">${d.value}</text>
      `;
    })
    .join('');

  return `<svg viewBox="0 0 ${width} ${height}" class="w-full h-auto" role="img">
    <line x1="${padding.left}" y1="${padding.top + chartH}" x2="${width - padding.right}" y2="${padding.top + chartH}" stroke="var(--color-line-strong)" stroke-width="1"/>
    ${bars}
  </svg>`;
}

export function lineChart(
  data: Array<{ label: string; value: number }>,
  opts: { width?: number; height?: number; color?: string } = {}
): string {
  const width = opts.width ?? 640;
  const height = opts.height ?? 220;
  const color = opts.color ?? 'var(--color-brand)';
  const padding = { top: 16, bottom: 28, left: 12, right: 12 };
  const max = Math.max(1, ...data.map((d) => d.value));
  const min = Math.min(0, ...data.map((d) => d.value));
  const chartH = height - padding.top - padding.bottom;
  const chartW = width - padding.left - padding.right;
  const step = data.length > 1 ? chartW / (data.length - 1) : 0;

  const points = data.map((d, i) => {
    const x = padding.left + i * step;
    const y = padding.top + chartH - ((d.value - min) / (max - min || 1)) * chartH;
    return { x, y, ...d };
  });

  const pathD = points
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
    .join(' ');

  const areaD = `${pathD} L ${points[points.length - 1].x.toFixed(1)} ${(padding.top + chartH).toFixed(1)} L ${points[0].x.toFixed(1)} ${(padding.top + chartH).toFixed(1)} Z`;

  const dots = points
    .map(
      (p) =>
        `<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="3" fill="${color}"><title>${p.label}: ${p.value}</title></circle>
         <text x="${p.x.toFixed(1)}" y="${height - 8}" text-anchor="middle" font-size="10" font-family="var(--font-mono)" fill="var(--color-ink-soft)">${p.label}</text>`
    )
    .join('');

  return `<svg viewBox="0 0 ${width} ${height}" class="w-full h-auto" role="img">
    <line x1="${padding.left}" y1="${padding.top + chartH}" x2="${width - padding.right}" y2="${padding.top + chartH}" stroke="var(--color-line-strong)" stroke-width="1"/>
    <path d="${areaD}" fill="${color}" opacity="0.08"/>
    <path d="${pathD}" fill="none" stroke="${color}" stroke-width="2"/>
    ${dots}
  </svg>`;
}

export function donutChart(
  data: Array<{ label: string; value: number; color: string }>,
  opts: { size?: number; thickness?: number } = {}
): string {
  const size = opts.size ?? 160;
  const thickness = opts.thickness ?? 22;
  const r = size / 2 - thickness / 2;
  const cx = size / 2;
  const cy = size / 2;
  const total = data.reduce((s, d) => s + d.value, 0) || 1;
  const circumference = 2 * Math.PI * r;

  let offset = 0;
  const segments = data
    .map((d) => {
      const frac = d.value / total;
      const dash = frac * circumference;
      const seg = `<circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${d.color}" stroke-width="${thickness}"
        stroke-dasharray="${dash.toFixed(2)} ${(circumference - dash).toFixed(2)}"
        stroke-dashoffset="${(-offset).toFixed(2)}" transform="rotate(-90 ${cx} ${cy})">
        <title>${d.label}: ${d.value}</title>
      </circle>`;
      offset += dash;
      return seg;
    })
    .join('');

  return `<svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" role="img">
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="var(--color-line)" stroke-width="${thickness}"/>
    ${segments}
    <text x="${cx}" y="${cy}" text-anchor="middle" dominant-baseline="middle" font-size="20" font-weight="600" font-family="var(--font-display)" fill="var(--color-ink)">${total}</text>
  </svg>`;
}
