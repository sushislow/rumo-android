# Complemento de privacidade — 1.4.0-beta

A versão 1.4 adiciona uma conta opcional que começa desativada. Quando habilitada pelo responsável e aberta pelo usuário, Firebase Authentication processa e-mail, senha ou identidade Google para autenticação. A conta e a compra são separadas do diário. O APK agora tem permissão de internet para essas ações nativas, mas a WebView do diário só recebe recursos internos e não pode acessar servidores externos.

O código não acrescenta Analytics nem ferramentas de rastreamento. Os registros locais continuam no cofre, e nenhuma senha/token de conta é disponibilizada à ponte JavaScript do diário. O SDK de autenticação mantém os dados de sessão na área privada do aplicativo. A recuperação por link do e-mail não redefine o PIN nem a senha de backup. Sair/excluir conta de acesso não apaga automaticamente o diário local.

O servidor comercial proposto guarda identidade da conta e transações, sem check-ins. Antes de habilitar vendas, este rascunho precisa ser substituído por informações completas do responsável, contato, fornecedores, bases legais, retenção, direitos e procedimentos de atendimento. Não é uma declaração de conformidade jurídica ou auditoria de segurança.

---

## Histórico do rascunho 1.3 (as mudanças da versão atual estão descritas acima)

# Rumo — aviso de privacidade para a versão beta 1.3

Rascunho para testes pessoais com adultos. Deve ser revisto antes de disponibilização comercial ampla.

## Registros no APK

Diário, preferências (incluindo plano e gatilhos personalizados) e conquistas são guardados no diretório interno sem backup automático do app, em arquivo criptografado com AES-GCM. A chave pertence ao Android Keystore e não é exportada ao JavaScript. A criptografia é usada mesmo com o PIN desativado. As chaves legadas do armazenamento web são removidas logicamente após a migração ser confirmada; não há promessa de apagamento forense de cópias antigas.

PIN e biometria são um bloqueio nativo da interface. O PIN é verificado com derivação PBKDF2, sal aleatório e comparação em tempo constante; o verificador e a espera após tentativas também ficam no cofre. O PIN não é armazenado em texto claro nem enviado a um servidor. Biometria usa a confirmação do Android; o Rumo não recebe a impressão digital ou o rosto. O bloqueio volta quando o app sai de primeiro plano. As capturas do app são bloqueadas pelo Android, dentro das limitações da plataforma.

A proteção depende da integridade do aparelho e do Android. Um sistema comprometido, acesso de depuração autorizado ou processos comprometidos têm riscos que esse bloqueio não elimina. A chave local é perdida ao limpar os dados ou desinstalar.

## Widget

A contagem está oculta por padrão. Ativar o widget com contagem permite guardar um pequeno resumo separado: métrica, quantidade, data de atualização e indicação de pendência. Esse resumo não contém notas, gatilhos, PIN ou diário completo. O número pode ser visto por quem olhar a tela inicial, independentemente do PIN do Rumo. Ao desativar, o resumo numérico é removido e o widget se torna um atalho neutro.

## Compartilhamento e contato

O app não tem permissão INTERNET, conta, publicidade, telemetria ou servidor próprio. Textos copiados e arquivos exportados só são compartilhados por ação explícita da pessoa. O backup protegido por senha é criptografado; JSON simples, CSV e textos copiados são legíveis. O PIN, dados biométricos e a chave local não são exportados.

O contato de sugestão/bug abre o aplicativo de e-mail da pessoa com mensagem, versão do Rumo e versão do Android. O diário não é anexado automaticamente. A pessoa revisa e confirma o envio no cliente de e-mail, que tem suas próprias regras de privacidade e mostra o remetente ao destinatário. Evite escrever detalhes íntimos no relato de um bug. O destinatário real ainda precisa ser configurado em rumo_support.xml; enquanto estiver vazio, o app só permite copiar a mensagem.

## Backup, restauração e exclusão

Não existe recuperação remota de PIN ou senha de backup. Guarde uma cópia protegida fora do app antes de atualizar, reinstalar ou trocar de celular. Backups antigos v1/v2 podem ser importados; o novo conteúdo v3 inclui preferências, plano, gatilhos e conquistas. Ao restaurar preferências, lembretes e contagem do widget permanecem desligados até reativação. A restauração não altera o PIN nativo.

Apagar os check-ins e conquistas pelo menu exige duas confirmações e mantém PIN, plano e preferências, como informado no diálogo. Limpar dados no Android ou desinstalar apaga também o cofre e as chaves locais. Cópias exportadas e e-mails já enviados permanecem fora do controle do Rumo e precisam ser administrados pela pessoa.

## Finalidade

Autoconhecimento e apoio a escolhas pessoais. Nenhum diagnóstico, promessa de cura ou substituição de cuidado profissional. Conquistas e contadores são opcionais. Dias sem registro permanecem desconhecidos; pensamentos e desejo sexual não são tratados como recaída.
