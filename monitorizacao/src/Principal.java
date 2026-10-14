import central.SistemaCentral;

/**
 * Ponto de entrada da aplicação (main).
 * Limita-se a delegar a execução no SistemaCentral.
 * Suporta o modo de teste de carga caso seja passado o argumento "carga" ou a propriedade -Dcarga=true.
 */
public class Principal {
    public static void main(String[] args) {
        SistemaCentral sistema = new SistemaCentral();
        if ((args.length > 0 && "carga".equalsIgnoreCase(args[0])) || Boolean.getBoolean("carga")) {
            sistema.configurarSensoresPersonalizado(20, 1);
        }
        sistema.executar();
    }
}
