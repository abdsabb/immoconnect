import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerAnnoncesAdmin, chargerComptes } from '../../services/admin'
import { formatPrixBien } from '../../services/biens'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeEnTete, classeLigne, classeTableau } from './Administration'

const BADGES = {
  disponible: 'bg-succes/10 text-succes',
  sous_option: 'bg-ambre/30 text-nuit',
  vendu: 'bg-perle text-gray-700',
  loue: 'bg-perle text-gray-700',
  archive: 'bg-gray-200 text-gray-600',
}

// Supervision : l'agence est privée, sa direction voit et corrige les annonces de tous ses agents.
export default function AnnoncesAgents() {
  const { t } = useTranslation()
  const [agentId, setAgentId] = useState('')
  const agents = useQuery({ queryKey: ['admin', 'agents'], queryFn: () => chargerComptes({ role: 'agent', taille: 100 }) })
  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'annonces', agentId],
    queryFn: () => chargerAnnoncesAdmin(agentId || undefined),
  })

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.annonces.explication')}</p>
      <label className="block max-w-sm text-sm font-semibold text-nuit">{t('admin.annonces.agent')}
        <select value={agentId} onChange={(e) => setAgentId(e.target.value)} className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal">
          <option value="">{t('admin.annonces.tousLesAgents')}</option>
          {agents.data?.contenu.map((a) => <option key={a.id} value={a.id}>{a.prenom} {a.nom}</option>)}
        </select>
      </label>
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && data.length === 0 && <p className="text-gray-600">{t('annonce.aucune')}</p>}
      {data && data.length > 0 && (
        <div className="overflow-x-auto">
          <p className="mb-2 text-sm text-gray-600">{t('admin.annonces.nombre', { count: data.length })}</p>
          <table className={classeTableau}>
            <thead>
              <tr className={classeEnTete}>
                <th>{t('admin.annonces.annonce')}</th><th>{t('admin.annonces.agent')}</th><th>{t('bien.statutLabel')}</th>
                <th>{t('annonce.vues')}</th><th>{t('annonce.favoris')}</th><th>{t('annonce.demandes')}</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.map((a) => (
                <tr key={a.id} className={classeLigne}>
                  <td>
                    <p className="font-semibold text-nuit">{a.titre}</p>
                    <p className="text-xs text-gray-500">{t(`offre.${a.typeOffre}`)} · {a.categorie} · {a.ville} · {formatPrixBien(a, t)}</p>
                  </td>
                  <td>{a.agent}</td>
                  <td><span className={`text-xs font-semibold px-2 py-1 rounded-full ${BADGES[a.statut]}`}>{t(`annonce.statut.${a.statut}`)}</span></td>
                  <td>{a.indicateurs.vues}</td>
                  <td>{a.indicateurs.favoris}</td>
                  <td>{a.indicateurs.demandesEnAttente}</td>
                  <td className="whitespace-nowrap text-right">
                    <Link to={`/admin/annonces/${a.id}`} className="font-semibold text-turquoise hover:underline">{t('annonce.modifier')}</Link>
                    {a.statut !== 'archive' && <> · <Link to={`/biens/${a.id}`} className="text-turquoise hover:underline">{t('admin.annonces.voir')}</Link></>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
