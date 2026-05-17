\documentclass[11pt, a4paper]{article}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage{lmodern}
\let\showhyphens\relax  % evitare warning di babel
\usepackage[italian]{babel}
\usepackage[margin=2.5cm, headheight=15pt]{geometry}
\usepackage{graphicx}
\usepackage{float}
\usepackage[hidelinks]{hyperref}
\usepackage{tabularx}
\usepackage{booktabs}
\usepackage{enumitem}
\usepackage{titlesec}
\usepackage[table]{xcolor}
\usepackage{amsmath}
\usepackage{listings}
\usepackage{fancyhdr}
\usepackage[most]{tcolorbox}
\usepackage{amssymb}
\usepackage{mathpazo}
\usepackage{microtype}
\usepackage[strings]{underscore}
\usepackage{fontawesome5}
\usepackage{pdflscape}
\usepackage{tikz}
\usetikzlibrary{trees, positioning}
\usepackage{makecell}
\renewcommand\theadfont{\bfseries}
\usepackage{needspace}   
\usepackage{placeins}    

% --- Gestione silenziosa dei warning ---
\usepackage{silence}
\WarningFilter{latex}{Command \showhyphens has changed.}
\usepackage{ragged2e}
\emergencystretch 3em 

% --- COLORI ---
\definecolor{primary}{RGB}{139, 0, 0}      % Rosso Primario (riferimento D1/D2) 
\definecolor{primarydark}{RGB}{100, 0, 0}  
\definecolor{secondary}{RGB}{70, 70, 70}    
\definecolor{tertiary}{RGB}{120, 120, 120}
\definecolor{boxbg}{RGB}{252, 252, 254}

\hypersetup{
    colorlinks=true,
    linkcolor=primary,
    filecolor=secondary,
    urlcolor=primary,
}

% --- CONFIGURAZIONE HEADER E FOOTER ---
\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\textcolor{secondary}{\footnotesize \leftmark}}
\fancyhead[R]{\textcolor{secondary}{\small Trento Smart Mountain \faMountain\  - D3}}
\fancyfoot[C]{\thepage}
\renewcommand{\headrulewidth}{0.4pt}

% --- FORMATTAZIONE TITOLI ---
\titleformat{\section}{\Large\bfseries\color{primary}}{\thesection}{1em}{}
\titleformat{\subsection}{\large\bfseries\color{primarydark}}{\thesubsection}{1em}{}
\titleformat{\subsubsection}{\normalsize\bfseries\color{secondary}}{\thesubsubsection}{1em}{}

% --- ABSTRACTBOX (stile allineato a D1/D2) ---
\newtcolorbox{abstractbox}{
    enhanced,
    colback=secondary!5,
    colframe=secondary!50,
    boxrule=0.5pt,
    leftrule=5pt,
    colbacktitle=secondary!10,
    coltitle=primary!80!black,
    fonttitle=\bfseries\Huge,
    title={ \faMountain\ \textbf{Abstract Sprint 1}},
    attach boxed title to top left={yshift=-3mm, xshift=8mm},
    boxed title style={
        boxrule=0.2pt,
        colback=secondary!10,
        sharp corners,
        arc=0pt,
        before skip=0pt,
        after skip=0pt
    },
    before upper={\renewcommand{\arraystretch}{1.2}},
    width=0.85\textwidth,
    left=10mm,
    right=10mm,
    top=5mm,
    bottom=5mm,
    drop fuzzy shadow=secondary!80,
    sharp corners,
    arc=0pt,
    borderline west={3pt}{0pt}{primary!80!black}
}

\begin{document}
\RaggedRight

