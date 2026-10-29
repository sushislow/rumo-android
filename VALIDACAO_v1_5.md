# Validação da base comercial 1.5.0-beta

## Verificado nesta entrega

- Compilação Android de `assembleDebug`, `assembleCommercialDebug` e `bundleRelease`: sucesso, API 36/Java 17/AGP 8.10.1/Gradle 8.11.1, incluindo Billing 9.1.0. O AAB local é de validação, sem assinatura de produção; não foi enviado à Play Store.
- Licenças Java: 141 verificações com RSA real gerada temporariamente, conta/origem/pacote/ambiente, vencimento, duração, algoritmo, alteração de assinatura/conteúdo e decodificação.
- Criptografia do cofre: AES-GCM, integridade, IVs, chave incorreta, PIN e limitação de tentativas passaram.
- Lógica do diário e recursos anteriores: migração, datas/lacunas, conquistas, estratégias, widget, exportação/importação protegida passaram.
- Interface DOM: oito temas, senha oculta/mostrar, comprimento/confirmação, backup protegido, cancelar e limpeza ao bloquear em segundo plano passaram.
- XML Android válido. O verificador comercial recusou corretamente os campos ainda não configurados.
- Servidor Web: TypeScript estrito passou; 21 testes com SQL real em memória e provedores simulados passaram. Casos: identidade e preço confiáveis, assinaturas, compra pendente/indevida, compra Web/Play, restauração sem duplicar, comprovante em outra conta, reembolsos, notificações Google autenticadas e atualização concorrente fora de ordem.
- Página de apresentação: texto do novo fluxo, compra única e uso offline conferidos no navegador. Mantém ausência de cobrança e configuração pública vazia.

## Ainda não verificado/configurado

Não houve cadastro/login real Firebase, envio real de verificação/recuperação, transação sandbox/real Mercado Pago, compra pela faixa de testes Google Play, RTDN real ou assinatura com chave do proprietário. Não houve teste nativo em emulador ou aparelho nesta etapa. Teclado, insets Android 16, PIN/biometria, ciclo de vida da compra e atualização de instalação existente exigem aparelho real.

O servidor comercial precisa ser publicado/configurado na conta do proprietário. A página Sites publicada é apresentação estática e não ativa o Worker por si só. Não há venda, produto Play cadastrado, conta de suporte definitiva nem publicação na loja.

## Limites práticos

O debug pessoal não cobra nem exige licença; não é o produto comercial. Os builds comerciais ficam fechados se faltar configuração ou autorização. A última licença válida pode funcionar offline por até sete dias, inclusive após um reembolso que o celular ainda não recebeu. O acesso e o diário são independentes: conta não restaura check-ins e revogação não os apaga.

Os testes automatizados comprovam os cenários exercitados, não equivalem a auditoria independente, aprovação da loja ou garantia contra modificação do APK.
