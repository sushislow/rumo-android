# Atualizar o Rumo 1.4.0-beta no Codespaces

Este ZIP contém o código Android completo, preservando a estrutura da versão 1.3.0. A conta começa desativada, e não há cobrança. Faça um backup protegido no app e guarde-o fora do aplicativo **antes** de atualizar.

## Atualizar o repositório Android

Envie `Rumo_Atualizacao_v1_4_0_beta.zip` para a raiz do Codespaces, onde ficam `app` e `settings.gradle`. Não use o repositório da demo web.

Execute:

```bash
unzip -o Rumo_Atualizacao_v1_4_0_beta.zip
git status
```

Confira as alterações. Depois:

```bash
git add app tests .github .gitignore README.md CHANGELOG.md GUIA_ATUALIZACAO_1_4.md CONFIGURAR_CONTA_1_4.md VALIDACAO_v1_4.md PRIVACIDADE_RASCUNHO.md
git commit -m "Rumo 1.4.0 beta: conta opcional, oito temas e senhas personalizadas"
git push
```

Em GitHub → Actions, acompanhe **Gerar APK do Rumo**. A execução testa a lógica, a criptografia e os campos de senha antes de compilar. Baixe o artefato `Rumo-APK`.

Teste primeiro com registros fictícios. Uma atualização por cima da versão existente só é possível com a mesma chave de assinatura. Se o Android pedir desinstalação, não apague sua instalação até confirmar que o backup externo e sua senha estão disponíveis.

## O que muda sem configurar serviços

- O painel inicial ganha um visual atualizado.
- Dados → Seu tema passa a oferecer oito temas: Oceano, Violeta, Floresta, Areia clara, Aurora, Grafite, Céu e Terracota.
- O PIN e os diálogos nativos usam as cores escolhidas.
- As senhas de backup aparecem em um formulário próprio do Rumo, mascaradas por padrão, com mostrar/ocultar, confirmação, validação e cancelar.
- Dados → Minha conta e acesso informa que o serviço ainda está em preparação. O diário continua disponível.

## Para ativar a conta

Leia `CONFIGURAR_CONTA_1_4.md`. É preciso configurar seu projeto Firebase e a página HTTPS pública. A recuperação da senha da conta funciona por link de uso único no e-mail; não é um código numérico de seis dígitos implementado pelo Rumo.

A conta do usuário não altera o PIN e não recupera check-ins de outro aparelho. O backup protegido continua necessário. O app agora declara permissão de internet para a conta nativa. A parte que apresenta o diário permanece restrita aos arquivos internos; tokens e senhas de conta não passam pela ponte JavaScript do diário.

## Antes de vender

A página comercial/servidor é um projeto separado. O guia de lançamento acompanha esse projeto. Credenciais, suporte, preço, documentos comerciais definitivos e validação em aparelho continuam pendentes. Este ZIP é uma beta, e não uma confirmação de prontidão comercial.

Use APK release com chave estável para distribuir, mantendo a mesma chave nas atualizações. Não publique chaves privadas, senhas, backups ou registros de usuários no GitHub.
