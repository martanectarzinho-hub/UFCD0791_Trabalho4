package registo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursos partilhado entre as threads produtoras (sensores) e a thread consumidora/gestora (SistemaCentral).
 * Encapsula todo o histórico de leituras e estatísticas por sensor.
 * Todos os métodos de leitura e escrita são completamente sincronizados (intrinsic lock).
 */
public class RegistoCentral {
    private final List<Leitura> historico = new ArrayList<>();
    private int contadorTotal = 0;
    private final Map<String, AcumuladorStats> estatisticas = new LinkedHashMap<>();

    /**
     * Acumulador interno mutável (privado a RegistoCentral) para agregados por sensor.
     */
    private static class AcumuladorStats {
        private final String sensorNome;
        private int totalLeituras = 0;
        private double minimo = Double.MAX_VALUE;
        private double maximo = -Double.MAX_VALUE;
        private double soma = 0.0;
        private String unidade = "";
        private Leitura ultimaLeitura = null;

        AcumuladorStats(String sensorNome) {
            this.sensorNome = sensorNome;
        }

        void acumular(Leitura leitura) {
            totalLeituras++;
            soma += leitura.getValor();
            if (leitura.getValor() < minimo) {
                minimo = leitura.getValor();
            }
            if (leitura.getValor() > maximo) {
                maximo = leitura.getValor();
            }
            unidade = leitura.getUnidade();
            ultimaLeitura = leitura;
        }

        EstatisticaSensor toSnapshot() {
            double minExibicao = totalLeituras > 0 ? minimo : 0.0;
            double maxExibicao = totalLeituras > 0 ? maximo : 0.0;
            return new EstatisticaSensor(sensorNome, totalLeituras, minExibicao, maxExibicao, soma, unidade);
        }
    }

    /**
     * Regista uma nova leitura no sistema central.
     * Operação composta e atómica realizada numa única secção crítica sincronizada:
     * 1. Adiciona ao histórico de leituras;
     * 2. Atualiza os acumuladores estatísticos por sensor;
     * 3. Incrementa o contador global de leituras.
     *
     * @param leitura objeto de leitura imutável a registar
     */
    public synchronized void registar(Leitura leitura) {
        if (leitura == null) {
            return;
        }
        historico.add(leitura);
        contadorTotal++;

        AcumuladorStats stats = estatisticas.computeIfAbsent(leitura.getSensorNome(), AcumuladorStats::new);
        stats.acumular(leitura);
    }

    /**
     * Devolve uma cópia defensiva do histórico de leituras.
     *
     * @return nova lista contendo as leituras registadas até ao momento
     */
    public synchronized List<Leitura> getHistorico() {
        return new ArrayList<>(historico);
    }

    /**
     * Devolve uma cópia defensiva do mapa com a leitura mais recente de cada sensor.
     *
     * @return mapa com o nome do sensor como chave e a sua última leitura como valor
     */
    public synchronized Map<String, Leitura> getUltimasLeituras() {
        Map<String, Leitura> ultimas = new LinkedHashMap<>();
        for (Map.Entry<String, AcumuladorStats> entry : estatisticas.entrySet()) {
            if (entry.getValue().ultimaLeitura != null) {
                ultimas.put(entry.getKey(), entry.getValue().ultimaLeitura);
            }
        }
        return ultimas;
    }

    /**
     * Devolve uma lista de instantâneos estatísticos parciais (por sensor).
     *
     * @return lista de objetos EstatisticaSensor
     */
    public synchronized List<EstatisticaSensor> getEstatisticasParciais() {
        List<EstatisticaSensor> snapshots = new ArrayList<>();
        for (AcumuladorStats stats : estatisticas.values()) {
            snapshots.add(stats.toSnapshot());
        }
        return snapshots;
    }

    /**
     * Devolve o contador global de leituras.
     *
     * @return total de leituras registadas
     */
    public synchronized int getContadorTotal() {
        return contadorTotal;
    }

    /**
     * Devolve a soma das contagens individuais de leituras por sensor.
     *
     * @return soma das contagens por sensor
     */
    public synchronized int getSomaLeiturasPorSensor() {
        int soma = 0;
        for (AcumuladorStats stats : estatisticas.values()) {
            soma += stats.totalLeituras;
        }
        return soma;
    }

    /**
     * Devolve o número total de elementos atualmente guardados no histórico.
     *
     * @return tamanho do histórico
     */
    public synchronized int getTamanhoHistorico() {
        return historico.size();
    }

    /**
     * Verifica a invariante de consistência do sistema:
     * contador total == soma das contagens por sensor == número de registos no histórico.
     *
     * @return true se a invariante for respeitada; false caso contrário
     */
    public synchronized boolean verificarInvariante() {
        int somaPorSensor = getSomaLeiturasPorSensor();
        int tamanhoHist = historico.size();
        return contadorTotal == somaPorSensor && contadorTotal == tamanhoHist;
    }
}
