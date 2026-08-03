import { LegalPageLayout } from '../components/legal/LegalPageLayout'

const TMDB_LOGO_URL = 'https://www.themoviedb.org/assets/2/v4/logos/v2/blue_square_2-d537fb228cf3ded904ef09b136fe3fec72548ebc1fea3fbbd1ad9e36364db38b.svg'

export function AboutPage() {
  return (
    <LegalPageLayout eyebrow="Créditos" title="Sobre o Reelz" updatedAt="3 de agosto de 2026">
      <section>
        <h2>Decidir sem transformar a noite em catálogo</h2>
        <p>
          O Reelz combina seus streamings e filtros rápidos para sortear uma opção elegível, excluindo
          o que você já assistiu. O beta existe para descobrir se essa experiência realmente reduz o
          tempo até a decisão.
        </p>
      </section>
      <section className="rounded-2xl border border-[#01b4e4]/20 bg-[#0d253f]/35 p-5">
        <a href="https://www.themoviedb.org" target="_blank" rel="noreferrer" aria-label="Visitar o TMDB">
          <img src={TMDB_LOGO_URL} alt="TMDB" className="h-16 w-24 object-contain" />
        </a>
        <p className="mt-3">
          This product uses the TMDB API but is not endorsed or certified by TMDB.
        </p>
        <p className="mt-2">
          Metadados, pôsteres e informações de disponibilidade são obtidos por meio dos serviços do
          TMDB.
        </p>
      </section>
      <section>
        <h2>Disponibilidade</h2>
        <p>
          Informações de onde assistir são fornecidas pelo JustWatch via TMDB e podem mudar. O link
          exibido no resultado preserva a atribuição recebida da fonte.
        </p>
      </section>
      <section>
        <h2>Documentos</h2>
        <p>
          Consulte os <a href="/terms">Termos de Uso</a> e a <a href="/privacy">Política de Privacidade</a>.
        </p>
      </section>
    </LegalPageLayout>
  )
}
