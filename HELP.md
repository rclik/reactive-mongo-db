# Reactive Mongo DB

Bu projede Spring Boot da reactive programlama ile Mongo DB uygulaması yapacağız.

## Projenin Oluşturulması

### Gerekli Library lerin Kurulması

İlk olarak kullanacağımız library ler;

- Spring Reactive Web (Mono, Flux ile rest service lerinin yazılması için)
- Spring Data Reactive MongoDB (Mongo DB işlemlerini reactive olarak yapabilmemizi sağlayan library)
- Validation (rest model object lerimizin validation ı için kullanacağız)
- Lombok
- MapConstruct
- Spring Boot Developer Tools

MapConstruct ayarlamasını daha sonra yapacağız. O hariç tüm library leri kuracak şekilde Spring Initializer dan projemizi oluşturuyoruz. build.gradle dosyamız şöyle oluyor;

```json
plugins {
 id 'java'
 id 'org.springframework.boot' version '3.1.0'
 id 'io.spring.dependency-management' version '1.1.0'
}

group = 'com.rcelik.springguru'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

configurations {
 compileOnly {
  extendsFrom annotationProcessor
 }
}

repositories {
 mavenCentral()
}

dependencies {
 implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive' 
 implementation 'org.springframework.boot:spring-boot-starter-validation'
 implementation 'org.springframework.boot:spring-boot-starter-webflux'

 compileOnly 'org.projectlombok:lombok'

 developmentOnly 'org.springframework.boot:spring-boot-devtools'

 annotationProcessor 'org.projectlombok:lombok'

 testImplementation 'org.springframework.boot:spring-boot-starter-test'
 testImplementation 'io.projectreactor:reactor-test'
}

tasks.named('test') {
 useJUnitPlatform()
}
```

### Mongo DB ve Mongo Express nin Docker Kurulumu

Docker da Mongo DB ve Mongo DB yi yöneceğimiz tool u kurmak için bir docker-compose.yml oluşturduk;

```yml
# Use root/example as user/password credentials
version: '3.1'

services:

  mongo:
    image: mongo
    restart: always
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: example
      ports:
        - "27017:27017"

  mongo-express:
    image: mongo-express
    restart: always
    ports:
      - 8081:8081
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: root
      ME_CONFIG_MONGODB_ADMINPASSWORD: example
      ME_CONFIG_MONGODB_URL: mongodb://root:example@mongo:27017/
```

docker compose up komutunu çalıştırdıktan sonra ilgili docker container ları oluşuturulup, çalıştırılacaklar.
sonrasında bir browser aracılığıyla localhost:8081 ile mongo-express in UI ına ulaşabiliriz. eğer doğru şekilde ulaşabiliyorsak sorun yok demektir. bir sonraki adıma geçebiliriz.

### Domain Objelerinin Oluşturulması

Bir önceki domain ile aynı olmasını istiyoruz.

Bunun için model package ındaki BeerDTO ve domain package ındaki Beer objesini oluşturuyoruz.
Model package inde rest dünyasına açılacak object lerin class ları bulunurken, domain package i içerisinde database tarafında kullanılacak objectlerin class ları bulunur.

Genel olarak DTO larda validation ları da alalım ve sonuç olarak;

```java
package com.rcelik.springguru.reactivemongodb.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Beer {
    private Integer id;
    private String beerName;
    private String beerStyle;
    private String upc;
    private Integer quantitiyOnHand;
    private BigDecimal price;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
}

package com.rcelik.springguru.reactivemongodb.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BeerDTO {
    private Integer id;

    @NotBlank
    @Size(min = 3, max = 255)
    private String beerName;
    
    @Size(min = 1, max = 255)
    private String beerStyle;

    @Size(min = 1, max = 25)
    private String upc;
    
    private Integer quantitiyOnHand;
    private BigDecimal price;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}

```

> DTO class ında AllArgs ve NoArgs Constructor annotation larını eklememiz lazım.
> Yoksa user dan gelen objeler serialize edilemiyorlar.

Şimdi MapConstruct library sini kullanarak bu iki object arasındaki conversion ları oluşturalım.

#### MapConstruct Library Kurulumu

MapConstruct domain ve model objeleri arasında conversionları bizim için yapan library dir. Compile time da gerekli class ları annotation lara bakarak oluşturur.

ilk olarak annotation processor un compile time da çalışmasını sağlamamız lazım;

