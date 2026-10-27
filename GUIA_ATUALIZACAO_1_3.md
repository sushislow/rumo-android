# Rumo Beta 1.3.0 — atualizar pelo GitHub

Pacote completo para atualizar o projeto Android 1.2.x, mantendo a nova logo. O arquivo é código para recompilar, não um APK já assinado com a sua chave.

## Antes de atualizar

No Rumo atual, abra **Dados → Backup protegido por senha**. Guarde o arquivo fora do app e lembre a senha do backup. Ela é independente do PIN novo.

Instalar sobre o aplicativo existente depende da mesma assinatura. Um APK debug gerado em outro ambiente pode ter assinatura diferente. Se o Android exigir desinstalação, tenha o backup externo antes: desinstalar remove os registros e as chaves locais. Depois instale e restaure o backup. Se você usa o workflow de release, mantenha a mesma chave e os mesmos segredos do repositório.

## Terminal do Codespaces

Envie `Rumo_Atualizacao_v1_3_0.zip` para a raiz do projeto Android, onde existem `app` e `settings.gradle`. Execute um comando por vez:

```bash
unzip -o Rumo_Atualizacao_v1_3_0.zip
git status
git add app tests .github .gitignore README.md CHANGELOG.md PRIVACIDADE_RASCUNHO.md GUIA_ATUALIZACAO_1_3.md REFERENCIAS_1_3.md VALIDACAO_v1_3.md
git commit -m "Atualizar Rumo beta 1.3.0: privacidade, conquistas e widget"
git push
```

Na branch `main`, o GitHub Actions deve iniciar **Gerar APK do Rumo**. Quando terminar, baixe **Artifacts → Rumo-APK**, extraia e instale `app-debug.apk`. Se não iniciar, use **Actions → Gerar APK do Rumo → Run workflow → main**.

O workflow testa o diário, as contagens e a criptografia antes de compilar. A compilação depende de Java 17, Gradle 8.9, plataforma Android 35 e Build Tools 35.0.0, já configurados.

## Configurar o e-mail de feedback

O destinatário está vazio porque o endereço ainda não foi informado. Nenhum e-mail fictício foi colocado no aplicativo.

Depois de criar o endereço, edite somente:

`app/src/main/res/values/rumo_support.xml`

Preencha a string com o seu endereço real:

```xml
<string name="support_email" translatable="false">SEU_ENDERECO_REAL</string>
```

Substitua o texto acima pelo e-mail; não coloque senha. Salve, faça commit e recompile. Enquanto estiver vazio, a interface informa que o canal está em preparação e oferece **Copiar mensagem**.

O botão de envio abre um rascunho no aplicativo de e-mail escolhido pela pessoa. O envio depende da confirmação dela nesse aplicativo. Se não houver cliente de e-mail, a pessoa pode copiar o texto. O remetente do e-mail fica visível a quem recebe.

## Ativar PIN e biometria

1. Abra **Dados → Proteção e bloqueio por PIN → Criar PIN**.
2. Escolha de 6 a 12 números e repita-os. Zeros iniciais são aceitos.
3. Saia do Rumo e volte: a tela nativa deve pedir o PIN antes de exibir o diário.
4. Em **Gerenciar bloqueio**, a biometria pode ser ativada se houver biometria forte cadastrada e compatível no aparelho.

Depois de cinco erros, há espera de pelo menos 30 segundos, aumentando com novas tentativas. A espera e o verificador do PIN persistem no cofre criptografado. Alterar ou desativar o PIN exige confirmar o PIN atual.

Não existe recuperação remota do PIN. Se a biometria estiver ativa, ela pode permitir abrir o diário e exportar um backup. Sem um meio de desbloquear, reinstalar e restaurar exige um backup externo e a senha dele. O PIN e os dados biométricos não entram no backup exportado.

A criptografia local funciona também quando o PIN está desativado. O PIN controla a abertura do diário; não substitui o bloqueio de tela do Android nem protege um sistema comprometido. A chave do cofre é do Android Keystore e não é exportada ao JavaScript.

## Migração dos registros antigos

Ao abrir a atualização sobre a instalação anterior, os registros e preferências antigos são copiados para o cofre. A remoção das antigas chaves do armazenamento web só ocorre após a gravação e a leitura de confirmação do cofre. Um erro interrompe a abertura e preserva a cópia anterior, em vez de mostrar um diário vazio como se estivesse tudo certo.

