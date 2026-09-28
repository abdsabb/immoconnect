import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'

const ancre = (i) => `section-${i + 1}`

function Liens({ liens }) {
  return (
    <ul className="mt-3 space-y-1">
      {liens.map((lien) => (
        <li key={lien.texte}>
          {lien.chemin
            ? <Link to={lien.chemin} className="font-semibold text-turquoise underline">{lien.texte}</Link>
            : <a href={lien.url} target="_blank" rel="noreferrer" className="font-semibold text-turquoise underline">{lien.texte}</a>}
        </li>
      ))}
    </ul>
  )
}

function Tableau({ tableau }) {
  return (
    <div className="mt-4 overflow-x-auto rounded-xl border border-gray-200">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="bg-perle text-nuit">
          <tr>{tableau.colonnes.map((colonne) => <th key={colonne} scope="col" className="px-4 py-3 font-semibold">{colonne}</th>)}</tr>
        </thead>
        <tbody>
          {tableau.lignes.map(([premiere, ...suite]) => (
            <tr key={premiere} className="border-t border-gray-200 align-top">
              <th scope="row" className="px-4 py-3 font-semibold text-nuit">{premiere}</th>
              {suite.map((cellule) => <td key={cellule} className="px-4 py-3 text-gray-700">{cellule}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// Mentions légales et politique de confidentialité (livrable 18). Une seule page pour les deux :
// le texte vient du dictionnaire « legal », section par section, dans la langue du visiteur.
export default function PageLegale({ page }) {
  const { t } = useTranslation('legal')
  const sections = t(`${page}.sections`, { returnObjects: true })

  return (
    <article className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-3xl font-bold text-nuit">{t(`${page}.titre`)}</h1>
      <p className="mt-1 text-sm text-gray-500">{t('miseAJour')}</p>

      <aside className="mt-6 rounded-xl border border-ambre bg-ambre/10 p-5">
        <h2 className="font-titre font-bold text-nuit">{t('demo.titre')}</h2>
        <p className="mt-1 text-sm text-gray-800">{t('demo.texte')}</p>
      </aside>

      <nav aria-label={t('sommaire')} className="mt-6 rounded-xl bg-perle p-5">
        <h2 className="font-titre font-bold text-nuit">{t('sommaire')}</h2>
        <ol className="mt-2 list-decimal space-y-1 pl-5 text-sm">
          {sections.map((section, i) => (
            <li key={section.titre}><a href={`#${ancre(i)}`} className="text-turquoise underline">{section.titre}</a></li>
          ))}
        </ol>
      </nav>

      {sections.map((section, i) => (
        <section key={section.titre} id={ancre(i)} className="mt-8 scroll-mt-4">
          <h2 className="text-xl font-bold text-nuit">{i + 1}. {section.titre}</h2>
          {section.paragraphes?.map((paragraphe) => <p key={paragraphe} className="mt-3 text-gray-800">{paragraphe}</p>)}
          {section.liste && (
            <ul className="mt-3 list-disc space-y-1 pl-5 text-gray-800">
              {section.liste.map((element) => <li key={element}>{element}</li>)}
            </ul>
          )}
          {section.tableau && <Tableau tableau={section.tableau} />}
          {section.liens && <Liens liens={section.liens} />}
        </section>
      ))}
    </article>
  )
}