```yml
configurations {
 compileOnly {
  extendsFrom annotationProcessor
 }
}
```

MapContruct lombok ile ortak çalışması için bir ayar yapamız lazım;

```yml
 annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}", 
                     "org.projectlombok:lombok-mapstruct-binding:${lombokMapstructBindingVersion}"
```

şimdi de bunların versyonları bir variable dan alalım;

```yml
ext {
  mapstructVersion = "1.5.3.Final"
  lombokMapstructBindingVersion = "0.2.0"
}
```

Şimdi ayrı bir configuration ımız daha kaldı. Bunda da MapConstruct in çalışırken SpringBoot için çalıştığını söylememiz lazım

```yml
compileJava {
    options.compilerArgs += [
            '-Amapstruct.defaultComponentModel=spring',
    ]
}
```

Bu kadar. Artık MapConstruct ı kullanabiliriz;

MapStruct interface ler üzerinden çalışır. Compile time da bu interface ler üzerinden conversion logic lerinin concrete class ları oluşturulur.

mappers adından bir package oluşturalım ve altına BeerMapper interface i oluşturalım;

```java
package com.rcelik.springguru.reactivemongodb.mappers;

import org.mapstruct.Mapper;

import com.rcelik.springguru.reactivemongodb.domain.Beer;
import com.rcelik.springguru.reactivemongodb.model.BeerDTO;

@Mapper
public interface BeerMapper {

    Beer beerDTOToBeer(BeerDTO beerDTO);

    BeerDTO beerToBeerDTO(Beer beer);
}
```

Bu kadar. Compile edildiğinde interface in concrete class ı oluşacak ve bunu /build/classes altında görebilirsin.

### Database Connection Ayarlaması

Mongo DB docker ile kurulmuş durumda. Şimdi application ımızın mongo db yi connection ını nasıl yapacağımıza bakalım.

Configuration ımızı Java based yapalım. yml based de yapılabilir ama şimdilik java yı ele alalım.

Configuration ımız için öncelikle bir configuration class ı oluşturuyoruz ve bu class ın AbstractReactiveMongoConfiguration class ını extend etmesini istiyoruz;

```java
@Configuration
public class MongoConfig extends AbstractReactiveMongoConfiguration {
    // need to update the database name for the application
    @Override
    protected String getDatabaseName() {
        return "sfg";
    }
    // creates mongo client bean that runs reactive programming manner
    @Bean
    MongoClient mongoClient() {
        return MongoClients.create();
    }

    // to authorize the application with mongo db credential, need to override that
    // method
    @Override
    protected void configureClientSettings(Builder builder) {
        // root/example are user/password for used mongo db docker container
        // admin is general database name
        MongoCredential credential = MongoCredential.createCredential("root", "admin", "example".toCharArray());

        // need to initialize cluster settings for mongo db docker container
        // 127.0.0.1 is the mongo docker container access url and 27017 is the port 
        builder.credential(credential).applyToClusterSettings(settings -> {
            settings.hosts(Collections.singletonList(new ServerAddress("127.0.0.1", 27017)));
        });
    }
}
```

Bu class aracılığıyla MongoDB configuration ını yapacağız.

Bu class ı extend ettiğimzde *getDatabaseName* metodunu override etmemezi isityor. Yapıyoruz. Bu configuration ile application ımızın kullancağı database name ını veriyoruz.

MongoDB nin bu library sinde default olarak non-reactive client ı çalıştırılıyor. Bunu override etmek için ise mongoClient i kendimiz oluşturmalıyız. Bunun için mongoClient method unda client ımızı oluşturuyoruz;

docker container daki mongo db yi kullanmasını istiyoruz, bunun için de hem url hem de auth configuration larını yapmamız lazım.
bunun için de configureClientSettings methodunu override ediyoruz.

container a bağlanmak için kullanılan credential ları veriyoruz.

sonra da cluster bilgisini tanımlıyoruz.

Bu kadar.

Şimdi configuration tamamdır, beer repository imizi kuralım.

MongoDB deki bir kısıtlama id nin string olmasıdır.

Onun için ilk olarak domain class ımızdaki id property sini String yapıyoruz. Sonrasında class ın bir mongo db entity si olduğunu belirtmek için Document annotation ını kullanıyoruz. ve içindeki id property sine id annotation ı ile işaretliyoruz.

```java
package com.rcelik.springguru.reactivemongodb.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document
public class Beer {
    @Id
    private String id;
    private String beerName;
    private String beerStyle;
    private String upc;
    private Integer quantitiyOnHand;
    private BigDecimal price;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
```

