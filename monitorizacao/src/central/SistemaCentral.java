package central;

import registo.EstatisticaSensor;
import registo.Leitura;
import registo.RegistoCentral;
import sensores.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Módulo central de controlo do sistema de monitorização.
 * Responsável por configurar e arrancar as threads dos sensores,
 * disponibilizar o menu de consola interativo, coordenar o encerramento (graceful shutdown)
 * e emitir o relatório final de consistência.
 */
public class SistemaCentral {
    private final RegistoCentral registoCentral;
    private final List<Sensor> sensores;
    private final List<Thread> threads;
    private long instanteInicioMs;

    public SistemaCentral() {
        this.registoCentral = new RegistoCentral();
        this.sensores = new ArrayList<>();
        this.threads = new ArrayList<>();
    }

    /**
     * Configura a lista padrão de sensores exigida pelas especificações do trabalho.
     * Método centralizado para facilidade de manutenção e testes de carga.
     */
    public void configurarSensores() {
        sensores.clear();
        sensores.add(new Sensor("TEMP-01", "°C", 18.0, 27.0, 500, registoCentral));
        sensores.add(new Sensor("HUM-01", "%", 40.0, 60.0, 800, registoCentral));
        sensores.add(new Sensor("PRES-01", "hPa", 1000.0, 1025.0, 1000, registoCentral));
    }

    /**
     * Configura um número customizado de sensores para testes de carga.
     *
     * @param numSensores     quantidade de sensores a criar
     * @param intervaloBaseMs intervalo base em ms para todos os sensores
     */
    public void configurarSensoresPersonalizado(int numSensores, long intervaloBaseMs) {
        sensores.clear();
        for (int i = 1; i <= numSensores; i++) {
            String nome = String.format("SENS-%02d", i);
            sensores.add(new Sensor(nome, "unid", 10.0, 100.0, intervaloBaseMs, registoCentral));
        }
    }

    /**
     * Inicia a execução do sistema central: arranca as threads e entra no loop do menu.
     */
    public void executar() {
        if (sensores.isEmpty()) {
            configurarSensores();
        }

        instanteInicioMs = System.currentTimeMillis();
        threads.clear();

        // Criar e arrancar cada thread exclusivamente com start()
        for (Sensor sensor : sensores) {
            String nomeThread = "Sensor-" + sensor.getNome();
            Thread t = new Thread(sensor, nomeThread);
            threads.add(t);
            t.start();
        }

        executarMenu();
    }

    /**
     * Loop principal do menu interativo lido de System.in.
     */
    private void executarMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean aExecutar = true;

        while (aExecutar) {
            System.out.println("\n--- MENU DE MONITORIZAÇÃO ---");
            System.out.println("[1] Últimas leituras");
            System.out.println("[2] Estatísticas parciais");
            System.out.println("[0] Encerrar");
            System.out.print("Opção > ");

            if (!scanner.hasNextLine()) {
                // EOF detetado (ex.: input redirecionado de /dev/null ou script) -> Encerramento automático
                System.out.println("\nFim de entrada (EOF) detetado. A iniciar encerramento...");
                aExecutar = false;
                break;
            }

            String linha = scanner.nextLine().trim();
            if (linha.isEmpty()) {
                continue;
            }

            switch (linha) {
                case "1":
                    exibirUltimasLeituras();
                    break;
                case "2":
                    exibirEstatisticasParciais();
                    break;
                case "0":
                    aExecutar = false;
                    break;
                default:
                    System.out.println("Opção inválida! Escolha 1, 2 ou 0.");
                    break;
            }
        }

