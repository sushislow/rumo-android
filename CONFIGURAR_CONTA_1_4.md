# Conta opcional — Google e e-mail/senha

## Configuração padrão

`app/src/main/res/values/rumo_account.xml` vem com `rumo_account_enabled=false` e campos vazios. Não há contas demonstrativas nem credenciais de exemplo que funcionem. Sem configuração, o diário continua utilizável e a área Conta explica o estado de testes.

## Configure na sua conta Firebase

1. Crie um projeto Spark (gratuito, sujeito a cotas). Não é necessário ativar Analytics, Firestore, Storage ou cobrança para autenticação por Google/e-mail nesta etapa.
2. Habilite Authentication → E-mail/Senha e Google. Defina política de senha no serviço com mínimo de 12 caracteres e mantenha uma conta por e-mail. Valide os limites exibidos no painel.
3. Registre o pacote Android `br.com.rumo.diario` e seus certificados SHA-1/SHA-256 de debug e release. A assinatura precisa corresponder ao APK instalado. O login Google pode falhar quando falta o certificado correto.
4. Registre um app Web **no mesmo projeto** para a página de venda. Configure os domínios autorizados e o cliente OAuth do tipo Web.
5. Preencha em `rumo_account.xml`: a API key pública do app Android, project ID, app ID Android, cliente OAuth Web (não o cliente Android) e a origem HTTPS pública da página de conta, sem barra final ou caminho. Esses valores identificam seu projeto; não são o segredo do Mercado Pago nem uma chave privada de conta de serviço.
6. Só então altere `rumo_account_enabled` para `true` e compile.

O código inicializa apenas Firebase Authentication, em uma instância nomeada, ao abrir voluntariamente a área de conta. Não adiciona Analytics, Firestore, sincronização de diário ou captura de registros. O SDK mantém a sessão da conta dentro dos dados privados do aplicativo. O diário continua no cofre separado.

## Como o Google funciona

O APK usa Credential Manager e a biblioteca Google ID, com o cliente Web configurado, e troca a credencial com Firebase Authentication. O login não roda dentro da WebView do diário. Em um aparelho sem um provedor compatível, a pessoa ainda pode usar e-mail e senha.

O token e a senha nunca são retornados por `getAppInfo` nem disponibilizados ao JavaScript do diário. A consulta do acesso usa HTTPS, sem redirecionar o token para outro destino. A opção de sair limpa a sessão de autenticação e o estado do Credential Manager.

## Recuperação de senha

Firebase envia um **link com código de uso único** ao e-mail. A conta de e-mail deve estar acessível. O usuário abre o link, escolhe uma nova senha e entra novamente. A página do Rumo trata `resetPassword` e `verifyEmail` quando configurada como URL de ação nos modelos de e-mail do projeto.

O PIN local não vai ao Firebase. Redefinir a senha da conta não altera o PIN nem a senha de um backup protegido. Perder acesso ao PIN e à biometria exige planejar a restauração pelo backup, como já informado pelo aplicativo.

Se a redefinição estiver na página padrão do Firebase, o fluxo funciona mas terá o visual do provedor. Configure o manipulador personalizado na página pública do Rumo para usar o tema da marca. O endereço privado de revisão não é adequado para enviar links aos compradores.

## Consulta da compra

Configure `rumo_account_origin` para a mesma origem HTTPS do Worker de produção/teste correspondente. O botão Conferir meu acesso valida a sessão no Firebase e consulta `/api/access` com token no cabeçalho, sem check-ins no corpo. A página web do projeto comercial fornece o download autenticado após aprovação.

A área de conta não bloqueia o diário desta beta nem implementa DRM. Proteger o link de download não impede um comprador de compartilhar o arquivo APK. Não prometa proteção contra redistribuição que o produto ainda não oferece.

## Exclusão da conta

O usuário pode excluir a conta de acesso após uma confirmação. Firebase pode exigir login recente; nesse caso é preciso sair, entrar e repetir. Isso remove a identidade de acesso, não os check-ins locais nem registros fiscais/de compra mantidos no servidor. O procedimento definitivo de exclusão e retenção precisa constar da política comercial, com o responsável e seu canal de contato.

## Testes necessários com os serviços configurados

- Cadastro e login de e-mail, senha errada, política de senha, verificação e recuperação por e-mail.
- Link expirado e já usado. Conferir o visual da página personalizada.
- Google com conta existente e nova, certificado de release e cancelamento do seletor.
- Entrar/sair/excluir conta sem alterar diário, PIN ou backup.
- Deixar o app em segundo plano e confirmar que a área de conta e suas senhas não ficam acima da tela de PIN ao retornar.
- Usar o diário sem internet; a consulta de conta pode ficar indisponível, mas os registros continuam locais.
- Conta diferente não pode baixar a compra de outra pessoa.

Referências oficiais: https://firebase.google.com/docs/auth/android/google-signin · https://firebase.google.com/docs/auth/custom-email-handler · https://firebase.google.com/docs/auth/limits