Sonrasında repository class ını oluşturuyoruz. Bu non-reactive ile aynı mantık sadece kullandığımız interface farklı;

```java
@Repository
public interface BeerRepository extends ReactiveMongoRepository<Beer, String> {
}
```

Bu durumda işlem tamamdır.

### Service Methodunu Yazalım

Service methodumuz saveBeer olsun. Bunda database e bir beer save edelim.

Test class ında başlarken önce bu testing Spring testi olacağını belirtelim. Bu *@SpringBootTest* annotaion ile bu service in spring context içinde çalışacağını vb. birçok şeyi belirtiyoruz.

Sonra kullanacağımız injection ları belirtelim;

```java
    @Autowired
    private BeerService beerService;

    @Autowired
    private BeerMapper beerMapper;
```

Bunları Spring context inden getirecek ve class a inject edecek. Mockito kullanmıoyruz. Bir integration testi yapıyoruz şu anda.

Şimdi de bir beer object i oluşturalım ve herbir test için bu object yeniden oluşturulsun;

```java
    private BeerDTO beerDTO;
    
    @BeforeEach
    void setUp(){
        beerDTO = beerMapper.beerToBeerDTO(generateTestBeer());
    }
```

Bu işlemlerden sonra da save method umuzun testini yazalım;

```java
    @Test
    void testSaveBeer() throws InterruptedException {
        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        savedMono.subscribe(
                savedDto -> System.out.println("saved beer id: %s".formatted(savedDto.getId())));

        TimeUnit.SECONDS.sleep(10);
    }
```

Burada sleep koymamız gerekiyor çünkü, test işlemden önce bitiyor. Reactive programlama da tam olarak bu işe yarıyor. Yani işlemleri başka bir thread de koşturduğu için, test bitiyor ama arkada işlem devam ediyor. İşlemin doğru çalışıp çalışmadığını görmek için main thread i biraz uyutmak gerekiyor.

Arka planda yapılan işleme bakalım;

```java
    @Override
    public Mono<BeerDTO> saveBeer(Mono<BeerDTO> beer) {
        return beer.map(beerMapper::beerDTOToBeer)
                .flatMap(beerRepository::save)
                .map(beerMapper::beerToBeerDTO);
    }
```

Burada yapılan işlemlerin sırası çok önemlidir;
ilk işlem Mono olarak gelen beerDto nun map lenmesi dir. Bu şekilde async olarak işlem devam edebiliyor.
`beer.map(beerMapper::beerDTOToBeer)`
ikinci işlemde ise map işleminden dönen 'Beer' object inin wrap bir producer ile wrap lenmesi ve save edilemesi işi var. sonrasında ise `Mono<Beer>` object i dönüyor.
üçüncü işlem olarak da '.map(beerMapper::beerToBeerDTO)' işlemi ile async olarak bir beer object i beerDto ya çeviriliyor.

Testimiz koştuğunda bir integration testi olduğundan database e bir kayıt atmalıdır.
Bunu da mongo-express den görebilirsin.

#### Test Thread ini Uyutmanın Yerine Await Yapısını Kullanma

bir önceki test de işlemin sonucunu görmek için ana thread i uyutmuştuk. ama tam olarak ne kadar uyutacağımızı bilemeyiz.
verimli veya verimsiz olabilir.
Bunun daha iyi bir çözümü var. java await library sini kullanmak. Bu yapıda bir object kullanarak işlem bittiğinden haberdar olabiliyoruz.
öncesinde library i ekleyelim;

```json
...
ext {
 ...
 awaitilityVersion = "4.2.0"
}

...
 testImplementation "org.awaitility:awaitility:${awaitilityVersion}"
...
```

Test dependency olarak ekliyoruz. Sonrasında ise kod olarak bir atomicBoolean oluşturup bekliyoruz;

```java
    @Test
    void testSaveBeer() {
        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        AtomicBoolean finished = new AtomicBoolean(false);

        savedMono.subscribe(
                savedDto -> {
                    System.out.println("saved beer id: %s".formatted(savedDto.getId()));
                    finished.set(true);
                });

        Awaitility.await().untilTrue(finished);
    }
```

It is very clear.

### Test etme yöntemleri Blocking veya Subscriber