        executarGracefulShutdown();
    }

    private void exibirUltimasLeituras() {
        System.out.println("\n=== ÚLTIMAS LEITURAS POR SENSOR ===");
        Map<String, Leitura> ultimas = registoCentral.getUltimasLeituras();
        if (ultimas.isEmpty()) {
            System.out.println("Nenhuma leitura efetuada até ao momento.");
        } else {
            for (Leitura l : ultimas.values()) {
                System.out.printf(Locale.GERMAN, "%-10s : %.1f %s (%s)%n", l.getSensorNome(), l.getValor(), l.getUnidade(), l.getInstante());
            }
        }
    }

    private void exibirEstatisticasParciais() {
        System.out.println("\n=== ESTATÍSTICAS PARCIAIS POR SENSOR ===");
        List<EstatisticaSensor> stats = registoCentral.getEstatisticasParciais();
        if (stats.isEmpty()) {
            System.out.println("Nenhuma estatística disponível.");
        } else {
            System.out.printf("%-10s %-10s %-10s %-10s %-10s %-8s%n", "Sensor", "Leituras", "Mínimo", "Máximo", "Média", "Unidade");
            for (EstatisticaSensor s : stats) {
                System.out.printf(Locale.GERMAN, "%-10s %-10d %-10.1f %-10.1f %-10.1f %-8s%n",
                        s.getSensorNome(), s.getTotalLeituras(), s.getMinimo(), s.getMaximo(), s.getMedia(), s.getUnidade());
            }
        }
    }

    /**
     * Executa o encerramento coordenado (graceful shutdown):
     * 1. Sinaliza a paragem a todos os sensores e interrompe pausas de sleep();
     * 2. Aguarda com join() que todas as threads terminem;
     * 3. Verifica que nenhuma thread permanece viva;
     * 4. Emite o relatório final sobre um estado estável e consistente.
     */
    private void executarGracefulShutdown() {
        long inicioEncerramento = System.currentTimeMillis();
        System.out.println("Encerramento coordenado iniciado...");

        // 1. Sinalizar paragem a todos os sensores
        for (Sensor sensor : sensores) {
            sensor.parar();
        }
        for (Thread t : threads) {
            t.interrupt();
        }

        // 2. Aguardar a terminação de cada thread com join()
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // Restaurar estado de interrupção se a thread main for interrompida
                Thread.currentThread().interrupt();
            }
        }

        long fimEncerramento = System.currentTimeMillis();
        long duracaoEncerramentoMs = fimEncerramento - inicioEncerramento;
        double duracaoMonitorizacaoSegundos = (fimEncerramento - instanteInicioMs) / 1000.0;

        // 3. Confirmar que nenhuma thread está ativa (isAlive() == false)
        boolean todasTerminadas = true;
        for (Thread t : threads) {
            if (t.isAlive()) {
                todasTerminadas = false;
                break;
            }
        }

        // 4. Emitir relatório final
        emitirRelatorioFinal(duracaoMonitorizacaoSegundos, duracaoEncerramentoMs, todasTerminadas);
    }

    /**
     * Imprime o relatório final de monitorização exatamente no formato especificado.
     */
    private void emitirRelatorioFinal(double duracaoMonitorizacaoSeg, long duracaoEncerramentoMs, boolean todasTerminadas) {
        System.out.println("================================================================");
        System.out.println("RELATÓRIO FINAL DE MONITORIZAÇÃO");
        System.out.println("================================================================");
        System.out.printf(Locale.GERMAN, "Duração da monitorização : %.1f s%n", duracaoMonitorizacaoSeg);
        System.out.printf("Duração do encerramento : %d ms%n", duracaoEncerramentoMs);
        System.out.println("----------------------------------------------------------------");
        System.out.printf("%-10s %10s %10s %10s %10s %-8s%n", "Sensor", "Leituras", "Mínimo", "Máximo", "Média", "Unidade");

        List<EstatisticaSensor> statsList = registoCentral.getEstatisticasParciais();
        for (EstatisticaSensor s : statsList) {
            System.out.printf(Locale.GERMAN, "%-10s %10d %10.1f %10.1f %10.1f %-8s%n",
                    s.getSensorNome(), s.getTotalLeituras(), s.getMinimo(), s.getMaximo(), s.getMedia(), s.getUnidade());
        }

        int contadorTotal = registoCentral.getContadorTotal();
        int somaPorSensor = registoCentral.getSomaLeiturasPorSensor();
        int registosHistorico = registoCentral.getTamanhoHistorico();
        boolean invarianteOK = registoCentral.verificarInvariante();

        System.out.println("----------------------------------------------------------------");
        System.out.printf("Total de leituras (contador) : %d%n", contadorTotal);
        System.out.printf("Soma das leituras por sensor : %d%n", somaPorSensor);
        System.out.printf("Registos no histórico : %d%n", registosHistorico);
        System.out.printf("Verificação de consistência : %s%n", invarianteOK ? "OK" : "FALHA");
        System.out.println("Estado final das threads :");
        for (Thread t : threads) {
            System.out.printf(" %-15s %s%n", t.getName(), t.getState());
        }
        System.out.printf("Todas as threads terminadas : %s%n", todasTerminadas ? "SIM" : "NÃO");
        System.out.println("================================================================");
    }
}
