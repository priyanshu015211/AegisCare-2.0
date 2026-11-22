import type { AuthUser, HealthResponse } from '../api/client';
import { api } from '../api/client';
import { escapeHtml } from './ui';

export interface NavItem {
  id: string;
  label: string;
  glyph: string;
  group: 'Workspace' | 'Insights';
}

export const NAV_ITEMS: NavItem[] = [
  { id: 'dashboard', label: 'Overview', glyph: '▦', group: 'Workspace' },
  { id: 'triage', label: 'Patient triage', glyph: '✚', group: 'Workspace' },
  { id: 'emergency', label: 'Emergency center', glyph: '⌁', group: 'Workspace' },
  { id: 'coordination', label: 'Coordination', glyph: '◇', group: 'Workspace' },
  { id: 'analytics', label: 'Analytics', glyph: '▥', group: 'Insights' },
];

export function renderShell(root: HTMLElement, activeId: string, user: AuthUser): HTMLElement {
  root.innerHTML = '';
  const layout = document.createElement('div');
  layout.className = 'app-layout';
  layout.appendChild(buildSidebar(activeId, user));

  const main = document.createElement('div');
  main.className = 'app-main';
  main.appendChild(buildTopbar(activeId));
  const content = document.createElement('main');
  content.id = 'page-content';
  content.className = 'page-content';
  main.appendChild(content);
  layout.appendChild(main);
  root.appendChild(layout);

  const signOut = root.querySelector<HTMLButtonElement>('#sign-out');
  signOut?.addEventListener('click', () => {
    api.logout();
    window.location.hash = '#home';
  });
  return content;
}

function buildSidebar(activeId: string, user: AuthUser): HTMLElement {
  const aside = document.createElement('aside');
  aside.className = 'app-sidebar';
  const visibleItems = user.role === 'COORDINATOR'
    ? NAV_ITEMS.filter(item => item.id !== 'triage')
    : NAV_ITEMS;
  const roleLabel = user.role === 'ADMIN' ? 'Administrator'
    : user.role === 'CLINICIAN' ? 'Clinician' : 'Care coordinator';
  const initials = user.fullName.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  aside.innerHTML = `
    <a class="brand-lockup" href="#home" aria-label="Return to AegisCare home">
      <span class="brand-mark">A</span>
      <span class="brand-copy"><strong>AegisCare</strong><small>Care operations</small></span>
    </a>
    <div class="sidebar-context"><span class="context-dot"></span> HEALTH NETWORK</div>
    <nav class="sidebar-nav" aria-label="Main navigation">
      ${(['Workspace', 'Insights'] as const).map(group => `
        <div class="nav-group">
          <div class="nav-group-label">${group}</div>
          ${visibleItems.filter(item => item.group === group).map(item => `
            <a href="#${item.id}" class="nav-link ${item.id === activeId ? 'active' : ''}" ${item.id === activeId ? 'aria-current="page"' : ''}>
              <span class="nav-glyph" aria-hidden="true">${item.glyph}</span><span>${item.label}</span>
              ${item.id === 'emergency' ? '<span class="nav-live-dot" title="Emergency monitoring"></span>' : ''}
            </a>
          `).join('')}
        </div>
      `).join('')}
    </nav>
    <div class="sidebar-footer">
      <div class="profile-avatar">${escapeHtml(initials || 'AC')}</div>
      <div class="profile-copy"><strong>${escapeHtml(user.fullName)}</strong><small>${escapeHtml(roleLabel)}</small></div>
      <button type="button" class="profile-menu" id="sign-out" aria-label="Sign out" title="Sign out">↗</button>
    </div>
  `;
  return aside;
}

function buildTopbar(activeId: string): HTMLElement {
  const bar = document.createElement('header');
  bar.className = 'app-topbar';
  const title = NAV_ITEMS.find(item => item.id === activeId)?.label ?? 'Overview';
  bar.innerHTML = `
    <div class="topbar-left">
      <button type="button" class="mobile-menu" id="mobile-menu" aria-label="Toggle navigation">☰</button>
      <div class="breadcrumb"><span>Workspace</span><span class="breadcrumb-separator">/</span><strong>${escapeHtml(title)}</strong></div>
    </div>
    <div class="topbar-right">
      <div class="topbar-date"><span class="topbar-date-label">LOCAL TIME</span><strong id="local-clock">--:--</strong></div>
      <div id="storage-indicator" class="storage-pill"><span class="status-dot"></span><span>Checking connection</span></div>
    </div>
  `;
  const menuButton = bar.querySelector<HTMLButtonElement>('#mobile-menu');
  menuButton?.addEventListener('click', () => document.body.classList.toggle('nav-open'));
  const clock = bar.querySelector<HTMLElement>('#local-clock');
  const updateClock = () => {
    if (clock) clock.textContent = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(new Date());
  };
  updateClock();
  window.setInterval(updateClock, 30_000);
  const storage = bar.querySelector<HTMLElement>('#storage-indicator');
  if (storage) void loadStorageStatus(storage);
  return bar;
}

async function loadStorageStatus(el: HTMLElement) {
  try {
    const health = await api.get<HealthResponse>('/system/health');
    const isMysql = health.storage.mode === 'mysql' && health.storage.connected;
    el.classList.toggle('is-connected', isMysql);
    el.classList.toggle('is-fallback', !isMysql);
    el.innerHTML = `<span class="status-dot"></span><span>${isMysql ? 'MySQL connected' : 'Development storage'}</span>`;
    el.title = health.storage.detail;
  } catch {
    el.classList.add('is-error');
    el.innerHTML = '<span class="status-dot"></span><span>Backend unavailable</span>';
  }
}
