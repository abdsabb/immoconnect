// Petits composants de formulaire partagés, conformes à la charte (focus visible, erreurs en rouge)
export function Champ({ label, erreur, children }) {
  return (
    <label className="block">
      <span className="block text-sm font-semibold text-nuit mb-1">{label}</span>
      {children}
      {erreur && <span role="alert" className="block mt-1 text-sm text-erreur">{erreur}</span>}
    </label>
  )
}

export const classeInput = (erreur) =>
  `w-full rounded-lg border px-3 py-2 ${erreur ? 'border-erreur' : 'border-gray-300'}`

export function BoutonPrincipal({ children, chargement, ...props }) {
  return (
    <button type="submit" disabled={chargement} {...props}
      className="w-full bg-corail hover:bg-corail/90 disabled:opacity-60 text-white font-titre font-bold rounded-lg px-4 py-3">
      {children}
    </button>
  )
}

/**
 * Transforme une réponse d'erreur problem+json de l'API en message et erreurs par champ.
 * Le « type » de l'erreur (dernier segment de son URI) identifie les cas que l'interface sait
 * traduire ; à défaut, le détail renvoyé par l'API est affiché tel quel.
 */
export function erreursApi(error, t) {
  const pd = error?.response?.data
  if (!pd) return { message: t('commun.erreurReseau'), champs: {}, type: null }
  const type = typeof pd.type === 'string' ? pd.type.split('/').pop() : null
  const detail = pd.detail ?? pd.title ?? t('commun.erreurReseau')
  return { message: type ? t(`erreur.${type}`, { defaultValue: detail }) : detail, champs: pd.champs ?? {}, type }
}