```java
    @Test
    @DisplayName("Test saveBeer using subsciber")
    void testSaveBeer() {
        Mono<BeerDTO> savedMono = beerService.saveBeer(Mono.just(beerDTO));

        AtomicBoolean finished = new AtomicBoolean(false);
        // we use AtomicReference here to check everything is fine.
        // for example if there exists an error, that passes atomicBoolean to be set as true but it does not succeed
        AtomicReference<BeerDTO> atomicDto = new AtomicReference<BeerDTO>();

        savedMono.subscribe(
                savedDto -> {
                    System.out.println("saved beer id: %s".formatted(savedDto.getId()));
                    finished.set(true);
                    atomicDto.set(savedDto);
                });

        Awaitility.await().untilTrue(finished);

        BeerDTO savedBeer = atomicDto.get();
        assertNotNull(savedBeer, "beer should not be null");
        assertNotNull(savedBeer.getId(), "saved object should have not null id");
    }

    @Test
    @DisplayName("Test saveBeer using block")
    void testSaveBeerWithBlock(){
        // this way does same thing with subscriber but it has less code
        BeerDTO savedBeer = beerService.saveBeer(Mono.just(beerDTO)).block();
        assertNotNull(savedBeer, "beer should not be null");
        assertNotNull(savedBeer.getId(), "saved object should have not null id");
        // in normal code we should not use block
    }
```

Blocking i de kullanabiliriz. Daha az kod yazılmasını sağlıyor. Subscriber da ise AtomicReference ı kullanmalıyız. Çünkü bir exception atıldığında bundan haberdar olmamız lazım.

Update beer method and explanation:

```java
    @Override
    public Mono<BeerDTO> updateBeer(String id, BeerDTO beerDTO) {
        return beerRepository.findById(id)
                .map(foundBeer -> {
                    foundBeer.setBeerName(beerDTO.getBeerName());
                    foundBeer.setBeerStyle(beerDTO.getBeerStyle());
                    foundBeer.setPrice(beerDTO.getPrice());
                    foundBeer.setQuantitiyOnHand(beerDTO.getQuantitiyOnHand());
                    foundBeer.setUpc(beerDTO.getUpc());
                    return foundBeer;
                }) // updating found beer with new one
                .flatMap(beerRepository::save) // wrapping Beer with Mono then saving it to database
                .map(beerMapper::beerToBeerDTO); // mapping Mono<Beer> to Mono<BeerDTO>
    }
```

Its tests:

```java
    @Test
    @DisplayName("updateBeer with blocking manner")
    void testUpdateBeerWithBlockingManner() {

        final String newName = "new beer name";
        BeerDTO savedBeerDTO = getSavedBeerDTO();
        savedBeerDTO.setBeerName(newName);

        BeerDTO updatedBeerDTO = beerService.updateBeer(savedBeerDTO.getId(), savedBeerDTO).block();
        assertEquals(savedBeerDTO.getId(), updatedBeerDTO.getId(), "beer id should not be changed");

        BeerDTO fetchBeerDTO = beerService.getBeer(savedBeerDTO.getId()).block();
        assertEquals(fetchBeerDTO.getBeerName(), newName, "beer name should be updated");
    }

    @Test
    @DisplayName("updateBeer with subscriber way")
    void testUpdateBeerWithSubscriber() {
        final String newName = "new beer name subscriber way";
        BeerDTO savedBeerDTO = getSavedBeerDTO();
        savedBeerDTO.setBeerName(newName);

        AtomicReference<BeerDTO> updatedBeerReference = new AtomicReference<BeerDTO>();
        beerService.updateBeer(savedBeerDTO.getId(), savedBeerDTO).subscribe(
                updatedBeerDto -> {
                    updatedBeerReference.set(updatedBeerDto);
                });

        // saying that wait until updating object is finished
        Awaitility.await().until(() -> updatedBeerReference.get() != null);
        assertEquals(newName, updatedBeerReference.get().getBeerName());
    }
```

Burada iki uygulamayı da deneyebiliriz. Subscriber şeklinde biraz değişiklik yaptım sadece. Buradaki subscriber da AtomicBoolean ı kullanmadım çünkü AtomicReference kendi başına yeterli geliyor. Aslına bakılırsa AtomicBoolean ın yaptığı işlem ile aynı şeyi AtomicReference da da yaptığımızdan birisini kullanmak yeterli geliyor.

Test ederken dikkat etmemiz gereken bir şey de, update edilen record un database den tekrardan alınmasıdır. Çünkü o şekilde database in tam olarak doğru çalışıp çalışmadığını test etmiş oluruz.

