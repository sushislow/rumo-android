# Validação — Rumo 1.5.1-beta

- Compilação Android de `assembleDebug` e `assembleCommercialDebug`: sucesso (70 tarefas). Versão 1.5.1-beta, versionCode 8.
- Testes existentes de lógica: migração, temas, estatísticas, restauração e backup AES-GCM passaram.
- Testes de recursos: datas ausentes/futuras, gatilhos, conquistas acumuladas e minimização de dados de widget/feedback passaram.
- Teste DOM de senhas: oito temas, visibilidade/validação, backup criptografado, cancelamento e limpeza ao bloquear passaram.
- Os testes Java de licença e integridade do cofre também passaram.
- Novo teste DOM: estados vazio/parcial/registrado, contagem acumulada, preferência de ocultação, navegação, calendário aberto e preservação de registros passaram.
- Revisão visual em navegador com quadros de celular e dados fictícios: início nos temas Oceano, Areia e Violeta; calendário expansível; sem rolagem horizontal em 390 px.

As telas nativas foram compiladas, mas não renderizadas em emulador nem testadas em aparelho nesta etapa. É necessário verificar tamanhos de fonte, barras do sistema, teclado, PIN e seleção de conta no celular.

Não foram criadas contas reais, feitas cobranças, publicados AABs na Play Store nem usados certificados do proprietário. As chaves locais de debug são temporárias e não devem substituir sua assinatura de atualização/publicação. Testes simulados não comprovam os fluxos reais de venda.
