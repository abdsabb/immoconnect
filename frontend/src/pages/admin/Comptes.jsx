import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../auth/AuthContext'
import { changerActivation, chargerComptes, creerAgent, formatDate } from '../../services/admin'
import { BoutonPrincipal, Champ, classeInput, erreursApi } from '../../components/Formulaire'
import { Avis, Pagination, classeBouton, classeEnTete, classeLigne, classeTableau } from './Administration'

// Cas A1 « Gérer les utilisateurs » : consulter, filtrer, désactiver ou réactiver un compte, ouvrir un compte agent
export default function Comptes() {
  const { t, i18n } = useTranslation()
  const { utilisateur } = useAuth()
  const queryClient = useQueryClient()
  const [filtres, setFiltres] = useState({ role: '', recherche: '' })
  const [saisie, setSaisie] = useState('')
  const [page, setPage] = useState(0)
  const [avis, setAvis] = useState({})
  const [formulaireOuvert, setFormulaireOuvert] = useState(false)

  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'comptes', filtres, page],
    queryFn: () => chargerComptes({ ...filtres, page, taille: 15 }),
    placeholderData: keepPreviousData,
  })
  const rafraichir = () => queryClient.invalidateQueries({ queryKey: ['admin'] })

  const activation = useMutation({
    mutationFn: ({ id, actif }) => changerActivation(id, actif),
    onSuccess: (compte) => setAvis({ message: t(compte.actif ? 'admin.compte.reactive' : 'admin.compte.desactive', { nom: `${compte.prenom} ${compte.nom}` }) }),
    onError: (e) => setAvis({ erreur: erreursApi(e, t).message }),
    onSettled: rafraichir,
  })

  const filtrer = (changement) => {
    setPage(0)
    setFiltres({ ...filtres, ...changement })
  }

  return (
    <div className="space-y-4">
      <Avis {...avis} />
      <div className="flex flex-wrap items-end gap-3">
        <form onSubmit={(e) => { e.preventDefault(); filtrer({ recherche: saisie }) }} className="flex gap-2">
          <input value={saisie} onChange={(e) => setSaisie(e.target.value)} placeholder={t('admin.compte.recherche')}
            aria-label={t('admin.compte.recherche')} className="rounded-lg border border-gray-300 px-3 py-2" />
          <button type="submit" className={`${classeBouton} bg-turquoise text-white`}>{t('admin.filtrer')}</button>
        </form>
        <select value={filtres.role} onChange={(e) => filtrer({ role: e.target.value })} aria-label={t('admin.compte.role')}
          className="rounded-lg border border-gray-300 px-3 py-2 bg-white">
          <option value="">{t('admin.compte.tousLesRoles')}</option>
          {['membre', 'agent', 'admin'].map((r) => <option key={r} value={r}>{t(`admin.role.${r}`)}</option>)}
        </select>
        <button type="button" onClick={() => setFormulaireOuvert(!formulaireOuvert)} className={`${classeBouton} bg-corail text-white ml-auto`}>
          {t('admin.compte.nouvelAgent')}
        </button>
      </div>

      {formulaireOuvert && (
        <NouvelAgent onCree={(agent) => {
          setFormulaireOuvert(false)
          setAvis({ message: t('admin.compte.agentCree', { nom: `${agent.prenom} ${agent.nom}`, matricule: agent.matricule }) })
          rafraichir()
        }} />
      )}

      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {data && (
        <>
          <p className="text-sm text-gray-600">{t('admin.compte.nombre', { count: data.totalElements })}</p>
          <div className="overflow-x-auto">
            <table className={classeTableau}>
              <thead>
                <tr className={classeEnTete}>
                  <th>{t('auth.nom')}</th><th>{t('auth.email')}</th><th>{t('admin.compte.role')}</th>
                  <th>{t('admin.compte.inscritLe')}</th><th>{t('admin.compte.etat')}</th><th></th>
                </tr>
              </thead>
              <tbody>
                {data.contenu.map((c) => (
                  <tr key={c.id} className={classeLigne}>
                    <td className="font-semibold text-nuit">{c.prenom} {c.nom}{c.matricule && <span className="block text-xs font-normal text-gray-500">{c.matricule}</span>}</td>
                    <td>{c.email}</td>
                    <td>{t(`admin.role.${c.role}`)}{c.niveauAcces && <span className="text-xs text-gray-500"> · {t('admin.compte.niveau', { niveau: c.niveauAcces })}</span>}</td>
                    <td>{formatDate(c.dateInscription, i18n.resolvedLanguage)}</td>
                    <td>
                      <span className={`text-xs font-semibold px-2 py-1 rounded-full ${c.actif ? 'bg-succes/10 text-succes' : 'bg-erreur/10 text-erreur'}`}>
                        {t(c.actif ? 'admin.compte.actif' : 'admin.compte.inactif')}
                      </span>
                    </td>
                    <td className="text-right">
                      {c.id !== utilisateur?.id && (
                        <button type="button" disabled={activation.isPending}
                          onClick={() => (c.actif ? window.confirm(t('admin.compte.confirmerDesactivation', { nom: `${c.prenom} ${c.nom}` })) : true)
                            && activation.mutate({ id: c.id, actif: !c.actif })}
                          className={`${classeBouton} ${c.actif ? 'border-2 border-erreur text-erreur hover:bg-erreur/10' : 'bg-turquoise text-white'}`}>
                          {t(c.actif ? 'admin.compte.desactiver' : 'admin.compte.reactiver')}
                        </button>
                      )}
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

function NouvelAgent({ onCree }) {
  const { t } = useTranslation()
  const [erreur, setErreur] = useState(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: { langue: 'fr' } })
  const requis = { required: t('auth.requis') }

  const soumettre = async (valeurs) => {
    setErreur(null)
    try {
      onCree(await creerAgent(valeurs))
    } catch (e) {
      const { message, champs } = erreursApi(e, t)
      Object.entries(champs).forEach(([champ, msg]) => setError(champ, { message: msg }))
      if (!Object.keys(champs).length) setErreur(e.response?.data?.detail ?? message)
    }
  }

  return (
    <form onSubmit={handleSubmit(soumettre)} noValidate className="bg-perle rounded-xl p-4 space-y-4">
      <h2 className="font-titre font-bold text-nuit">{t('admin.compte.nouvelAgent')}</h2>
      <p className="text-sm text-gray-600">{t('admin.compte.explicationAgent')}</p>
      {erreur && <p role="alert" className="text-erreur text-sm">{erreur}</p>}
      <div className="grid sm:grid-cols-3 gap-4">
        <Champ label={t('auth.prenom')} erreur={errors.prenom?.message}><input className={classeInput(errors.prenom)} {...register('prenom', requis)} /></Champ>
        <Champ label={t('auth.nom')} erreur={errors.nom?.message}><input className={classeInput(errors.nom)} {...register('nom', requis)} /></Champ>
        <Champ label={t('auth.email')} erreur={errors.email?.message}><input type="email" className={classeInput(errors.email)} {...register('email', requis)} /></Champ>
        <Champ label={t('admin.compte.telephonePro')} erreur={errors.telephonePro?.message}>
          <input type="tel" className={classeInput(errors.telephonePro)} {...register('telephonePro', requis)} />
        </Champ>
        <Champ label={t('auth.langue')}>
          <select className={classeInput()} {...register('langue')}>
            <option value="fr">Français</option><option value="nl">Nederlands</option><option value="en">English</option>
          </select>
        </Champ>
        <Champ label={t('admin.compte.motDePasseProvisoire')} erreur={errors.motDePasse?.message}>
          <input type="password" autoComplete="new-password" className={classeInput(errors.motDePasse)}
            {...register('motDePasse', { ...requis, minLength: { value: 8, message: t('auth.motDePasseCourt') } })} />
        </Champ>
      </div>
      <BoutonPrincipal chargement={isSubmitting}>{t('admin.compte.creerAgent')}</BoutonPrincipal>
    </form>
  )
}
