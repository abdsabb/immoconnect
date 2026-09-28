import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'

const THEMES = ['facade', 'sejour', 'cuisine', 'chambre', 'bains', 'jardin', 'terrasse', 'hall', 'vue', 'plan', 'terrain', 'garage', 'commerce']

async function chargerCredits() {
  const reponse = await fetch('/credits-photos.json')
  if (!reponse.ok) throw new Error('credits')
  return reponse.json()
}

// Crédits des photos de démonstration : les licences Creative Commons demandent de citer
// l'auteur, la licence et la source de chaque photo, à un endroit que le visiteur peut trouver.
export default function CreditsPhotos() {
  const { t } = useTranslation()
  const { data: credits = [], isPending, isError } = useQuery({ queryKey: ['credits-photos'], queryFn: chargerCredits, staleTime: Infinity })

  return (
    <section className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t('credits.titre')}</h1>
      <p className="mt-3 text-gray-700">{t('credits.intro')}</p>
      <p className="mt-2 text-gray-700">{t('credits.modifications')}</p>
      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {isError && <p role="alert" className="mt-6 text-erreur">{t('commun.erreurReseau')}</p>}
      {THEMES.map((theme) => {
        const photos = credits.filter((c) => c.theme === theme)
        if (photos.length === 0) return null
        return (
          <div key={theme} className="mt-8">
            <h2 className="text-xl font-semibold text-nuit">{t(`credits.theme.${theme}`)}</h2>
            <ul className="mt-2 space-y-1 text-sm text-gray-700">
              {photos.map((photo) => (
                <li key={photo.source}>
                  <a href={photo.source} rel="noreferrer" target="_blank" className="text-turquoise underline">{photo.titre}</a>
                  {' — '}{photo.auteur}{' — '}
                  <a href={photo.urlLicence} rel="noreferrer license" target="_blank" className="underline">{photo.licence}</a>
                </li>
              ))}
            </ul>
          </div>
        )
      })}
    </section>
  )
}
