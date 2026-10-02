import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerContacts, formatDateHeure, traiterContact } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, Pagination, classeBouton } from './Administration'

// Messages reçus par le formulaire de contact : l'administrateur répond par e-mail, puis classe la demande.
export default function DemandesContact() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const queryClient = useQueryClient()
  const [enAttente, setEnAttente] = useState(true)
  const [page, setPage] = useState(0)
  const [avis, setAvis] = useState({})
  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'contacts', enAttente, page],
    queryFn: () => chargerContacts({ enAttente, page, taille: 20 }),
  })

  const traiter = async (demande) => {
    setAvis({})
    try {
      await traiterContact(demande.id)
      queryClient.invalidateQueries({ queryKey: ['admin', 'contacts'] })
    } catch (e) {
      setAvis({ erreur: erreursApi(e, t).message })
    }
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.contact.explication')}</p>
      <div className="flex flex-wrap gap-2">
        {[true, false].map((choix) => (
          <button key={String(choix)} type="button" onClick={() => { setEnAttente(choix); setPage(0) }} aria-pressed={enAttente === choix}
            className={`${classeBouton} ${enAttente === choix ? 'bg-nuit text-white' : 'bg-perle text-nuit'}`}>
            {t(choix ? 'admin.contact.enAttente' : 'admin.contact.toutes')}
          </button>
        ))}
      </div>
      <Avis {...avis} />
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && data.contenu.length === 0 && <p className="text-gray-600">{t('admin.contact.aucune')}</p>}
      <ul className="space-y-3">
        {data?.contenu.map((d) => (
          <li key={d.id} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div>
                <p className="font-titre font-bold text-nuit">{d.sujet}</p>
                <p className="text-sm text-gray-600">
                  {d.nom} · <a href={`mailto:${d.email}?subject=${encodeURIComponent(`Re: ${d.sujet}`)}`} className="text-turquoise underline">{d.email}</a>
                  {d.telephone && <> · <a href={`tel:${d.telephone.replace(/[^+\d]/g, '')}`} className="text-turquoise underline">{d.telephone}</a></>}
                </p>
              </div>
              <span className="text-xs text-gray-500">{formatDateHeure(d.creeLe, langue)}</span>
            </div>
            <p className="mt-3 whitespace-pre-line break-words text-gray-800">{d.message}</p>
            <div className="mt-3">
              {d.traitee ? (
                <span className="text-xs font-semibold text-succes">
                  {t('admin.contact.traiteePar', { qui: d.traitePar ?? '—', quand: formatDateHeure(d.traiteLe, langue) })}
                </span>
              ) : (
                <button type="button" onClick={() => traiter(d)} className={`${classeBouton} bg-turquoise text-white`}>{t('admin.contact.traiter')}</button>
              )}
            </div>
          </li>
        ))}
      </ul>
      {data && <Pagination page={page} total={data.totalPages} onChange={setPage} />}
    </div>
  )
}
