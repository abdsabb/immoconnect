import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerAlertes, formatDateHeure } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, Pagination, classeEnTete, classeLigne, classeTableau } from './Administration'

const COULEURS = {
  rafale_echecs: 'bg-ambre/20 text-nuit',
  enumeration: 'bg-erreur/10 text-erreur',
  cle_revoquee: 'bg-erreur/10 text-erreur',
}

// Détection d'intrusion (chapitre 9.6) : rafales d'échecs de connexion, énumération d'identifiants,
// clés API révoquées présentées. Chaque alerte est aussi envoyée par e-mail aux super-administrateurs.
export default function Alertes() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const [page, setPage] = useState(0)
  const { data, isPending, error } = useQuery({
    queryKey: ['admin', 'alertes', page],
    queryFn: () => chargerAlertes({ page, taille: 20 }),
  })

  return (
    <div className="space-y-4">
      <p className="text-sm text-gray-600">{t('admin.alerte.explication')}</p>
      {error && <Avis erreur={erreursApi(error, t).message} />}
      {isPending && <p className="text-gray-500">{t('commun.chargement')}</p>}
      {data && data.contenu.length === 0 && <p className="text-gray-600">{t('admin.alerte.aucune')}</p>}
      {data && data.contenu.length > 0 && (
        <div className="overflow-x-auto">
          <table className={classeTableau}>
            <thead>
              <tr className={classeEnTete}>
                <th>{t('admin.alerte.date')}</th><th>{t('admin.alerte.type')}</th><th>{t('admin.alerte.ip')}</th><th>{t('admin.alerte.detail')}</th>
              </tr>
            </thead>
            <tbody>
              {data.contenu.map((a) => (
                <tr key={a.id} className={classeLigne}>
                  <td className="whitespace-nowrap">{formatDateHeure(a.creeLe, langue)}</td>
                  <td>
                    <span className={`text-xs font-semibold px-2 py-1 rounded-full ${COULEURS[a.type] ?? 'bg-perle text-nuit'}`}>
                      {t(`admin.alerte.types.${a.type}`, { defaultValue: a.type })}
                    </span>
                  </td>
                  <td className="font-mono text-sm">{a.ip}</td>
                  <td>{a.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={page} total={data.totalPages} onChange={setPage} />
        </div>
      )}
      <p className="text-xs text-gray-500">{t('admin.alerte.procedure')}</p>
    </div>
  )
}
