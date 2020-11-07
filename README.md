# TemaSI
made with love by C Teodor

# Demo + HowItWorks
<a href="http://www.youtube.com/watch?feature=player_embedded&v=4OPvebWucDs
" target="_blank"><img src="http://img.youtube.com/vi/4OPvebWucDs/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>

# HowToUse

Pentru a rula programul, trebuie sa rulezi cele trei clase: KeyManager (KM), ClientHttp (A), ClientHttpB (B). 

La rularea fiecaruia, userul va trebui initializa anumite componente ale clasei. Initializarea ar trebui sa se faca in ordinea KM -> A -> B pentru ca programul sa ruleze corespunzator.

# HowItWorks

Fiecare nod (KM, A, B) este un server HTTP. Nodul KM va avea un port si ip prestabilit, pe cand celelalte doua noduri vor fi initializate de catre user. 

Nodul KM este initializat cu cheia K3, prin care va cripta si decripta mesaje catre si de la cei doi clienti. 

Cele doua noduri sunt initializate cu portul la care vor avea server, cheia care trebuie sa corespunda cu cea a nodului K3, modul de criptare dorit.

Dupa ce nodurile si A si B ruleaza si au trimis informatiile necesare (necriptate), KM va trimite fiecarui client cate un mesaj criptat ECB cu modul de criptare prin care va trebui sa comunice cei doi clienti, adresa clientului cu care trebuie sa comunice, cheia cu care vor cripta/decripta si iv-ul, in cazul in care modul ales este OFB. Raspunsul primit de la cei doi clienti este encriptat in modul de criptare ales, cu cheia K3 si eventual iv-ul initializat de catre KM. Restul de comunicare intre KM si A, B se va face in modul ECB.

Urmeaza ca KM sa trimita un mesaj de start, iar A va incepe sa cripteze si sa trimita 10 blocuri de text dintr-un fisier prestabilit. Dupa ce trimite cele 10 blocuri, KM va fi semnalat de catre A, iar la primirea celor 10 blocuri, B va trimite si el un mesaj. Cand ambele replici au fost primite, KM va trimite un mesaj ambilor clienti pentru a continua transferul.

La final, nodul A trimite lui KM un mesaj de terminare. Iar KM va afisa ca transferul s-a terminat.

# TheCryptoPart

Pentru criptare/decriptare am creat o clasa AES. Aceasta are functiile statice encryptECB, encryptOFB si decryptECB, decryptOFB care au fost folosite in comunicarea intre noduri. Acestea primesc mesaje, chei si iv toate sub forma de string.

Pentru criptare, atat EFB cat si OFB se folosesc de functie encryptBlock care primeste un sir de bytes si-i cripteaza folosind algoritmul AES/ECB/NoPadding. Pentru implementarea modurilor, m-am asigurat ca encryptBlock primeste doar siruri de bytes de lungime 16, adica 128 de biti. In mod similar exista si functia decryptBlock

# ECB 
La primirea unui mesaj, algoritmul se asigura ca acesta are lungime multiplu de 16, in caz contrar, se adauga mesajului atati \0 pana cand mesajul va fi multiplu de 16. Din acest moment, la fiecare 16 bytes criptam si ii stocam intr-un nou sir de bytes. La sfarsit, returnam encodarea Base64 a bitilor criptati ai mesajului.
Decriptarea functioneaza in mod similar. Primeste ca mesaj o codare Base64, o decodeaza, returneaza null daca decodarea nu este multiplu de 16. Altfel, ia fiecare 16 bytes, ii decripteaza, stocheaza intr-un sir de bytes, returneaza stringul asociat acelui sir.

# OFB

In mod similar, OFB padeaza mesajul primit cu \0 pana lungimea mesajului este multiplu de 16. Face XOR intre primul bloc si vectorul de initializare criptat cu cheia primita si stocheaza rezultatul intr-un nou sir de bytes. La pasul doi, se va cripta rezultatul criptarii de la primul pas, si se va face XOR cu al doilea block de mesaj, s.a.m.d.
Decriptarea functioneaza in mod similar.





