import { useState } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { useTranslation } from 'react-i18next'
import { BoutonPrincipal } from './Formulaire'
import { formatMontant } from '../services/rendezVous'

// Stripe.js n'est chargé qu'une fois par clé, et seulement quand un paiement s'affiche
const chargements = {}
const stripePour = (clePublique) => (chargements[clePublique] ??= loadStripe(clePublique))

/**
 * Formulaire de carte Stripe Elements (livrable 16) : les champs sont des iframes hébergées par
 * Stripe, le numéro de carte ne transite donc ni par cette page ni par notre serveur.
 */
export default function PaiementStripe({ clePublique, intention, onPaye }) {
  const { i18n } = useTranslation()
  const options = {
    clientSecret: intention.clientSecret,
    locale: i18n.resolvedLanguage,
    appearance: { theme: 'stripe', variables: { colorPrimary: '#2A9D8F', colorDanger: '#C62828', borderRadius: '8px' } },
  }
  return (
    <Elements stripe={stripePour(clePublique)} options={options}>
      <FormulaireCarte intention={intention} onPaye={onPaye} />
    </Elements>
  )
}

function FormulaireCarte({ intention, onPaye }) {
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
    // L'authentification forte (3-D Secure) s'ouvre dans une fenêtre de Stripe, sans quitter la page
    const { error, paymentIntent } = await stripe.confirmPayment({ elements, redirect: 'if_required' })
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
