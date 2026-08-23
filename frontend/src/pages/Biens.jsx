import { useTranslation } from 'react-i18next'

// Liste des biens (gabarit « rubrique ») — branchée sur GET /api/v1/biens au Sprint 2.
export default function Biens() {
  const { t } = useTranslation()
  return (
    <section className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('biens.titre')}</h1>
      <p className="mt-4 text-gray-600">{t('biens.vide')}</p>
    </section>
  )
}
