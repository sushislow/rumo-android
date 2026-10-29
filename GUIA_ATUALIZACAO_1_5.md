# Atualizar o código para 1.5.0-beta

Use o Codespaces do repositório Android do Rumo. Faça backup do diário antes de instalar qualquer nova compilação. Este ZIP contém código e instruções; não contém serviços ativos, chaves privadas nem APK pronto para vendas.

1. Envie `Rumo_Atualizacao_v1_5_0_beta.zip` inteiro para a raiz do Codespaces, onde ficam `app/` e `settings.gradle`.
2. Execute um comando por vez:

```bash
unzip -o Rumo_Atualizacao_v1_5_0_beta.zip
```

```bash
git status
```

```bash
git add app tests scripts .github .gitignore build.gradle gradle.properties settings.gradle README.md CHANGELOG.md CONFIGURAR_VENDAS_1_5.md GUIA_ATUALIZACAO_1_5.md VALIDACAO_v1_5.md PRIVACIDADE_RASCUNHO.md
```

```bash
git commit -m "Preparar Rumo 1.5 beta: conta, licença e compra única"
```

```bash
git push
```

3. Em GitHub → Actions → Gerar APK do Rumo, espere o resultado. `Rumo-APK` continua sendo a versão pessoal de testes. `Rumo-Ativacao-Teste` exige conta/licença e mostra a preparação enquanto os serviços não estiverem configurados.
4. Leia `CONFIGURAR_VENDAS_1_5.md` para configurar Firebase, servidor, produto da Play Store e assinatura. O workflow de release manual recusa uma configuração comercial incompleta.

Se um comando falhar, pare nesse ponto e copie o erro. Não use force push, não limpe os dados do celular e não compartilhe segredos de assinatura.

O projeto Web/servidor é separado. Não envie o ZIP do site por cima da raiz do Android.
