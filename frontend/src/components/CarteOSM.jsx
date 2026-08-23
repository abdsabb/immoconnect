import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import iconeUrl from 'leaflet/dist/images/marker-icon.png'
import iconeRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import ombreUrl from 'leaflet/dist/images/marker-shadow.png'

// Vite ne résout pas les icônes par défaut de Leaflet : on les déclare explicitement.
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({ iconUrl: iconeUrl, iconRetinaUrl: iconeRetinaUrl, shadowUrl: ombreUrl })

/**
 * Carte OpenStreetMap (cas V3) — tuiles OSM sous licence ODbL : l'attribution
 * « © les contributeurs OpenStreetMap » est obligatoire et toujours affichée (livrable 18, §5).
 */
export default function CarteOSM({ latitude, longitude, titre, zoom = 14, className = 'h-72' }) {
  const position = [Number(latitude), Number(longitude)]
  return (
    <div className={`${className} rounded-xl overflow-hidden border border-gray-200`}>
      <MapContainer center={position} zoom={zoom} scrollWheelZoom={false} className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">les contributeurs OpenStreetMap</a>'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={position}>
          <Popup>{titre}</Popup>
        </Marker>
      </MapContainer>
    </div>
  )
}
