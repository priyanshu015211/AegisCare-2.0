const icon = (name: string) => {
  const paths: Record<string, string> = {
    heart: '<path d="M20.8 8.7c0 5.2-8.8 10-8.8 10s-8.8-4.8-8.8-10A4.7 4.7 0 0 1 12 6.4a4.7 4.7 0 0 1 8.8 2.3Z"/><path d="M4.5 12h4l2-3.5 3 7 2-3.5h4"/>',
    compass: '<circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2.2 4.8-4.8 2.2 2.2-4.8 4.8-2.2Z"/>',
    people: '<path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="10" cy="7" r="4"/><path d="M20 21v-2a4 4 0 0 0-3-3.9M16 3.2a4 4 0 0 1 0 7.6"/>',
    shield: '<path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z"/><path d="m9 12 2 2 4-4"/>',
    arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    check: '<path d="m5 12 4 4L19 6"/>',
    pulse: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
  };
  return `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round">${paths[name] ?? ''}</svg>`;
};

export function renderLanding(root: HTMLElement) {
  root.innerHTML = `
    <div class="public-site">
      <header class="public-header">
        <a class="public-brand" href="#home" aria-label="AegisCare home">
          <span class="public-brand-mark">a</span>
          <span><strong>AegisCare</strong><small>Care, connected.</small></span>
        </a>
        <nav class="public-nav" aria-label="Main navigation">
          <a href="#why-aegis">Our approach</a>
          <a href="#how-it-works">How it works</a>
          <a href="#for-teams">For care teams</a>
        </nav>
        <a class="public-header-cta" href="#dashboard">Care team workspace <span>↗</span></a>
      </header>

      <main>
        <section class="public-hero">
          <div class="public-hero-copy">
            <div class="public-kicker"><span></span> A more human way to coordinate care</div>
            <h1>When health feels uncertain, <em>care should feel closer.</em></h1>
            <p class="public-hero-lede">Being unwell is hard enough. AegisCare helps care teams listen, assess and coordinate the next step — with the person, not just the patient record, at the centre.</p>
            <div class="public-hero-actions">
              <a class="public-button-primary" href="#how-it-works">Get to know AegisCare ${icon('arrow')}</a>
              <a class="public-text-link" href="#for-teams">I'm part of a care team <span>↗</span></a>
            </div>
            <div class="public-reassurance"><span class="reassurance-icon">${icon('heart')}</span><span><strong>People first. Always.</strong><small>Thoughtful coordination for the moments that matter.</small></span></div>
          </div>
          <div class="public-hero-art" aria-label="Illustration representing connected, compassionate care" role="img">
            <div class="art-sun"></div><div class="art-orbit orbit-one"></div><div class="art-orbit orbit-two"></div>
            <div class="art-card art-card-top"><span class="art-card-icon">${icon('heart')}</span><span><b>A little more clarity</b><small>One step at a time</small></span></div>
            <div class="art-person"><div class="person-hair"></div><div class="person-head"></div><div class="person-neck"></div><div class="person-body"></div><div class="person-arm"></div></div>
            <div class="art-plant plant-one"><i></i><i></i><i></i><i></i><b></b></div>
            <div class="art-card art-card-bottom"><span class="art-check">${icon('check')}</span><span><b>Care that connects</b><small>People · Teams · Next steps</small></span></div>
            <span class="art-caption">A calmer path through care</span>
          </div>
        </section>

        <section class="public-trust-strip" aria-label="AegisCare principles">
          <div><span class="trust-symbol">01</span><strong>Listen first</strong><small>Understand the person behind the symptoms.</small></div>
          <div><span class="trust-symbol">02</span><strong>Make the next step clear</strong><small>Bring useful information into one place.</small></div>
          <div><span class="trust-symbol">03</span><strong>Keep care connected</strong><small>Help teams coordinate around what matters.</small></div>
        </section>

        <section class="public-section public-why" id="why-aegis">
          <div class="public-section-heading">
            <div class="public-kicker"><span></span> The reason we're here</div>
            <h2>Good care is more than a decision.<br><em>It's how people feel along the way.</em></h2>
          </div>
          <div class="public-why-copy">
            <p>When someone is worried about their health, every unanswered question can feel bigger. And when care teams are stretched across patients, facilities and urgent decisions, it can be difficult to keep the whole picture in view.</p>
            <p>AegisCare is being built to make that picture clearer — helping teams bring assessments, urgent cases and coordination into a shared workspace, while keeping human judgement at the heart of care.</p>
            <a class="public-inline-link" href="#how-it-works">See the thinking behind AegisCare ${icon('arrow')}</a>
          </div>
        </section>

        <section class="public-how" id="how-it-works">
          <div class="how-intro">
            <div class="public-kicker"><span></span> A clearer path forward</div>
            <h2>Care moves better<br>when we're <em>connected.</em></h2>
            <p>A simple view of how AegisCare supports the people coordinating care.</p>
          </div>
          <div class="how-steps">
            <article class="how-step"><span class="step-number">01</span><span class="step-icon">${icon('people')}</span><h3>Understand the situation</h3><p>Capture symptoms and relevant details in a structured assessment, so the care team has a clearer starting point.</p></article>
            <article class="how-step"><span class="step-number">02</span><span class="step-icon">${icon('pulse')}</span><h3>Recognise what needs attention</h3><p>Bring risk indicators and escalation signals into view to support timely review by qualified people.</p></article>
            <article class="how-step"><span class="step-number">03</span><span class="step-icon">${icon('compass')}</span><h3>Coordinate the next step</h3><p>Give teams a shared picture of emergencies, facility capacity and care coordination activity.</p></article>
          </div>
        </section>

        <section class="public-human" id="for-teams">
          <div class="human-mark">${icon('shield')}</div>
          <div class="human-copy"><div class="public-kicker">For the people providing care</div><h2>Technology should make room for <em>more human care.</em></h2><p>AegisCare gives care teams an operational view of triage, emergencies and coordination — with AI-assisted insights designed to support decisions, not replace clinical judgement.</p></div>
          <a class="public-button-light" href="#dashboard">Explore the care workspace ${icon('arrow')}</a>
        </section>

        <section class="public-note">
          <span class="note-mark">i</span>
          <p><strong>A note on your wellbeing</strong> AegisCare is a care-coordination project, not an emergency service or a substitute for professional medical advice. If you think you may be experiencing a medical emergency, contact your local emergency services or go to the nearest emergency department.</p>
        </section>
      </main>

      <footer class="public-footer">
        <a class="public-brand" href="#home"><span class="public-brand-mark">a</span><span><strong>AegisCare</strong><small>Care, connected.</small></span></a>
        <span>Built around people. Designed to help care teams connect the dots.</span>
        <a href="#dashboard">Care team access ↗</a>
      </footer>
    </div>
  `;
}
