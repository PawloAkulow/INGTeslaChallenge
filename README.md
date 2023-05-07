This is a part of coding challenge by ING in Poland
https://www.ing.pl/pionteching#step1

Zadanie konkursowe

Konkurs trwa od 27 marca do 14 maja.
Zadanie polega na napisaniu kodu źródłowego programu i programów uruchamiających zoptymalizowanych pod kątem oszczędzania energii potrzebnej do ich prawidłowego działania.
W ramach zadania uczestnik musi rozwiązać 3 niezależne problemy: Serwis bankomatów, Transakcje płatnicze oraz Gra online – zapoznaj się z zadaniami.
Kod źródłowy programu musi zostać przygotowany w języku programowania Java i umieszczony w publicznym repozytorium kodu (serwis internetowy pozwalający na zdeponowanie kodu źródłowego zadania, wykorzystujący system kontroli wersji Git). Publiczne repozytorium powinno posiadać skonfigurowaną statyczną analizę bezpieczeństwa zdeponowanego kodu źródłowego SAST (Static Application Security Testing).
Programy uruchamiające czyli build.sh i run.sh zostaną przygotowane w języku skryptowym bash i zdeponowane w repozytorium. Uruchomienie build.sh doprowadzi do stworzenia programu wykonywalnego w oparciu o kod źródłowy dostępny w repozytorium kodu. Uruchomienie run.sh doprowadzi do uruchomienia procesu programu wykonywalnego w środowisku.
Środowisko będzie kreowane na platformie chmury publicznej Operatora Chmury Krajowej przy zadanych parametrach:  
Procesor – Intel(R) Xenon(R) CPU @ 2,00Ghz 
RAM – 3 GB
system operacyjny - Debian GNU/Linux 11 (bullseye)
dostępne oprogramowanie – openjdk 17, maven 3.6.3, gradle 8.0.1

Kryteria oceny zadania konkursowego

Uruchomiony program zwraca poprawne wyniki dla losowo wygenerowanych przypadków testowych.
Uczestnik udokumentował raportem przeprowadzonej analizy statycznej bezpieczeństwa zdeponowanego w repozytorium kodu (SAST) iż zdeponowany kod nie zawiera podatności.
Uruchomiony program będzie poddany testom przez godzinę przy zadanej częstotliwości żądań jakie musi wykonać – 10 żądań/sekundę. Maksymalny czas obsługi jednego przypadku testowego nie przekroczy 3 sekund.
Zadanie w trakcie uruchamiania testów uzyska najniższy czas obsługi dla 90-tego percentyla testów obciążeniowych którym zostanie poddane.
Najlepsze 10 zadań będzie przedmiotem oceny przez Kapitułę konkursu, która wyłoni zwycięzcę wybierając najbardziej poprawne inżyniersko rozwiązanie, bazując na swojej wiedzy eksperckiej oraz praktykom ‘software craftmanship’.
