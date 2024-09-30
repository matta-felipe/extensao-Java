
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private static final Map<String, Coordenada> coordenadaCache = new HashMap<>();

    public static void main(String[] args) {
        configureLogging();

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

    private static void configureLogging() {
        LogManager.getLogManager().reset();
        LOGGER.setLevel(Level.INFO);
        
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });
        LOGGER.addHandler(ch);
    }

    private static File promptForFile(String message) {
        System.out.println(message);
        return null;
    }

    private static List<Empresa> loadData(File file) {
        List<Empresa> empresas = new ArrayList<>();
        try (Reader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, 
             CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build())) {

            
            for (CSVRecord record : csvParser) {
                Empresa empresa = new Empresa(
                    record.get("endereco"),
                    record.get("numero"),
                    record.get("cep")
                );
                empresas.add(empresa);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading CSV file", e);
            return null;
        }
        return empresas;
    }

    private static boolean hasRequiredColumns(List<Empresa> empresas, List<String> requiredColumns) {
        if (empresas.isEmpty()) {
            return false;
        }
        Empresa firstEmpresa = empresas.get(0);
        return requiredColumns.stream().allMatch(col -> 
            firstEmpresa.getClass().getDeclaredFields()[0].getName().equals(col) ||
            firstEmpresa.getClass().getDeclaredFields()[1].getName().equals(col) ||
            firstEmpresa.getClass().getDeclaredFields()[2].getName().equals(col)
        );
    }

    private static void processEmpresas(List<Empresa> empresas) {
        List<Empresa> validEmpresas = new ArrayList<>();
        List<String> geocodingLog = new ArrayList<>();

        for (int i = 0; i < empresas.size(); i++) {
            Empresa empresa = empresas.get(i);
            String address = empresa.getFullAddress();
            Coordenada coordenada = getCoordinates(address);
            
            if (coordenada != null) {
                empresa.setCoordenada(coordenada);
                validEmpresas.add(empresa);
                geocodingLog.add("Sucesso: " + address);
            } else {
                geocodingLog.add("Falha: " + address);
            }
            
            double progress = (double) (i + 1) / empresas.size();
            updateProgress(progress, i + 1, empresas.size());
        }

        LOGGER.info(validEmpresas.toString());

        if (validEmpresas.isEmpty()) {
            System.out.println("Nenhuma coordenada válida encontrada. Verifique os endereços.");
        } else {
            displayMap(validEmpresas);
        }

        System.out.println("Dados das empresas e MEIs nos Flexais, Maceió.");
        displayDataFrame(empresas);

        System.out.println("Log de Geocodificação");
        displayGeocodingLog(geocodingLog);

        displayStatistics(empresas.size(), validEmpresas.size());
    }

    private static Coordenada getCoordinates(String address) {
        if (coordenadaCache.containsKey(address)) {
            return coordenadaCache.get(address);
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String encodedAddress = java.net.URLEncoder.encode(address, StandardCharsets.UTF_8);
        String urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddress + "&key=" + API_KEY;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = reader.lines().collect(Collectors.joining());
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response);
                    
                    if (root.has("results") && root.get("results").size() > 0) {
                        JsonNode location = root.get("results").get(0).get("geometry").get("location");
                        double lat = location.get("lat").asDouble();
                        double lon = location.get("lng").asDouble();
                        Coordenada coordenada = new Coordenada(lat, lon);
                        coordenadaCache.put(address, coordenada);
                        return coordenada;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error getting coordinates", e);
        }

        return null;
    }

    private static void updateProgress(double progress, int current, int total) {
        System.out.printf("Processando: %d/%d endereços (%.2f%%)\n", current, total, progress * 100);
    }
    private static void displayMap(List<Empresa> empresas) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head>")
            .append("<script src=\"https://maps.googleapis.com/maps/api/js?key=")
            .append(API_KEY).append("\"></script>")
            .append("<style> #map { height: 100vh; width: 100%; } </style>")
            .append("</head><body>")
            .append("<div id=\"map\"></div>")
            .append("<script>")
            .append("function initMap() {")
            .append("var map = new google.maps.Map(document.getElementById('map'), {")
            .append("zoom: 12,")
            .append("center: {lat: -9.62603, lng: -35.74768}");
            
        for (Empresa empresa : empresas) {
            html.append(",")
                .append("new google.maps.Marker({")
                .append("position: {lat: ").append(empresa.getCoordenada().getLat())
                .append(", lng: ").append(empresa.getCoordenada().getLon())
                .append("},")
                .append("map: map,")
                .append("title: '").append(empresa.getFullAddress()).append("'")
                .append("})");
        }
        html.append("};")
            .append("initMap();")
            .append("</script></body></html>");

        try (PrintWriter out = new PrintWriter("map.html")) {
            out.println(html.toString());
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error writing HTML file", e);
        }

        System.out.println("Mapa gerado em map.html. Abra o arquivo no navegador.");
    }
    private static void displayDataFrame(List<Empresa> empresas) {
        for (Empresa empresa : empresas) {
            System.out.println(empresa);
        }
    }

    private static void displayGeocodingLog(List<String> log) {
        for (String entry : log) {
            System.out.println(entry);
        }
    }

    private static void displayStatistics(int totalAddresses, int successfulGeocodes) {
        System.out.println("Total de endereços: " + totalAddresses);
        System.out.println("Endereços geocodificados com sucesso: " + successfulGeocodes);
        System.out.printf("Taxa de sucesso: %.2f%%\n", (double) successfulGeocodes / totalAddresses * 100);
    }
}


class Empresa {
    private String endereco;
    private String numero;
    private String cep;
    private Coordenada coordenada;

    public Empresa(String endereco, String numero, String cep) {
        this.endereco = endereco;
        this.numero = numero;
        this.cep = cep;
    }

    public String getFullAddress() {
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


class Coordenada {
    private double lat;
    private double lon;

    public Coordenada(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }   

    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        return "Coordenada{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
