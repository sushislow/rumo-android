# Assinatura fixa, testes e eventual publicação

## 1. Por que isso importa?

O APK `app-debug.apk` gerado no GitHub Actions pode ser assinado por uma **chave de depuração diferente a cada execução**. Em geral, APKs com assinaturas diferentes não atualizam uns aos outros. **Faça backup antes de desinstalar o Rumo**. Para distribuição estável, use uma chave de release permanente.

## 2. Gerar keystore pessoal

Em um ambiente confiável, com Java `keytool` instalado, execute:

```bash
keytool -genkeypair -v -keystore rumo-upload.jks -alias rumo -keyalg RSA -keysize 3072 -validity 10000
```

Guarde `rumo-upload.jks`, a senha do arquivo e a senha da chave **fora do GitHub e com cópia segura**. Se perder a chave de assinatura, poderá perder a possibilidade de atualizar usuários fora do Play App Signing.

## 3. Configurar GitHub Actions secrets

No repositório `rumo-android`: **Settings → Secrets and variables → Actions → New repository secret**. Crie:

- `RUMO_KEYSTORE_BASE64`: base64 do arquivo `.jks` (por exemplo, `base64 -w 0 rumo-upload.jks` em Linux).
- `RUMO_KEYSTORE_PASSWORD`: senha do arquivo `.jks`.
- `RUMO_KEY_ALIAS`: `rumo` (ou alias usado).
- `RUMO_KEY_PASSWORD`: senha da chave.

Jamais cole esses valores em mensagens, issues, chats, arquivos commitados ou prints públicos. Não compartilhe chave nem senhas conosco.

Execute **Actions → Gerar APK de release assinado → Run workflow**. O artifact `Rumo-Release-Assinado` conterá `app-release.apk`. **Ainda não testado em compilação real neste ambiente**; confirme no seu Actions e no telefone.

### Transição importante

O primeiro APK de release provavelmente **não terá a mesma assinatura do APK debug já instalado**. Faça backup da versão antiga, desinstale a versão debug, instale release e restaure. Depois, preserve o mesmo pacote `br.com.rumo.diario` e a mesma keystore para atualizações. Nunca aumente versionCode para trás.

## 4. Antes de publicar/vender

1. Armazenamento local com criptografia em repouso e proteção de acesso apropriada, testes de restauração/migração, auditoria de privacidade.
2. Revisar comunicação de saúde com psicólogo e política de privacidade com profissional jurídico.
3. Requisitos Google Play: ficha do aplicativo, declaração de apps de saúde (se aplicável), Política de Privacidade publicada em URL estável e visível no app, formulário Data Safety verdadeiro; revisar política atualizada.
4. Contas pessoais do Google Play criadas após 13/11/2023: teste fechado com **12 pessoas inscritas por 14 dias consecutivos**, seguido da solicitação de acesso a produção (regras vigentes na consulta de 26/10/2026; revalidar antes de enviar).
5. Preço sugerido para testar disposição a pagar: R$ 9,90 pagamento único **apenas uma hipótese**; consultar taxas, impostos e regras de pagamento atualizadas. Priorizar recursos essenciais gratuitos e alternativas acessíveis.
6. Vender só depois de comprovar usabilidade, estabilidade e conformidade — não vender como terapia ou cura.

Referências oficiais: https://support.google.com/googleplay/android-developer/answer/14151465?hl=pt-br e https://support.google.com/googleplay/android-developer/answer/18258653
