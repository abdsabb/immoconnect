import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerBien, formatPrix } from '../services/biens'
import { Photo } from '../components/CarteBien'
import CarteOSM from '../components/CarteOSM'

// Fiche d'un bien (gabarit « article », maquette Figure 15) : galerie, caractéristiques,
// localisation OSM et panneau d'action persistant « Prendre rendez-vous » / « Envoyer un message ».
export default function BienDetail() {
  const { id } = useParams()
  const { t } = useTranslation()
  const [photoActive, setPhotoActive] = useState(0)
  const { data: bien, isPending, error } = useQuery({ queryKey: ['bien', id], queryFn: () => chargerBien(id) })

  if (isPending) return <p className="mx-auto max-w-6xl px-4 py-10 text-gray-500">{t('commun.chargement')}</p>
  if (error) {
    return (
      <section className="mx-auto max-w-6xl px-4 py-10">
        <p role="alert" className="text-erreur">{error.response?.status === 404 ? t('bien.introuvable') : t('biens.erreur')}</p>
        <Link to="/biens" className="mt-4 inline-block text-turquoise underline">{t('bien.retour')}</Link>
      </section>
    )
  }

  const photos = bien.photos ?? []
  return (
    <section className="mx-auto max-w-6xl px-4 py-8">
      <nav aria-label="Fil d'Ariane" className="text-sm text-gray-500">
        <Link to="/" className="hover:text-turquoise">{t('nav.accueil')}</Link> › <Link to="/biens" className="hover:text-turquoise">{t('nav.biens')}</Link> › <span className="text-nuit">{bien.titre}</span>
      </nav>

      <div className="mt-4 grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div>
            <Photo src={photos[photoActive]?.url} alt={photos[photoActive]?.legende ?? bien.titre} className="w-full h-[26rem] rounded-xl" />
            {photos.length > 1 && (
              <div className="mt-2 flex gap-2 overflow-x-auto" role="list" aria-label={t('bien.galerie')}>
                {photos.map((p, i) => (
                  <button key={p.ordre} type="button" onClick={() => setPhotoActive(i)} aria-pressed={i === photoActive}
                    className={`shrink-0 rounded-lg overflow-hidden border-2 ${i === photoActive ? 'border-corail' : 'border-transparent'}`}>
                    <Photo src={p.url} alt={p.legende ?? `${bien.titre} ${p.ordre}`} className="w-24 h-16" />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div>
            <h1 className="text-3xl font-bold text-nuit">{bien.titre}</h1>
            <p className="text-gray-600">{bien.categorie.nom} · {bien.codePostal} {bien.ville}</p>
          </div>

          <dl className="grid grid-cols-2 sm:grid-cols-4 gap-4 bg-perle rounded-xl p-4">
            <div><dt className="text-xs uppercase text-gray-500">{t('bien.superficie')}</dt><dd className="font-semibold">{bien.superficie} m²</dd></div>
            <div><dt className="text-xs uppercase text-gray-500">{t('bien.chambresLabel')}</dt><dd className="font-semibold">{bien.nbChambres}</dd></div>
            <div><dt className="text-xs uppercase text-gray-500">{t('bien.statutLabel')}</dt><dd className="font-semibold">{t(`statut.${bien.statut}`)}</dd></div>
            <div><dt className="text-xs uppercase text-gray-500">{t('bien.publieLe')}</dt><dd className="font-semibold">{new Date(bien.publieLe).toLocaleDateString('fr-BE')}</dd></div>
          </dl>

          <div>
            <h2 className="text-xl font-bold text-nuit">{t('bien.description')}</h2>
            <p className="mt-2 whitespace-pre-line text-gray-700 leading-relaxed">{bien.description}</p>
          </div>

          <div>
            <h2 className="text-xl font-bold text-nuit">{t('bien.localisation')}</h2>
            <p className="mt-1 text-sm text-gray-500">{t('bien.adresseApres')}</p>
            <CarteOSM latitude={bien.latitude} longitude={bien.longitude} titre={bien.titre} className="mt-3 h-80" />
          </div>
        </div>

        <aside className="lg:sticky lg:top-6 self-start bg-white border border-gray-200 rounded-xl p-5 shadow-sm space-y-4">
          <p className="font-titre text-3xl font-extrabold text-corail">{formatPrix(bien.prix)}</p>
          <Link to="/connexion" state={{ from: `/biens/${id}` }} className="block text-center bg-corail hover:bg-corail/90 text-white font-titre font-bold rounded-lg px-4 py-3">
            {t('bien.prendreRdv')}
          </Link>
          <Link to="/connexion" state={{ from: `/biens/${id}` }} className="block text-center border-2 border-nuit text-nuit hover:bg-perle font-titre font-semibold rounded-lg px-4 py-3">
            {t('bien.envoyerMessage')}
          </Link>
          <div className="pt-4 border-t border-gray-200 text-sm">
            <p className="text-xs uppercase text-gray-500">{t('bien.agent')}</p>
            <p className="font-semibold text-nuit">{bien.agent.nomComplet}</p>
            <p className="text-gray-600">{bien.agent.telephonePro}</p>
          </div>
        </aside>
      </div>
    </section>
  )
}
