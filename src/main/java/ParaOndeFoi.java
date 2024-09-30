
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class ParaOndeFoi {
    private static final Logger LOGGER = Logger.getLogger(ParaOndeFoi.class.getName());
    private static final String API_KEY = System.getenv("YOUR_GOOGLE_MAPS_API_KEY");
    private static final Map<String, Coordinate> coordinateCache = new HashMap<>();

    public static void main(String[] args) {
        configureLogging(),

        System.out.println("PARA ONDE FOI?");

        File uploadedFile = promptForFile("Escolha o arquivo CSV das empresas");

        if (uploadedFile != null) {
            List<Empresa> empresas = loadData(uploadedFile);

            if (empresas != null) {
                List<String> requiredColumns = Arrays.asList("endereco", "numero", "cep");
                if (!hasRequiredColumns(empresas, requiredColumns)) {
                    System.err.println("O arquivo CSV deve conter as colunas: 'endereco', 'numero', 'cep'.");
                } else {
                    processEmpresas(empresas);
                }
            }
        } else {
            System.out.println("Por favor, faça o upload de um arquivo CSV para visualizar o mapa e os dados.");
        }
    }

class Empresa {
    private String endereco;
    private String numero;
    private String cep;
    private Coordenada Coordenada;

    public Empresa(String endereco, String numero, String cep) {
        this.endereco = endereco;
        this.numero = numero;
        this.cep = cep;
    }

    public String getFullAdress() {
        return endereco + ", " + numero + ", " + cep;
    }

    public void setCoordenada(Coordenada coordenada) {
        this.coordenada = coordenada;
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    @Override
    public String toString() {
        return "Empresa{" +
                "endereco='" + endereco + '\'' +
                ", numero='" + numero + '\'' +
                ", cep='" + cep + '\'' +
                ", coordenada=" + coordenada +
                '}';
    }
}




class Coordenada {}