package sensores;

import registo.Leitura;
import registo.RegistoCentral;

import java.time.Instant;
import java.util.Random;

/**
 * Tarefa periódica concorrente que simula a leitura de um sensor ambiental.
 * Implemementa a interface Runnable e gere o seu ciclo de vida através de uma flag volatile
 * combinada com o sinal de interrupção (interrupt()).
 */
public class Sensor implements Runnable {
    private final String nome;
    private final String unidade;
    private final double minValor;
    private final double maxValor;
    private final long intervaloBaseMs;
    private final RegistoCentral registoCentral;

    private volatile boolean ativo = true;
    private int leiturasEfetuadas = 0;

    /**
     * Construtor do sensor.
     *
     * @param nome             nome/identificador do sensor (ex.: TEMP-01)
     * @param unidade          unidade de medida (ex.: °C, %, hPa)
     * @param minValor         valor mínimo da gama de dados simulados
     * @param maxValor         valor máximo da gama de dados simulados
     * @param intervaloBaseMs  intervalo base de leitura em milissegundos
     * @param registoCentral   referência para o recurso partilhado RegistoCentral
     */
    public Sensor(String nome, String unidade, double minValor, double maxValor, long intervaloBaseMs, RegistoCentral registoCentral) {
        if (nome == null || unidade == null || registoCentral == null) {
            throw new IllegalArgumentException("Parâmetros do sensor não podem ser nulos.");
        }
        if (minValor > maxValor) {
            throw new IllegalArgumentException("minValor não pode ser superior a maxValor.");
        }
        if (intervaloBaseMs <= 0) {
            throw new IllegalArgumentException("O intervalo base deve ser positivo.");
        }
        this.nome = nome;
        this.unidade = unidade;
        this.minValor = minValor;
        this.maxValor = maxValor;
        this.intervaloBaseMs = intervaloBaseMs;
        this.registoCentral = registoCentral;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public int getLeiturasEfetuadas() {
        return leiturasEfetuadas;
    }

    /**
     * Sinaliza a paragem cooperativa da tarefa do sensor.
     */
    public void parar() {
        this.ativo = false;
    }

    @Override
    public void run() {
        Random random = new Random();

        while (ativo && !Thread.currentThread().isInterrupted()) {
            // Generates value within range [minValor, maxValor]
            double valorMedido = minValor + (maxValor - minValor) * random.nextDouble();

            // Creates immutable Leitura
            Leitura leitura = new Leitura(nome, valorMedido, unidade, Instant.now());

            // Registers reading atomically in shared resource
            registoCentral.registar(leitura);
            leiturasEfetuadas++;

            // Echo reading to console with thread name
            System.out.printf("[%s] Leituras: %.1f %s%n", Thread.currentThread().getName(), valorMedido, unidade);

            // Calculates effective interval: base interval ± 20% jitter
            // random.nextDouble() * 0.4 - 0.2 gives range [-0.2, 0.2]
            double jitterPercentual = (random.nextDouble() * 0.4) - 0.2;
            long jitterMs = Math.round(intervaloBaseMs * jitterPercentual);
            long tempoPausaMs = intervaloBaseMs + jitterMs;
            if (tempoPausaMs < 1) {
                tempoPausaMs = 1;
            }

            // Sleep OUTSIDE critical section / synchronized blocks
            try {
                Thread.sleep(tempoPausaMs);
            } catch (InterruptedException e) {
                // Restore interrupt flag to preserve Java Thread interrupt state
                Thread.currentThread().interrupt();
                // Terminate reading cycle cleanly
                break;
            }
        }

        // Ordered output message upon loop termination
        System.out.println("[" + Thread.currentThread().getName() + "] terminado de forma controlada (" + leiturasEfetuadas + " leituras).");
    }
}