% --- FRONTESPIZIO ---
\begin{titlepage} 
    \centering
    \vspace*{3cm}
    {\Huge \textbf{\textcolor{primary}{Trento Smart Mountain \faMountain\ }}}\\[1cm]
    {\LARGE Deliverable D3 - Running the Sprint!}\\[2cm]

    \begin{table}[ht!]
        \centering
        \large
        \renewcommand{\arraystretch}{1.5}
        \begin{tabular}{ll}
            \textbf{Gruppo:} & ID - 6 \\
            \textbf{Componenti:} & Federico Cattelan - 242111 \\
                                 & Marco Christian Stoica - 246443 \\
                                 & Giacomo Radin - 242907 \\
        \end{tabular}
    \end{table}

    \vfill

    \includegraphics[width=0.3\textwidth]{logo_unitn.png}
    
    \vspace{1.5cm} 
    
    {\large \textbf{Scadenza:} 17/05/2026}\\[1cm]
    {\normalsize Anno Accademico 2025/2026}
\end{titlepage}

% --- PAGINAABSTRACT ---
\newpage
\thispagestyle{empty}
\begin{center}
    \textit{``Le grandi cose non vengono fatte d'impulso, ma attraverso una serie di piccole cose combinate assieme.''} 
\end{center}
    \vspace{1cm}
    \vspace*{\fill}
    \begin{center}
    \begin{abstractbox}
        \large
        Il Deliverable D3 descrive l'adozione del framework Agile SCRUM per lo sviluppo di Trento Smart Mountain. In questo documento vengono formalizzate le dinamiche del team durante lo \textit{Sprint 1}, focalizzato sulla creazione delle fondamenta architetturali e dei servizi core di gestione utente e sicurezza. Vengono dettagliati il Product Backlog, la strategia di branching e i risultati dei meeting di review e retrospection, validando la fattibilità tecnica delle soluzioni offline-first e crittografiche delineate nelle fasi precedenti.
    \end{abstractbox}
    \end{center}
    \vspace*{\fill}
\begin{center}
    \textcolor{secondary}{\rule{0.3\textwidth}{0.4pt}}
    \vspace{0.3cm}
    \footnotesize\textcolor{secondary}{\\Trento Smart Mountain \faMountain\  — Running the Sprint!}
\end{center}

\newpage
\tableofcontents
\newpage
\
\section{Sezione Introduttiva}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item \textbf{Team Members:}
    \begin{itemize}[label=\textcolor{secondary}{$\circ$}]
        \item Federico Cattelan (Matricola: 242111) - Account GitHub: \href{https://github.com/federicocattelan}{@federicoca}
        \item Marco Christian Stoica (Matricola: 246443) - Account GitHub: \href{https://github.com/marcostoica}{@STUSSY-user}
        \item Giacomo Radin (Matricola: 242907) - Account GitHub: \href{https://github.com/giacomoradin}{@giacomoradin}
    \end{itemize}
    \item \textbf{Project Idea:} 
    Trento Smart Mountain è un ecosistema digitale innovativo (Offline-First) per l'ambiente montano. Integra sicurezza proattiva (rete ibrida 4G/BLE Mesh per inoltro SOS) e sostenibilità tramite gamification educativa e tracciamento ambientale, minimizzando la dipendenza da reti always-on e promuovendo l'Edge Computing nei rifugi alpini.
    \item \textbf{Links:}
    \begin{itemize}[label=\textcolor{secondary}{$\circ$}]
        \item \textbf{Repository GitHub:} \href{https://github.com/giacomoradin/Trento_Smart_Mountain}{https://github.com/giacomoradin/Trento\_Smart\_Mountain}
        \item \textbf{Apiary (Soggetta a versionamento):} \href{https://trentosmartmountain.docs.apiary.io}{https://trentosmartmountain.docs.apiary.io}
    \end{itemize}
\end{itemize}

\section{Sezione Generale}

\subsection{Strategia di Branching}
Il team ha adottato una strategia basata su \textit{Git Flow} semplificata, evitando rigorosamente la logica ”Master only strategy”:
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item \textbf{\texttt{main}:} Branch stabile, contiene esclusivamente codice verificato e funzionante per le release.
    \item \textbf{\texttt{develop}:} Branch di integrazione principale per lo sviluppo corrente.
    \item \textbf{\texttt{<Nome-feature>}:} Branch temporanei e isolati (es. \texttt{API-Meteo}) creati a partire da \texttt{develop} per lo sviluppo di nuove funzionalità.
    \item \textbf{\texttt{bugfix/<descrizione>}:} Branch dedicati alla risoluzione tempestiva di problemi individuati.
\end{itemize}

\subsection{Product Backlog}
Di seguito le storie principali del Product Backlog estratte dai file di tracciamento e stimate in termini di Importanza.

\begin{table}[H]
    \centering
    \rowcolors{2}{secondary!5}{white}
    \arrayrulecolor{gray!30}
    \begin{tabularx}{\textwidth}{>{\bfseries\color{primary!80!black}}c l X c}
        \thead{ID} & \thead{Attore} & \thead{User Story} & \thead{Importanza} \\
        \midrule
        3 & Tutti gli utenti & Registrazione account & 2 \\
        1 & Tutti gli utenti & Autenticazione al sistema & 1 \\
        42 & Sistema & Foreground Service per tracking continuo & 3 \\
        18 & Capogruppo & Gestione sessione escursione & 4 \\
        7 & Partecipante & Checklist equipaggiamento dinamica & 5 \\
        10 & Partecipante & Tracciamento GPS in background & 6 \\
        11 & Partecipante & Invio segnale SOS & 9 \\
        \bottomrule
    \end{tabularx}
\end{table}
\textit{Nota: Il file completo del Product Backlog è stato redatto in formato CSV, documentando per ogni entry il criterio ”How to Demo”.}

\subsection{Definizione di ”Done”}
Per dichiarare una User Story come completata (Done), il team ha formalizzato i seguenti criteri di accettazione:
\begin{enumerate}
    \item \textbf{Completamento funzionale:} Il comportamento descritto della user story è implementato ed è verificabile sul ramo di integrazione.
    \item \textbf{Qualità del Codice:} Il codice è completo di commenti ove necessario (logica non ovvia, scelte importanti, edge case) e segue lo stile deciso dal team.
    \item \textbf{Integrazione nel repository:} Il lavoro è integrato nel codice condiviso (merge nel branch di sviluppo), senza conflitti non risolti.
    \item \textbf{Build e stabilità minima:} Il progetto Android compila senza errori. Il Backend esegue i comandi \texttt{npm} previsti senza errori bloccanti.
    \item \textbf{Verifica manuale:} È stata eseguita una prova del flusso principale della story (peer-review interna). Eventuali bug evidenti invalidano lo stato di Done.
    \item \textbf{Tracciabilità:} La story è aggiornata nello strumento di tracciamento (Product/Sprint Backlog).
    \item \textbf{Documentazione leggera:} Eventuale aggiornamento dei file di setup concorde col team (es. \texttt{docs/setup\_mobile.md}).
\end{enumerate}

\newpage
\section{Sezione Sprint \#1}

\subsection{Goal}
Il goal dello Sprint \#1 è stabilire le fondamenta architetturali (Backend Node.js + MongoDB) e implementare i moduli core di gestione utente (Login/Registrazione). In parallelo, per l'infrastruttura Mobile (Android), l'obiettivo è configurare l'accesso offline tramite JWT e avviare il layer hardware critico (Foreground Service, generazione chiavi crittografiche ECC).

\subsection{Sprint Planning (Sprint Backlog)}

\begin{table}[H]
    \centering
    \rowcolors{2}{secondary!5}{white}
    \arrayrulecolor{gray!30}
    \begin{tabularx}{\textwidth}{>{\bfseries\color{primary!80!black}}c X l c}
        \thead{ID US} & \thead{Sprint Task} & \thead{Volunteer} & \thead{Estimate} \\
        \midrule
        3 & Sviluppo UI registrazione utente con validazione input & Giacomo & 2 \\
        3 & Setup endpoint API per registrazione e hashing password & Marco & 2 \\
        3 & Integrazione DB per persistenza dati utente & Federico & 1 \\
        3 & Progettazione e implementazione collezione utente DB & Marco & 3 \\
        1 & Sviluppo UI form di login & Giacomo & 2 \\
        1 & Gestione token JWT in Local Storage per accesso offline & Federico & 1 \\
        42 & Implementazione Android Foreground Service con notifica & Federico & 4 \\
        42 & Richiesta permessi a runtime (Location/Bluetooth) & Marco & 2 \\
        40 & Generazione coppie chiavi ECC (Ed25519) per device & Giacomo & 4 \\
        38 & Generazione UUID v4 lato client per eventi idempotenti & Federico & 2 \\
        \bottomrule
    \end{tabularx}
\end{table}

\textit{Il Burndown chart è gestito tramite foglio di calcolo dedicato e riflette l'andamento reale dell'effort rimanente durante l'esecuzione dello sprint.}

\subsection{Test Cases}

\begin{table}[H]
    \centering
    \rowcolors{2}{secondary!5}{white}
    \arrayrulecolor{gray!30}
    \begin{tabularx}{\textwidth}{l c X X}
        \thead{\color{primary!80!black}Test ID} & \thead{\color{primary!80!black}US} & \thead{\color{primary!80!black}Azione / Input} & \thead{\color{primary!80!black}Output Atteso} \\
        \midrule
        \textbf{TC-01} & 3 & Chiamata POST \texttt{/users} con email e password validi & Status 201, utente salvato (password in hash) \\
        \textbf{TC-02} & 3 & Chiamata POST \texttt{/users} con email già in DB & Status 409 Conflict \\
        \textbf{TC-03} & 1 & Chiamata POST \texttt{/api/v1/auth/login} con password errata & Status 401 Unauthorized \\
        \textbf{TC-04} & 42 & L'utente manda l'app in background durante l'escursione & Il tracciamento GPS continua, l'OS non termina il processo \\
        \textbf{TC-05} & 40 & Inizializzazione applicazione in fase offline & Generata coppia di chiavi ECC locale per firma digitale SOS \\
        \bottomrule
    \end{tabularx}
\end{table}

\newpage

\subsection{Sprint Review}
Durante il meeting di Sprint Review, il team ha effettuato una demo dell'architettura iniziale. Marco ha mostrato il backend Node.js operativo, illustrando come la persistenza utente avvenga correttamente su MongoDB senza memorizzare la password in chiaro. Giacomo ha eseguito il deploy dell'app sul dispositivo fisico, dimostrando il corretto avvio del Foreground Service (notifica persistente) necessario per il tracking e per il Doze Mode. Dalla discussione è emerso che i permessi per la localizzazione in background su Android 9+ richiedono ulteriori attenzioni nella UI di onboarding per massimizzare la chiarezza verso l'utente.

\subsection{Product Backlog Refinement}
Alla luce delle direzioni prese durante la redazione del Deliverable D2 (transizione dalla raccolta fisica dei rifiuti a gamification NFC e telemetria automatica nei rifugi), il team ha provveduto a ripulire il Product Backlog rimuovendo i task di ”Scansione QR per ritiro sacchetto” precedentemente considerati, introducendo invece le nuove priorità inerenti allo \textit{Store-and-Forward} e all’\textit{Event Sourcing} che diventeranno il focus per lo Sprint \#2.

\subsection{Sprint Retrospective}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item \textbf{Cosa ha funzionato:} La separazione rigida dei compiti tra Backend (Marco) e Mobile Client (Giacomo) ha permesso di accelerare moltissimo l'inizio dello sprint. La strategia di branching con i feature branches si è dimostrata robusta, prevenendo conflitti accidentali e garantendo stabilità al branch develop.
    \item \textbf{Cosa non ha funzionato:} Le stime temporali su alcuni aspetti crittografici a basso livello (gestione chiavi ECC native su Android Keystore) sono risultate troppo ottimistiche. La configurazione dei permessi hardware ha sottratto più tempo del previsto a causa dei vincoli crescenti delle nuove API Android.
    \item \textbf{Dinamiche di team e Agile:} Poiché si sta adottando un approccio parzialmente asincrono per il Daily Scrum (via chat), talvolta i blocchi tecnici vengono notificati con un lieve ritardo; si è deciso di istituzionalizzare una breve \textit{sync call} vocale bisettimanale per mitigare il problema in vista dello Sprint \#2.



\end{itemize}

\end{document}