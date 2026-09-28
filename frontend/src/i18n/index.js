import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import fr from './fr.json'
import nl from './nl.json'
import en from './en.json'
import legalFr from './legal/fr.json'
import legalNl from './legal/nl.json'
import legalEn from './legal/en.json'

// Multilinguisme FR/NL/EN (contrainte TFE). Trois sources de textes :
// - « translation » : l'interface, livrée avec l'application (fichiers JSON ci-dessus) ;
// - « legal » : mentions légales et politique de confidentialité, des textes longs tenus à part ;
// - « contenu » : les textes du site gérés par l'administrateur (cas A6), servis par l'API
//   GET /traductions/{code}. Ils priment sur l'interface et changent sans nouvelle livraison.
i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      fr: { translation: fr, legal: legalFr },
      nl: { translation: nl, legal: legalNl },
      en: { translation: en, legal: legalEn },
    },
    fallbackLng: 'fr',
    supportedLngs: ['fr', 'nl', 'en'],
    ns: ['translation', 'contenu', 'legal'],
    defaultNS: 'translation',
    interpolation: { escapeValue: false },
    detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'] },
  })

// « accueil.titre » → { accueil: { titre } } : i18next lit les clés comme des chemins
function imbriquer(dictionnaire) {
  const arbre = {}
  for (const [cle, valeur] of Object.entries(dictionnaire)) {
    const parties = cle.split('.')
    let noeud = arbre
    parties.slice(0, -1).forEach((partie) => {
      if (typeof noeud[partie] !== 'object') noeud[partie] = {}
      noeud = noeud[partie]
    })
    noeud[parties.at(-1)] = valeur
  }
  return arbre
}

/** Charge les textes gérés par l'administrateur. Sans réponse de l'API, l'interface garde ses textes livrés. */
export async function chargerContenus(langue = i18n.resolvedLanguage) {
  if (!langue) return
  try {
    const reponse = await fetch(`/api/v1/traductions/${langue}`, { headers: { Accept: 'application/json' } })
    if (!reponse.ok) return
    i18n.removeResourceBundle(langue, 'contenu')
    i18n.addResourceBundle(langue, 'contenu', imbriquer(await reponse.json()), true, true)
    // Prévient les composants affichés que de nouveaux textes sont là
    i18n.emit('languageChanged', i18n.language)
  } catch {
    // Réseau indisponible : les textes livrés suffisent
  }
}

// Au démarrage puis à chaque changement de langue, si les textes de cette langue ne sont pas encore chargés
i18n.on('languageChanged', (langue) => {
  const resolue = i18n.resolvedLanguage ?? langue
  if (!i18n.hasResourceBundle(resolue, 'contenu')) chargerContenus(resolue)
})
chargerContenus()

export default i18n
