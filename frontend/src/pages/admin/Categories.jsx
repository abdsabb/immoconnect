import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerCategories } from '../../services/annonces'
import { creerCategorie, modifierCategorie, supprimerCategorie } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

const VIDE = { id: null, nom: '', description: '' }

// Cas A3 « Gérer les catégories de biens »
export default function Categories() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [edition, setEdition] = useState(VIDE)
  const [avis, setAvis] = useState({})
  const [enCours, setEnCours] = useState(false)
  const { data, isPending } = useQuery({ queryKey: ['categories'], queryFn: chargerCategories })

  const executer = async (action, message) => {
    setAvis({})
    setEnCours(true)
    try {
      await action()
      setAvis({ message })
      setEdition(VIDE)
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    } catch (e) {
      const { message: texte, champs } = erreursApi(e, t)
      setAvis({ erreur: Object.values(champs)[0] ?? e.response?.data?.detail ?? texte })
    } finally {
      setEnCours(false)
    }
  }

  const enregistrer = (e) => {
    e.preventDefault()
    const categorie = { nom: edition.nom, description: edition.description }
    executer(() => (edition.id ? modifierCategorie(edition.id, categorie) : creerCategorie(categorie)), t('admin.categorie.enregistree'))
  }

  return (
    <div className="space-y-4">
      <Avis {...avis} />
      <form onSubmit={enregistrer} className="bg-perle rounded-xl p-4 grid sm:grid-cols-4 gap-3 items-end">
        <label className="text-sm font-semibold text-nuit">{t('admin.categorie.nom')}
          <input required maxLength="60" value={edition.nom} onChange={(e) => setEdition({ ...edition, nom: e.target.value })}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        <label className="text-sm font-semibold text-nuit sm:col-span-2">{t('bien.description')}
          <input maxLength="1000" value={edition.description ?? ''} onChange={(e) => setEdition({ ...edition, description: e.target.value })}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal" />
        </label>
        <div className="flex gap-2">
          <button type="submit" disabled={enCours} className={`${classeBouton} bg-corail text-white`}>
            {t(edition.id ? 'profil.enregistrer' : 'admin.ajouter')}
          </button>
          {edition.id && <button type="button" onClick={() => setEdition(VIDE)} className={`${classeBouton} text-nuit`}>{t('admin.annuler')}</button>}
        </div>
      </form>

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && (
        <table className={classeTableau}>
          <thead><tr className={classeEnTete}><th>{t('admin.categorie.nom')}</th><th>{t('bien.description')}</th><th></th></tr></thead>
          <tbody>
            {data.map((c) => (
              <tr key={c.id} className={classeLigne}>
                <td className="font-semibold text-nuit">{c.nom}</td>
                <td className="text-gray-600">{c.description ?? '—'}</td>
                <td className="text-right whitespace-nowrap">
                  <button type="button" onClick={() => setEdition({ id: c.id, nom: c.nom, description: c.description ?? '' })}
                    className="text-turquoise font-semibold hover:underline">{t('annonce.modifier')}</button>
                  <button type="button" disabled={enCours}
                    onClick={() => window.confirm(t('admin.categorie.confirmerSuppression', { nom: c.nom }))
                      && executer(() => supprimerCategorie(c.id), t('admin.categorie.supprimee'))}
                    className="ml-4 text-erreur font-semibold hover:underline disabled:opacity-60">{t('annonce.supprimerPhoto')}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
