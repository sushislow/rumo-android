# Rumo — Diário de progresso · 1.4.0-beta

Beta Android com calendário, check-ins, gatilhos personalizados, conquistas acumuladas, plano pessoal, widget discreto, cofre local, PIN, biometria opcional e backup protegido.

Esta atualização traz oito temas, painel inicial atualizado e campos de senha próprios do Rumo. A conta opcional com Google/e-mail foi implementada e começa **desativada**, aguardando a configuração do projeto Firebase e da página pública. Ela não sincroniza o diário e não substitui o PIN ou a senha do backup.

- Atualização pelo Codespaces: `GUIA_ATUALIZACAO_1_4.md`.
- Conta e recuperação de senha: `CONFIGURAR_CONTA_1_4.md`.
- Resultado dos testes e limites: `VALIDACAO_v1_4.md`.
- Assinatura para distribuição: `GUIA_ASSINATURA_E_PUBLICACAO.md`.

```bash
node tests/rumo-smoke.js
node tests/rumo-features.js
```

O teste DOM de senhas e temas está em `tests/security-ui.cjs`; o GitHub Actions instala sua dependência temporária e o executa. A interface de conta, o PIN, a biometria e o widget ainda exigem testes com os serviços configurados e em aparelho real. A página e o servidor de compra são um projeto separado.

O projeto preserva Java no Android e HTML/CSS/JavaScript interno para o diário. A privacidade depende da arquitetura, controle de acesso e criptografia, não de uma linguagem que seria automaticamente privada. Não há promessa de cura, certificação de segurança ou prevenção garantida de redistribuição do APK.
