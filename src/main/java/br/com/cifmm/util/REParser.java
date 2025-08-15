package br.com.cifmm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class REParser {
    
    /**
     * Analisa uma string de REs separados por vírgula
     * @param input String do JTextField (ex: "12790,14359,14237")
     * @return Lista de REs válidos
     */
    public static List<String> parseREs(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(input.split(","))
                .map(String::trim)                    // Remove espaços
                .filter(re -> !re.isEmpty())          // Remove vazios
                .filter(REParser::isValidRE)          // Valida formato
                .collect(Collectors.toList());
    }
    
    /**
     * Verifica se o RE tem formato válido
     * @param re String do RE
     * @return true se válido
     */
    public static boolean isValidRE(String re) {
        // Aceita apenas números, pode ter zeros à esquerda
        return re.matches("^\\d+$") && re.length() >= 3 && re.length() <= 8;
    }
    
    /**
     * Verifica se a entrada contém múltiplos REs
     * @param input String do JTextField
     * @return true se contém vírgulas (múltiplos REs)
     */
    public static boolean isMultipleREs(String input) {
        return input != null && input.contains(",");
    }
    
    /**
     * Formata um RE com zeros à esquerda se necessário
     * @param re RE original
     * @return RE formatado
     */
    public static String formatRE(String re) {
        // Garante que tem pelo menos 5 dígitos (ajuste conforme sua necessidade)
        return String.format("%05d", Integer.parseInt(re));
    }
    
    /**
     * Conta quantos REs válidos existem na string
     * @param input String do JTextField
     * @return Número de REs válidos
     */
    public static int countValidREs(String input) {
        return parseREs(input).size();
    }
}