Delete beer tesinin kodu ve testi

```java
    @Override
    public Mono<Void> deleteBeer(String id) {
        return beerRepository.deleteById(id);
    }
```

```java
    @Test
    @DisplayName("deleteBeer with subscriber manner")
    void testDeleteBeerWithSubscriberManner() {
        BeerDTO savedDto = getSavedBeerDTO();

        AtomicBoolean finished = new AtomicBoolean(false);
        String id = savedDto.getId();

        // needed to be deleted from database
        beerService.deleteBeer(id).then(Mono.fromRunnable( () -> finished.set(true))).subscribe();

        Awaitility.await().untilTrue(finished);

        BeerDTO foundBeer = beerService.getBeer(id).block();
        assertNull(foundBeer, "beer should be deleted");
    }
```

burada biraz tricky şeyler ekledik. Mono olarak yeni bir entry ekledim. Çünkü Mono<Void> a subscribe olduğumda herhangi bişey return etmediğinden AtomicBoolean ı true ya setleyemiyordum. DeleteBeer işlemi bittikten sonra then kısmı ile işlemin bittiğini söyleyebildim.

Block kullanılması daha kolay.

### Spring Data Kullanarak Database e Query Edilmesi

Spring Data nın bize verdiği query özelliği reactive MongoDB için de kullanılabilir. Genel olarak aynı özellikleri destekliyor. Ama yine de dokümantasyona bakmak gerekir. (Spring Data Mongo documentation)

Örneğin, database deki beerName e göre gelen ilk record u dönmek istersek, repository class ına şu methodu yazmak gerek;

```java
    Mono<Beer> findFirstByBeerName(String beerName);
```

First keyword ü bulunan ilk record u dönecek anlamına geliyor.
By keyword ünden sonra da hangi property ye göre arama yapıcak anlamına geliyor.

bunun implementation ı da şu şekilde;

```java
    @Override
    public Mono<BeerDTO> findFirstBeerByName(String beerName) {
        return beerRepository.findFirstByBeerName(beerName).map(beerMapper::beerToBeerDTO);
    }
```

Bunun da testlerine bakalım hemen;

```java
    @Test
    @DisplayName("should return first beer whose name is given")
    void testFindFirstBeerByNameWithSubscriberWay() {
        BeerDTO savedBeer = getSavedBeerDTO();
        String savedBeerName = savedBeer.getBeerName();

        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<BeerDTO> atomicReference = new AtomicReference<BeerDTO>();

        beerService.findFirstBeerByName(savedBeerName).subscribe(
                foundBeer -> {
                    finished.set(true);
                    atomicReference.set(foundBeer);
                });

        Awaitility.await().untilTrue(finished);
        BeerDTO foundBeer = atomicReference.get();

        assertNotNull(foundBeer, "beer should be found");
        assertEquals(savedBeerName, foundBeer.getBeerName(), "beer name should be same as requested name");
    }

    @Test
    @DisplayName("should return first beer whose name is given")
    void testFindFirstBeerByNameWithBlocingWay() {
        BeerDTO savedBeer = getSavedBeerDTO();
        String savedBeerName = savedBeer.getBeerName();

        BeerDTO foundBeer = beerService.findFirstBeerByName(savedBeerName).block();

        assertNotNull(foundBeer, "beer should be found");
        assertEquals(savedBeerName, foundBeer.getBeerName(), "beer name should be same as requested name");
    }
```

Eğer query miz birden fazla record dönecekse, Producer olarak Flux kullanacağız ve findFirst keyword ü yerine find keyword ünü kullanacağız;

```java
    Flux<Beer> findByBeerStyle(String beerStyle);
```

Bunun service kodu da şöyle olmalı;

```java
    @Override
    public Flux<BeerDTO> findAllByBeerStyle(String beerStyle) {
        return beerRepository.findByBeerStyle(beerStyle)
                .map(beerMapper::beerToBeerDTO);
    }
```

Bayağı düz mantık.

Şimdi de testini yazalım;

### Database e Başlangıçta Data Eklenmesi

DB initialization da data ekleyebiliriz. Veya var olan dataları silebiliriz.

Bunun için daha önce de kullandığımız **CommandLineRunner** interface ini kullanacağız. Bu durumda, spring context initialize olurken bu interface i implement eden class içerisindeki run method u call edilir. Bu method içerisinde de yapmak istediğimiz işlemleri yapabiliriz.

