import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { changerStatutArticle, chargerArticlesAdmin, formatDate, supprimerArticle } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Photo } from '../../components/CarteBien'
import { Avis, Pagination, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

const BADGES = { brouillon: 'bg-ambre/30 text-nuit', publie: 'bg-succes/10 text-succes', archive: 'bg-gray-200 text-gray-600' }

// Cas A2 « Gérer les articles du blog » : brouillon → publié → archivé (règle RA4)
export default function Articles() {
  const { t, i18n } = useTranslation()
  const queryClient = useQueryClient()
  const [statut, setStatut] = useState('')
  const [page, setPage] = useState(0)
  const [avis, setAvis] = useState({})
  const [enCours, setEnCours] = useState(false)

  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'articles', statut, page],
    queryFn: () => chargerArticlesAdmin({ statut, page, taille: 15 }),
    placeholderData: keepPreviousData,
  })

  const executer = async (action, message) => {
    setAvis({})
    setEnCours(true)
    try {
      await action()
      setAvis({ message })
      queryClient.invalidateQueries({ queryKey: ['admin', 'articles'] })
      queryClient.invalidateQueries({ queryKey: ['articles'] })
    } catch (e) {
      setAvis({ erreur: erreursApi(e, t).message })
    } finally {
      setEnCours(false)
    }
  }

  return (
    <div className="space-y-4">
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}
      <div className="flex flex-wrap items-center gap-3">
        <select value={statut} onChange={(e) => { setPage(0); setStatut(e.target.value) }} aria-label={t('bien.statutLabel')}
          className="rounded-lg border border-gray-300 px-3 py-2 bg-white">
          <option value="">{t('admin.article.tousLesStatuts')}</option>
          {Object.keys(BADGES).map((s) => <option key={s} value={s}>{t(`admin.article.statut.${s}`)}</option>)}
        </select>
        <Link to="/admin/articles/nouveau" className={`${classeBouton} bg-corail text-white ml-auto`}>{t('admin.article.nouveau')}</Link>
      </div>

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && (
        <>
          <div className="overflow-x-auto">
            <table className={classeTableau}>
              <thead>
                <tr className={classeEnTete}>
                  <th>{t('admin.article.titre')}</th><th>{t('annonce.categorie')}</th><th>{t('bien.statutLabel')}</th><th>{t('bien.publieLe')}</th><th></th>
                </tr>
              </thead>
              <tbody>
                {data.contenu.map((a) => (
                  <tr key={a.id} className={classeLigne}>
                    <td className="font-semibold text-nuit">
                      <div className="flex items-center gap-3">
                        <Photo src={a.imageUrl} alt="" className="h-10 w-14 shrink-0 rounded" />
                        <span>{a.titre}<span className="block text-xs font-normal text-gray-500">{a.auteur}</span></span>
                      </div>
                    </td>
                    <td>{a.categorie}</td>
                    <td><span className={`text-xs font-semibold px-2 py-1 rounded-full ${BADGES[a.statut]}`}>{t(`admin.article.statut.${a.statut}`)}</span></td>
                    <td className="whitespace-nowrap">{formatDate(a.publieLe, i18n.resolvedLanguage)}</td>
                    <td className="text-right whitespace-nowrap space-x-3">
                      <Link to={`/admin/articles/${a.id}`} className="text-turquoise font-semibold hover:underline">{t('annonce.modifier')}</Link>
                      {a.statut !== 'publie' && (
                        <button type="button" disabled={enCours} className="text-succes font-semibold hover:underline disabled:opacity-60"
                          onClick={() => executer(() => changerStatutArticle(a.id, 'publier'), t('admin.article.publie'))}>{t('admin.article.publier')}</button>
                      )}
                      {a.statut === 'publie' && (
                        <button type="button" disabled={enCours} className="text-nuit font-semibold hover:underline disabled:opacity-60"
                          onClick={() => executer(() => changerStatutArticle(a.id, 'archiver'), t('admin.article.archive'))}>{t('annonce.archiver')}</button>
                      )}
                      <button type="button" disabled={enCours} className="text-erreur font-semibold hover:underline disabled:opacity-60"
                        onClick={() => window.confirm(t('admin.article.confirmerSuppression', { titre: a.titre }))
                          && executer(() => supprimerArticle(a.id), t('admin.article.supprime'))}>{t('annonce.supprimerPhoto')}</button>
                    </td>
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
