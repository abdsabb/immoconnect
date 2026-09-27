import { useState } from 'react'
import { useQuery, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerActionsJournal, chargerJournal, formatDateHeure } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, Pagination, classeEnTete, classeLigne, classeTableau } from './Administration'

const VIDE = { action: '', utilisateurId: '', du: '', au: '' }

// Cas A4 « Consulter les rapports d'audit d'activité » (contrainte de l'épreuve) : lecture seule, filtrable
export default function Journal() {
  const { t, i18n } = useTranslation()
  const [filtres, setFiltres] = useState(VIDE)
  const [page, setPage] = useState(0)

  const actions = useQuery({ queryKey: ['admin', 'journal-actions'], queryFn: chargerActionsJournal })
  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'journal', filtres, page],
    queryFn: () => chargerJournal({ ...filtres, page, taille: 25 }),
    placeholderData: keepPreviousData,
  })

  const filtrer = (nom) => (e) => {
    setPage(0)
    setFiltres({ ...filtres, [nom]: e.target.value })
  }
  const champ = 'rounded-lg border border-gray-300 px-3 py-2 bg-white'

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.journal.explication')}</p>
      <div className="flex flex-wrap items-end gap-3">
        <label className="text-sm">{t('admin.journal.action')}
          <select value={filtres.action} onChange={filtrer('action')} className={`block ${champ}`}>
            <option value="">{t('admin.journal.toutes')}</option>
            {actions.data?.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </label>
        <label className="text-sm">{t('admin.journal.utilisateur')}
          <input type="number" min="1" value={filtres.utilisateurId} onChange={filtrer('utilisateurId')} className={`block w-32 ${champ}`} />
        </label>
        <label className="text-sm">{t('admin.journal.du')}
          <input type="date" value={filtres.du} onChange={filtrer('du')} className={`block ${champ}`} />
        </label>
        <label className="text-sm">{t('admin.journal.au')}
          <input type="date" value={filtres.au} onChange={filtrer('au')} className={`block ${champ}`} />
        </label>
        <button type="button" onClick={() => { setPage(0); setFiltres(VIDE) }} className="text-turquoise font-semibold underline pb-2">
          {t('admin.journal.effacer')}
        </button>
      </div>

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {error && <Avis erreur={Object.values(erreursApi(error, t).champs)[0] ?? erreursApi(error, t).message} />}
      {data && (
        <>
          <p className="text-sm text-gray-600">{t('admin.journal.nombre', { count: data.totalElements })}</p>
          <div className="overflow-x-auto">
            <table className={classeTableau}>
              <thead>
                <tr className={classeEnTete}>
                  <th>{t('admin.journal.date')}</th><th>{t('admin.journal.utilisateur')}</th><th>{t('admin.journal.action')}</th>
                  <th>{t('admin.journal.objet')}</th><th>{t('admin.journal.ip')}</th>
                </tr>
              </thead>
              <tbody>
                {data.contenu.map((trace) => (
                  <tr key={trace.id} className={classeLigne}>
                    <td className="whitespace-nowrap">{formatDateHeure(trace.horodatage, i18n.resolvedLanguage)}</td>
                    <td><span className="font-semibold text-nuit">{trace.utilisateur}</span> <span className="text-xs text-gray-500">n° {trace.utilisateurId} · {t(`admin.role.${trace.role}`)}</span></td>
                    <td><code className="text-xs bg-perle rounded px-1 py-0.5">{trace.action}</code></td>
                    <td>{trace.entite}</td>
                    <td className="text-gray-600">{trace.ip}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} total={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
