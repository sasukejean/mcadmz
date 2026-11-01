# Comes Alive x DragonMineZ Bridge (mcadmz) 0.1.0

Mod complemento para Minecraft 1.20.1 (Forge 47.x). Nao altera os jars originais.
Precisa do Comes Alive 7.7.1-beta.5; o DragonMineZ 2.1.3 e opcional (usado para ler stats/TP do jogador).

## Como compilar (uma vez)
1. Instale o JDK 17.
2. Copie o jar do Comes Alive para `libs/` com o nome exato `mca-7.7.1.jar`.
3. Baixe o Forge MDK 1.20.1 (47.3.0) em files.minecraftforge.net e copie dele para ca:
   `gradlew`, `gradlew.bat` e a pasta `gradle/` (o wrapper).
4. Rode `gradlew build` (ou `gradlew.bat build` no Windows).
5. O mod sai em `build/libs/mcadmz-1.20.1-0.1.0.jar`. Coloque em `mods/` junto com o Comes Alive (e o DragonMineZ).

## O que ja funciona nesta versao
- **Stats dos NPCs** (Forca, Golpe, Resistencia, Vitalidade, Poder de Ki, Energia), nivel, poder e TP.
- **TP**: NPC ganha TP matando mobs, +1 passivo a cada 30 s, e quando o jogador/NPC da familia mata mobs perto dele.
  O jogador pode transferir TP do DragonMineZ com `/mcadmz tp give <npc> <qtd>`.
- **Stats sobem com TP**: auto-distribuicao (configuravel) ou manual com `/mcadmz allocate <npc> <stat> <pontos>`.
  Vitalidade aumenta a vida, Forca/Golpe o dano, Resistencia reduz dano recebido, Poder de Ki o dano de ki.
- **Tecnicas**: `/mcadmz teach <npc> <tecnica>` (Disparo de Ki, Kamehameha, Kaioken), pagas com o TP do NPC.
- **Ajuda em batalha**: NPCs adultos da familia do jogador (conjuge, pai/mae/filho, esposas extras) atacam
  quem esta atacando o jogador (corpo a corpo ou com ki).
- **Varias esposas**: item *Alianca Extra* (craftavel) com clique direito em um NPC adulto solteiro.
  `/mcadmz wives` lista as esposas extras.
- **Filhos ilimitados**: o Comes Alive nao tem limite configuravel de filhos; este mod nao adiciona nenhum.
- **Heranca**: filho nasce nivel 1 (nada investido) com piso = 10% da SOMA dos stats dos dois pais em cada stat.
  O limite de cada stat do filho = limite base + piso herdado, entao cada geracao tem teto maior.
  Para pais jogadores, le os stats do DragonMineZ; sem ele usa o valor base.
- **Scouter**: item (craftavel) ou `/mcadmz scan <npc>` mostra nivel, poder, TP, stats, heranca e tecnicas.
- Config em `config/mcadmz-common.toml`.

## Ainda NAO feito (proximas fases)
- Tela para trocar a aparencia dos NPCs igual ao DragonMineZ (precisa de camada de render propria).
- Tela propria de inventario/stats (hoje: Scouter e comandos; inventario: use o menu normal do Comes Alive).
- Formas/transformacoes, ki como recurso, tecnicas criadas pelo jogador.

## Pontos para testar primeiro
1. `/mcadmz tp add @e[type=...,limit=1,sort=nearest] 500` (OP) em um NPC e veja os stats subirem com o Scouter.
2. Alianca Extra em um NPC solteiro adulto estando ja casado.
3. Ter um filho e usar o Scouter nele: "herdado" deve ser ~10% da soma dos pais.
