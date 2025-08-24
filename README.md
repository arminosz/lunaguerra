# LunaGuerra

Um plugin para Spigot que fornece funcionalidades de guerras de clãs integrado com o **SimpleClans**. Permite que clãs batalhem em arenas personalizadas com kits configuráveis.

## Dependências

* SimpleClans (2.19.2 ou superior)
* Spigot 1.21+ (Versão nativa)

## Comandos

### `/guerra`

Comando principal para interagir com o plugin LunaGuerra. Todos os subcomandos são acessados através de `/guerra`.

### Subcomandos

#### 1. **`/guerra entrar [arena]`**

* **Descrição**: Faz que entre em uma guerra em andamento. O parâmetro opcional `arena` define qual arena especificamente entrará.
* **Permissão**: `lunaguerra.join`

#### 2. **`/guerra sair`**

* **Descrição**: Faz que saia da guerra atual.
* **Permissão**: `lunaguerra.join`

#### 3. **`/guerra camarote <arena>`**

* **Descrição**: Permite que um jogador assista a uma guerra na arena especificada.
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

* **Descrição**: Força o início de uma guerra na arena, ignorando a espera.
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

* **Descrição**: Define um nome de exibição para a arena, nomes com espaços devem estar entre aspas (")
* **Permissão**: `lunaguerra.admin`

#### 13. **`/guerra limit <arena> <number>`**

* **Descrição**: Define um limite de jogadores por clan para a arena.
* **Permissão**: `lunaguerra.admin`

#### 14. **`/guerra kit <arena>`**

* **Descrição**: Salva seu inventário e equipamento como kit padrão de uma arena específica
* * **OBS**: O inventário do player será limpado antes de receber o kit durante a guerra. Oriente os players a limpar o inventário antes de ir ao evento
* **Permissão**: `lunaguerra.admin`


## Tutorial: Configuração do LunaGuerra

1. Após reiniciar, o plugin vai gerar arquivos de configuração na pasta `plugins/LunaGuerra`.
2. Edite os arquivos `config.yml` e `messages.yml` conforme necessário.
3. Use o comando `/guerra reload` para recarregar as configurações

### Criando uma Arena

1. Use o comando `/guerra create <arena>` para criar uma nova arena.
2. Configure suas propriedades:

   * Use `/guerra displayname <arena> <name>` para definir um nome de exibição.
   * Use `/guerra limit <arena> <number>` para definir o limite de jogadores.
   * Use `/guerra set <arena> <camarote|espera|saida|inicio>`
     - Onde:
     - Camarote: Aonde players que não entraram na guerra podem espectar a guerra
     - Espera: Local onde players vão ficar esperando até a contagem acabar
     - Saida: Local onde players mortos/vencedores vão ser teleportados após o fim
     - Inicio: Local onde players vão ser teleportados, com o kit, depois do fim da contagem 

### Gerenciamento de Kits

1. Use `/guerra kit <arena>` para salvar seu atual inventário como o kit padrão da arena
   * **OBS**: O inventário do player será limpado antes de receber o kit

### Iniciando uma Guerra

1. Inicie a guerra com `/guerra start <arena>`.
2. Jogadores podem entrar em uma arena com `/guerra entrar <arena>` ou simplesmente `/guerra entrar`.
3. Caso tenha de parar uma guerra, use `/guerra stop <arena>` para forçá-la a parar, todos serão teletransportados para a saída
4. Caso tenha de iniciar a guerra imediatemente, pulando a contagem, use `/guerra forcestart <arena>` para começar logo
