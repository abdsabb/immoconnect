import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import fr from './fr.json'
import nl from './nl.json'
import en from './en.json'

// Multilinguisme FR/NL/EN (contrainte TFE) : l'interface est traduite côté client,
// les contenus dynamiques seront servis par l'API (GET /traductions/{code}).
i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { fr: { translation: fr }, nl: { translation: nl }, en: { translation: en } },
    fallbackLng: 'fr',
    supportedLngs: ['fr', 'nl', 'en'],
    interpolation: { escapeValue: false },
    detection: { order: ['localStorage', 'navigator'], caches: ['localStorage'] },
  })

export default i18n
