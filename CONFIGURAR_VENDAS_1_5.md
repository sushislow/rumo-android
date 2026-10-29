# Rumo 1.5.0-beta — conta, compra única e Play Store

Esta é a base de código do novo modelo comercial. O aplicativo ainda não foi publicado na Play Store. Firebase, pagamentos, domínio do servidor e chaves de produção não foram configurados. Não comece anúncios ou cobranças antes de testar o fluxo inteiro.

## O modelo escolhido

- Anúncio → site → criar conta do Rumo → checkout → compra aprovada → instalar gratuitamente pela Play Store → entrar na mesma conta.
- Busca na Play Store → instalar gratuitamente → criar conta → compra única pelo Google Play → ativação.
- Quem já comprou entra na conta e atualiza o acesso. Uma compra pelo site e uma compra pelo Google liberam o mesmo produto lógico.
- A senha do Rumo é gerenciada pelo Firebase. Uma conta da plataforma de pagamento não substitui essa conta.
- O servidor guarda conta, pedidos, comprovantes e estado da licença. O diário continua no cofre local. Login não restaura registros de outro celular.

## 1. Primeiro: Firebase gratuito

1. Use o projeto Firebase da sua conta. Para esta etapa, o plano Spark e Authentication bastam; não ative Analytics, Firestore ou armazenamento do diário.
2. Ative E-mail/Senha e Google em Authentication. Configure senha mínima de 12 caracteres e proteção contra enumeração.
3. Registre o Android `br.com.rumo.diario` e um aplicativo Web no **mesmo projeto**.
4. Registre os certificados SHA-1/SHA-256 dos builds de teste e, depois, do certificado de assinatura de aplicativos do Google Play. A chave de upload e a chave usada pelo Play podem ser diferentes.
5. Copie apenas a configuração pública para `app/src/main/res/values/rumo_account.xml`: API key, project ID, application ID Android e client ID Web usado pelo login Google. Ative `rumo_account_enabled` quando o serviço estiver pronto.
6. Configure os domínios Web autorizados, verificação de e-mail e recuperação por link de uso único. A recuperação não altera PIN nem senha de backup.

A integração Firebase disponível no ChatGPT ainda precisa ser instalada/conectada para configurarmos seu projeto por ela. Nenhuma senha ou chave privada precisa ser enviada no chat.

## 2. Servidor e página de compra

O código Web está no projeto separado **Rumo — Seu próximo passo**, pasta `backend`, com o guia de lançamento atualizado. A página pública atual é de apresentação. O Worker e o D1 precisam ser publicados/configurados para atender `/api/*`.

Use Node 24 e siga o guia Web para criar o D1, aplicar `backend/schema.sql` e configurar a origem HTTPS. Comece com `SALES_ENABLED=false`, `MP_MODE=test` e `PRICE_CENTS=0`. O preço ainda será escolhido por você; valores dos testes automatizados são fictícios.

Crie uma chave RSA de licença fora do repositório, em local privado seu:

```bash
mkdir -p "$HOME/rumo-segredos"
chmod 700 "$HOME/rumo-segredos"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$HOME/rumo-segredos/license-private.pem"
chmod 600 "$HOME/rumo-segredos/license-private.pem"
openssl pkey -in "$HOME/rumo-segredos/license-private.pem" -pubout -out "$HOME/rumo-segredos/license-public.pem"
```

- Guarde a **privada** somente como segredo `LICENSE_PRIVATE_KEY` do servidor e em backup privado seu. Não é a mesma chave da assinatura do APK nem a conta de serviço Google.
- Copie a **pública SPKI PEM** para `rumo_license_public_key` no XML Android. Nunca copie uma chave privada para esse arquivo.
- Configure `rumo_account_origin` com a origem HTTPS exata do Worker, sem barra final/caminho. Ela deve coincidir com `PUBLIC_ORIGIN`.
- `rumo_purchase_mode` precisa coincidir com `MP_MODE`: `test` durante testes, `live` no lançamento. Use ambiente e chaves separados para testes e produção.

## 3. Google Play e compra única

