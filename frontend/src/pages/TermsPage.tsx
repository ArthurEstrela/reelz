import { LegalPageLayout } from '../components/legal/LegalPageLayout'

export function TermsPage() {
  return (
    <LegalPageLayout eyebrow="Documento legal" title="Termos de Uso" updatedAt="3 de agosto de 2026">
      <section>
        <h2>1. Sobre o beta</h2>
        <p>
          O Reelz é um produto em fase beta criado para ajudar pessoas a escolher filmes. Recursos,
          limites e disponibilidade podem mudar durante os testes. Ao participar, você entende que
          falhas e interrupções podem ocorrer e concorda em fornecer feedback de forma voluntária.
        </p>
      </section>
      <section>
        <h2>2. Conta e segurança</h2>
        <p>
          Você deve fornecer informações verdadeiras, manter sua senha protegida e não compartilhar
          acesso indevido. A conta é pessoal. Atividades automatizadas, abuso da API, tentativa de
          invasão ou prejuízo a outros participantes não são permitidos.
        </p>
      </section>
      <section>
        <h2>3. Recomendações e disponibilidade</h2>
        <p>
          A roleta é uma ferramenta de descoberta, não uma garantia de disponibilidade. Catálogos de
          streaming mudam e podem conter atrasos ou imprecisões. Sempre confirme preço, assinatura e
          disponibilidade no serviço indicado antes de assistir ou contratar algo.
        </p>
      </section>
      <section>
        <h2>4. Conteúdo de terceiros</h2>
        <p>
          Títulos, pôsteres, notas, sinopses, marcas e links pertencem aos respectivos titulares. O
          Reelz não hospeda nem transmite filmes. Links externos seguem os termos e políticas de seus
          próprios operadores.
        </p>
      </section>
      <section>
        <h2>5. Suspensão e encerramento</h2>
        <p>
          Podemos limitar ou encerrar contas usadas de forma abusiva ou que comprometam o beta. Você
          pode excluir sua conta em “Conta e privacidade”; os dados pessoais serão anonimizados e o
          acesso será encerrado imediatamente.
        </p>
      </section>
      <section>
        <h2>6. Responsabilidade</h2>
        <p>
          O serviço é oferecido no estado atual durante o beta. Dentro dos limites legais, não nos
          responsabilizamos por alterações de catálogo, indisponibilidade de terceiros ou decisões de
          compra tomadas fora do Reelz.
        </p>
      </section>
    </LegalPageLayout>
  )
}
