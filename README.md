# Using kafka with Java simple project example

This project is a simple **stock** and **sale** microsservices with Java Spring

# How to use
## 1. Kafka install (Windows Version without Docker)
1. Go to **https://kafka.apache.org/downloads** official site download page
2. Install version **3.9.1 > Binary downloads > Scala 2.13 [(kafka_2.13-3.9.1.tgz)](https://dlcdn.apache.org/kafka/3.9.1/kafka_2.12-3.9.1.tgz)**
3. Unzip the folder
4. Enter in downloaded kafka folder

## Initializing Kafka and Zookeeper
1. Open Terminal
2. Run Zookeeper
```powershell
.\bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```
3. Open other Terminal
4. Run Kafka
```powershell
.\bin\windows\kafka-server-start.bat config\server.properties
```
Keep both running.

## 2. Config Kafka Tool
### Step 1: Download and Install Kafka Tool
https://www.kafkatool.com/download.html

### Step 2: Open Kafka Tool and Add a New Cluster
1. Run downloaded Kafka Tool *.exe* file
2. Open Kafka Tool (it's named as **Offset Explorer**)
3. Go to ```file``` > ```Add Cluster```
4. Fill in the Cluster details:
    - **Cluster Name:** Choose a descritive name, like "shop-services".
    - **Bootstrap Servers:** Insert ```localhost:9092``` *(or the Kafka server address)*
    - **Kafka Cluster Version:** Select the version that matches your Kafka instalation.
    - **Zookeeper:** Insert ```localhost:2181``` *(or the Zookeeper server address)*.

## 3. Clone this Repo
```git
git clone https://github.com/rafaluket/java-kafka-simple-example.git
```
1. Open/Run ```estoqueservice``` project. (Our Consumer)
2. Open/Run ```vendaservice``` project. (Our Producer)

## 4. TEST TIME 🔥
### cURL
```shell
curl --request POST
  --url http://localhost:8081/venda
  --header 'Content-Type: application/json'
  --data 6b214d60-3805-4d50-99ef-0e29eb417122
```
```data``` its just a random UUID for our idProduto
### Insomnia
Create a POST request

- URL: http://localhost:8081/venda
- Body: json
- DATA: faker=>randomUUID

#### Response
Our ```estoqueservice``` application should return the log:
```shell
Venda recebida no estoque: 6b214d60-3805-4d50-99ef-0e29eb417122
```

# How it works
In Kafka you need a Producer service to serve your information and a Consumer listening to kafka events.
In our scenario we have a producer ```vendaservice``` and a consumer ```estoqueservice```.

### Producer
1. ```application.properties```
```properties
spring.application.name=venda-service
server.port=8081
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```
In properties we defined:
- producer application *port* 
- *server* and *port* where our kafka is running.
- *key/value* serialized as String.

2. Create the config class of our producer

In this class we will pass our __BOOTSTRAP_SERVERS_CONFIG__ which corresponds to the URL of our Cluster
```JAVA
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

3. To send a message, we just need to use the ```kafkaTemplate.send```
```JAVA
@RestController
@RequestMapping("/venda")
public class VendaController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping
    public ResponseEntity<Void> realizarVenda(@RequestBody String idProduto){
        kafkaTemplate.send("estoque-topico", idProduto);
        return ResponseEntity.ok().build();
    }
}
```
Our producer ```vendaservice``` will be a simple Controller receiving a POST *(HTTP METHOD)* with parameter *String idProduto* as a body request content. and use the kafkaTemplate.send with our topic and string as parameter.

### Consumer
1. First we will set the kafka properties in ```application.properties```
```properties
spring.application.name=estoque-service
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=estoque-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.auto-offset-reset=earliest
```
In properties we defined:
- consumer *group id* (because differents consumer can use the same producer data)
- *server* and port where our kafka is running
- *key/value* deserialized as String.
- *offset* to define the pattern of topic read (*earliest* takes the first message received in the kafka to process)

2. Create the config class of our consumer

In this class we will pass our configuration as consumer and enable Kafka
```JAVA
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "estoque-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
```
3. To listen a message, we just need to use the ```@KafkaListener```
```JAVA
@Service
public class EstoqueListener {
    @KafkaListener(topics = "estoque-topico", groupId = "estoque-group")
    public void processarVenda(String mensagem){
        System.out.println("Venda recebida no estoque: " + mensagem);
    }
}
```