1. Conclua o cadastro/verificação da conta de desenvolvedor e os testes exigidos no Play Console. O código não paga nem cria essa conta por você.
2. Cadastre o Rumo como instalação gratuita, informando claramente que o uso exige compra.
3. Crie um produto de aquisição única **não consumível**, ID `rumo_acesso_completo`, com uma única opção de compra permanente e preço escolhido por você. Não use aluguel, pré-venda ou múltiplas ofertas nesta primeira integração.
4. Crie/configure uma conta de serviço com acesso mínimo necessário à Google Play Developer API para consultar e confirmar compras. O JSON privado vai apenas para `PLAY_SERVICE_ACCOUNT_JSON` no servidor; nunca para o app, GitHub ou chat.
5. No Worker configure `PLAY_PACKAGE_NAME=br.com.rumo.diario`, `PLAY_PRODUCT_ID=rumo_acesso_completo`. Ative `rumo_play_billing_enabled` no Android após preparar o produto.
6. Publique um AAB assinado na faixa de teste e cadastre testadores de licença. Instale pelo link da faixa, usando a conta testadora da loja. O pagamento de teste usa os meios de teste do Google, sem cobrança real.
7. Configure notificações RTDN/Pub/Sub autenticadas para `/api/play/notifications`. A audience OIDC é a URL HTTPS completa desse endpoint. O e-mail da identidade de push vai em `PLAY_PUSH_SERVICE_ACCOUNT_EMAIL`. O guia Web explica o limite offline e a revalidação.

O app envia ao Google um identificador de conta ofuscado. O servidor exige esse vínculo e valida produto, estado, quantidade, reembolso e ambiente. Um comprovante não pode ativar outra conta do Rumo. A compra não é consumida; a confirmação é feita no servidor.

## 4. Compra no site

A implementação atual usa Mercado Pago Checkout Pro. Kirvano não foi integrada nesta entrega. É possível trocar o provedor depois, mantendo a mesma identidade e licença.

Configure vendedor, credenciais de teste e webhook assinado conforme o guia Web. Primeiro use compradores e pagamentos de teste. O servidor define o preço e só libera após aprovação; parâmetros de retorno da página não concedem acesso.

O checkout só fica disponível se a publicação/entrega pela Play Store e a assinatura de licença estiverem configuradas. `PLAY_STORE_PUBLISHED=true` é uma declaração operacional que só deve ser ativada após confirmar o link real da loja. Ela não publica o app automaticamente.

Dentro do Android, as compras usam Google Play Billing. A tela nativa não abre o checkout externo. As regras de links externos variam por mercado e programa; não acrescente um link de pagamento no app sem rever essas regras.

## 5. Três builds com finalidades distintas

| Build | Para que serve | Exige licença |
|---|---|---|
| `debug` / artefato `Rumo-APK` | Continuar testando o diário pessoal | Não. Nunca distribuir como produto comercial. |
| `commercialDebug` / `Rumo-Ativacao-Teste` | Conferir a tela de ativação e a integração | Sim. Fica fechado se a configuração estiver incompleta. |
| `release` / `Rumo-Release-Assinado` | AAB da loja e APK comercial | Sim. |

O workflow de release exige a configuração pública e os quatro segredos de assinatura existentes. Produz `app-release.aab` e `app-release.apk`. AAB vai ao Play Console. Preserve sua chave de assinatura: uma chave diferente pode impedir atualizar instalações atuais. Faça backup do diário antes de qualquer instalação.

## 6. Offline e privacidade

O servidor emite uma autorização assinada válida por até sete dias, ligada à conta, produto, ambiente e pacote. O Android verifica a assinatura com a chave pública. A autorização é revalidada ao voltar ao app, com renovação após 24 horas de uso online; sem conexão, a última autorização ainda válida permite usar o diário.

Depois de sete dias é necessário conectar. Um reembolso conhecido pelo servidor impede nova autorização. Um celular offline pode continuar usando a autorização até ela vencer. Os registros locais não são apagados por falta de licença ou reembolso. A implementação reduz acesso indevido, mas um app local não tem proteção inviolável contra modificação de APK ou aparelho comprometido.

O código protege a ponte JavaScript, antes de ler/escrever o diário, por PIN e licença no build comercial. Tokens, senhas e comprovantes ficam na parte nativa. O WebView do diário continua sem acesso à internet.

## 7. Validação antes das vendas

Testar em aparelho real: cadastro/e-mail, Google, recuperação, compra Play aprovada/pendente/cancelada, restauração na mesma conta, tentativa em outra conta, compra Web, webhook/RTDN, reembolso, modo offline e vencimento. Também testar teclado, PIN, biometria, importação/exportação e atualização de uma instalação existente sem perda de dados.

Antes de cobrar: preencher preço/escopo da licença, identidade e contato do vendedor, termos definitivos, privacidade, exclusão de conta, Data safety/classificação e demais requisitos do Play Console. A conta comercial e a publicação ainda não estão prontas apenas por compilar o projeto.

Fontes oficiais consultadas: [Pagamentos](https://support.google.com/googleplay/android-developer/answer/9858738?hl=pt-BR), [produtos únicos](https://developer.android.com/google/play/billing/one-time-products), [verificação segura](https://developer.android.com/google/play/billing/security), [API Android exigida](https://support.google.com/googleplay/android-developer/answer/11926878?hl=pt-BR), [RTDN](https://developer.android.com/google/play/billing/rtdn-reference).
