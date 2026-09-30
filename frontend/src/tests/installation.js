// Préparation commune des tests Vitest : assertions DOM et i18n en français, sans détection du navigateur
import '@testing-library/jest-dom/vitest'
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import fr from '../i18n/fr.json'
import legalFr from '../i18n/legal/fr.json'

i18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  resources: { fr: { translation: fr, legal: legalFr } },
  ns: ['translation', 'contenu', 'legal'],
  defaultNS: 'translation',
  interpolation: { escapeValue: false },
})
