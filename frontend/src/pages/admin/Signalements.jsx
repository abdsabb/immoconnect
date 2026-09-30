import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerSignalements, formatDateHeure, trancherSignalement } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, Pagination, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

const STATUTS = ['ouvert', 'retire', 'conserve']

// Signalements de contenus (chapitre 11, règlement sur les services numériques) : le gestionnaire
// examine, retire ou conserve, et motive sa décision, qui est définitive et journalisée.
export default function Signalements() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const queryClient = useQueryClient()
  const [statut, setStatut] = useState('ouvert')
  const [page, setPage] = useState(0)
  const [avis, setAvis] = useState({})
  const [enCours, setEnCours] = useState(null)
  const [decisions, setDecisions] = useState({})
  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'signalements', statut, page],
    queryFn: () => chargerSignalements({ statut: statut || undefined, page, taille: 20 }),
  })

  const trancher = async (signalement, decision) => {
    const motivation = (decisions[signalement.id] ?? '').trim()
    if (!motivation) {
      setAvis({ erreur: t('admin.signalement.motivationRequise') })
      return
    }
    setAvis({})
    setEnCours(signalement.id)
    try {
      await trancherSignalement(signalement.id, { statut: decision, decision: motivation })
      setAvis({ message: t(decision === 'retire' ? 'admin.signalement.retireFait' : 'admin.signalement.conserveFait') })
      queryClient.invalidateQueries({ queryKey: ['admin', 'signalements'] })
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      setAvis({ erreur: Object.values(champs)[0] ?? message })
    } finally {
      setEnCours(null)
    }
  }

  const lienContenu = (s) => {
    if (s.typeContenu === 'bien') return <Link to={`/biens/${s.contenuId}`} className="text-turquoise underline">{s.apercu ?? `#${s.contenuId}`}</Link>
    if (s.typeContenu === 'article') return <Link to={`/blog/${s.contenuId}`} className="text-turquoise underline">{s.apercu ?? `#${s.contenuId}`}</Link>
    return <span className="italic">« {s.apercu ?? `#${s.contenuId}`} »</span>
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.signalement.explication')}</p>
      <div className="flex flex-wrap gap-2">
        {STATUTS.map((s) => (
          <button key={s} type="button" onClick={() => { setStatut(s); setPage(0) }} aria-pressed={statut === s}
            className={`${classeBouton} ${statut === s ? 'bg-nuit text-white' : 'bg-perle text-nuit'}`}>
            {t(`admin.signalement.statuts.${s}`)}
          </button>
        ))}
      </div>
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && data.contenu.length === 0 && <p className="text-gray-600">{t('admin.signalement.aucun')}</p>}
      {data && data.contenu.length > 0 && (
        <div className="overflow-x-auto">
          <table className={classeTableau}>
            <thead>
              <tr className={classeEnTete}>
                <th>{t('admin.signalement.date')}</th><th>{t('admin.signalement.contenu')}</th><th>{t('admin.signalement.motif')}</th>
                <th>{t('admin.signalement.auteur')}</th><th>{t('admin.signalement.decision')}</th>
              </tr>
            </thead>
            <tbody>
              {data.contenu.map((s) => (
                <tr key={s.id} className={classeLigne}>
                  <td className="whitespace-nowrap">{formatDateHeure(s.creeLe, langue)}</td>
                  <td>
                    <p className="text-xs uppercase text-gray-500">{t(`admin.signalement.types.${s.typeContenu}`)}</p>
                    <p>{lienContenu(s)}</p>
                  </td>
                  <td>
                    <p className="font-semibold text-nuit">{t(`signalement.motifs.${s.motif}`)}</p>
                    <p className="text-gray-700">{s.description}</p>
                  </td>
                  <td>{s.auteur}<br /><span className="text-xs text-gray-500">{t(`role.${s.auteurRole}`, { defaultValue: s.auteurRole })}</span></td>
                  <td className="min-w-72">
                    {s.statut === 'ouvert' ? (
                      <div className="space-y-2">
                        <textarea rows="2" maxLength="500" placeholder={t('admin.signalement.motivation')}
                          value={decisions[s.id] ?? ''} onChange={(e) => setDecisions({ ...decisions, [s.id]: e.target.value })}
                          className="block w-full rounded-lg border border-gray-300 px-2 py-1 text-sm" />
                        <div className="flex gap-2">
                          <button type="button" disabled={enCours === s.id} onClick={() => trancher(s, 'retire')}
                            className={`${classeBouton} bg-erreur text-white`}>{t('admin.signalement.retirer')}</button>
                          <button type="button" disabled={enCours === s.id} onClick={() => trancher(s, 'conserve')}
                            className={`${classeBouton} border-2 border-nuit text-nuit`}>{t('admin.signalement.conserver')}</button>
                        </div>
                      </div>
                    ) : (
                      <div>
                        <span className={`text-xs font-semibold px-2 py-1 rounded-full ${s.statut === 'retire' ? 'bg-erreur/10 text-erreur' : 'bg-succes/10 text-succes'}`}>
                          {t(`admin.signalement.statuts.${s.statut}`)}
                        </span>
                        <p className="mt-1 text-gray-700">{s.decision}</p>
                        <p className="text-xs text-gray-500">{s.traitePar} · {formatDateHeure(s.traiteLe, langue)}</p>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} total={data.totalPages} onChange={setPage} />
        </div>
      )}
    </div>
  )
}
