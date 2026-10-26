# Correção de GitHub Actions: Failed to find package 'tools'

O action android-actions/setup-android@v3 pedia `tools` por padrão. Este pacote foi descontinuado.

Este projeto usa `android-actions/setup-android@v4` com `packages: 'platform-tools'`.

Se você já enviou o projeto ao GitHub, NÃO precisa substituí-lo todo. Baixe o arquivo `Rumo_Corrigir_GitHub_Actions.zip` e aplique SOMENTE `.github/workflows/gerar-apk.yml`, ou edite o mesmo arquivo manualmente.

Depois execute:

```sh
git add .github/workflows/gerar-apk.yml
git commit -m "Corrigir instalacao do Android SDK"
git push
```

A compilação pode ser repetida em GitHub > Actions. Não foi compilado localmente.
