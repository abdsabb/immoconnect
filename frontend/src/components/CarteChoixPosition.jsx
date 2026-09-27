import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import './CarteOSM'

// Centre de Bruxelles : point de départ d'une annonce qui n'a pas encore de position
const BRUXELLES = [50.8466, 4.3528]

function Clic({ onChoisir }) {
  useMapEvents({ click: (e) => onChoisir(e.latlng.lat.toFixed(6), e.latlng.lng.toFixed(6)) })
  return null
}

function Recentrage({ position }) {
  const carte = useMap()
  useEffect(() => {
    if (position) carte.setView(position)
  }, [carte, position])
  return null
}

/**
 * Carte de saisie de la position d'un bien (cas AG1) : l'agent clique à l'emplacement du bien,
 * la latitude et la longitude du formulaire sont remplies. Même fond OpenStreetMap que le site public.
 */
export default function CarteChoixPosition({ latitude, longitude, onChoisir, className = 'h-72' }) {
  const valide = latitude !== '' && longitude !== '' && !Number.isNaN(Number(latitude)) && !Number.isNaN(Number(longitude))
  const position = valide ? [Number(latitude), Number(longitude)] : null
  return (
    <div className={`${className} rounded-xl overflow-hidden border border-gray-200`}>
      <MapContainer center={position ?? BRUXELLES} zoom={position ? 15 : 12} className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">les contributeurs OpenStreetMap</a>'
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Clic onChoisir={onChoisir} />
        <Recentrage position={position} />
        {position && <Marker position={position} />}
      </MapContainer>
    </div>
  )
}
