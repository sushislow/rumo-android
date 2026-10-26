# Rumo — aplicativo Android (mesmo esquema do QuissaGesso)

Este ZIP é um **projeto Android completo**. O GitHub Actions vai gerar `app-debug.apk` para instalar no celular.

## Primeira instalação (GitHub, sem GitHub Pages)

1. Extraia o ZIP em uma pasta. O conteúdo é o próprio projeto, inclusive a pasta oculta `.github`.
2. Crie um **repositório NOVO** no GitHub (por exemplo, `rumo-android`). **Não use o repositório do QuissaGesso.** O repositório pode ser privado. Nesse caso, habilite o GitHub Actions se for solicitado.
3. Faça upload de **todos os arquivos e pastas** do projeto para a raiz do novo repositório, inclusive `.github` (importante!). Ou, no GitHub Codespaces, extraia o ZIP na raiz desse novo repositório.
4. Confirme que o arquivo `.github/workflows/gerar-apk.yml` aparece no repositório.
5. Abra **Actions → Gerar APK do Rumo → Run workflow** (ou aguarde a execução automática após enviar os arquivos).
6. Abra a execução concluída e baixe o **Artifact** chamado `Rumo-APK`.
7. Extraia o arquivo baixado. Instale `app-debug.apk` no Android, autorizando a instalação pelo aplicativo usado para abrir o APK se necessário.

## Onde estão os arquivos

- Tela, calendário e check-ins: `app/src/main/assets/index.html`
- Código Android, importação e exportação: `app/src/main/java/br/com/rumo/diario/MainActivity.java`
- Gerar APK no GitHub Actions: `.github/workflows/gerar-apk.yml`

## Atenção máxima: dados e atualizações

- Este aplicativo armazena os registros **localmente no WebView do Android**, separados do navegador Chrome. Dados da versão web NÃO migram automaticamente para o aplicativo.
- **Antes de qualquer atualização, exporte o backup JSON** em Configurações → Exportar cópia JSON. Guarde em uma pasta privada.
- **O APK de testes é assinado pelo GitHub Actions com chave de depuração que pode variar entre execuções.** Se uma versão nova não instalar sobre a antiga, NÃO desinstale antes de exportar backup: desinstalar apaga os dados locais.
- Para atualizar sem esse risco, será preciso configurar uma chave privada de assinatura fixa e gerar um APK de release assinado nas próximas versões. O projeto atual NÃO inclui essa configuração.
- Instalar o Rumo pelo APK não exige GitHub Pages. O app funciona offline e não pede permissão de acesso à internet.
- Nunca envie seus arquivos de backup ao repositório, sobretudo se ele for público.
- O app permite a você escolher explicitamente onde salvar a cópia de segurança JSON e CSV. A importação usa o seletor de documentos do Android.

## Se o GitHub Actions falhar

Vá na aba Actions, abra a execução com falha e copie as linhas finais do erro para o Atlas. A execução do GitHub, e não este ambiente, compila o APK Android.
