import { api } from '../api/client';

export function renderLogin(root: HTMLElement): void {
  root.innerHTML = `
    <main class="auth-screen">
      <section class="auth-card" aria-labelledby="login-title">
        <a class="auth-brand" href="#home" aria-label="AegisCare home">
          <span class="auth-brand-mark">A</span>
          <span><strong>AegisCare</strong><small>Care operations</small></span>
        </a>
        <div class="auth-eyebrow">SECURE WORKSPACE</div>
        <h1 id="login-title">Welcome back.</h1>
        <p class="auth-intro">Sign in with the account provided by your AegisCare administrator.</p>
        <form id="login-form" class="auth-form" novalidate>
          <label for="login-email">Work email</label>
          <input id="login-email" name="email" type="email" autocomplete="username" maxlength="320" required placeholder="you@hospital.org" />
          <label for="login-password">Password</label>
          <input id="login-password" name="password" type="password" autocomplete="current-password" maxlength="72" required placeholder="Enter your password" />
          <p id="login-error" class="auth-error" role="alert" aria-live="polite"></p>
          <button id="login-submit" type="submit" class="auth-submit">Sign in <span aria-hidden="true">→</span></button>
        </form>
        <div class="auth-footnote">Access is limited to authorised care-team members. Patient access is not enabled in this workspace.</div>
        <a class="auth-back" href="#home">← Back to AegisCare</a>
      </section>
      <aside class="auth-aside">
        <div class="auth-aside-copy">
          <span class="auth-aside-kicker">CARE, CONNECTED.</span>
          <h2>Keep the whole person in view.</h2>
          <p>A shared workspace for assessment, urgent review and thoughtful care coordination.</p>
        </div>
        <div class="auth-aside-flow" aria-label="Care workflow: listen, review, connect">
          <span>01 <b>Listen</b></span><i></i><span>02 <b>Review</b></span><i></i><span>03 <b>Connect</b></span>
        </div>
      </aside>
    </main>
  `;

  const form = root.querySelector<HTMLFormElement>('#login-form');
  const error = root.querySelector<HTMLElement>('#login-error');
  const submit = root.querySelector<HTMLButtonElement>('#login-submit');
  form?.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!form.reportValidity() || !error || !submit) return;
    const data = new FormData(form);
    const email = String(data.get('email') ?? '').trim();
    const password = String(data.get('password') ?? '');
    error.textContent = '';
    submit.disabled = true;
    submit.textContent = 'Signing in…';
    try {
      await api.login(email, password);
      if (window.location.hash === '#dashboard') window.dispatchEvent(new Event('hashchange'));
      else window.location.hash = '#dashboard';
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Sign in failed';
      error.textContent = message.startsWith('401')
        ? 'Those details do not match an active account.'
        : 'We could not sign you in. Check the service connection and try again.';
    } finally {
      submit.disabled = false;
      submit.innerHTML = 'Sign in <span aria-hidden="true">→</span>';
    }
  });
}
