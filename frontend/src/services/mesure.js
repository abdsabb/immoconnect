// Mesure d'audience Matomo, auto-hébergée sur le même domaine (chapitre 10) : aucune donnée ne part chez
// un tiers et aucun cookie n'est déposé (disableCookies), d'où l'absence de bandeau de consentement.
// Rien n'est chargé tant que l'administrateur n'a pas déclaré le site dans Matomo (MATOMO_SITE_ID).
let active = false

export function demarrerMesure(mesure) {
  if (active || !mesure?.siteId) return
  active = true
  const paq = (window._paq = window._paq ?? [])
  paq.push(['disableCookies'])
  // Respecte le signal « Ne pas me pister » du navigateur
  paq.push(['setDoNotTrack', true])
  paq.push(['setTrackerUrl', `${mesure.url}matomo.php`])
  paq.push(['setSiteId', String(mesure.siteId)])
  const script = document.createElement('script')
  script.async = true
  script.src = `${mesure.url}matomo.js`
  document.head.appendChild(script)
}

/** Une page vue : appelée à chaque changement de route, l'application ne rechargeant jamais la page. */
export function pageVue(chemin, titre) {
  if (!active) return
  window._paq.push(['setCustomUrl', chemin])
  window._paq.push(['setDocumentTitle', titre])
  window._paq.push(['trackPageView'])
}

/** Conversions suivies : prise de rendez-vous, envoi de message, création de compte. */
export function conversion(action, detail) {
  if (!active) return
  window._paq.push(['trackEvent', 'Conversion', action, detail])
}
