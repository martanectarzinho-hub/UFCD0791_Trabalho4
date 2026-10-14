package registo;

/**
 * Classe auxiliar imutável que representa um instantâneo estatístico das leituras de um sensor.
 */
public final class EstatisticaSensor {
    private final String sensorNome;
    private final int totalLeituras;
    private final double minimo;
    private final double maximo;
    private final double soma;
    private final String unidade;

    public EstatisticaSensor(String sensorNome, int totalLeituras, double minimo, double maximo, double soma, String unidade) {
        this.sensorNome = sensorNome;
        this.totalLeituras = totalLeituras;
        this.minimo = minimo;
        this.maximo = maximo;
        this.soma = soma;
        this.unidade = unidade;
    }

    public String getSensorNome() {
        return sensorNome;
    }

    public int getTotalLeituras() {
        return totalLeituras;
    }

    public double getMinimo() {
        return minimo;
    }

    public double getMaximo() {
        return maximo;
    }

    public double getSoma() {
        return soma;
    }

    public double getMedia() {
        return totalLeituras > 0 ? soma / totalLeituras : 0.0;
    }

    public String getUnidade() {
        return unidade;
    }
}
