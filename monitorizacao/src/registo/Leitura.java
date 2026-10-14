package registo;

import java.time.Instant;
import java.util.Objects;

/**
 * Classe imutável que representa uma leitura individual efetuada por um sensor.
 * Encapsula o nome do sensor, o valor medido, a unidade de medida e o instante de captura.
 */
public final class Leitura {
    private final String sensorNome;
    private final double valor;
    private final String unidade;
    private final Instant instante;

    /**
     * Construtor da leitura imutável.
     *
     * @param sensorNome nome do sensor que efetuou a leitura
     * @param valor      valor numérico medido
     * @param unidade    unidade de medida (ex.: °C, %, hPa)
     * @param instante   instante em que a leitura foi realizada
     */
    public Leitura(String sensorNome, double valor, String unidade, Instant instante) {
        this.sensorNome = Objects.requireNonNull(sensorNome, "O nome do sensor não pode ser nulo");
        this.valor = valor;
        this.unidade = Objects.requireNonNull(unidade, "A unidade de medida não pode ser nula");
        this.instante = Objects.requireNonNull(instante, "O instante não pode ser nulo");
    }

    public String getSensorNome() {
        return sensorNome;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public Instant getInstante() {
        return instante;
    }

    @Override
    public String toString() {
        return String.format("[%s] %.1f %s (%s)", sensorNome, valor, unidade, instante);
    }
}
