import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { archiverAnnonce, chargerMesAnnonces } from '../services/annonces'
import { formatPrixBien } from '../services/biens'
import { Photo } from '../components/CarteBien'
import { erreursApi } from '../components/Formulaire'

const BADGES = {
  disponible: 'bg-succes/10 text-succes',
  sous_option: 'bg-ambre/30 text-nuit',
  vendu: 'bg-perle text-gray-700',
  loue: 'bg-perle text-gray-700',
  archive: 'bg-gray-200 text-gray-600',
}

// Cas AG6 « Consulter le tableau de bord de ses annonces » et AG3 « Archiver une annonce »
export default function MesAnnonces() {
  const { t } = useTranslation()
  const { utilisateur } = useAuth()
  const queryClient = useQueryClient()
  const [erreur, setErreur] = useState(null)
  const cle = ['annonces', utilisateur?.id]

  const { data: annonces, isPending, isError } = useQuery({ queryKey: cle, queryFn: chargerMesAnnonces })
  const archivage = useMutation({
    mutationFn: archiverAnnonce,
    onMutate: () => setErreur(null),
    onError: (e) => setErreur(erreursApi(e, t).message),
    onSettled: () => queryClient.invalidateQueries({ queryKey: cle }),
  })

  const archiver = (annonce) => {
    if (window.confirm(t('annonce.confirmerArchivage', { titre: annonce.titre }))) archivage.mutate(annonce.id)
  }

  const enLigne = (annonces ?? []).filter((a) => a.statut === 'disponible' || a.statut === 'sous_option').length
  const total = (nom) => (annonces ?? []).reduce((somme, a) => somme + a.indicateurs[nom], 0)

  return (
    <section className="mx-auto max-w-6xl px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold text-nuit">{t('annonce.mesAnnonces')}</h1>
        <Link to="/annonces/nouvelle" className="bg-corail hover:bg-corail/90 text-white font-titre font-bold rounded-lg px-4 py-3">
          {t('annonce.nouvelle')}
        </Link>
      </div>

      {erreur && <p role="alert" className="mt-6 rounded-lg bg-erreur/10 text-erreur px-4 py-3">{erreur}</p>}
      {isPending && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {isError && <p role="alert" className="mt-6 text-erreur">{t('commun.erreurReseau')}</p>}

      {annonces && (
        <>
          <dl className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
            <Indicateur titre={t('annonce.enLigne')} valeur={`${enLigne} / ${annonces.length}`} />
            <Indicateur titre={t('annonce.favoris')} valeur={total('favoris')} />
            <Indicateur titre={t('annonce.demandes')} valeur={total('demandesEnAttente')} accent={total('demandesEnAttente') > 0} />
            <Indicateur titre={t('annonce.visites')} valeur={total('visitesAVenir')} />
          </dl>

          {annonces.length === 0 && <p className="mt-6 text-gray-600">{t('annonce.aucune')}</p>}
          <ul className="mt-6 space-y-3">
            {annonces.map((a) => (
              <li key={a.id} className="bg-white border border-gray-200 rounded-xl shadow-sm p-4 flex flex-wrap gap-4 items-center">
                <Photo src={a.photos[0]?.url} alt={a.titre} className="w-28 h-20 rounded-lg shrink-0" />
                <div className="flex-1 min-w-52">
                  <p className="font-titre font-bold text-nuit">{a.titre}</p>
                  <p className="text-sm text-gray-600">{t(`offre.${a.typeOffre}`)} · {a.categorie} · {a.adresse}, {a.codePostal} {a.ville}</p>
                  <p className="font-titre font-extrabold text-corail">{formatPrixBien(a, t)}</p>
                </div>
                <ul className="text-sm text-gray-700 min-w-40">
                  <li>{t('annonce.favoris')} : <strong>{a.indicateurs.favoris}</strong></li>
                  <li>{t('annonce.demandes')} : <strong>{a.indicateurs.demandesEnAttente}</strong></li>
                  <li>{t('annonce.visites')} : <strong>{a.indicateurs.visitesAVenir}</strong></li>
                </ul>
                <div className="flex flex-col items-end gap-2">
                  <span className={`text-xs font-semibold px-2 py-1 rounded-full ${BADGES[a.statut]}`}>{t(`annonce.statut.${a.statut}`)}</span>
                  <div className="flex gap-2">
                    <Link to={`/annonces/${a.id}`} className="rounded-lg px-3 py-2 text-sm font-titre font-semibold bg-turquoise text-white hover:bg-turquoise/90">
                      {t('annonce.modifier')}
                    </Link>
                    {a.statut !== 'archive' && (
                      <button type="button" disabled={archivage.isPending} onClick={() => archiver(a)}
                        className="rounded-lg px-3 py-2 text-sm font-titre font-semibold border-2 border-erreur text-erreur hover:bg-erreur/10 disabled:opacity-60">
                        {t('annonce.archiver')}
                      </button>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}

function Indicateur({ titre, valeur, accent = false }) {
  return (
    <div className={`rounded-xl p-4 ${accent ? 'bg-ambre/30' : 'bg-perle'}`}>
      <dt className="text-xs uppercase text-gray-500">{titre}</dt>
      <dd className="font-titre text-2xl font-extrabold text-nuit">{valeur}</dd>
    </div>
  )
}
