# Peterwolf's Groundworks Loader (Minecraft 26.3 Fabric)

Ciężka przemysłowa ładowarka kołowa przygotowana dla silnika materiałów sypkich **Peterwolf's Groundworks** na wersję **Minecraft 26.3 (Wilderness Bound)**.

## 🚜 Wygląd i Model 3D

Model ładowarki został zaprojektowany z dbałością o detale na podstawie referencyjnych ciężkich ładowarek kołowych (CAT / Volvo / SDLG):
- **4 potężne koła budowlane**: szerokie opony z głębokimi klockami bieżnika w jodełkę, żółte felgi stalowe oraz wystające piasty przekładni planetarnych z wieńcem śrub. Koła obracają się płynnie podczas jazdy.
- **Skręt przegubowy (Articulated Steering)**: rama ładowarki łamie się w centralnym przegubie za pomocą bocznych siłowników hydraulicznych.
- **Przedział silnikowy i przeciwwaga**: żółta skośna pokrywa silnika z bocznymi żaluzjami nawiewu, tylny grill chłodnicy, masywna żeliwna przeciwwaga z zagłębionymi światłami, pionowa rura wydechowa z klapką przeciwdeszczową oraz cyklonowy filtr powietrza.
- **Kabina ROPS/FOPS**: panoramiczne przyciemniane szyby bezpieczne, fotel operatora z podłokietnikami, kolumna kierownicy, joysticki hydrauliczne, konsola wskaźników, boczne drabinki wejściowe, żółte barierki ochronne, lusterka zewnętrzne i obrotowy kogut ostrzegawczy na dachu.
- **Wysięgnik i kinematyka Z-Bar**: podwójne wygięte ramiona podnoszące z poprzecznicą wzmacniającą, siłowniki podnoszenia oraz centralna dźwignia kołyskowa Z-bar z siłownikiem wywrotu i łącznikiem łyżki.
- **Ciężka łyżka załadunkowa**: szerokość 2.8m (zakrywa ślad kół), profil łyżki z 6 kutymi zębami krawędzi natarcia, noże boczne, płyty ślizgowe od spodu, daszek przeciwsypowy i dynamiczna trójwymiarowa warstwa nabieranego urobku wewnątrz misy.

---

## 🎮 Sterowanie

| Klawisz | Funkcja |
|---|---|
| **W** | Jazda do przodu (napęd na 4 koła) |
| **S** | Hamowanie / Bieg wsteczny |
| **A** | Skręt w lewo (przegub ramy i kół) |
| **D** | Skręt w prawo (przegub ramy i kół) |
| **Strzałka w górę (↑)** | **Podnoszenie wysięgnika** (unoszenie łyżki do załadunku) |
| **Strzałka w dół (↓)** | **Obniżanie wysięgnika** (opuszczanie łyżki do poziomu gruntu) |
| **Strzałka w prawo (→)** | **Otwieranie / wysyp łyżki** (dumping urobku na hałdę lub wywrotkę) |
| **Strzałka w lewo (←)** | **Zamykanie łyżki** (curling / zamykanie łyżki do transportu) |
| **PPM na ładowarkę** | Wejście za kierownicę / jazda |
| **Shift + LPM / PPM (pusta)** | Podniesienie pojazdu w formie itemu do ekwipunku |

---

## 🧱 Integracja z Peterwolf's Groundworks

- **Nabieranie gruntu**: gdy łyżka jest opuszczona do poziomu gruntu lub zagłębiona, a ładowarka jedzie do przodu na hałdę ziemi, piasku lub żwiru, krawędź tnąca skrawa mikrowoksele i ładuje je do łyżki (pojemność do 768 jednostek = 1.5 bloku materiału sypkiego).
- **Transport**: zamknięta łyżka (Strzałka w lewo) bezpiecznie przewozi urobek bez gubienia materiału.
- **Wysyp grawitacyjny**: po przechyleniu łyżki w dół (Strzałka w prawo powyżej 18°) urobek wysypuje się strugą w dół na ziemię pod krawędzią łyżki z cząsteczkami i dźwiękiem sypania.
- **100% zachowanie objętości**: każda jednostka pobrana z gruntu trafia do łyżki, a każda jednostka wysypana trafia do terenu.

---

## 🛠️ Kompilacja i uruchomienie

Mod jest w pełni zgodny z Minecraft **26.3**, Java **25** i Fabric API **0.160.7+26.3**.

```bash
./gradlew test build
```
Zbudowany plik JAR znajduje się w `build/libs/pw_groundworks_loader-0.1.0+26.3.jar`.
