# Validação da versão beta 1.3.0

## Verificações concluídas

- Compilação limpa: `clean :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`, com Java 17, Gradle 8.9, SDK e Build Tools 35.0.0: **BUILD SUCCESSFUL**.
- Android Lint: sem erros; permanecem avisos de compatibilidade de versões, uso intencional de JavaScript no WebView, aparência de ícones e targetSdk 34. O tratamento de transferência/backup do Android 12+ foi explicitado.
- APK: applicationId br.com.rumo.diario; versionName 1.3.0; versionCode 5. Widget e permissão biométrica presentes; nenhuma permissão INTERNET no manifesto final mesclado.
- Os arquivos index.html, features.js e sw.js dentro do APK foram comparados byte a byte com o código final.
- `tests/rumo-smoke.js`: diário legado, temas, lembretes, mensagens, estatísticas, backup AES-GCM, restauração v3, preservação de datas existentes, rejeição de datas futuras, gatilhos/alternativa, preferências e proteção contra fórmulas no CSV.
- `tests/rumo-features.js`: rótulos normalizados/deduplicados, limites, datas, lacunas, contagem acumulada, conquistas preservadas após consumo, resumo semanal e minimização de dados no widget/e-mail.
- `tests/VaultCryptoTest.java` em JVM: AES-GCM, nonce novo, autenticação de formato/IV/cifra, rejeição de chave errada e adulterações, PINs com zeros iniciais, algoritmos de derivação e espera progressiva.
- `tests/ui-smoke.js` em Chromium: fluxo com ponte nativa simulada, migração confirmada e recusa preservando cópia anterior, rascunho, gatilhos personalizados, texto malicioso escapado, modo rápido, alternativa, plano, widget/e-mail sem diário, quatro temas e visualização móvel/desktop.
- XMLs dos recursos e configuração de e-mail vazio conferidos.

## Limites desta verificação

Não houve teste em um aparelho físico. O emulador sem aceleração de hardware não concluiu a inicialização dentro do período de teste. Os quatro testes instrumentados de Android Keystore/arquivo/PIN foram **compilados, mas não executados** neste ambiente. O teste em JVM não substitui a execução no Android.

É necessário conferir criação/desbloqueio por PIN, retorno após segundo plano, biometria, migração real da instalação anterior e comportamento do widget no aparelho e launcher usados. O envio de e-mail depende do destinatário real e do cliente instalado; o endereço ainda não foi informado e permanece vazio.

Este pacote é beta. Uma compilação bem-sucedida não é uma auditoria de segurança nem validação clínica. Teste primeiro com dados fictícios e faça backup externo protegido antes de atualizar sua instalação pessoal.