Bu metod içerisine önce eski dataları silmek için;

deleteAll() method unu kullanalım. Delete işlemi bittikten sonra da initial datayı load edelim. Delete işleminin bitmesini beklemek için doOnSuccess() method unu kullanıyoruz. En son subscribe() metodunu eklemeyi unutmayalım ki bu kod çalışsın. Subscriber olmasa Producer çalışmaz.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class BootStrapData implements CommandLineRunner {

    private final BeerRepository beerRepository;

    @Override
    public void run(String... args) throws Exception {
        beerRepository.deleteAll().doOnSuccess(success -> {
            loadInitialBeerData();
            log.info("Old data is removed");
        }).subscribe();
    }

    private void loadInitialBeerData() {
        Beer beer1 = Beer.builder()
                .beerName("Space Dust")
                .beerStyle("IPA")
                .price(BigDecimal.TEN)
                .quantitiyOnHand(12)
                .upc("12121213")
                .build();
        Beer beer2 = Beer.builder()
                .beerName("Efes")
                .beerStyle("IPA")
                .price(new BigDecimal("10.99"))
                .quantitiyOnHand(132)
                .upc("12121213")
                .build();
        Beer beer3 = Beer.builder()
                .beerName("Sunshine City")
                .beerStyle("IPA")
                .price(new BigDecimal("13.99"))
                .quantitiyOnHand(144)
                .upc("12121213")
                .build();

        beerRepository.save(beer1).subscribe();
        beerRepository.save(beer2).subscribe();
        beerRepository.save(beer3).subscribe();
    }
}

```

## Spring WebFlux.fn

Spring MVC ile rest controller larımızı yazarak reactive programlama yapabiliriz. Ama Spring WebFlux.fn kullanarak daha reactive programlamaya uygun bir still ile de aynı işi yapabiliriz. Bu kısımda Spring WebFlux.fn kullanrak rest controller larımızı hazırlayacağız.

Burada rest controller ismi yerine, bizim router olarımız olacak. router lar request leri karşılayacak ve doğru handler lara kodu gönderecekler. Yani kabaca, rest controller ların yerine router lar ve handler lar olacak. Router lar configuration işini yapacak handler lar ise doğru service leri çağıracaklar. Spring açısından bir farklılık yok yani controller larda router larda bir singleton olarak çalışıyorlar. Yani spring context e eklenecek bir bean olacaklar. Ama bizim isimlendirmemiz biraz değişecek ve kodlama stilimiz biraz güncellenecek o kadar.

### Handler ve Router Oluşturmak

Öncelikle Handler ve Router larımızı koymak için bir package oluşturmamız lazım. Bu package in ismi ise best practice lere göre *..web.fn* olmalıdır. Önce bu package i oluşturalım ve içerisine ilk olarak BeerHandler class ını oluşturalım.

BeerHandler class ı Spring context ine koyulan bir bean olması için @Component annotation ını class a ekleyelim.
Bu class ın amacı, router dan gelen request lerden service için gerekli olan bilgileri alarak service methodlarını çağırmaktır. Ayrıca dönecek response un da nasıl olacağına karar vermektir.

Bu handler in ilk görevi tüm beer ları dönmek olsun. Beer ları service class ından alacak ama gelen request ve dönecek response u burası ayarlamalı;

```java
@Component
@RequiredArgsConstructor
public class BeerHandler {
    private final BeerService beerService;

    public Mono<ServerResponse> listBeers(ServerRequest request) {
        // it returns all elements on a ServerResponse object
        return ServerResponse.ok().body(beerService.listBeers(), BeerDTO.class);
    }
}
```

Burada görüldüğü gibi methodumuz bir **Mono<ServerResponse>** objesi dönüyor. Bunun return kodu da 200, ve body sinde service den gelen object var.
ServerResponse ve ServerRequest objelerine dikkat etmek gerekiyor, bu class in package ı *org.springframework.web.reactive.function.server*.

Şimdi endpoint leri ayarlayalım. Aynı package içerisine BeerRouter class ı oluşturalım. Bu class bir configuration class ıdır. Ve Spring context in bunu anlaması için @Configuration annotation ını class in başına ekleyelim. Bu class in içerisine endpoint path lerini static olarak verebiliriz. Ayrıca route işlemini yapması için RouterFunction bean ini oluşturalım;

```java
@Configuration
@RequiredArgsConstructor
public class BeerRouter {
    /**
     * endpoints paths
     */
    public static final String BEER_PATH = "/api/v3/beers";
    public static final String BEER_ID = BEER_PATH + "/{beerId}";


