import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { BoutonPrincipal, Champ, classeInput } from './Formulaire'
import { formatMontant, payerEnSimulation } from '../services/rendezVous'

// Formulaire du mode simulation (développement sans compte Stripe) : mêmes cartes de test que Stripe
export default function PaiementSimule({ intention, onPaye }) {
  const { t, i18n } = useTranslation()
  const [numeroCarte, setNumeroCarte] = useState('4242 4242 4242 4242')
  const [erreur, setErreur] = useState(null)
  const [enCours, setEnCours] = useState(false)

  const soumettre = async (e) => {
    e.preventDefault()
    setErreur(null)
    setEnCours(true)
    try {
      await payerEnSimulation(intention, numeroCarte)
    } catch (echec) {
      // E1 — carte refusée (402) : rien n'est réservé, le membre peut réessayer
      setErreur(echec.response?.status === 402 ? t('rdv.paiementRefuse') : t('commun.erreurReseau'))
      setEnCours(false)
      return
    }
    await onPaye()
    setEnCours(false)
  }

  return (
    <form onSubmit={soumettre} className="space-y-4">
      <p className="rounded-lg bg-ambre/20 text-nuit text-sm px-4 py-3">{t('rdv.simulation')}</p>
      <Champ label={t('rdv.numeroCarte')} erreur={erreur}>
        <input inputMode="numeric" autoComplete="off" pattern="[0-9 ]{16,19}" required value={numeroCarte}
          onChange={(e) => setNumeroCarte(e.target.value)} className={classeInput(erreur)} />
      </Champ>
      <BoutonPrincipal chargement={enCours}>
        {t('rdv.payer', { montant: formatMontant(intention.montant, i18n.resolvedLanguage) })}
      </BoutonPrincipal>
    </form>
  )
}
