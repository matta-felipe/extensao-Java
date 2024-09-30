# Para Onde Foi?

## Descrição
"Para Onde Foi?" é uma aplicação Java (em desenvolvimento) que processa dados de empresas a partir de um arquivo CSV, geocodifica seus endereços usando a API do Google Maps e gera um mapa interativo com a localização de estabelecimentos comerciais e MEIs antes e pós tremores de terra em Maceió. 

## Funcionalidades
* Leitura de dados de empresas a partir de um arquivo CSV
* Geocodificação de endereços usando a API do Google Maps
* Geração de um mapa interativo com marcadores para cada empresa
* Exibição de estatísticas sobre o processo de geocodificação
* Cache de coordenadas para otimizar requisições à API

## Pré-requisitos
* Docker

## Dependências
* Java 8 ou superior
* Maven (para gerenciamento de dependências)
* Chave de API do Google Maps

## Configuração
1. Clone o repositório.
2. Configure a variável de ambiente em exemplo.env
3. Construa a imagem Docker com o comando:
  ```bash
  docker build -t para-onde-foi .
  ```

## Uso
1. Execute a aplicação no Docker: 
  ```bash
  docker run -e YOUR_GOOGLE_MAPS_API_KEY=your_api_key -v /src/main/resources/data/enderecos.csv:/data para-onde-foi
  ```
  Substitua `your_api_key` pela sua chave de API do Google Maps.

2. A aplicação processará os dados, realizará a geocodificação e gerará um arquivo HTML com o mapa.


## Dados
1. Os dados em enderecos.csv são PÚBLICOS e podem ser baixados nesse domínio da Receita Federal https://dadosabertos.rfb.gov.br/CNPJ/dados_abertos_cnpj/2024-09/ ou no domínio https://casadosdados.com.br/solucao/cnpj/pesquisa-avancada .

2. O arquivo CSV deve conter as seguintes colunas:
* endereco
* numero
* cep

## Saída
* Um arquivo HTML (`map.html`) contendo o mapa interativo com as localizações das empresas.
* Logs de geocodificação e estatísticas no console.

## Notas
* Os dados estão no formato .csv para fins de teste. O mais adequado seria fazer requisicões a uma API e armazenar em um banco de dados.
* A aplicação inclui um mecanismo de limitação de taxa para respeitar os limites da API do Google Maps.
* As coordenadas são armazenadas em cache para evitar requisições desnecessárias à API.


## Licença
[MIT]