    private final BeerHandler beerHandler;

    /**
     * creates a RouterFunction bean that holds configuration for beer endpoints
    */
    @Bean
    RouterFunction<ServerResponse> beerRoutes() {
        return RouterFunctions.route()
                .GET(BEER_PATH, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::listBeers)
                .build();
    }
}
```

Aslına bakılırsa çok açık bir kod. Burada client in bize request in Accept header ını application/json olarak göndermesi gerektiğini söylüyoruz. Yoksa hata verir kod. Generic olan */* da gönderse olur.

Şimdi bunun Integration testini yazalım.

```java
@SpringBootTest // used to initialize Spring Context and run that tests on it
@AutoConfigureWebTestClient // configuring WebTestClient so that it can be accessible on that class
public class BeerEndpointTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("All beers should be returned")
    void testListBeers() {
        webTestClient.get().uri(BeerRouter.BEER_PATH)
                .accept(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.size()", 3);
    }
}
```

İlk olarak bu testin integration test olması için end-to-end test etmesini istiyoruz.
Bunun için Spring context inin oluşturulması ve ayağa kaldırılması lazım. Bu yüzden şu annotation ını kullanıyoruz. @SpringBootTest
Sonra WebTestClient objesinin ayarlanıp Spring context e eklenmesi için @AutoConfigureWebTestClient annotation ını kullanıyoruz.
Configure edilen WebTestClient bean ını test class ımıza inject edip kullanıyoruz.

Gerisi eski konular.

### Path Variable Nasıl Alınır

Şimdi id verilerek bu id ye sahip beer datasını dönen kodu yazalım.

Request objesinden beerId yi handler class ında alacağız. Çünkü handler class ının yaptığı iş buydu.

```java
    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        return ServerResponse.ok()
                .body(
                        beerService.getBeer(request.pathVariable("beerId")),
                        BeerDTO.class);
    }
```

Bunun router kısmı da şöyle olacak;

```java
    @Bean
    RouterFunction<ServerResponse> beerRoutes() {
        return RouterFunctions.route()
                .GET(BEER_PATH, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::listBeers)
                .GET(BEER_ID, RequestPredicates.accept(MediaType.APPLICATION_JSON), beerHandler::getBeerById)
                .build();
    }
}
```

İlk kısımda kullanacağıımız path ı veriyoruz ".../beers/{beerId}", sonra da user ın json tipi için request edebileceğini söylüyoruz. Sonra da handler metodumuzu çağırıyoruz.

### Yeni bir record Nasıl oluşturulur?

Yeni bir record oluşutrumak için request den body alıp, onu BeerDTO ya dönüştürmek lazım. Sonrasında service methoduna bu objeyi vereceğiz. Sonra da boş bir Server response ve içerisinde location header ını koyacağız.

```java
    public Mono<ServerResponse> createNewBeer(ServerRequest request) {
        return beerService.saveBeer(request.bodyToMono(BeerDTO.class))
                .flatMap(beerDTO -> ServerResponse
                                        .created(UriComponentsBuilder.fromPath(BeerRouterConfig.BEER_ID).build(beerDTO.getId()))
                                    .build());
    }
```

Return ederken bu kere ServerResponse.ok gibi başlamıyoruz. Return işlemini flatMap ile objeyi transform ederek istediğimiz tipe güncelleyeceğiz.
İlk işimiz Mono<BeerDTO> dönen service metodumuzu return call ediyoruz. İçine de Mono<BeerDTO> ile request den aldığımız body yi veriyoruz;
**beerService.saveBeer(request.bodyToMono(BeerDTO.class))**
sonra bu işlem database işlemini yapacak, gelen objeyi transform etmeliyiz ki Mono<BeerDTO> dan Mono<ServerResponse> return edebilelim. Bu işlem için de flatMap ı kullanıyoruz.

Bu işlemden sonra bunun integration testini yazalım;

```java
    @Test
    @DisplayName("Should create new beer")
    void testCreateBeer() {
        BeerDTO beerDto = BeerServiceImplTest.genereateTestBeerDTO();
        webTestClient.post()
                        .uri(BeerRouterConfig.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(beerDto), BeerDTO.class)
                        .exchange()
                        .expectStatus().isCreated()
                        .expectHeader().exists(HttpHeaders.LOCATION);
    }
