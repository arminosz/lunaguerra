# LunaGuerra
[![Download](https://img.shields.io/badge/Download-FF6B6B?style=for-the-badge&logo=download&logoColor=white)](https://www.spigotmc.org/resources/lunaguerra.128286)
![Version](https://img.shields.io/badge/version-1.5.0-purple.svg) 

Um plugin para Spigot que fornece funcionalidades de guerras de clãs integrado com o **SimpleClans**. Permite que clãs batalhem em arenas personalizadas com kits configuráveis.
> PS: *If you're an English speaker, please refer to the [`config_en.yml`](./config_en.yml) file for guidance. **It has no functional use** and you should use the standard config.yml, this one is provided only for reference. You can also click the download button above to see a tutorial and translated commands*

## Dependências

* SimpleClans (2.19.2 ou superior)
* PlaceholderAPI (2.11.6 ou superior) (Obrigatória para uso de placeholders)
* Spigot 1.21+ (Versão nativa)

## Comandos

### `/guerra`

Comando principal para interagir com o plugin LunaGuerra. Todos os subcomandos são acessados através de `/guerra`.

### Subcomandos

#### 1. **`/guerra entrar [arena]`**
* **Descrição**: Entrar em uma guerra em andamento. O parâmetro opcional `arena` define qual arena especificamente entrará.
* **Permissão**: `lunaguerra.join`

#### 2. **`/guerra sair`**
* **Descrição**: Sair da guerra atual e teleportar para a posição de saída corretamente.
* **Permissão**: `lunaguerra.join`

#### 3. **`/guerra camarote <arena>`**
* **Descrição**: Assistir a uma guerra na arena especificada.
* **Permissão**: `lunaguerra.spectate`

#### 4. **`/guerra vencedores [quantidade]`**
* **Descrição**: Mostra os últimos vencedores das guerras. O parâmetro opcional `quantidade` define quantos vencedores exibir.
* **Permissão**: Disponível para todos os jogadores.

#### 5. **`/guerra info <arena>`**
* **Descrição**: Mostra informações sobre a arena especificada.
* **Permissão**: `lunaguerra.info`

#### 6. **`/guerra start <arena>`**
* **Descrição**: Inicia uma guerra na arena especificada.
* **Permissão**: `lunaguerra.admin`

#### 7. **`/guerra stop <arena>`**
* **Descrição**: Para a guerra em andamento na arena especificada.
* **Permissão**: `lunaguerra.admin`

#### 8. **`/guerra forcestart <arena>`**
* **Descrição**: Força o início de uma guerra na arena, ignorando a contagem.
* **Permissão**: `lunaguerra.admin`

#### 9. **`/guerra create <arena>`**
* **Descrição**: Cria uma nova arena para guerras.
* **Permissão**: `lunaguerra.admin`

#### 10. **`/guerra delete <arena>`**
* **Descrição**: Remove a arena especificada.
* **Permissão**: `lunaguerra.admin`

#### 11. **`/guerra reload`**
* **Descrição**: Recarrega as configurações do plugin.
* **Permissão**: `lunaguerra.admin`

#### 12. **`/guerra displayname <arena> <name>`**
* **Descrição**: Define um nome de exibição para a arena, nomes com espaços devem estar entre aspas (").
* **Permissão**: `lunaguerra.admin`

#### 13. **`/guerra limit <arena> <number>`**
* **Descrição**: Define um limite de jogadores por clã para a arena.
* **Permissão**: `lunaguerra.admin`

#### 14. **`/guerra kit <arena>`**
* **Descrição**: Salva seu inventário e equipamento como kit padrão da arena.
* **OBS**: O inventário do player será limpado antes de receber o kit durante a guerra.
* **Permissão**: `lunaguerra.admin`

#### 15. **`/guerra top`**
* **Descrição**: Exibe o top 10 jogadores (por kills) e top 10 clãs (por vitórias).
* **Permissão**: Disponível para todos os jogadores.

#### 16. **Sistema de Banimentos**
* **Comandos**:
  * `/guerra banclan <nome> [motivo]` – Banir um clã
  * `/guerra banplayer <nome> [motivo]` – Banir um jogador
  * `/guerra unbanclan <nome>` – Desbanir um clã
  * `/guerra unbanplayer <nome>` – Desbanir um jogador
  * `/guerra baninfo <nome>` – Ver informações de banimentos
* Clãs e jogadores banidos são removidos imediatamente de guerras ativas e não podem participar de novas guerras.
* Clãs ou jogadores banidos são automaticamente removidos do /clan top

## Configuração do LunaGuerra

1. Após reiniciar, o plugin vai gerar arquivos de configuração na pasta `plugins/LunaGuerra`.
2. Edite os arquivos `config.yml` e `messages.yml` conforme necessário.
3. Use o comando `/guerra reload` para recarregar as configurações.

### Criando uma Arena

1. Use `/guerra create <arena>` para criar uma nova arena.
2. Configure suas propriedades:
   * `/guerra displayname <arena> <name>` – Define um nome de exibição.
   * `/guerra limit <arena> <number>` – Define o limite de jogadores.
   * `/guerra set <arena> <camarote|espera|saida|inicio>`
     - Camarote: Local de espectador
     - Espera: Local de espera antes do início
     - Saida: Local para teleporte após o fim ou morte
     - Inicio: Local de início com kit

### Gerenciamento de Kits

1. Use `/guerra kit <arena>` para salvar seu inventário como kit padrão da arena.
* **OBS**: O inventário do player será limpo antes de receber o kit.

### Iniciando uma Guerra

1. Use `/guerra start <arena>` para iniciar.
2. Jogadores podem entrar com `/guerra entrar <arena>` ou apenas `/guerra entrar`.
3. Para parar uma guerra, use `/guerra stop <arena>`; todos serão teleportados para a saída.
4. Para iniciar imediatamente, ignorando contagem, use `/guerra forcestart <arena>`.

### Placeholders:
  * `%lunaguerra_top_name_<rank>%` – Nome do clã na posição <rank>
  * `%lunaguerra_top_value_<rank>%` – Vitórias do clã na posição <rank>
  * `%lunaguerra_player_top_name_<rank>%` – Nome do jogador na posição <rank>
  * `%lunaguerra_player_top_value_<rank>%` – Kills do jogador na posição <rank>

### Discord Webhook

O LunaGuerra  suporta notificações via Discord Webhook para eventos de guerra (início e fim).

#### 1. Ativando o Webhook
1. Abra o arquivo `config.yml` na pasta `plugins/LunaGuerra`.
2. Localize a chave `webhook_url` e adicione a URL do seu webhook do Discord:
```yaml
webhook_url: "SUA_URL_DO_WEBHOOK_AQUI"
````

#### 2. Configuração de Embeds

1. Na primeira inicialização do plugin, será criada a pasta `plugins/LunaGuerra/discord`.
2. Dentro dessa pasta, haverá arquivos para os **embeds de mensagens**:

   * `war_started.json` – Embed para início de guerra
   * `war_winner.json` – Embed para fim de guerra
3. Você pode personalizar esses arquivos à vontade (Recomendo usar o site [Embed Builder](https://glitchii.github.io/embedbuilder)). Após qualquer modificação, use:

```
/guerra reload
```

para que o plugin recarregue as configurações e atualize os embeds.

#### 3. Placeholders Disponíveis

##### Início de Guerra (`war_started.json`)

* `{event}` – Nome da arena onde começou a guerra
* `{timestamp}` – Hora que a guerra começou

##### Fim de Guerra (`war_winner.json`)

* `{timestamp}` – Hora que a guerra terminou
* `{top}` – Ranking gerado como o `/guerra top`, atualizado após a vitória
* `{winner_value}` – Total de kills do clã vencedor
* `{winner_name}` – Nome do clã vencedor
* `{event}` – Nome da arena da guerra
