import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerClesApi, formatDate, formatDateHeure, genererCleApi, revoquerCleApi } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

// Cas A7 « Gérer l'API RESTful et ses clés d'accès » (contrainte de l'épreuve).
// Une clé n'est affichée qu'une fois, à sa création : le serveur n'en conserve que l'empreinte.
export default function ClesApi() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const queryClient = useQueryClient()
  const [libelle, setLibelle] = useState('')
  const [nouvelle, setNouvelle] = useState(null)
  const [avis, setAvis] = useState({})
  const [enCours, setEnCours] = useState(false)
  const { data, isPending, error } = useQuery({ queryKey: ['admin', 'cles-api'], queryFn: chargerClesApi })

  const executer = async (action) => {
    setAvis({})
    setEnCours(true)
    try {
      await action()
      queryClient.invalidateQueries({ queryKey: ['admin', 'cles-api'] })
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      setAvis({ erreur: Object.values(champs)[0] ?? message })
    } finally {
      setEnCours(false)
    }
  }

  const generer = (e) => {
    e.preventDefault()
    setNouvelle(null)
    executer(async () => {
      setNouvelle(await genererCleApi(libelle.trim()))
      setLibelle('')
    })
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.cle.explication')}</p>
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}

      <form onSubmit={generer} className="bg-perle rounded-xl p-4 flex flex-wrap items-end gap-3">
        <label className="text-sm font-semibold text-nuit flex-1 min-w-64">{t('admin.cle.libelle')}
          <input required maxLength="80" value={libelle} onChange={(e) => setLibelle(e.target.value)} placeholder={t('admin.cle.libelleExemple')}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        <button type="submit" disabled={enCours} className={`${classeBouton} bg-corail text-white`}>{t('admin.cle.generer')}</button>
      </form>

      {nouvelle && (
        <div role="status" className="rounded-xl border-2 border-succes bg-succes/10 p-4 space-y-2">
          <p className="font-semibold text-succes">{t('admin.cle.creee', { libelle: nouvelle.libelle })}</p>
          <code className="block break-all rounded bg-white px-3 py-2 text-sm text-nuit" data-testid="cle-api">{nouvelle.cle}</code>
          <p className="text-sm text-nuit">{t('admin.cle.avertissement')}</p>
        </div>
      )}

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && (
        <div className="overflow-x-auto">
          <table className={classeTableau}>
            <thead>
              <tr className={classeEnTete}>
                <th>{t('admin.cle.libelle')}</th><th>{t('admin.cle.creeLe')}</th><th>{t('admin.cle.creeePar')}</th>
                <th>{t('admin.cle.derniereUtilisation')}</th><th>{t('admin.compte.etat')}</th><th></th>
              </tr>
            </thead>
            <tbody>
              {data.map((cle) => (
                <tr key={cle.id} className={classeLigne}>
                  <td className="font-semibold text-nuit">{cle.libelle ?? '—'}</td>
                  <td>{formatDate(cle.creeLe, langue)}</td>
                  <td>{cle.creeePar}</td>
                  <td>{cle.derniereUtilisation ? formatDateHeure(cle.derniereUtilisation, langue) : t('admin.cle.jamais')}</td>
                  <td>
                    <span className={`text-xs font-semibold px-2 py-1 rounded-full ${cle.active ? 'bg-succes/10 text-succes' : 'bg-erreur/10 text-erreur'}`}>
                      {t(cle.active ? 'admin.cle.active' : 'admin.cle.revoquee')}
                    </span>
                  </td>
                  <td className="text-right">
                    {cle.active && (
                      <button type="button" disabled={enCours}
                        onClick={() => window.confirm(t('admin.cle.confirmerRevocation', { libelle: cle.libelle ?? cle.id }))
                          && executer(() => revoquerCleApi(cle.id))}
                        className={`${classeBouton} border-2 border-erreur text-erreur hover:bg-erreur/10`}>{t('admin.cle.revoquer')}</button>
                    )}
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
