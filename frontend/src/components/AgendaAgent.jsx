import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import dayGridPlugin from '@fullcalendar/daygrid'
import fr from '@fullcalendar/core/locales/fr'
import nl from '@fullcalendar/core/locales/nl'
import enGb from '@fullcalendar/core/locales/en-gb'

const LOCALES = { fr, nl, en: enGb }
// Couleurs de la charte selon l'état du rendez-vous : demande en attente (ambre), confirmé (turquoise),
// honoré (nuit), annulé (gris)
const COULEURS = {
  demande: { backgroundColor: '#F4A261', borderColor: '#F4A261', textColor: '#1B3A4B' },
  confirme: { backgroundColor: '#2A9D8F', borderColor: '#2A9D8F', textColor: '#ffffff' },
  honore: { backgroundColor: '#1B3A4B', borderColor: '#1B3A4B', textColor: '#ffffff' },
  annule: { backgroundColor: '#9CA3AF', borderColor: '#9CA3AF', textColor: '#ffffff' },
}
const DUREE_VISITE_MS = 60 * 60 * 1000

/**
 * Agenda de l'agent (cas AG5) : ses visites sur une grille hebdomadaire FullCalendar, en plus de la
 * liste. Un clic sur un créneau fait défiler la page jusqu'au rendez-vous, où se prennent les décisions.
 */
export default function AgendaAgent({ rendezVous, langue, onChoix }) {
  const { t } = useTranslation()
  const evenements = rendezVous.map((rdv) => ({
    id: String(rdv.id),
    title: `${rdv.membre} — ${rdv.bienTitre}`,
    start: rdv.dateHeure,
    end: new Date(new Date(rdv.dateHeure).getTime() + DUREE_VISITE_MS).toISOString(),
    ...COULEURS[rdv.statut],
    classNames: rdv.statut === 'annule' ? ['line-through', 'opacity-70'] : [],
    extendedProps: { statut: rdv.statut, type: rdv.type },
  }))
  return (
    <div className="mt-6 rounded-xl border border-gray-200 bg-white p-3 text-sm shadow-sm" data-testid="agenda">
      <FullCalendar
        plugins={[timeGridPlugin, dayGridPlugin]}
        initialView="timeGridWeek"
        locale={LOCALES[langue] ?? fr}
        firstDay={1}
        headerToolbar={{ left: 'prev,next today', center: 'title', right: 'timeGridWeek,timeGridDay,dayGridMonth' }}
        slotMinTime="08:00:00"
        slotMaxTime="21:00:00"
        allDaySlot={false}
        height="auto"
        nowIndicator
        events={evenements}
        eventClick={(info) => { info.jsEvent.preventDefault(); onChoix?.(Number(info.event.id)) }}
        eventContent={(arg) => (
          <div className="overflow-hidden px-1 leading-tight">
            <div className="text-[11px] font-semibold">{arg.timeText}{arg.event.extendedProps.type === 'premium' ? ' ★' : ''}</div>
            <div className="truncate text-[11px]">{arg.event.title}</div>
          </div>
        )}
      />
      <p className="mt-2 flex flex-wrap gap-3 text-xs text-gray-600">
        {Object.entries(COULEURS).map(([statut, c]) => (
          <span key={statut} className="flex items-center gap-1">
            <span className="inline-block h-3 w-3 rounded" style={{ backgroundColor: c.backgroundColor }} aria-hidden="true" />
            {t(`rdv.statut.${statut}`)}
          </span>
        ))}
        <span>★ {t('rdv.premium')}</span>
      </p>
    </div>
  )
}
