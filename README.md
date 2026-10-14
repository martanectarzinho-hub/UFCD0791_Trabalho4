# UFCD 0791 – Programação em JAVA (avançada)
## Trabalho Prático 4: Aplicação com Threads – Sensores

### 1. Identificação do Formando e Versão do JDK
* **Formando:** Carlos Marques
* **Repositório GitHub:** [https://github.com/martanectarzinho-hub/UFCD0791_Trabalho4](https://github.com/martanectarzinho-hub/UFCD0791_Trabalho4)
* **Versão do JDK utilizada (desenvolvimento e testes):**
```text
javac 21.0.12.1
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS, mixed mode, sharing)
```

---

### 2. Instruções Exatas de Compilação
A partir da raiz da pasta `monitorizacao/`:
```bash
javac -Xlint:all -d bin src/*.java src/central/*.java src/registo/*.java src/sensores/*.java
```

---

### 3. Instruções Exatas de Execução
A partir da raiz da pasta `monitorizacao/`:
```bash
java -cp bin Principal
```

---

### 4. Descrição Sucinta da Arquitetura
A aplicação adota uma arquitetura concorrente desacoplada baseada no modelo Produtor-Consumidor/Recurso Partilhado:
* **`Principal`**: Ponto de entrada (`main`), delegando a execução no `SistemaCentral`.
* **`SistemaCentral`**: Módulo coordenador que configura e arranca as threads dos sensores, gere o menu interativo em `System.in`, coordena o encerramento gracioso (*graceful shutdown*) e emite o relatório final.
* **`Sensor`** (implementa `Runnable`): Tarefa periódica concorrente que gera dados simulados (gama e jitter ±20%), cria objetos `Leitura` e regista-os no `RegistoCentral`. Cada sensor corre na sua própria thread nomeada.
* **`Leitura`**: Objeto imutável (campos `private final`, sem *setters*) representando uma medição individual (sensor, valor, unidade e instante).
* **`RegistoCentral`**: **Único recurso partilhado** entre threads. Armazena o histórico completo de leituras, contador global e acumuladores de estatísticas por sensor.
* **`EstatisticaSensor`**: Objeto auxiliar imutável no package `registo` que representa um instantâneo estatístico (contagem, mínimo, máximo, média e unidade) de um sensor.
* **Monitor (Lock):** O recurso partilhado `RegistoCentral` é protegido pelo seu **monitor intrínseco** (*intrinsic lock*). Todos os métodos que acedem ou alteram o seu estado interno são declarados como `synchronized`. A inserção no histórico, atualização estatística e incremento do contador ocorrem de forma atómica dentro da mesma secção crítica.

---

### 5. Justificação Técnica Obrigatória do Mecanismo de Interrupção
O mecanismo de paragem cooperativo combina uma **flag `volatile boolean ativo`** com o envio do sinal **`Thread.interrupt()`**:
(a) A visibilidade da flag `ativo` entre a thread `main` e as threads dos sensores é garantida pela semântica do Java Memory Model (JMM), onde a escrita numa variável `volatile` estabelece uma relação *happens-before* com qualquer leitura subsequente da mesma variável.
(b) Quando o pedido de encerramento chega enquanto o sensor está bloqueado em `Thread.sleep()`, a JVM interrompe o estado de bloqueio e lança uma `InterruptedException`. O sensor captura a exceção de forma explícita, restaura a flag de interrupção com `Thread.currentThread().interrupt()` e sai imediatamente do ciclo de leitura de forma ordenada.
(c) A latência de encerramento medida no relatório final situou-se na ordem dos 33 ms (garantidamente inferior ao limite máximo de 2 segundos), sendo totalmente coerente com o desbloqueio imediato proporcionado por `interrupt()`.
(d) O método `Thread.stop()` não constitui uma alternativa válida por ser inerentemente inseguro e ter sido removido nas versões recentes do JDK: a sua invocação liberta abruptamente todos os monitores detidos pela thread, expondo objetos partilhados num estado inconsistente (*damaged objects*).

---

### 6. Evidência de Teste (Execução Real de 10 Segundos)
```text
Encerramento coordenado iniciado...
[Sensor-HUM-01] terminado de forma controlada (13 leituras).
[Sensor-PRES-01] terminado de forma controlada (10 leituras).
[Sensor-TEMP-01] terminado de forma controlada (20 leituras).
================================================================
RELATÓRIO FINAL DE MONITORIZAÇÃO
================================================================
Duração da monitorização : 9,9 s
Duração do encerramento : 33 ms
----------------------------------------------------------------
Sensor       Leituras     Mínimo     Máximo      Média Unidade 
HUM-01             13       40,3       59,1       49,9 %       
TEMP-01            20       18,4       26,8       22,5 °C      
PRES-01            10     1006,6     1020,0     1012,8 hPa     
----------------------------------------------------------------
Total de leituras (contador) : 43
Soma das leituras por sensor : 43
Registos no histórico        : 43
Verificação de consistência  : OK
Estado final das threads     :
 Sensor-TEMP-01  TERMINATED
 Sensor-HUM-01   TERMINATED
 Sensor-PRES-01  TERMINATED
Todas as threads terminadas  : SIM
===============================================================
```
