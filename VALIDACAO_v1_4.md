# Validação — Rumo 1.4.0-beta

Data: 28/10/2026. Esta é uma preparação para testes, não uma auditoria ou certificação de prontidão comercial.

## Resultado obtido

- **Compilação Android limpa: passou.** Java 17, Gradle 8.9, Android SDK 35/Build Tools 35.0.0. Comando: `gradle clean :app:assembleDebug --no-daemon`. Resultado final: `BUILD SUCCESSFUL`, 35 tarefas executadas. O APK de debug gerado usa uma chave de teste; não representa sua assinatura definitiva de distribuição.
- **Testes de lógica do diário: passaram.** Registros antigos, novos campos opcionais, temas, lembretes desligados inicialmente, mensagens, estatísticas, backup AES-GCM e restauração/mesclagem.
- **Testes de recursos: passaram.** Gatilhos personalizados, lacunas de datas, datas futuras, conquistas permanentes, resumo semanal e minimização dos dados de widget/e-mail.
- **Testes Java de criptografia: passaram.** Integridade AES-GCM, IV aleatório, chave incorreta, formato e hashing de PIN e atraso entre tentativas.
- **Interface por DOM: passou.** Oito temas com persistência; senha mascarada e mostrar/ocultar; mínimo de caracteres e confirmação; criação de backup criptografado; cancelamento sem arquivo; descarte dos campos ao solicitar bloqueio nativo.
- **Inspeção visual no navegador: realizada.** Página em desktop e moldura de celular de 390 px; temas do diário e campos da conta na página. A versão de apresentação foi publicada com acesso privado para revisão. A página publicada exige a conta ChatGPT do proprietário; o navegador de QA chegou ao controle de acesso, não a uma sessão autenticada do proprietário.
- **XML do Android: analisado sem erro.** Manifest, layouts, cores e recursos.
- **Servidor comercial separado: 11 testes passaram.** Código TypeScript e SQL com SQLite em memória, e respostas simuladas de autenticação/pagamento. Ver `GUIA_LANCAMENTO.md` no projeto web.

## Limites explícitos

1. Não foi usado emulador nem aparelho físico nesta execução. Não houve teste real de biometria, PIN na tela Android, widget na tela inicial ou seletor de contas Google. São pendências de validação no aparelho.
2. O Firebase vem desativado e sem configurações. Não foram criadas contas reais, enviados e-mails de recuperação nem validados os certificados SHA do proprietário. A implementação precisa ser configurada e testada ponta a ponta no projeto do Rumo.
3. O servidor de pagamento não recebeu credenciais nem transações reais/de sandbox. As vendas continuam desativadas. Os testes de integração com os provedores são pendentes.
4. A janela do backup foi testada funcionalmente no DOM com WebCrypto, além dos testes de criptografia. A prévia HTTP do navegador não oferece o mesmo contexto criptográfico da origem HTTPS interna do APK; ela não foi usada como prova da exportação no aparelho.
5. A compilação de release com sua chave definitiva não foi realizada. O build de debug não resolve requisitos de assinatura, distribuição, suporte, termos, privacidade ou cadastro do desenvolvedor.
6. O login não sincroniza o diário, não recupera o PIN e não recupera a senha do backup. O controle de download comercial não impede compartilhar um APK já baixado. A beta não impõe DRM ao diário local.

## Conferência antes de atualizar dados reais

Exporte um backup protegido e guarde sua senha fora do app. Instale a beta de testes usando a mesma chave quando possível. Verifique PIN/biometria ao alternar apps e voltar, contraste dos oito temas, senhas e cancelamento, exportação/restauração e widget. Só depois configure a conta e confira login, e-mail de verificação, recuperação, sair/excluir conta e separação do diário.

Não desinstale sua versão com registros reais para experimentar uma chave de debug diferente sem confirmar o backup externo e sua senha. Faça os primeiros testes com dados fictícios.
