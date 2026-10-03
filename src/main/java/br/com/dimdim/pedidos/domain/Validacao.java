package br.com.dimdim.pedidos.domain;

final class Validacao {
    private Validacao() {}

    static String texto(String valor, int limite, String campo) {
        if (valor == null || valor.strip().isBlank()) {
            throw new RegraNegocioException(campo + " é obrigatório.");
        }
        String limpo = valor.strip();
        if (limpo.length() > limite) {
            throw new RegraNegocioException(campo + " deve ter no máximo " + limite + " caracteres.");
        }
        return limpo;
    }
}
