import { LegalPageLayout } from '../components/legal/LegalPageLayout'

export function PrivacyPage() {
  return (
    <LegalPageLayout eyebrow="Transparência" title="Política de Privacidade" updatedAt="28 de agosto de 2026">
      <section>
        <h2>1. Dados tratados</h2>
        <ul>
          <li>Nome, e-mail, país, fuso horário e senha armazenada exclusivamente como hash.</li>
          <li>Streamings selecionados, filmes assistidos, lista Quero Ver e escolhas do onboarding.</li>
          <li>Eventos de uso pseudonimizados, como sessão, giros e abertura de links de streaming.</li>
          <li>Notas e comentários que você decidir enviar pelo formulário de feedback do beta.</li>
          <li>Dados técnicos mínimos de segurança, diagnóstico e prevenção de abuso.</li>
        </ul>
      </section>
      <section>
        <h2>2. Finalidades</h2>
        <p>
          Utilizamos esses dados para operar a conta, evitar recomendações repetidas, manter segurança,
          corrigir falhas e medir se o beta realmente ajuda as pessoas a decidir. Não vendemos dados
          pessoais e a dashboard administrativa não exibe e-mails ou histórico individual.
        </p>
      </section>
      <section>
        <h2>3. Armazenamento local</h2>
        <p>
          O navegador usa sessionStorage para manter a sessão autenticada e um identificador aleatório
          da sessão de produto. Eles são removidos quando a sessão do navegador termina ou no logout,
          conforme o item.
        </p>
      </section>
      <section>
        <h2>4. Compartilhamento e terceiros</h2>
        <p>
          O CineGiro utiliza infraestrutura de hospedagem e fornecedores externos de catálogo e
          disponibilidade, incluindo TMDB e Movie of the Night. Ao abrir “onde
          assistir”, você é direcionado a uma página externa e passa a estar sujeito à política desse
          serviço. Não utilizamos publicidade comportamental neste beta fechado.
        </p>
      </section>
      <section>
        <h2>5. Retenção e segurança</h2>
        <p>
          Eventos de analytics são mantidos por até 180 dias por padrão. Dados da conta permanecem
          enquanto ela estiver ativa ou pelo prazo necessário para obrigações legais e segurança.
          Aplicamos conexão protegida no deploy, controle de acesso, hash de senha e auditoria técnica.
        </p>
      </section>
      <section>
        <h2>6. Seus direitos</h2>
        <p>
          Você pode consultar e corrigir seu perfil ou excluir a conta diretamente em “Conta e
          privacidade”. Na exclusão, os identificadores pessoais são anonimizados e a sessão é encerrada.
          Para outros direitos, use o canal privado informado no convite; não publique dados pessoais em
          issues ou fóruns públicos.
        </p>
      </section>
    </LegalPageLayout>
  )
}
