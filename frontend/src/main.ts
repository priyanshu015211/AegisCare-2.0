import './styles/main.css';
import { renderShell } from './components/shell';
import { renderDashboard } from './pages/dashboard';
import { renderTriage } from './pages/triage';
import { renderEmergency } from './pages/emergency';
import { renderCoordination } from './pages/coordination';
import { renderAnalytics } from './pages/analytics';
import { renderLanding } from './pages/landing';

const routes: Record<string, (el: HTMLElement) => void | Promise<void>> = {
  dashboard: renderDashboard,
  triage: renderTriage,
  emergency: renderEmergency,
  coordination: renderCoordination,
  analytics: renderAnalytics,
};

const root = document.getElementById('app')!;

function currentRoute(): string {
  const hash = window.location.hash.replace('#', '');
  return routes[hash] ? hash : 'home';
}

function navigate() {
  const requestedHash = window.location.hash.replace('#', '');
  if (requestedHash && !routes[requestedHash] && document.getElementById(requestedHash)) return;
  const routeId = currentRoute();
  if (routeId === 'home') {
    document.title = 'AegisCare — Care, connected.';
    renderLanding(root);
    if (requestedHash && requestedHash !== 'home') {
      document.getElementById(requestedHash)?.scrollIntoView();
    } else {
      window.scrollTo(0, 0);
    }
    return;
  }
  document.title = `AegisCare — ${routeId[0].toUpperCase()}${routeId.slice(1)}`;
  const content = renderShell(root, routeId);
  const handler = routes[routeId];
  void handler(content);
}

window.addEventListener('hashchange', navigate);
navigate();
