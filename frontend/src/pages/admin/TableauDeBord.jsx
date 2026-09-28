import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { chargerStatistiques, formatEuros } from '../../services/admin'
import { erreursApi } from '../../components/Formulaire'
import { Avis, classeEnTete, classeLigne, classeTableau } from './Administration'

// Cas A8 « Consulter les statistiques du site »
export default function TableauDeBord() {
  const { t, i18n } = useTranslation()
  const langue = i18n.resolvedLanguage
  const { data, isPending, error } = useQuery({ queryKey: ['admin', 'statistiques'], queryFn: chargerStatistiques })

  if (isPending) return <p className="text-gray-500">{t('commun.chargement')}</p>
  if (error) return <Avis erreur={erreursApi(error, t).message} />

  const enLigne = data.biensParStatut.disponible + data.biensParStatut.sous_option
  return (
    <div className="space-y-8">
      <dl className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Chiffre titre={t('admin.stat.biensEnLigne')} valeur={enLigne} />
        <Chiffre titre={t('admin.stat.membres')} valeur={data.comptesParRole.membre} />
        <Chiffre titre={t('admin.stat.visitesAVenir')} valeur={data.visitesAVenir} />
        <Chiffre titre={t('admin.stat.revenus')} valeur={formatEuros(data.revenusPremium, langue)} accent />
        <Chiffre titre={t('admin.stat.paiements')} valeur={data.paiementsReussis} />
        <Chiffre titre={t('admin.stat.rembourse')} valeur={formatEuros(data.montantRembourse, langue)} />
        <Chiffre titre={t('admin.stat.favoris')} valeur={data.favoris} />
        <Chiffre titre={t('admin.stat.messages')} valeur={data.messages} />
      </dl>

      <div className="grid md:grid-cols-3 gap-6">
        <Repartition titre={t('admin.stat.biens')} valeurs={data.biensParStatut} libelle={(s) => t(`annonce.statut.${s}`)} />
        <Repartition titre={t('admin.stat.rendezVous')} valeurs={data.rendezVousParStatut} libelle={(s) => t(`rdv.statut.${s}`)} />
        <Repartition titre={t('admin.stat.comptes')} libelle={(r) => t(`admin.role.${r}`)}
          valeurs={{ ...data.comptesParRole, desactive: data.comptesDesactives }} />
      </div>

      <div>
        <h2 className="text-xl font-bold text-nuit">{t('admin.stat.communes')}</h2>
        <table className={`mt-3 ${classeTableau}`}>
          <thead><tr className={classeEnTete}><th>{t('annonce.ville')}</th><th>{t('admin.stat.biensDisponibles')}</th><th>{t('admin.stat.prixMoyen')}</th></tr></thead>
          <tbody>
            {data.communes.map((c) => (
              <tr key={c.ville} className={classeLigne}>
                <td className="font-semibold text-nuit">{c.ville}</td><td>{c.biensDisponibles}</td><td>{c.prixMoyen == null ? '—' : formatEuros(c.prixMoyen, langue)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function Chiffre({ titre, valeur, accent = false }) {
  return (
    <div className={`rounded-xl p-4 ${accent ? 'bg-corail/10' : 'bg-perle'}`}>
      <dt className="text-xs uppercase text-gray-500">{titre}</dt>
      <dd className="font-titre text-2xl font-extrabold text-nuit">{valeur}</dd>
    </div>
  )
}

function Repartition({ titre, valeurs, libelle }) {
  return (
    <div className="border border-gray-200 rounded-xl p-4">
      <h2 className="font-titre font-bold text-nuit">{titre}</h2>
      <dl className="mt-2 space-y-1 text-sm">
        {Object.entries(valeurs).map(([cle, nombre]) => (
          <div key={cle} className="flex justify-between"><dt className="text-gray-600">{libelle(cle)}</dt><dd className="font-semibold text-nuit">{nombre}</dd></div>
        ))}
      </dl>
    </div>
  )
}
