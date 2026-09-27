import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerTraductions, enregistrerTraduction, supprimerTraduction } from '../../services/admin'
import { chargerContenus } from '../../i18n'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

const LANGUES = ['fr', 'nl', 'en']
const VIDE = { cle: '', fr: '', nl: '', en: '', nouvelle: true }

// Cas A6 « Gérer les langues du site » (contrainte de l'épreuve : multilinguisme).
// Les textes enregistrés ici sont ceux du site public : ils changent dès l'enregistrement.
export default function Traductions() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [recherche, setRecherche] = useState('')
  const [edition, setEdition] = useState(null)
  const [avis, setAvis] = useState({})
  const [enCours, setEnCours] = useState(false)
  const { data, isPending, error } = useQuery({ queryKey: ['admin', 'traductions'], queryFn: chargerTraductions })

  const executer = async (action, message) => {
    setAvis({})
    setEnCours(true)
    try {
      await action()
      setAvis({ message })
      setEdition(null)
      queryClient.invalidateQueries({ queryKey: ['admin', 'traductions'] })
      // Le site public relit ses textes, dans les trois langues
      await Promise.all(LANGUES.map((langue) => chargerContenus(langue)))
    } catch (e) {
      const { message: texte, champs } = erreursApi(e, t)
      setAvis({ erreur: Object.values(champs)[0] ?? texte })
    } finally {
      setEnCours(false)
    }
  }

  const enregistrer = (e) => {
    e.preventDefault()
    const valeurs = Object.fromEntries(LANGUES.filter((l) => edition[l].trim()).map((l) => [l, edition[l].trim()]))
    executer(() => enregistrerTraduction(edition.cle.trim(), valeurs), t('admin.traduction.enregistree'))
  }

  const terme = recherche.trim().toLowerCase()
  const lignes = (data ?? []).filter((ligne) => !terme || ligne.cle.includes(terme)
    || Object.values(ligne.valeurs).some((valeur) => valeur.toLowerCase().includes(terme)))

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.traduction.explication')}</p>
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}

      <div className="flex flex-wrap gap-3">
        <input value={recherche} onChange={(e) => setRecherche(e.target.value)} placeholder={t('admin.traduction.recherche')}
          aria-label={t('admin.traduction.recherche')} className="rounded-lg border border-gray-300 px-3 py-2 flex-1 min-w-64" />
        <button type="button" onClick={() => setEdition(VIDE)} className={`${classeBouton} bg-corail text-white`}>{t('admin.traduction.nouvelle')}</button>
      </div>

      {edition && (
        <form onSubmit={enregistrer} className="bg-perle rounded-xl p-4 space-y-3">
          <label className="block text-sm font-semibold text-nuit">{t('admin.traduction.cle')}
            <input required readOnly={!edition.nouvelle} maxLength="120" value={edition.cle} placeholder="accueil.bandeau"
              onChange={(e) => setEdition({ ...edition, cle: e.target.value })}
              className={`mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-mono font-normal ${edition.nouvelle ? '' : 'bg-gray-100'}`} />
          </label>
          <div className="grid sm:grid-cols-3 gap-3">
            {LANGUES.map((langue) => (
              <label key={langue} className="block text-sm font-semibold text-nuit uppercase">{langue}
                <textarea rows="3" maxLength="2000" value={edition[langue]} onChange={(e) => setEdition({ ...edition, [langue]: e.target.value })}
                  className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal normal-case" />
              </label>
            ))}
          </div>
          <div className="flex gap-2">
            <button type="submit" disabled={enCours} className={`${classeBouton} bg-corail text-white`}>{t('profil.enregistrer')}</button>
            <button type="button" onClick={() => setEdition(null)} className={`${classeBouton} text-nuit`}>{t('admin.annuler')}</button>
          </div>
        </form>
      )}

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && (
        <>
          <p className="text-sm text-gray-600">{t('admin.traduction.nombre', { count: lignes.length })}</p>
          <div className="overflow-x-auto">
            <table className={classeTableau}>
              <thead><tr className={classeEnTete}><th>{t('admin.traduction.cle')}</th><th>FR</th><th>NL</th><th>EN</th><th></th></tr></thead>
              <tbody>
                {lignes.map((ligne) => (
                  <tr key={ligne.cle} className={classeLigne}>
                    <td><code className="text-xs">{ligne.cle}</code></td>
                    {LANGUES.map((langue) => (
                      <td key={langue} className={ligne.valeurs[langue] ? 'text-nuit' : 'text-erreur'}>{ligne.valeurs[langue] ?? t('admin.traduction.manquante')}</td>
                    ))}
                    <td className="text-right whitespace-nowrap">
                      <button type="button" className="text-turquoise font-semibold hover:underline"
                        onClick={() => setEdition({ cle: ligne.cle, fr: ligne.valeurs.fr ?? '', nl: ligne.valeurs.nl ?? '', en: ligne.valeurs.en ?? '', nouvelle: false })}>
                        {t('annonce.modifier')}
                      </button>
                      <button type="button" disabled={enCours} className="ml-4 text-erreur font-semibold hover:underline disabled:opacity-60"
                        onClick={() => window.confirm(t('admin.traduction.confirmerSuppression', { cle: ligne.cle }))
                          && executer(() => supprimerTraduction(ligne.cle), t('admin.traduction.supprimee'))}>
                        {t('annonce.supprimerPhoto')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}
