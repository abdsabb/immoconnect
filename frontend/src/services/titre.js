import { useEffect } from 'react'

const SITE = 'ImmoConnect'

/**
 * Titre de l'onglet pour la page affichée. La première page arrive du serveur avec son titre (rendu
 * côté serveur, chapitre 10) ; ensuite, la navigation se fait dans le navigateur et c'est ici que le
 * titre suit. Sans titre propre, une page porte le nom du site.
 */
export function useTitrePage(titre) {
  useEffect(() => {
    document.title = titre ? `${titre} — ${SITE}` : SITE
  }, [titre])
}
