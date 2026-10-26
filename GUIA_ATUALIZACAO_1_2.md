# Rumo Beta 1.2 — Atualização do APK

## Antes de atualizar

1. Abra o Rumo já instalado, entre em **Dados → Backup protegido por senha**. Confira que guardou o arquivo fora do app. A senha do backup é indispensável.
2. Não desinstale o APK antigo apenas para instalar a atualização: APKs debug criados em execuções diferentes podem ter assinaturas distintas; só restaure os dados se precisar reinstalar.
3. Faça as alterações apenas no repositório **Rumo**; não no QuissaGesso.

## Como atualizar pelo Codespaces (o arquivo de atualização está na raiz do Codespaces)

```bash
unzip -o Rumo_Atualizacao_v1_2.zip
git add .
git commit -m "Rumo beta 1.2: temas, mensagens e lembretes"
git push
```

Se `git commit` disser que não há mudanças, confira se extraiu o ZIP dentro da raiz do repositório.

Abra **GitHub → Actions → Gerar APK do Rumo** e veja se a compilação passou. Em **Artifacts**, baixe `Rumo-APK`, extraia `app-debug.apk` e instale no Android. A compilação do APK desta versão **não foi validada em aparelho** antes da entrega do código.

## O que testar no celular

- Dados → Seu tema: alternar entre **Oceano**, **Violeta**, **Floresta**, **Areia clara**; fechar e reabrir. O tema deve ser preservado.
- Registro: salvar um dia sem consumo e outro com consumo, verificar mensagens diferentes e se a edição é preservada.
- Dados → Lembretes: selecionar **1**, **2**, **3**, voltar para **Desligado**. A autorização do Android deve ser exibida ao ativar quando necessário. Lembretes **não são garantidos no minuto**: modo econômico, restrições de bateria e o próprio Android podem atrasá-los.
- Deixar a tela bloqueada: o Rumo não deve exibir dados íntimos em notificações.
- Os dados de `rumo_diario_v1` devem continuar intactos após atualização compatível (mesmo identificador de pacote e mesma assinatura). Não é possível conservar o armazenamento local após uma desinstalação sem backup.
- Faça restauração de backup apenas com uma cópia protegida e guardada em lugar seguro.

## Observações sobre privacidade

- Os textos das notificações são genéricos. Não mencionam pornografia, recaída ou uso sexual.
- A permissão de notificações é solicitada apenas se o usuário ativá-las.
- Os horários são mantidos no armazenamento do Android; não são enviados para servidores. As preferências visuais ficam no armazenamento local do WebView.
- O app continua **sem permissão INTERNET**. Os registros locais ainda não têm criptografia própria em repouso; não é uma versão comercial pronta.
- Temas e horários **não fazem parte** do backup do diário e podem ser configurados novamente.

## Sem promessas de tratamento

O app apoia auto-observação e estratégias pessoais, mas não é um tratamento validado nem um substituto para psicoterapia.
