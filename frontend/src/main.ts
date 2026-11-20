import './styles/main.css';
import { renderShell } from './components/shell';
import { renderDashboard } from './pages/dashboard';
import { renderTriage } from './pages/triage';
import { renderEmergency } from './pages/emergency';
import { renderCoordination } from './pages/coordination';
import { renderAnalytics } from './pages/analytics';
import { renderLanding } from './pages/landing';
import { api, clearSession, getAccessToken, type AuthUser } from './api/client';
import { renderLogin } from './pages/login';

const routes: Record<string, (el: HTMLElement) => void | Promise<void>> = {
  dashboard: renderDashboard,
  triage: renderTriage,
  emergency: renderEmergency,
  coordination: renderCoordination,
  analytics: renderAnalytics,
};

function getAppRoot(): HTMLElement {
  const element = document.getElementById('app');

  if (!(element instanceof HTMLElement)) {
    throw new Error('AegisCare application root was not found');
  }

  return element;
}

let navigationSequence = 0;

function currentRoute(): string {
  const hash = window.location.hash.replace('#', '');
  return routes[hash] ? hash : 'home';
}

async function navigate(): Promise<void> {
  const sequence = ++navigationSequence;
  const root = getAppRoot();
  const requestedHash = window.location.hash.replace('#', '');
  if (requestedHash && !routes[requestedHash] && document.getElementById(requestedHash)) return;
  let routeId = currentRoute();

  if (routeId === 'home') {
    document.title = 'AegisCare — Care, connected.';
    renderLanding(root);
    if (requestedHash && requestedHash !== 'home') document.getElementById(requestedHash)?.scrollIntoView();
    else window.scrollTo(0, 0);
    return;
  }

  if (!getAccessToken()) {
    document.title = 'AegisCare — Care team sign in';
    renderLogin(root);
    return;
  }

  let user: AuthUser;
  try {
    user = await api.get<AuthUser>('/auth/me');
  } catch {
    if (sequence !== navigationSequence) return;
    clearSession();
    document.title = 'AegisCare — Care team sign in';
    renderLogin(root);
    return;
  }
  if (sequence !== navigationSequence) return;

  if (user.role === 'COORDINATOR' && routeId === 'triage') {
    window.location.hash = '#dashboard';
    return;
  }

  document.title = `AegisCare — ${routeId[0].toUpperCase()}${routeId.slice(1)}`;
  const content = renderShell(root, routeId, user);
  await routes[routeId](content);
}

window.addEventListener('hashchange', () => { void navigate(); });
window.addEventListener('aegiscare:auth-expired', () => { void navigate(); });
void navigate();
