// Contenu d'un article : du texte brut, découpé en blocs séparés par une ligne vide. Deux marques seulement,
// en début de ligne : « ## » pour un intertitre, « - » pour un élément de liste. Tout passe par React, qui
// échappe ce qu'il affiche : aucune balise saisie par un rédacteur n'est interprétée.
export function blocs(contenu) {
  return (contenu ?? '').split(/\n\s*\n/).map((bloc) => bloc.trim()).filter(Boolean).map((bloc) => {
    if (bloc.startsWith('## ')) return { type: 'titre', texte: bloc.slice(3).trim() }
    const lignes = bloc.split('\n').map((l) => l.trim())
    if (lignes.every((l) => l.startsWith('- '))) return { type: 'liste', elements: lignes.map((l) => l.slice(2).trim()) }
    return { type: 'paragraphe', texte: bloc }
  })
}

export default function ContenuArticle({ contenu, className = '' }) {
  const liste = blocs(contenu)
  // Le premier paragraphe est le chapeau de l'article : il est mis en avant
  const chapeau = liste.findIndex((b) => b.type === 'paragraphe')
  return (
    <div className={`space-y-5 leading-relaxed text-gray-800 ${className}`}>
      {liste.map((bloc, i) => {
        if (bloc.type === 'titre') return <h2 key={i} className="pt-4 text-2xl font-bold text-nuit">{bloc.texte}</h2>
        if (bloc.type === 'liste') {
          return (
            <ul key={i} className="space-y-2">
              {bloc.elements.map((element, j) => (
                <li key={j} className="flex gap-3">
                  <span aria-hidden="true" className="mt-2.5 h-2 w-2 shrink-0 rounded-full bg-corail" />
                  <span>{element}</span>
                </li>
              ))}
            </ul>
          )
        }
        return (
          <p key={i} className={`whitespace-pre-line ${i === chapeau ? 'text-lg font-medium text-nuit' : ''}`}>{bloc.texte}</p>
        )
      })}
    </div>
  )
}
