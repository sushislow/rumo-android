# Rumo — Diário de Progresso | Beta 1.3.0

Aplicativo Android pessoal e offline para registrar hábitos, gatilhos, escolhas e bem-estar. Não é tratamento nem instrumento diagnóstico.

## Atualização e uso

Leia **GUIA_ATUALIZACAO_1_3.md** para comandos do Codespaces, backup, configuração do e-mail, PIN e widget. O pacote contém a logo aprovada na versão 1.2.1.

## Nesta versão

- Cofre local AES-GCM com chave não exportável do Android Keystore.
- PIN nativo de 6 a 12 números, espera após tentativas incorretas e bloqueio ao sair do app.
- Biometria forte opcional, quando disponível no aparelho.
- Migração confirmada do armazenamento anterior e restauração de backups v1/v2.
- Gatilhos personalizados, presentes no histórico, gráficos, resumo e backup.
- Check-in rápido opcional e campo para registrar a alternativa tentada.
- Plano pessoal de situação → ação na Ajuda rápida.
- Conquistas cumulativas permanentes, sem ranking, e opção de ocultá-las.
- Resumo semanal de registros, habilidades, gatilhos e alternativas úteis relatadas.
- Widget Android opcional, discreto e sem diário/notas; contagem oculta por padrão.
- Feedback por aplicativo de e-mail, com revisão antes do envio e alternativa de copiar.
- Quatro temas, lembretes opcionais e backup protegido por senha continuam disponíveis.

## Endereço de suporte pendente

`app/src/main/res/values/rumo_support.xml` contém `support_email` vazio até o responsável informar o destinatário real. Enquanto isso, o app oferece copiar a mensagem. Nenhum endereço inventado ou senha é incluído.

## Compilação

Java 17, Gradle 8.9, compileSdk 35, Build Tools 35.0.0, minSdk 24, targetSdk 34. `versionCode` 5; `versionName` 1.3.0.

O workflow **Gerar APK do Rumo** executa os testes básicos e produz **Rumo-APK**. O workflow de release usa os segredos da chave de assinatura existente. Não coloque keystore, senhas ou backups pessoais no GitHub.

## Verificação

```bash
node tests/rumo-smoke.js
node tests/rumo-features.js
mkdir -p build/crypto-tests
javac -d build/crypto-tests app/src/main/java/br/com/rumo/diario/VaultCrypto.java tests/VaultCryptoTest.java
java -cp build/crypto-tests br.com.rumo.diario.VaultCryptoTest
gradle :app:assembleDebug :app:lintDebug
```

`tests/ui-smoke.js` é opcional, exige Playwright/Chromium e verifica a interface com uma ponte nativa simulada. Não simula a segurança do Android Keystore ou sensores biométricos. Para usá-lo, instale Playwright no ambiente de testes; `RUMO_CHROMIUM` pode indicar um executável já instalado.

Os testes instrumentados em `app/src/androidTest` verificam o cofre, migração, integridade e PIN no Android. Usam um diretório separado dos registros reais. Para executá-los em um emulador/dispositivo de testes:

```bash
gradle :app:connectedDebugAndroidTest
```

Biometria, retorno após segundo plano e posicionamento do widget precisam também de conferência no aparelho/launcher usado. Não confunda o sucesso de uma compilação com uma auditoria de segurança ou validação clínica.

## Privacidade

Sem permissão INTERNET, publicidade, contas, analytics ou servidor próprio. Abertura de e-mail e exportação de arquivos são ações explícitas da pessoa e podem usar outros aplicativos. O widget usa somente agregados autorizados, visíveis na tela inicial. JSON/CSV sem proteção e texto copiado são legíveis.

A criptografia local não recupera um PIN esquecido e não protege um Android comprometido. Backups externos protegidos continuam necessários antes de reinstalar ou trocar de aparelho. Consulte PRIVACIDADE_RASCUNHO.md e as referências em REFERENCIAS_1_3.md.

Os resultados e limites dos testes desta preparação estão em **VALIDACAO_v1_3.md**.
