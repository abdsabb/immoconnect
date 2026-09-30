import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { erreursApi } from '../components/Formulaire'

// Lien reçu par e-mail après l'inscription : confirme l'adresse et ouvre la session.
export default function Activation() {
  const { t } = useTranslation()
  const { activer } = useAuth()
  const [adresse] = useSearchParams()
  const [etat, setEtat] = useState('en-cours')
  const [erreur, setErreur] = useState(null)
  const lance = useRef(false)

  const jeton = adresse.get('jeton')
  useEffect(() => {
    if (!jeton || lance.current) return
    lance.current = true // un lien ne sert qu'une fois : ne jamais l'envoyer deux fois
    activer(jeton).then(() => setEtat('fait')).catch((e) => { setEtat('erreur'); setErreur(erreursApi(e, t).message) })
  }, [activer, jeton, t])
  const affichage = jeton ? etat : 'erreur'
  const messageErreur = jeton ? erreur : t('auth.activationLienManquant')

  return (
    <section className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-bold text-nuit">{t('auth.activationTitre')}</h1>
      {affichage === 'en-cours' && <p className="mt-6 text-gray-500">{t('commun.chargement')}</p>}
      {affichage === 'fait' && (
        <>
          <p role="status" className="mt-6 rounded-lg bg-succes/10 px-4 py-3 text-succes">{t('auth.activationFaite')}</p>
          <p className="mt-6 text-sm"><Link to="/biens" className="text-turquoise font-semibold underline">{t('accueil.rechercher')}</Link></p>
        </>
      )}
      {affichage === 'erreur' && (
        <>
          <p role="alert" className="mt-6 rounded-lg bg-erreur/10 px-4 py-3 text-erreur">{messageErreur}</p>
          <p className="mt-6 text-sm text-gray-600">{t('auth.activationAide')} <Link to="/connexion" className="text-turquoise underline">{t('nav.connexion')}</Link></p>
        </>
      )}
    </section>
  )
}