```

Bu işlemden sonra getBeerById nin testini yazalım;

```java
    @Test
    @DisplayName("Should get created beer")
    void testGetBeerById() {
        BeerDTO beerDto = BeerServiceImplTest.genereateTestBeerDTO();

        webTestClient.post()
                        .uri(BeerRouterConfig.BEER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Mono.just(beerDto), BeerDTO.class)
                        .exchange().expectHeader().value(HttpHeaders.LOCATION, locationHeaderValue -> {
                            String beerId = locationHeaderValue.substring(locationHeaderValue.lastIndexOf("/") + 1);
                            webTestClient.get()
                                            .uri(BeerRouterConfig.BEER_ID, beerId)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .exchange()
                                            .expectStatus().isOk()
                                            .expectBody().jsonPath("$.id", beerDto.getId());
                        });
    }
```

Buradaki işlem biraz daha karmaşık. Öncelikle elimizde bir id yok. Bu yüzden ilk bir id si elimizde olan bir beer objesi yaratalım.
Sonra bu objeyi id si üzerinden getBeerById endpoint ini çağıralım..

Buradaki ince nokta, **exchange().expectHeader().value(HttpHeaders.LOCATION, consumer)** kısmı. Consumer içerisinde diğer functional call umuzu yapabiliriz.

### Update i yapalım

```java
    public Mono<ServerResponse> updateBeer(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .flatMap(beerDTO -> {
                    return beerService.updateBeer(request.pathVariable("beedId"), beerDTO);
                }).flatMap(savedDto -> {
                    return ServerResponse.noContent().build();
                });
    }
```

Buradaki server api miz ilk parametre olarak String ve BeerDTO class objesini istiyor. Bu yüzden ilk olarak body den BeerDto objesini almamız lazım.
bodyToMono yu kullanarak başlıyoruz ve sonra path den de beer id yi alıyoruz. ve service methodunu çağırabiliyoruz. Bundan sonra Mono<ServerResponse> objesi dönebilmek için flatMap ile response u oluşturarak dönüyoruz.

### Patch İşlemi

Patch de update ile aynı işlemleri yapıyoruz.

```java
    public Mono<ServerResponse> patchBeerById(ServerRequest request) {
        return request.bodyToMono(BeerDTO.class)
                .flatMap(requestedDto -> beerService.patchBeer(request.pathVariable("beerId"), requestedDto))
                .flatMap(patchedDto -> ServerResponse.noContent().build());
    }
```

Öncelikli olarak request i mono dan dto object ine döndürüyoruz. Sonra sınra patch metodunu call ediyoruz. Onun return ettiği değer de Mono<BeerDTO> tipindeki obje üzerinden de Mono<ServerResponse> objesi oluşurup return ediyoruz.

### Delete İşlemi

Benzer işlemi yapıyoruz. Burada farklı olan **Mono<Void> deleteBeer(String id)** metodu Mono<Void> dönüyor. Burada map gibi işlemler kullanamıyoruz. .then(returnValue) yapıyoruz.

```java
    public Mono<ServerResponse> deleteById(ServerRequest request) {
        return beerService.deleteBeer(request.pathVariable("beerId"))
                .then(ServerResponse.noContent().build());
    }
```

Bu kadar.

### Error Code Olarak Not Found u Kullanma

getById için;

```java
    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        return beerService.getBeer(request.pathVariable("beerId"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(beerDto -> ServerResponse.ok().bodyValue(beerDto));
    }
```

Ya da şöyle yapılabilir;

```java
        return ServerResponse.ok().body(
                beerService.getBeer(request.pathVariable("beerId"))
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))),
                BeerDTO.class);
```

Diğerlerini de service lerde bakabilirsin.

### Query Parametresi Almak

Bir request den nasıl query parametresini alabiliriz ona bakalım şimdi.

Request den query parametresi alınması işlemi de handler class ında olmalıdır. Çünkü http request inden bilgi alınması orada olmalıdır.

Query parametreleri genelde optional dır. Onun için kodu hazırlayalım;


```java
    public Mono<ServerResponse> listBeers(ServerRequest request) {
        Flux<BeerDTO> result;

        if (request.queryParam("beerStyle").isPresent()) {
            result = beerService.findAllByBeerStyle(request.queryParam("beerStyle").get());
        } else {
            result = beerService.listBeers();
        }

        return ServerResponse.ok().body(result, BeerDTO.class);
    }
````

