---
name: dev
description: Use this skill whenever the user's message begins with "/dev". This is Karol's personal command for writing or editing Java code according to his own conventions - no inline comments, non-anemic domain models, design patterns without overengineering, SOLID, and modern Java 25+ features, with architectural reasoning explained in a dedicated section at the end. On explicit request it also writes unit tests (JUnit 5 + Mockito), integration tests (Testcontainers), and acceptance tests (Cucumber/Gherkin) in the same style. Also encodes context-specific architecture for his iFlash project (Saga, outbox, CQRS, Kafka, Spring Cloud Gateway). Do not apply these conventions to Java code requests that don't start with "/dev".
---

# /dev — styl kodowania Javy

Ten skill aktywuje się wyłącznie, gdy wiadomość zaczyna się od `/dev`. Poza tym kontekstem nie narzucaj tych zasad. Reprezentuje osobiste konwencje pisania kodu Java, które mają być stosowane konsekwentnie za każdym razem, gdy jest aktywny.

## Zasady ogólne

### 1. Brak komentarzy w kodzie
Nie dodawaj komentarzy (`//`, `/* */`, Javadoc) w generowanym lub edytowanym kodzie. Kod ma być czytelny sam w sobie dzięki dobremu nazewnictwu i małej odpowiedzialności metod/klas. Wyjaśnienia nie trafiają do kodu - idą do sekcji "Decyzje architektoniczne" na końcu odpowiedzi (patrz "Format odpowiedzi" niżej).

Wyjątek: Javadoc na publicznym API bibliotek/modułów współdzielonych, jeśli zostanie o to poproszony explicite.

### 2. Dopasuj się do istniejącego stylu
Zanim napiszesz kod, sprawdź dostępny kontekst - istniejące pliki, konwencje nazewnictwa, strukturę pakietów, styl formatowania. Trzymaj się tego, co już jest w projekcie, zamiast narzucać domyślny styl. Jeśli nie ma istniejącego kodu do naśladowania, użyj konwencji standardowych dla nowoczesnej Javy/Spring.

### 3. Rich domain model (non-anemic)
Preferuj bogate obiekty domenowe z zachowaniem, a nie same gettery/settery i logikę rozmytą w serwisach. Encje i value objecty powinny hermetyzować własne niezmienniki i logikę biznesową. Unikaj publicznych setterów tam, gdzie to możliwe - preferuj metody wyrażające intencję biznesową (np. `order.cancel()` zamiast `order.setStatus(CANCELLED)`).

### 4. Wzorce projektowe - bez overengineeringu
Stosuj wzorce projektowe tam, gdzie realnie rozwiązują problem (np. Strategy dla wymiennych algorytmów, Factory dla złożonej konstrukcji, Repository dla dostępu do danych, Builder dla obiektów z wieloma opcjonalnymi polami). Nie wprowadzaj wzorca "na zapas" ani żeby zademonstrować wiedzę. Jeśli prostszy kod bez wzorca jest równie czytelny i wystarczający, wybierz jego.

### 5. SOLID
Stosuj zasady SOLID (SRP, OCP, LSP, ISP, DIP) jako domyślne wytyczne projektowe, ale nie cytuj ich wprost w kodzie ani nie rób z nich ćwiczenia akademickiego - mają służyć czytelności i łatwości zmiany, a nie być celem samym w sobie.

### 6. Nowoczesna Java (25+)
Domyślnie sięgaj po nowoczesne funkcje języka tam, gdzie faktycznie pasują:
- **Records** dla DTO, value objects, niemutowalnych struktur danych
- **Sealed classes/interfaces** do modelowania zamkniętych hierarchii (np. stany domenowe, wyniki operacji)
- **Pattern matching w switch** (włącznie z record patterns) zamiast łańcuchów if-else/instanceof
- **Virtual threads** do zadań I/O-bound zamiast tradycyjnych puli wątków, gdzie ma to sens
- **Structured concurrency** zamiast ręcznego zarządzania ExecutorService, gdy pasuje do kontekstu
- **Text blocks** dla wieloliniowych stringów (SQL, JSON w testach)
- `var` tam, gdzie typ jest oczywisty z kontekstu, ale nie kosztem czytelności

Nie stosuj tych funkcji na siłę - priorytetem jest czytelność i zgodność ze stylem projektu.

## Testy (tylko na wyraźną prośbę)

Nie generuj testów automatycznie przy każdym `/dev` - tylko gdy wyraźnie o nie poproszę (np. "dodaj testy", "napisz testy jednostkowe/integracyjne/akceptacyjne"). Gdy poproszę, stosuj:

- **Testy jednostkowe**: JUnit 5 + Mockito. Nazwy metod testowych opisowe i czytelne (np. `shouldRejectOrderWhenBalanceInsufficient()`); struktura given-when-then wyrażona samą organizacją kodu, bez opisywania jej w komentarzach.
- **Testy integracyjne**: JUnit 5 + Testcontainers - realne zależności (baza danych, Kafka itd.) zamiast mockowania infrastruktury.
- **Testy akceptacyjne**: Cucumber/Gherkin - scenariusze w plikach `.feature`, z odpowiadającymi im step definitions w Javie.

Te same zasady co w kodzie produkcyjnym mają zastosowanie: brak komentarzy w kodzie testów, nowoczesna Java, brak overengineeringu (np. bez rozbudowanych fabryk testowych dla prostych przypadków).

## Format odpowiedzi

1. Kod - zgodny z powyższymi zasadami, bez komentarzy.
2. Na końcu odpowiedzi osobna sekcja **"Decyzje architektoniczne"**: krótkie, konkretne wyjaśnienie kluczowych wyborów - dlaczego taki wzorzec/struktura, jakie były realne alternatywy, dlaczego użyto danej funkcji Javy. Bez lania wody - tylko decyzje, które faktycznie wymagają uzasadnienia. Jeśli kod jest trywialny i nie ma nietrywialnych decyzji do wyjaśnienia, można tę sekcję pominąć lub napisać jedno zdanie.

## Kontekst projektu iFlash

Gdy zadanie dotyczy iFlash (symulator giełdy), korzystaj z poniższego kontekstu architektonicznego bez dopytywania o niego od nowa:
- Architektura mikroserwisowa: matching engine, order gateway, broker mock, settlement module, rate API - zorganizowane jako Git submodules w mono-repo
- Kafka jako event bus między procesami
- Outbox pattern do atomowej publikacji eventów
- API gateway: Spring Cloud Gateway
- PostgreSQL z wykorzystaniem logical replication
- Monitoring Kafki: Micrometer
- Saga pattern - zarówno choreografia, jak i orkiestracja (w trakcie implementacji)
- CQRS z podziałem port/adapter, cache: Redis + Caffeine

Jeśli zadanie dotyczy modułu lub warstwy nieopisanej powyżej, dopytaj o szczegóły zamiast zakładać.