Essa remoção é lógica; não é uma promessa de apagamento forense de cópias antigas, backups ou dispositivos comprometidos.

## Gatilhos, estratégias e modo rápido

- As opções prontas continuam disponíveis.
- No check-in, escreva um rótulo curto e toque **Adicionar gatilho**. Salve o check-in depois.
- São até 30 opções pessoais, 60 caracteres por rótulo e até 20 gatilhos selecionados por dia.
- Maiúsculas/minúsculas e formas equivalentes de acentos não criam opções duplicadas.
- Em **Dados → Meus gatilhos personalizados**, ocultar uma opção mantém os registros antigos e os gráficos.
- **Qual alternativa você tentou?** permite até 300 caracteres; é opcional e também vai para o resumo copiável e o backup.
- No modo rápido, os detalhes ficam recolhidos. Eles continuam disponíveis, e alternar o modo ou adicionar um gatilho preserva o rascunho atual.
- Rascunhos não salvos não têm garantia de recuperação após o processo do app ser encerrado pelo Android. Use **Salvar check-in** para persistir.

## Plano, conquistas e resumo semanal

Em **Dados → Meu plano**, registre uma situação e uma ação viável. Esse plano aparece primeiro na **Ajuda rápida**.

Conquistas reconhecem registros, dias sem consumo, impulsos interrompidos e alternativas que ajudaram. São cumulativas; um dia com consumo não retira conquistas anteriores. Não há ranking, moeda, sorteio ou bloqueio das ferramentas de ajuda. Em **Evolução**, uma visão compacta mostra destaques e permite expandir todas as conquistas.

Você pode ocultar conquistas e contadores em **Dados → Check-in e conquistas**. Dias sem registro e datas futuras não são somados como sucesso. A sequência opcional só usa dias consecutivos confirmados e sinaliza lacunas como pendências.

O resumo semanal inclui apenas os últimos sete dias registrados. “Alternativas úteis” resume relatos da pessoa; a frequência de um gatilho não comprova causalidade e os números não diagnosticam uma condição.

## Widget

1. Abra **Dados → Widget na tela inicial**.
2. Escolha a métrica: total registrado sem consumo, total de registros ou sequência confirmada.
3. Se quiser exibir o número fora do app, ative a permissão de mostrar a contagem e toque **Salvar widget**.
4. Toque **Adicionar à tela inicial** e confirme no Android. Se o launcher não oferecer o atalho, mantenha um espaço vazio da tela inicial pressionado → **Widgets → Rumo**.

O widget é opcional. Por padrão, a contagem fica oculta. Quando ativada, fica visível a quem olhar a tela inicial, independentemente do PIN do app. O rótulo neutro “dias de progresso” representa o total de dias registrados sem consumo.

Tocar abre o check-in e respeita o PIN. O widget não soma automaticamente um dia sem registro. A sequência pode mostrar “Abra para atualizar” quando estiver desatualizada. O horário de atualização é administrado pelo Android; a visualização informa a data de atualização. O widget usa somente um resumo autorizado, sem notas, gatilhos, PIN ou conteúdo do diário.

## Backups e compatibilidade

Backups antigos das versões 1 e 2 continuam importáveis. O conteúdo novo usa a versão 3 e inclui preferências, plano, gatilhos e conquistas; o envelope protegido mantém AES-GCM e PBKDF2-SHA256. PIN, biometria e chave local não são exportados.

Ao importar, datas futuras/invalidas são descartadas; dias existentes na cópia substituem os correspondentes após confirmação, preservando os demais. Preferências restauradas deixam lembretes e contagem do widget desligados até a pessoa reativar. A importação não pode desativar o PIN nativo.

O JSON simples e o CSV continuam sem criptografia. Fórmulas de planilha em campos textuais são neutralizadas no CSV. Prefira o backup protegido por uma senha longa e única.

## Conferência no aparelho

Teste com informações fictícias antes de convidar outras pessoas: atualização/restauração, criação de PIN, retorno após segundo plano, PIN incorreto, biometria, adicionar/ocultar gatilho, plano, todos os temas, widget no launcher usado e rascunho de e-mail. A disponibilidade de biometria e os diálogos do launcher variam por aparelho.

A demo pública do GitHub Pages não é atualizada automaticamente por este pacote. O cofre e o bloqueio são recursos do APK Android.
