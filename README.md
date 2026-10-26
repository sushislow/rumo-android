# Rumo — Diário de Progresso | Versão beta 1.2.0

Aplicativo Android offline e local para acompanhar hábitos ligados ao uso problemático de pornografia. Gratuito durante o período de testes; **não é ferramenta terapêutica validada**.

## Novidades da versão 1.2

- **Lembretes locais opcionais**, de zero a três vezes por dia. Ativados apenas por decisão do usuário e permissão do Android; horários aproximados pelo WorkManager, que continua mesmo após reinício.
- **Quatro temas**: Oceano, Violeta, Floresta e Areia clara. A escolha fica salva neste aparelho.
- **Mensagens após check-in**: reconhecimento de dias sem consumo; apoio e sugestões concretas para dias com consumo; mensagens neutras para registros parciais. Não são enviadas como notificações.
- **Privacidade das notificações**: títulos e textos neutros, conteúdo oculto na tela de bloqueio (`VISIBILITY_SECRET`). Não lê as observações do diário para gerar lembretes.
- **Calendário com sinais de status**, além das cores.

## Nesta versão

- Calendário: dias sem consumo / com consumo / não registrados (distinção crucial).
- Check-in: vontade, gatilhos, hábitos, banheiro, sofrimento opcional (0–10), interrupção de impulso e estratégias alternativas.
- Evolução: 7 e 30 dias; gráfico simples dos últimos 14 dias; frequência dos gatilhos, uso de estratégias e sofrimento médio apenas de dias registrados.
- Ajuda rápida: afastar gatilhos, observação guiada do impulso durante dois minutos, questionar justificativas automáticas e plano após deslizes.
- Backup JSON criptografado AES-256-GCM com senha de 10+ caracteres via PBKDF2-SHA256 (250 mil iterações). Importa backup v1/v2 e protegido v2.
- Exportações JSON/CSV legíveis apenas após aviso explícito.
- WebView local via WebViewAssetLoader, **sem permissão INTERNET**, capturas de tela bloqueadas no Android.

## Limitações explícitas

- O backup protegido é criptografado; **os dados locais no `localStorage` ainda não são criptografados em repouso**. Não vender/publicar como produto sensível seguro até revisar e implementar armazenamento criptografado e auditoria.
- Usuários que esquecem a senha do backup protegido não podem restaurá-lo.
- Não há sincronização, conta, envio ao ChatGPT, analytics ou anúncios.
- Sem diagnóstico, taxa de sucesso ou promessa de cura.
- As orientações de apoio não substituem profissional de saúde.
- O app usa WebView e requer testes reais em dispositivos Android.

## Atualizar projeto no mesmo repositório do Rumo

1. **Antes de qualquer atualização do APK:** abrir Rumo → Dados → **Backup protegido por senha** se já tiver v1.1, ou `Exportar cópia JSON` se tiver v1.0. Confirme que o arquivo foi salvo fora do app; não compartilhe com outras pessoas.
2. Extraia os arquivos deste ZIP na raiz do **mesmo repositório Rumo**, substituindo somente os arquivos do Rumo. Não envie o ZIP fechado como único arquivo. Preserve `.git` e `.github`.
3. Codespaces: `unzip -o Rumo_Beta_Android_v1_2.zip && git add . && git commit -m "Atualizar Rumo beta 1.2" && git push`.
4. Em Actions → **Gerar APK do Rumo**, baixe o artifact Rumo-APK, extraia `app-debug.apk`.
5. Se a versão debug tiver assinatura diferente, ela não instalará sobre a antiga. **Não desinstale sem backup.** Para atualizações duráveis, configure assinatura fixa em `GUIA_ASSINATURA_E_PUBLICACAO.md`.
6. Não commite arquivos de backup (mesmo criptografados) nem keystores no GitHub.

### Arquivos centrais

- `app/src/main/assets/index.html`: UI, dados, gráficos, backup, exercícios.
- `app/src/main/java/br/com/rumo/diario/MainActivity.java`: WebView offline, ponte de exportação, proteções Android.
- `.github/workflows/gerar-apk.yml`: APK debug experimental.
- `.github/workflows/gerar-apk-release.yml`: APK release com assinatura fixa, **exige configurar secrets**.

## Checklist do que testar

Veja `PLANO_TESTES_E_QUESTIONARIO.md`. Teste importação v1, atualização sem perda de dados, datas, gráficos, backup protegido e restore antes de convidar voluntários.

Veja `GUIA_ATUALIZACAO_1_2.md` para procedimentos e testes de notificações, permissões e temas.
