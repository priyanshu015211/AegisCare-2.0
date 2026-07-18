import type { HealthResponse } from '../api/client';
import { api } from '../api/client';

export interface NavItem {
  id: string;
  label: string;
  glyph: string;
}

export const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Overview', glyph: '\u25A6' },
  { id: 'triage', label: 'Patient Triage', glyph: '\u2695' },
  { id: 'emergency', label: 'Emergency Center', glyph: '\u26A0' },
  { id: 'coordination', label: 'Coordination', glyph: '\u25C8' },
  { id: 'analytics', label: 'Analytics', glyph: '\u2237' },
];

export function renderShell(root: HTMLElement, activeId: string): HTMLElement {
  root.innerHTML = '';

  const layout = document.createElement('div');
  layout.className = 'min-h-screen flex bg-paper text-ink';

  layout.appendChild(buildSidebar(activeId));

  const main = document.createElement('div');
  main.className = 'flex-1 flex flex-col min-w-0';

  main.appendChild(buildTopbar(activeId));

  const content = document.createElement('main');
  content.id = 'page-content';
  content.className = 'flex-1 p-6 lg:p-8 max-w-[1400px] w-full mx-auto';
  main.appendChild(content);

  layout.appendChild(main);
  root.appendChild(layout);

  return content;
}

function buildSidebar(activeId: string): HTMLElement {
  const nav = document.createElement('aside');
  nav.className = 'w-[220px] shrink-0 bg-console text-paper-raised flex flex-col border-r border-console-line';

  nav.innerHTML = `
    <div class="px-5 py-6 border-b border-console-line">
      <div class="flex items-baseline gap-2">
        <span class="text-lg font-semibold tracking-tight">AegisCare</span>
      </div>
      <div class="text-[11px] uppercase tracking-[0.14em] text-paper-raised/50 mt-1 font-mono">
        Coordination Console
      </div>
    </div>
    <nav class="flex-1 py-3" id="nav-list"></nav>
    <div class="px-5 py-4 border-t border-console-line text-[11px] font-mono text-paper-raised/40">
      v2.0 &mdash; JVM edition
    </div>
  `;

  const list = nav.querySelector('#nav-list') as HTMLElement;
  NAV_ITEMS.forEach((item) => {
    const isActive = item.id === activeId;
    const a = document.createElement('a');
    a.href = `#${item.id}`;
    a.className = [
      'flex items-center gap-3 px-5 py-2.5 text-sm border-l-2 transition-colors',
      isActive
        ? 'border-brand bg-white/[0.06] text-white font-medium'
        : 'border-transparent text-paper-raised/60 hover:text-white hover:bg-white/[0.04]',
    ].join(' ');
    a.innerHTML = `<span class="w-4 text-center opacity-80">${item.glyph}</span><span>${item.label}</span>`;
    list.appendChild(a);
  });

  return nav;
}

function buildTopbar(activeId: string): HTMLElement {
  const bar = document.createElement('header');
  bar.className = 'h-14 border-b border-line bg-paper-raised flex items-center justify-between px-6 shrink-0';

  const title = NAV_ITEMS.find((n) => n.id === activeId)?.label ?? '';
  bar.innerHTML = `
    <div class="text-sm text-ink-soft font-mono uppercase tracking-wide">${title}</div>
    <div id="storage-indicator" class="flex items-center gap-2 text-xs font-mono text-ink-soft">
      <span class="w-2 h-2 rounded-full bg-line-strong animate-pulse"></span> checking storage&hellip;
    </div>
  `;

  loadStorageStatus(bar.querySelector('#storage-indicator') as HTMLElement);

  return bar;
}

async function loadStorageStatus(el: HTMLElement) {
  try {
    const health = await api.get<HealthResponse>('/system/health');
    const mode = health.storage.mode;
    const isSupabase = mode === 'supabase';
    const dotColor = isSupabase ? 'bg-severity-low' : 'bg-severity-medium';
    const label = isSupabase ? 'Supabase (Postgres)' : 'Local file store (fallback)';
    el.innerHTML = `
      <span class="w-2 h-2 rounded-full ${dotColor}"></span>
      <span title="${health.storage.detail}">${label}</span>
    `;
  } catch {
    el.innerHTML = `
      <span class="w-2 h-2 rounded-full bg-severity-critical"></span>
      <span>Backend unreachable</span>
    `;
  }
}
