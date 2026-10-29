# Atualizar o Rumo para 1.5.1-beta

Este pacote é o código Android completo. O site continua separado. Não é um APK pronto para vender.

## Antes de instalar
Exporte o backup protegido do diário e guarde a senha. Atualizações de um APK precisam da mesma chave de assinatura. A chave debug automática do GitHub pode mudar entre execuções; se o Android recusar a atualização, não desinstale nem limpe os dados. Primeiro precisamos acertar a assinatura estável e preservar o backup.

## No Codespaces do repositório Android
Envie `Rumo_Atualizacao_v1_5_1_beta.zip` para a raiz, onde ficam `app/` e `settings.gradle`. Execute um comando por vez:

```bash
unzip -o Rumo_Atualizacao_v1_5_1_beta.zip
```

```bash
git status
```

```bash
git add app tests .github README.md CHANGELOG.md GUIA_ATUALIZACAO_1_5_1.md VALIDACAO_v1_5_1.md
```

```bash
git commit -m "Melhorar início e boas-vindas do Rumo 1.5.1"
```

```bash
git push
```

Em GitHub > Actions > Gerar APK do Rumo, aguarde a execução. O arquivo `Rumo-APK` testa o diário sem exigir licença. O arquivo `Rumo-Ativacao-Teste` mostra a nova entrada e exige conta/licença; o servidor de ativação ainda precisa ser configurado. Não distribua o APK debug pessoal como produto comercial.

A configuração Firebase Android enviada já está neste pacote; o ZIP de configuração anterior não precisa ser reaplicado. Se você já preencheu alguma configuração adicional no GitHub depois dele (servidor/licença/suporte), preserve esses valores ao revisar o diff antes do commit. Nunca inclua chaves privadas ou backups pessoais no GitHub.

## Testar no celular
Confira a tela inicial, ajuda rápida, revisão de um registro, troca de mês e os temas claro/escuro. Registre um dia parcial e verifique se ele continua diferente de um dia sem consumo. Confirme que os dados antigos e seu PIN permanecem.

## O que falta para vender
1. Assinatura estável, certificados SHA do Android e teste real de login/recuperação no aparelho.
2. Servidor de acesso, banco de pedidos e chave de licença configurados.
3. Produto Google Play e checkout Web em ambiente de testes, com preço e alcance da licença definidos.
4. Verificar compra aprovada, pendente, cancelamento, reembolso, restauração e bloqueio de conta sem compra.
5. Suporte, política de privacidade, termos comerciais, página de exclusão e preparação/publicação na Play Store.

O login Web foi confirmado pelo proprietário. Isso valida uma etapa de autenticação; ainda não valida pagamento, licença, Android ou distribuição.
