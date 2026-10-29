# 1.5.0-beta — base comercial

- Conta e licença obrigatórias nos builds comerciais antes de carregar o diário.
- Compra única Google Play nativa, preço da loja e restauração de comprovante para validação no servidor.
- Licença RSA ligada a conta/pacote/ambiente, com até sete dias offline e renovação online.
- Servidor separado reconhece compras Web e Play; revalidação, reembolsos e notificações autenticadas.
- Android API 36, tratamento de insets das barras, AGP 8.10.1/Gradle 8.11.1 e Billing 9.1.0.
- Actions para teste pessoal, teste comercial e release AAB/APK com verificação de configuração.
- Serviços reais, chaves do proprietário e publicação na loja continuam pendentes.

# Rumo 1.4.0-beta — 28/10/2026

- Painel inicial atualizado e oito temas; novas opções Aurora, Grafite, Céu e Terracota.
- Telas nativas de PIN e conta usam a paleta escolhida, com campos arredondados e contraste próprio.
- Backup protegido usa um formulário de senha do Rumo, com confirmação, mostrar/ocultar e cancelamento.
- A abertura em segundo plano fecha os formulários sensíveis antes do bloqueio local.
- Conta opcional com Firebase Authentication: Google pelo Credential Manager, e-mail/senha, verificação e recuperação por link de uso único.
- A conta começa desativada e exige configuração explícita em `rumo_account.xml`. A conta não envia o diário e não substitui o PIN ou o backup.
- Permissão de rede adicionada para a conta nativa. A WebView continua restrita aos arquivos locais e tem CSP sem conexões de rede.
- Interface para consultar o acesso à compra; download autenticado é preparado no projeto web separado.
- Testes de interface por DOM verificam oito temas, senhas mascaradas, erros, cancelamento, exportação criptografada e limpeza ao bloquear.

O pacote é beta. Configuração de identidade/pagamento, assinatura definitiva, teste em aparelho e validação comercial continuam necessários.

---

# Histórico do Rumo

## v1.3.0 (beta) — 26/10/2026

- Cofre local AES-GCM/Android Keystore, PIN nativo com espera progressiva e biometria forte opcional.
- Migração do diário antigo confirmada antes da remoção das antigas chaves web.
- Conquistas cumulativas permanentes e contadores opcionais; dias desconhecidos não viram sucessos.
- Widget neutro com contagem opt-in e abertura do check-in respeitando o PIN.
- Contato por e-mail externo com confirmação; destinatário configurável e atualmente pendente.
- Gatilhos personalizados, modo rápido, alternativa tentada, plano pessoal e resumo semanal.
- Backup v3 inclui preferências/conquistas; v1/v2 continuam importáveis. PIN/chave local ficam fora dos backups.
- Atualização de versionCode para 5 e verificação expandida de UI, contagens e criptografia.

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
