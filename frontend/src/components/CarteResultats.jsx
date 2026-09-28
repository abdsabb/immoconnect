import { useEffect } from 'react'
import { Link } from 'react-router'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useTranslation } from 'react-i18next'
import { formatPrixBien } from '../services/biens'

const BRUXELLES = [50.8466, 4.3528]

// Épingle de la charte : corail, et bleu nuit agrandie pour le bien survolé dans la liste
const epingle = (couleur, taille) => L.divIcon({
  className: '',
  iconSize: [taille, taille * 1.3],
  iconAnchor: [taille / 2, taille * 1.3],
  popupAnchor: [0, -taille * 1.2],
  html: `<svg viewBox="0 0 24 31" width="${taille}" height="${taille * 1.3}" aria-hidden="true">
    <path d="M12 0C5.4 0 0 5.3 0 11.8 0 20.6 12 31 12 31s12-10.4 12-19.2C24 5.3 18.6 0 12 0z" fill="${couleur}" stroke="#fff" stroke-width="1.5"/>
    <circle cx="12" cy="11.5" r="4.2" fill="#fff"/></svg>`,
})
const EPINGLE = epingle('#E76F51', 26)
const EPINGLE_ACTIVE = epingle('#1B3A4B', 34)

// La carte se recadre sur les résultats affichés : elle suit les filtres et la pagination
function Cadrage({ positions }) {
  const carte = useMap()
  const cle = positions.map((p) => p.join(',')).join(';')
  useEffect(() => {
    if (positions.length === 0) carte.setView(BRUXELLES, 11)
    else if (positions.length === 1) carte.setView(positions[0], 14)
    else carte.fitBounds(positions, { padding: [30, 30], maxZoom: 14 })
    // cle résume les positions : le recadrage ne se rejoue que si elles changent
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [carte, cle])
  return null
}

/**
 * Carte des résultats (cas V3, maquette Figure 14) : une épingle par bien de la page affichée.
 * Survoler une carte de résultat met son épingle en avant, et l'inverse.
 * Tuiles OSM sous licence ODbL : l'attribution reste affichée (livrable 18, §5).
 */
export default function CarteResultats({ biens, actif, surSurvol, className = 'h-96' }) {
  const { t } = useTranslation()
  const positions = biens.map((bien) => [Number(bien.latitude), Number(bien.longitude)])
  return (
    <div className={`${className} rounded-xl overflow-hidden border border-gray-200`}>
      <MapContainer center={BRUXELLES} zoom={11} scrollWheelZoom={false} className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">les contributeurs OpenStreetMap</a>'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Cadrage positions={positions} />
        {biens.map((bien, i) => (
          <Marker key={bien.id} position={positions[i]} icon={bien.id === actif ? EPINGLE_ACTIVE : EPINGLE}
            zIndexOffset={bien.id === actif ? 1000 : 0} title={bien.titre}
            eventHandlers={{ mouseover: () => surSurvol(bien.id), mouseout: () => surSurvol(null) }}>
            <Popup>
              <Link to={`/biens/${bien.id}`} className="font-semibold">{bien.titre}</Link>
              <br />{formatPrixBien(bien, t)}
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  )
}
