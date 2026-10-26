# Rumo — Histórico de mudanças

## v1.2.0 (beta) — 26/10/2026

- Temas pré-definidos: Oceano, Violeta, Floresta e Areia clara, salvos localmente.
- Lembretes Android 0-3/dia opt-in e horários configuráveis via WorkManager, sujeito a atrasos; permissão Android 13+.
- Notificações discretas, ocultas na tela de bloqueio e sem detalhes íntimos.
- Mensagens curtas, baseadas no status do registro, após salvar e reabrir.
- Calendário com sinais gráficos além das cores.
- `versionCode` atualizado para 3. Compatibilidade com `rumo_diario_v1` mantida.
- Não há criptografia adicional para o diário em repouso. Testar Android real antes de distribuir.

# Mudanças

## 1.1.0 — 26/10/2026 (beta)

- Mantido armazenamento `rumo_diario_v1` (mesmas entradas anteriores continuam legíveis).
- Check-in ampliado com sofrimento opcional, interrupção de impulso e estratégias alternativas.
- Evolução ampliada com visualização de 14 dias e contagens úteis.
- Nova tela Ajuda rápida com observação do impulso e ferramentas práticas.
- Backup AES-GCM protegido por senha e importação compatível com backup v1 e v2.
- Avisos antes de exportações sem criptografia.
- Proteção contra screenshots no APK e política CSP para conteúdo local.
- Workflow opcional de APK release com assinatura fixa (requer secrets).

### Pendente (antes de venda)

- Criptografia e desbloqueio de dados locais em repouso; avaliação de biometria.
- Auditoria independente de segurança e testes extensivos em Android.
- Revisão clínica e jurídica; distribuição piloto; validação de proposta de valor.
