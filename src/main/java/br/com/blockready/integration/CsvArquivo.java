package br.com.blockready.integration;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.blockready.exception.BusinessException;

/**
 * Leitor de CSV dos datasets públicos. Detecta o delimitador (; ou ,), remove BOM, entende aspas
 * e indexa as colunas pelo cabeçalho normalizado (minúsculas, sem acentos, não alfanumérico vira "_").
 * Assim a importação tolera variações de nome de coluna entre versões dos arquivos oficiais.
 */
public final class CsvArquivo {

    /** Uma linha de dados: acesso pelos nomes normalizados possíveis da coluna. */
    public record Linha(int numero, Map<String, String> valores) {

        /** Primeiro valor não vazio entre os apelidos de coluna informados; null se nenhum existir. */
        public String texto(String... apelidos) {
            for (String apelido : apelidos) {
                String v = valores.get(apelido);
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
            return null;
        }
    }

    private final List<String> cabecalho;
    private final List<Linha> linhas;
    private final char delimitador;

    private CsvArquivo(List<String> cabecalho, List<Linha> linhas, char delimitador) {
        this.cabecalho = cabecalho;
        this.linhas = linhas;
        this.delimitador = delimitador;
    }

    public static CsvArquivo ler(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new BusinessException("O arquivo enviado está vazio.");
        }
        String texto = new String(conteudo, StandardCharsets.UTF_8);
        if (!texto.isEmpty() && texto.charAt(0) == '﻿') {
            texto = texto.substring(1);
        }
        List<String> fisicas = quebrarLinhas(texto);
        if (fisicas.isEmpty()) {
            throw new BusinessException("O arquivo não possui linhas de dados.");
        }
        char delim = detectarDelimitador(fisicas.get(0));
        List<String> cab = separar(fisicas.get(0), delim).stream().map(CsvArquivo::normalizar).toList();
        if (cab.stream().allMatch(String::isBlank)) {
            throw new BusinessException("O cabeçalho do arquivo está vazio.");
        }
        List<Linha> dados = new ArrayList<>();
        for (int i = 1; i < fisicas.size(); i++) {
            String bruta = fisicas.get(i);
            if (bruta.isBlank()) {
                continue;
            }
            List<String> campos = separar(bruta, delim);
            Map<String, String> valores = new LinkedHashMap<>();
            for (int c = 0; c < cab.size(); c++) {
                valores.put(cab.get(c), c < campos.size() ? campos.get(c) : "");
            }
            dados.add(new Linha(i + 1, valores));
        }
        return new CsvArquivo(cab, dados, delim);
    }

    /** Quebra em linhas físicas respeitando quebras dentro de aspas. */
    private static List<String> quebrarLinhas(String texto) {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"') {
                dentroDeAspas = !dentroDeAspas;
                atual.append(c);
            } else if ((c == '\n' || c == '\r') && !dentroDeAspas) {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                linhas.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        if (atual.length() > 0) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    private static char detectarDelimitador(String cabecalho) {
        long pontoEVirgula = cabecalho.chars().filter(c -> c == ';').count();
        long virgula = cabecalho.chars().filter(c -> c == ',').count();
        return pontoEVirgula >= virgula ? ';' : ',';
    }

    private static List<String> separar(String linha, char delim) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (dentroDeAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        dentroDeAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                dentroDeAspas = true;
            } else if (c == delim) {
                campos.add(atual.toString().trim());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString().trim());
        return campos;
    }

    /** Normaliza nomes de coluna: minúsculas, sem acentos, não alfanumérico vira "_". */
    public static String normalizar(String nome) {
        String semAcentos = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public List<String> cabecalho() { return cabecalho; }
    public List<Linha> linhas() { return linhas; }
    public char delimitador() { return delimitador; }
    public boolean possuiColuna(String... apelidos) {
        for (String a : apelidos) {
            if (cabecalho.contains(a)) {
                return true;
            }
        }
        return false;
    }
}
