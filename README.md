# Rumo — Diário de progresso · 1.5.0-beta

Base para instalação gratuita pela Play Store com compra única de acesso ligada à conta. Diário local criptografado, PIN, biometria opcional, backup protegido, oito temas, conquistas e widget são preservados.

A versão comercial verifica a licença antes de carregar o diário. Conta Firebase e compra pelo site ou Google Play convergem para a mesma autorização. A autorização assinada permite até sete dias offline. Tokens, senhas e comprovantes nunca entram no JavaScript do diário.

**Serviços ainda não configurados. Não está pronto para vendas.** Não há publicação Play, produto cadastrado, credenciais, chave comercial ou transações reais nesta entrega.

- `GUIA_ATUALIZACAO_1_5.md`: enviar esta atualização ao GitHub/Codespaces.
- `CONFIGURAR_VENDAS_1_5.md`: configurar contas, servidor, produto e assinaturas.
- `VALIDACAO_v1_5.md`: testes feitos e limites.

O GitHub Actions gera dois APKs de teste: `Rumo-APK` (debug pessoal, sem exigência de compra) e `Rumo-Ativacao-Teste` (commercialDebug, com conta/licença obrigatórias). **Nunca distribua o debug pessoal como produto comercial.** O release exige licença e o workflow manual exige configuração pública e assinatura antes de produzir AAB/APK.

```bash
node tests/rumo-smoke.js
node tests/rumo-features.js
bash tests/run-java-tests.sh
```

A interface DOM é testada por `tests/security-ui.cjs` (jsdom 26.1.0). Android: Java 17, AGP 8.10.1, Gradle 8.11.1, compile/target API 36, mínimo API 24 e Play Billing 9.1.0. Não substituímos o diário existente por outro projeto.

O site e o Worker TypeScript são um projeto separado. Dentro do aplicativo, a compra usa o Google Play Billing; não há botão para checkout externo. O preço é obtido do Google. Kirvano não está integrada.

Sem promessa de cura, certificação de segurança ou proteção inviolável contra redistribuição/modificação de APK. Documentos 1.3/1.4 preservados são históricos; o guia 1.5 prevalece para o modelo comercial.
