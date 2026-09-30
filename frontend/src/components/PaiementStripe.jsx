import { useState } from 'react'
// Variante « pure » : l'import ordinaire ajoute le script de Stripe à toutes les pages du site.
// Ici, il n'est demandé à Stripe qu'au moment d'afficher un paiement (politique de confidentialité).
import { loadStripe } from '@stripe/stripe-js/pure'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { useTranslation } from 'react-i18next'
import { BoutonPrincipal } from './Formulaire'
import { formatMontant } from '../services/rendezVous'

// Stripe.js n'est chargé qu'une fois par clé, et seulement quand un paiement s'affiche
const chargements = {}
const stripePour = (clePublique) => (chargements[clePublique] ??= loadStripe(clePublique))

/**
 * Formulaire de paiement Stripe Elements (livrable 16) : carte ou Bancontact. Les champs sont des
 * iframes hébergées par Stripe, le numéro de carte ne transite donc ni par cette page ni par notre
 * serveur. Bancontact redirige vers la banque puis revient sur « retour », avec l'identifiant du paiement.
 */
export default function PaiementStripe({ clePublique, intention, onPaye, retour, avantRedirection }) {
  const { i18n } = useTranslation()
  const options = {
    clientSecret: intention.clientSecret,
    locale: i18n.resolvedLanguage,
    appearance: { theme: 'stripe', variables: { colorPrimary: '#2A9D8F', colorDanger: '#C62828', borderRadius: '8px' } },
  }
  return (
    <Elements stripe={stripePour(clePublique)} options={options}>
      <FormulaireCarte intention={intention} onPaye={onPaye} retour={retour} avantRedirection={avantRedirection} />
    </Elements>
  )
}

function FormulaireCarte({ intention, onPaye, retour, avantRedirection }) {
  const { t, i18n } = useTranslation()
  const stripe = useStripe()
  const elements = useElements()
  const [erreur, setErreur] = useState(null)
  const [enCours, setEnCours] = useState(false)

  const soumettre = async (e) => {
    e.preventDefault()
    if (!stripe || !elements) return
    setErreur(null)
    setEnCours(true)
    // Carte : l'authentification forte (3-D Secure) s'ouvre dans une fenêtre de Stripe, sans quitter la page.
    // Bancontact : Stripe redirige vers la banque, puis revient sur « retour » ; la demande est mise de côté avant
    avantRedirection?.()
    const { error, paymentIntent } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: retour ?? window.location.href },
      redirect: 'if_required',
    })
    if (error || paymentIntent?.status !== 'succeeded') {
      // E1 — paiement refusé : rien n'est réservé, le membre peut réessayer
      setErreur(error?.message ?? t('rdv.paiementRefuse'))
      setEnCours(false)
      return
    }
    await onPaye()
    setEnCours(false)
  }

  return (
    <form onSubmit={soumettre} className="space-y-4">
      <PaymentElement />
      {erreur && <p role="alert" className="text-erreur text-sm">{erreur}</p>}
      <BoutonPrincipal chargement={enCours || !stripe}>
        {t('rdv.payer', { montant: formatMontant(intention.montant, i18n.resolvedLanguage) })}
      </BoutonPrincipal>
    </form>
  )
}
