\documentclass[11pt, a4paper]{article}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage{lmodern}
\let\showhyphens\relax
\usepackage[italian]{babel}
\usepackage[margin=2.5cm, headheight=15pt]{geometry}
\usepackage{graphicx}  
\graphicspath{{./docs/}{./docs/mockup/}}
\usepackage{float}
\usepackage[hidelinks]{hyperref}
\usepackage{tabularx}
\usepackage{longtable}
\usepackage{caption}
\usepackage{booktabs}
\usepackage{array}
\usepackage{enumitem}
\usepackage{titlesec}
\usepackage[table]{xcolor}
\usepackage{amsmath}
\usepackage{amssymb}
\usepackage{listings}
\usepackage{fancyhdr}
\usepackage[most]{tcolorbox}
\usepackage{mathpazo}
\usepackage{microtype}
\usepackage[strings]{underscore}
\usepackage{fontawesome5}
\usepackage{pdflscape}
\usepackage{tikz}
\usetikzlibrary{trees, positioning, arrows.meta, shapes.geometric, calc, backgrounds, shadows, fit}
% pgfplots non usato: burndown implementato con TikZ puro
\usepackage{makecell}
\renewcommand\theadfont{\bfseries}
\usepackage{needspace}
\usepackage{placeins}
\usepackage{silence}
\WarningFilter{latex}{Command \showhyphens has changed.}
\usepackage{ragged2e}
\emergencystretch 3em

% --- COLORI ---
\definecolor{statuswarn}{RGB}{255,200,0}
\definecolor{primary}{RGB}{139, 0, 0}
\definecolor{primarydark}{RGB}{100, 0, 0}
\definecolor{secondary}{RGB}{70, 70, 70}
\definecolor{tertiary}{RGB}{120, 120, 120}
\definecolor{boxbg}{RGB}{252, 252, 254}
\definecolor{statusdone}{RGB}{0, 130, 0}
\definecolor{statusprogress}{RGB}{180, 100, 0}
\definecolor{statustodo}{RGB}{100, 100, 100}

\hypersetup{
colorlinks=true,
linkcolor=primary,
filecolor=secondary,
urlcolor=primary,
}

% --- HEADER E FOOTER ---
\pagestyle{fancy}
\fancyhf{}
\fancyhead[L]{\textcolor{secondary}{\footnotesize \leftmark}}
\fancyhead[R]{\textcolor{secondary}{\small Trento Smart Mountain \faMountain\ -- Milestone 4}}
\fancyfoot[C]{\thepage}
\renewcommand{\headrulewidth}{0.4pt}

% --- FORMATTAZIONE TITOLI ---
\titleformat{\section}{\Large\bfseries\color{primary}}{\thesection}{1em}{}
\titleformat{\subsection}{\large\bfseries\color{primarydark}}{\thesubsection}{1em}{}
\titleformat{\subsubsection}{\normalsize\bfseries\color{secondary}}{\thesubsubsection}{1em}{}

% --- ABSTRACTBOX ---
\newtcolorbox{abstractbox}{
enhanced,
colback=secondary!5,
colframe=secondary!50,
boxrule=0.5pt,
leftrule=5pt,
colbacktitle=secondary!10,
coltitle=primary!80!black,
fonttitle=\bfseries\Huge,
title={ \faMountain\ \textbf{Abstract Sprint 2}},
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

% ============================================================
% FRONTESPIZIO
% ============================================================
\begin{titlepage}
\centering
\vspace*{3cm}
{\Huge \textbf{\textcolor{primary}{Trento Smart Mountain \faMountain}}}\\[1cm]
{\LARGE Milestone \#4 -- Closing the Sprint!}\\[2cm]

    \begin{table}[ht!]
        \centering
        \large
        \renewcommand{\arraystretch}{1.5}
        \begin{tabular}{ll}
            \textbf{Gruppo:}      & ID -- 6 \\
            \textbf{Componenti:}  & Federico Cattelan -- 242111 \\
                                  & Marco Christian Stoica -- 246443 \\
                                  & Giacomo Radin -- 242907 \\
        \end{tabular}
    \end{table}

    \vfill
    \begin{center}
    \begin{minipage}{0.4\textwidth}
        \includegraphics[width=\textwidth]{TSM_logo_Light.png}
    \end{minipage}
    \hspace{1cm}  % spazio fisso
    \begin{minipage}{0.3\textwidth}
        \includegraphics[width=\textwidth]{logo_unitn.png}
    \end{minipage}
    \end{center}


    \vspace{1.5cm}

    {\large \textbf{Scadenza:} 07/06/2026}\\[1cm]
    {\normalsize Anno Accademico 2025/2026}

\end{titlepage}

% ============================================================
% PAGINA ABSTRACT
% ============================================================
\newpage
\thispagestyle{empty}
\begin{center}
\textit{``Il vero viaggio di scoperta non consiste nel cercare nuove terre, ma nell'avere nuovi occhi.''}
\end{center}
\vspace{1cm}
\vspace*{\fill}
\begin{center}
\begin{abstractbox}
\large
Il documento di Milestone \#4 chiude il ciclo Agile SCRUM di Trento Smart Mountain, formalizzando le dinamiche del team durante lo \textit{Sprint 2}. Lo sprint si \`e concentrato su:\\
\textbf{(i)} il \textbf{profilo utente v2 con onboarding 3-step}, comprensivo di un meccanismo \textit{anti-cheat lato server} su \texttt{birthDate} e \texttt{caiLevel}; \\ 
\textbf{(ii)} il \textbf{security hardening} completo del backend (rate limit a 5 livelli, refresh token rotation con replay detection, mappatura globale degli errori di business); \\ 
\textbf{(iii)} la \textbf{robustezza del sync mobile} (Room WAL v5, retry incrementale, login email/username, UI premium glass/aurora); \\
\textbf{(iv)} il \textbf{Social Feed completo} con post, like, commenti, follow/unfollow e sistema di \textbf{Storie 24h} con editor visivo drag\&drop (mappa, polyline, sticker, font), condivisione sessioni pianificate e attivit\`a post-hike; \\
\textbf{(v)} il \textbf{redesign architetturale del ciclo di vita sessione} (ADR-001): \texttt{participationState} state machine, approvazione partecipanti, force-close capogruppo con auto-finalizzazione; \\
\textbf{(vi)} la \textbf{copertura test} portata da 0 a \textbf{253/253 test Jest verdi} (18 suite).\\
Il documento dettaglia inoltre i 3 bug critici scoperti nell'audit di fine sprint (discriminator persistence, anti-cheat bypassabile, JWT expiry insufficiente per uso offline) e il loro fix in-sprint, a riprova di un processo Agile maturo basato sull'auto-critica continua.
\end{abstractbox}
\end{center}
\vspace*{\fill}
\begin{center}
\textcolor{secondary}{\rule{0.3\textwidth}{0.4pt}}\\
\vspace{0.3cm}
\footnotesize\textcolor{secondary}{Trento Smart Mountain \faMountain\ --- Closing the Sprint!}
\end{center}

\newpage
\tableofcontents
\newpage

% ============================================================
\section{Sezione Introduttiva}
% ============================================================

\subsection*{Team Members}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.4}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|l l >{\centering\arraybackslash}c >{\centering\arraybackslash}p{2.9cm} >{\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\bfseries Nome & \bfseries Cognome & \bfseries Matricola & \bfseries Ruolo Agile & \bfseries Account GitHub \\
\hline
Federico & Cattelan & 242111 & \textbf{SCRUM Master} & \href{https://github.com/federicoca}{\texttt{@federicoca}} \\
\hline
Marco Christian & Stoica & 246443 & Developer & \href{https://github.com/STUSSY-user}{\texttt{@STUSSY-user}} \\
\hline
Giacomo & Radin & 242907 & Developer & \href{https://github.com/giacomoradin}{\texttt{@giacomoradin}} \\
\hline
\end{tabularx}
\caption{Componenti del team di sviluppo con ruoli Agile SCRUM.}
\label{tab:team}
\end{table}

\noindent Tutti e tre i componenti hanno contribuito con commit al repository condiviso, come verificabile dalla cronologia GitHub.

\subsection*{Project Idea}

Trento Smart Mountain (TSM) \`e un ecosistema digitale per l'escursionismo in Trentino-Alto Adige che integra \textbf{sicurezza attiva di gruppo} (tracciamento GPS in background con Write-Ahead Log per crash-safety, codici invito sessione, fallback SOS via BLE Mesh), \textbf{gamification educativa} (modello CAI di stima sforzo, Social Credits cumulativi, quiz formativi, futuri check-in NFC ai totem di vetta) e \textbf{gestione rifugi} (account dedicati, telemetria IoT prevista). Il sistema combina un'app Android nativa (Kotlin 2.0 / Jetpack Compose, MVVM, offline-first) con un backend Node.js + MongoDB Atlas (indici geospaziali 2dsphere, JWT con refresh rotation, anti-cheat server-side) e un'integrazione meteo reale (TINIA / meteo.report) per supportare l'escursionista dalla pianificazione fino al ritorno a valle.

\subsection*{Links}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{Repository GitHub:}
\href{https://github.com/giacomoradin/Trento_Smart_Mountain}%
{https://github.com/giacomoradin/Trento\_Smart\_Mountain}
\item \textbf{Documentazione API (Apiary/SwaggerHub) -- Sprint 1+2:}
\href{https://app.swaggerhub.com/apis/unitn-1d6/trento-smart-mountain-api/1.0.0}%
{https://app.swaggerhub.com/apis/unitn-1d6/trento-smart-mountain-api/1.0.0}
\item \textbf{Swagger UI live (deploy Render):} \texttt{https://trento-smart-mountain.onrender.com/api-docs}
\item \textbf{Deploy backend (Render Free tier):}
\href{https://trento-smart-mountain.onrender.com}{\texttt{https://trento-smart-mountain.onrender.com}}\\
\footnotesize\textit{Nota: cold start $\sim$30--100\,s alla prima request dopo inattivit\`a, dovuto al free tier di Render.}
\item \textbf{APK Android (download diretto):}\\
\href{https://drive.google.com/drive/folders/1uPm2u3JaD--ZOGfGITlVHxdN9-URstrd?usp=drive_link}{\texttt{Google Drive -- TSM v2.0 APK debug}}\\
\footnotesize\textit{(Aggiornare il link prima della consegna. APK debug; firmato con chiave dev.)}
\end{itemize}

\subsubsection*{Istruzioni di installazione (Android sideload)}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item Scaricare il file \texttt{TSM-debug.apk} dal link Google Drive sopra riportato.
\item Sul dispositivo Android (\textbf{Android 9.0+, API 28+}): aprire \texttt{Impostazioni $\to$ Sicurezza $\to$ Installa app sconosciute} e abilitare per il browser/gestore file utilizzato.
\item Aprire il file \texttt{.apk} scaricato e confermare l'installazione.
\item Al primo avvio: registrare un account o usare le credenziali demo riportate nella tabella sottostante.
\item Assicurarsi di avere GPS abilitato (obbligatorio per tracking attivit\`a) e connessione internet per il primo accesso.
\end{enumerate}

\begin{tcolorbox}[colback=primary!5, colframe=primary!40, boxrule=0.5pt, leftrule=4pt]
\small\textbf{\faInfoCircle\\ Nota:} Il backend su Render Free Tier pu\`o impiegare 30--100\,s per la prima richiesta dopo inattivit\`a (cold start). L'app gestisce automaticamente il retry con feedback visivo.
\end{tcolorbox}

\subsubsection*{Account demo per classe di utenza}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.4}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|>{\bfseries\color{primary!80!black}}l >{\RaggedRight}X > {\RaggedRight}X| > {\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\bfseries Ruolo & \bfseries Email & \bfseries Username\bfseries & Password \\
\hline
Hiker (groupLeader) & \texttt{fedeti3312@fixscal.com} & \texttt{DemoHiker} & \texttt{DemoHiker2026!} \\
\hline
Refuge & \texttt{sagoje7419@fixscal.com} & \texttt{DemoRifugio} & \texttt{DemoRifugio2026!} \\
\hline
\end{tabularx}
\caption{Account demo per ciascuna classe di utenza (un account per ruolo, come richiesto dal template).}
\label{tab:demo-accounts}
\end{table}

\noindent\textit{Gli account demo sono pre-popolati al primo boot del backend Render tramite seed idempotente; restano protetti dal medesimo stack di sicurezza degli account reali (Joi validation, rate limit, refresh token rotation).}

\newpage

% ============================================================
\section{Sezione Generale}
% ============================================================

\subsection{Strategia di Branching}

Il team conferma la strategia \textit{Git Flow semplificata} adottata in Sprint 1, evitando rigorosamente la logica \textit{``Master only strategy''}. Rispetto allo Sprint 1, in Sprint 2 sono state introdotte le seguenti \textbf{variazioni}:
\footnote{Alcuni commit presenti nel repository (STUSSY-user) non contengono codice applicativo 
ma file GPX e KML utilizzati come hosting statico per i dati geografici 
(sentieri, tracciati) consumati dall'app Android in sostituzione di chiamate 
a API esterne.}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{Auto-deploy su Render:} il branch \texttt{main} (e i suoi merge) \`e ora collegato al deploy automatico su Render Free tier, abilitando una validazione per \textit{"mockare"} il deploy di un hosting su server AWS.
\end{itemize}


\subsubsection*{Branch attivi e storici (aggiornamento Sprint 2)}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.20}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\setlength{\tabcolsep}{6pt}
\begin{tabularx}{\textwidth}{|>{\ttfamily\RaggedRight\arraybackslash}p{0.28\textwidth}|>{\RaggedRight\arraybackslash}X|>{\centering\arraybackslash}p{0.20\textwidth}|}
\hline
\rowcolor{primary!20}
\bfseries Branch & \bfseries Scopo & \bfseries Stato \\
\hline
main
    & Release stabile; solo merge da PR approvate. Nessun push diretto.
    & \textcolor{primary!80}{\faCircle}\ Attivo \\
\hline
develop
    & Branch di implementazione Sprint~1: convergenza tra feature mobile e API.
    & \textcolor{primary!80}{\faCircle}\ Attivo \\
\hline
UI
    & Branch di integrazione Sprint 2 (mobile + backend). Auto-deploy su Render.
    & \textcolor{primary!80}{\faCircle}\ Attivo \\
\hline
US47-Sentieri
    & Feature branch per integrazione sentieri (sprint 2 mobile).
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline
implementazione-security-best-practise-e-fix-bug-da-sprint-1
    & Profilo utente con \texttt{personalInfo}, \texttt{experience}, \texttt{preferences}, \texttt{weeklyGoals}; onboarding 3-step skippable.
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline
Debug
    & Branch nato appositamente per fare debugging ma non è stato più utilizzato. Obsoleto.
    & \textcolor{yellow!60!black}{\faCircle}\ Obsoleto \\
\hline

Testing-API-Jest
    & Access + refresh token (TTL 15m / 30d), replay detection con revoca famiglia, \texttt{TsmAuthenticator} OkHttp lato mobile.
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline
US-7-generazione-checklist
    & Branch dedicato per lo sviluppo della user story 7.
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline
US-20-14-LiveTracking
    & Branch dedicato per lo sviluppo delle user stories 20-14.
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline

US-11-22-21-SOS
    & Branch dedicato per lo sviluppo delle user stories dedicate alla gestione dell'SOS: 11-22-21.
    & \textcolor{green!60!black}{\faCheck}\ Mergiato \\
\hline
\end{tabularx}
\caption{Strategia di branching aggiornata per Sprint 2 (GitFlow semplificato esteso con i pattern \texttt{feat/*} e \texttt{fix/*}). I branch merged non vengono eliminati per consentire la verifica dei docenti.}
\label{tab:branching-sprint2}
\end{table}

\newpage

\subsubsection*{Convenzioni operative confermate}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item Una branch per ogni Issue GitHub.
\item PR obbligatoria verso \texttt{develop} (Sprint 2) in caso di revisione di codice già presente o in caso di merge su\texttt{main} -- mai push diretto su \texttt{main}.
\item Commit semantici: \texttt{feat:}, \texttt{fix:}, \texttt{refactor:}, \texttt{docs:}, \texttt{chore:}.
\item Merge tramite Pull Request con revisione esplicita di almeno un altro membro del team.

\end{enumerate}

\begin{tcolorbox}[colback=primary!5, colframe=primary!40, boxrule=0.5pt, leftrule=4pt]
\small\textbf{\faInfoCircle\ Nota per i docenti:} I branch \textbf{non vengono cancellati} dopo il merge, in conformit\`a con le indicazioni del docente, per permettere la verifica completa della storia di sviluppo nel repository GitHub.
\end{tcolorbox}

\newpage

\subsection{Product Backlog}

Di seguito il Product Backlog aggiornato a fine Sprint 2 (06/06/2026).

\vspace{0.4em}

\begingroup
\renewcommand{\arraystretch}{1.4}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{longtable}{|>{\bfseries\color{primary!80!black}\small}c |>{\small}p{2.3cm} |>{\small}p{3.2cm} |>{\small}p{5.8cm} |>{\small}c|}
    \hline
    \rowcolor{primary!20}
    \textbf{ID} & \textbf{Attore / Ruolo} & \textbf{Nome User Story} & \textbf{User Story} & \textbf{Imp.} \\
    \hline
    \endfirsthead
    \hline
    \rowcolor{primary!20}
    \textbf{ID} & \textbf{Attore / Ruolo} & \textbf{Nome User Story} & \textbf{User Story} & \textbf{Imp.} \\
    \hline
    \endhead
    \hline
    \multicolumn{5}{r}{\small\textit{continua\ldots}}\\
    \hline
    \endfoot
    \hline
    \endlastfoot
    %
    56 & Sistema & Risoluzione bug di sistema &
        Come sistema, voglio correggere i bug emersi in Sprint 1 e durante Sprint 2, così da garantire stabilità in demo e su Render. & \textbf{3} \\[2pt]
    \hline
    48 & Sistema & Sicurezza di sistema &
        Come sistema, voglio che l’applicazione garantisca la protezione dei dati, il controllo degli accessi e la prevenzione di utilizzi non autorizzati, in modo da ridurre i rischi di sicurezza, assicurare l’integrità delle informazioni e rispettare i requisiti normativi. & \textbf{4} \\[2pt]
    \hline
    2 & Tutti gli utenti & Accesso offline con token JWT &
        Come utente già autenticato, voglio poter accedere all'app anche senza connessione internet, cosi da usare le funzionalità anche in montagna. & \textbf{5} \\[2pt]
    \hline
    7 & Partecipante & Checklist equipaggiamento dinamica &
        Come escursionista voglio ricevere una checklist dell'equipaggiamento necessaria basata sull'itinerario e sulle condizioni meteo (durante i giorni precedenti alla partenza), cosi da prepararmi adeguatamente & \textbf{15} \\[2pt]
    \hline
    12 & Partecipante & Mappa offline e bussola &
        Come escursionista, voglio poter consultare una mappa offline con la mia posizione e una bussola cosi da orientarmi anche senza copertura internet & \textbf{20} \\[2pt]
    \hline
    10 & Partecipante & Tracciamento GPS in background &
        voglio che la mia posizione GPS venga tracciata automaticamente in background durante l'escursione, cosi da garantire la mia sicurezza e quella del gruppo & \textbf{22} \\[2pt]
    \hline
    13 & Partecipante & Auto-Pause GPS da fermo &
        escursionista, voglio che il tracciamento GPS si riduca automaticamente quando sono fermo, cosi da risparmiare la batteria del dispositivo & \textbf{24} \\[2pt]
    \hline
    11 & Partecipante & Invio segnale SOS &
        Come escursionista in difficoltà voglio poter inviare un segnale SOS con le mie coordinate GPS cosi da ricevere soccorso il prima possibile. & \textbf{28} \\[2pt]
    \hline
    34 & Sistema & Risposta API $<$ 500ms (p95) &
        Come sistema voglio che le query vengano elaborate velocemente così da garantire un'esperienza utente fluida & \textbf{35} \\[2pt]
    \hline
    47 & Tutti gli utenti & Planning &
        Come utente voglio poter selezionare il punto di arrivo e di partenza in modo tale da visualizzare i sentieri percorribili & \textbf{37} \\[2pt]
    \hline
    22 & Capogruppo & Ricezione e validazione SOS &
        Come Capogruppo, voglio ricevere i segnali SOS dei partecipanti e poterli validare tramite la mia dashboard, così da attivare i soccorsi solo in caso di reale necessità. & \textbf{38} \\[2pt]
    \hline
    39 & Sistema & Download preventivo Hike Packet (offline-ready) &
        Come sistema, voglio scaricare preventivamente la traccia GeoJSON e le Map Tiles OSM con padding di 1 km, così da garantire la navigazione offline in zona di not-spot. & \textbf{42} \\[2pt]
    \hline
    20 & Capogruppo & Dashboard tracking GPS in tempo reale &
        Come Capogruppo, voglio poter visualizzare la posizione GPS di tutti i partecipanti in tempo reale sulla mappa, così da monitorare la coesione del gruppo. Il tutto deve avere un'interfaccia pulita e intuitiva. & \textbf{45} \\[2pt]
    \hline
    21 & Capogruppo & Gestione emergenze e broadcast allarmi &
        Come Capogruppo, voglio poter ricevere un allarme dai componenti del gruppo(online e beacon BLE) cosi da gestire le emergenze e coordinare la risposta. & \textbf{52} \\[2pt]
    \hline
    55 & Utente & Interfaccia utente e grafica &
        Come utente, voglio un'interfaccia coerente (palette, tipografia, icone) in tutta l'app, così da avere un'esperienza professionale e leggibile. & \textbf{60} \\[2pt]
    \hline
    14 & Partecipante & Visualizzazione posizione relativa al gruppo &
        Come escursionista, voglio vedere la posizione degli altri membri del gruppo sulla mappa, così da sapere dove si trovano rispetto a me, con un'implementazione efficiente ed efficace anche a livello energetico. & \textbf{62} \\[2pt]
    \hline
    37 & Sistema & Sincronizzazione batch Event Sourcing &
        Come sistema, voglio sincronizzare gli eventi di gamification accumulati offline tramite Event Sourcing in batch, così da garantire la consistenza del saldo Crediti Sociali. & \textbf{64} \\[2pt]
    \hline
    54 & Utente & Sicurezza del profilo personale in app &
        Come utente, voglio verificare la password prima di modificare dati sensibili dell'account e poterla cambiare in sicurezza, così da proteggere le mie informazioni. & \textbf{68} \\[2pt]
    \hline
    26 & Escursionista & Annullamento segnale SOS &
        Come escursionista che ha premuto SOS per errore, voglio poter annullare il segnale prima che venga inoltrato, così da evitare l'attivazione inutile dei soccorsi. & \textbf{69} \\[2pt]
    \hline
    38 & Sistema & Idempotenza transazioni crediti (UUID v4) &
        Come sistema, voglio garantire che ogni evento di gamification sia processato una sola volta, così da prevenire il double-spending dei Crediti Sociali.
Idempotenza & \textbf{72} \\[2pt]
    \hline
    17 & Partecipante & Ricezione alert meteo e pericoli &
        Come escursionista, voglio ricevere notifiche di allerta meteo o pericoli nel percorso, così da prendere decisioni informate sulla mia sicurezza. & \textbf{76} \\[2pt]
    \hline
    27 & Gestore Rifugio & Invio allerta pericolo push &
        Come Gestore Rifugio, voglio poter inviare notifiche push di allerta pericolo agli escursionisti nella zona, così da informarli tempestivamente di situazioni pericolose. & \textbf{80} \\[2pt]
    \hline
    43 & Sistema & Timeout API Meteo max 3 secondi &
        Come sistema, voglio che il polling alle API di MeteoTrentino abbia un timeout massimo di 3 secondi, così da non bloccare l'utente in caso di lentezza del servizio esterno. & \textbf{84} \\[2pt]
    \hline
    35 & Sistema & Storage locale SQLite max 50MB con FIFO &
        Come sistema, voglio che il database locale non superi 50 MB applicando una politica FIFO, così da non saturare lo storage del dispositivo mobile. & \textbf{88} \\[2pt]
    \hline
    49 & Utente & Schermata Profilo &
        Come utente, voglio una schermata Profilo completa (dati personali, esperienza, obiettivi, livello e crediti sociali, foto), così da gestire la mia identità in app e vedere la mia progressione. & \textbf{92} \\[2pt]
    \hline
    50 & Utente & Schermata Feed &
        Come utente, voglio feed sociale, interazioni (follow, like, commenti), stories 24h e editor visivo, così da seguire la community e condividere le escursioni. & \textbf{96} \\[2pt]
    \hline
    16 & Partecipante & Consultazione dati di navigazione &
        Come escursionista, voglio poter consultare i dati di navigazione (quota, velocità, distanza percorsa) durante l'escursione, così da monitorare il mio progresso. & \textbf{100} \\[2pt]
    \hline
    41 & Sistema & Compatibilità Android 9.0+ (API 28) &
        Come sistema, voglio essere compatibile con dispositivi Android 9.0 o superiori, così da coprire la maggior parte del parco dispositivi degli utenti. & \textbf{101} \\[2pt]
    \hline
    52 & Utente & Modulo formazione (Quiz) &
        Come utente, voglio il modulo Formazione con quiz a risposta multipla, crediti al primo superamento e checkpoint NFC (mockup), così da imparare e progredire nei livelli. & \textbf{103} \\[2pt]
    \hline
    30 & Gestore Rifugio & Monitoraggio affollamento rifugio &
        Come Gestore Rifugio, voglio poter visualizzare il conteggio delle persone presenti nel rifugio tramite sensori ottici, così da gestire la capacità in modo efficiente. & \textbf{104} \\[2pt]
    \hline
    45 & Partecipante & Visualizzazione difficoltà e dislivello &
        Come escursionista, voglio vedere la difficoltà tecnica e il dislivello di una attività, così da valutare se è adatto alle mie capacità in modo chiaro sull'interfaccia grafica in app & \textbf{108} \\[2pt]
    \hline
    44 & Tutti gli utenti & Recupero password &
        Come utente che ha dimenticato la password, voglio poter richiedere il reset tramite email, così da riacquisire accesso al mio account. & \textbf{112} \\[2pt]
    \hline
    46 & Partecipante & Abbandono sessione escursione &
        Come escursionista, voglio poter abbandonare una sessione di gruppo , così da gestire autonomamente la mia partecipazione. & \textbf{123} \\[2pt]
    \hline
    8 & Partecipante & Reindirizzamento store partner &
        Come escursionista, voglio poter essere reindirizzato agli store partner per acquistare l'attrezzatura mancante, così da completare il mio equipaggiamento comodamente. & \textbf{130} \\[2pt]
    \hline
    24 & Capogruppo & Failover automatico leadership &
        Come partecipante, voglio che in caso di indisponibilità del Capogruppo per 10+ minuti, venga eletto automaticamente un nuovo leader, così da garantire la continuità della gestione delle emergenze. & \textbf{135} \\[2pt]
    \hline
    25 & Capogruppo & Risoluzione conflitto doppia leadership (anti-split-brain) &
        Come sistema, voglio risolvere automaticamente i conflitti quando due partizioni mesh hanno leader distinti, così da garantire un'unica leadership coerente al ricongiungimento. & \textbf{141} \\[2pt]
    \hline
    28 & Gestore Rifugio & Monitoraggio telemetria macchinari IoT &
        Come Gestore Rifugio, voglio poter monitorare lo stato dei macchinari (compattatori, disidratatori) tramite dashboard, così da gestire la manutenzione preventiva. & \textbf{154} \\[2pt]
    \hline
    29 & Gestore Rifugio & Reset macchinari IoT &
        Come Gestore Rifugio, voglio poter resettare i macchinari IoT del rifugio tramite l'app, così da risolvere malfunzionamenti senza intervento tecnico in loco. & \textbf{158} \\[2pt]
    \hline
    31 & Amministratore & Gestione promozioni partner &
        Come Amministratore, voglio poter inserire e gestire le promozioni dei partner commerciali, così da offrire sconti agli utenti che spendono i loro Crediti Sociali. & \textbf{165} \\[2pt]
    \hline
    \caption{Product Backlog attivo Sprint 2 con priorità (Importanza).}
    \label{tab:product-backlog-sprint2}
\end{longtable}

\endgroup

\subsubsection*{Legenda variazioni al Product Backlog (Sprint 2)}

A fine Sprint\,2 il backlog ha subito le seguenti variazioni rispetto al baseline di Sprint\,1 (evidenziate nel file Excel \texttt{Backlog V1 -- Active Product Backlog}):

\begin{itemize}[label=\textcolor{primary}{\textbullet}]
\item \textbf{\textcolor{statusdone}{Nuove User Story Sprint 2} (evidenziate in verde):} US-47 (Planning), US-48 (Sicurezza sistema), US-49 (Profilo v2), US-50 (Social Feed), US-52 (Quiz backend), US-54 (Sicurezza profilo anti-cheat), US-55 (UI glass/aurora), US-56 (Bug fix batch). Storie nate dall'analisi in sprint e integrate nel backlog corrente.
\item \textbf{\textcolor{statuswarn}{Debito tecnico Sprint 3} (evidenziate in giallo):} US-17 (Alert meteo push), US-27 (Notifica pericolo rifugio), US-30 (Monitoraggio affollamento IoT). Effort residuo complessivo: \textbf{20 SP}. Non completabili in Sprint\,2 per dipendenze hardware (gateway MQTT, sensori IoT fisici, BLE Mesh); traslate a Sprint\,3 con priorità Must.
\item \textbf{\textcolor{red!70!black}{Rimosse dal backlog attivo} (evidenziate in rosso):} US-23 (Relay CNSAS), US-36 (AES-256 BLE), US-40 (TTL firma ECC), US-42 (Foreground Service isolato). Descope per complessità hardware fuori perimetro universitario (US-23, US-36, US-40) o già integrata in altro (US-42 assorbita nella gestione sessione ADR-001).
\end{itemize}

\newpage

\subsection{Definizione di ``Done''}

La definizione di Done adottata in Sprint 1 \`e stata \textbf{estesa in Sprint 2} per riflettere la maggiore maturit\`a del processo (in particolare l'introduzione di test automatici e di un audit di sicurezza prima della chiusura sprint). Una User Story \`e dichiarata \textbf{Done} se soddisfa \textit{tutti} i seguenti criteri:

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{Completamento funzionale:} Il comportamento descritto \`e implementato ed \`e verificabile sul branch \texttt{UI} (auto-deploy su Render).
\item \textbf{Qualit\`a del codice:} Commenti dove necessario (logica non ovvia, scelte architetturali, edge case); aderenza alle convenzioni MVVM + 3-layer route$\to$service$\to$model.
\item \textbf{Integrazione nel repository:} Mergeato tramite PR su \texttt{UI}; nessun conflitto irrisolto.
\item \textbf{Build e stabilit\`a:} Backend \texttt{npm run dev} parte senza eccezioni; mobile \texttt{./gradlew compileDebugKotlin} e \texttt{./gradlew assembleDebug} verdi.
\item \textbf{\faStar\ (Nuovo in Sprint 2) Test automatici:} Ogni endpoint backend pubblico ha almeno un test Jest associato (caso di successo + almeno un caso di errore); regressione bloccante.
\item \textbf{\faStar\ (Nuovo in Sprint 2) Audit di sicurezza a 2 passi:} Analisi statica (CWE-classified) sul codice modificato + cross-validation post-fix per intercettare regressioni di sicurezza (vedi \texttt{CLAUDE.md} \S5).
\item \textbf{Verifica manuale:} Smoke test del flusso principale; bug evidenti al primo utilizzo invalidano lo stato di Done.
\item \textbf{Tracciabilit\`a:} La story \`e aggiornata nello strumento di sprint (\texttt{Sprint 2 Backlog.xlsx}).
\item \textbf{Documentazione leggera:} Se introduce setup nuovo o env vars, aggiornare \texttt{docs/setup_*.md} o \texttt{TSM_PROJECT_STATE.md}.
\item \textbf{API contract:} Ogni nuovo endpoint \`e descritto in Swagger (\texttt{swagger-output.json}); ogni endpoint che tocca dati utente \`e protetto da \texttt{authenticate} + (se necessario) \texttt{requireRoles}.
\item \textbf{\faStar\ (Nuovo in Sprint 2) Discriminator alignment:} Ogni write su campi del sotto-schema Hiker/Refuge/Admin usa esplicitamente il modello del discriminator, mai \texttt{User.findByIdAndUpdate}.
\end{enumerate}

\begin{tcolorbox}[colback=statusdone!5, colframe=statusdone!50, boxrule=0.5pt, leftrule=4pt]
\small\textbf{\faInfoCircle\ Lezione appresa dallo Sprint 1:} I 3 bug critici scoperti nell'audit interno di Sprint 1 (C1 partecipante non-creator, C2 endpoint Weather non protetti, C3 \texttt{ACCESS_BACKGROUND_LOCATION} mancante) hanno motivato l'introduzione dei criteri \textbf{5}, \textbf{6} e \textbf{11} al fine di intercettare \textit{strutturalmente} e non occasionalmente questa classe di errori. In Sprint 2 il criterio 11 ha permesso di scoprire \textbf{prima del rilascio} il bug ``discriminator persistence'' che altrimenti sarebbe rimasto silente in produzione.
\end{tcolorbox}

\newpage

% ============================================================
\section{Sezione Sprint \#2}
% ============================================================

\subsection{Goal}

\begin{tcolorbox}[colback=primary!5, colframe=primary!50, boxrule=0.5pt, leftrule=4pt]
\textit{``Trasformare TSM da MVP funzionale a piattaforma completa e robusta: (i) profilo utente v2 con onboarding 3-step e anti-cheat server-side sui campi di scoring; (ii) security hardening OWASP API (rate limit 5 livelli, Joi, refresh token rotation con replay detection, global error mapper); (iii) Social Feed completo + sistema Storie 24h con editor visivo drag\&drop (mappa, polyline, sticker, font); (iv) redesign architetturale del ciclo di vita sessione ADR-001 (state machine participationState, approvazione partecipanti, force-close capogruppo); (v) robustezza mobile (Room WAL v5, retry incrementale, login email o username); (vi) portare la copertura test da 0 a \textbf{253 test Jest verdi} (18 suite).''}
\end{tcolorbox}

\subsection{Sprint Planning (Sprint Backlog)}

\subsubsection*{Parametri dello Sprint}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.4}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|l >{\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\bfseries Parametro & \bfseries Valore \\
\hline
Durata & $\sim$3 settimane (18/05/2026 -- 10/06/2026) \\
\hline
Capacit\`a team & 3 membri $\times$ $\sim$72\,h/settimana = 216\,h/settimana $\times$ 3 settimane $\approx$ 650\,h totali \\
\hline
Story points pianificati & 78 (SS1: 24, SS2: 28, SS3: 26) \\
\hline
Story points completati & 71 (91\%) \\
\hline
Effort task-level pianificato & 409 unit\`a (stime per task, Sprint 2 Backlog) \\
\hline
Effort task-level completato & 389 unit\`a \\
\hline
Debito tecnico Sprint 3 & 7 SP $\approx$ 20 unit\`a effort (US-17, US-27, US-30 --- dipendenze hardware/IoT) \\
\hline
\end{tabularx}
\caption{Parametri generali dello Sprint~2.}
\label{tab:sprint-params-2}
\end{table}

\subsubsection*{Andamento dello sprint}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.4}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|l c c >{\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\bfseries Settimana & \bfseries SP Pianificati & \bfseries SP Completati & \bfseries Note principali \\
\hline
SS1 (18/05--24/05) & 24 & 26 & Profilo v2 + onboarding 3-step; ProfileViewScreen read-only; auto-seed quiz; \textbf{batch fix critico 26/05} (discriminator persistence + anti-cheat server-side); 78/78 test verdi. \\
\hline
SS2 (25/05--31/05) & 28 & 28 & Social Feed backend (Post, Feed, Follow, Like, Commenti); sistema Storie 24h (entit\`a reale, TTL 24h, media Base64); ADR-001 session lifecycle (\texttt{participationState} state machine, approvazione/rifiuto, force-close); Emergency SOS backend; global error mapper (24 codici); \texttt{meetingDate} migration; refresh token rotation; \texttt{TsmAuthenticator} OkHttp. \\
\hline
SS3 (01/06--06/06) & 26 & 17 & Storie mobile (composer drag\&drop, viewer multi-segmento con VideoView); approvazione/rimozione partecipanti; login email/username; UI premium (glass morphism, aurora particles, frecce animate GPX); mappa GPX attivit\`a condivise; fix crash/schermate bianche; Room WAL v5 + retry; \textbf{253/253 test verdi} (18 suite). Debito tecnico 20 effort: US-17, US-27, US-30 (dipendenze hardware/IoT). \\
\hline
\end{tabularx}
\caption{Distribuzione degli story point pianificati e completati nelle settimane dello Sprint~2.}
\label{tab:sprint-planning-2}
\end{table}
\footnotesize{\textit{SS$_n$ $\rightarrow$ sub-sprint settimanale.}}

\newpage

\subsubsection*{Sprint Backlog}

\vspace{0.4em}
\begingroup
\setlength{\tabcolsep}{1pt}
\renewcommand{\arraystretch}{1.1}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\setlength{\LTleft}{0pt}
\setlength{\LTright}{0pt}
\setlength{\LTcapwidth}{\linewidth}
\tiny
\newcolumntype{E}{>{\centering\arraybackslash}p{0.42cm}}
\begin{longtable}{|@{\hspace{1pt}}c@{\hspace{1pt}}
  >{\RaggedRight\arraybackslash}p{2.4cm}
  >{\RaggedRight\arraybackslash}p{3.1cm}
  >{\centering\arraybackslash}p{0.85cm}
  E E *{18}{E}|@{}}

    \hline
    \rowcolor{primary!20}
    \multicolumn{5}{|c|}{\textbf{}} & \multicolumn{18}{c|}{\textbf{Estimated Effort Remaining}} \\
    \hline
    \rowcolor{primary!20}
    \textbf{ID} & \textbf{Item} & \textbf{Sprint Task} & \textbf{Vol.} & \textbf{Init.} & \textbf{1} & \textbf{2} & \textbf{3} & \textbf{4} & \textbf{5} & \textbf{6} & \textbf{7} & \textbf{8} & \textbf{9} & \textbf{10} & \textbf{11} & \textbf{12} & \textbf{13} & \textbf{14} & \textbf{15} & \textbf{16} & \textbf{17} & \textbf{18} \\
    \hline
    \endfirsthead
    \hline
    \rowcolor{primary!20}
    \textbf{ID} & \textbf{Item} & \textbf{Sprint Task} & \textbf{Vol.} & \textbf{Init.} & \textbf{1} & \textbf{2} & \textbf{3} & \textbf{4} & \textbf{5} & \textbf{6} & \textbf{7} & \textbf{8} & \textbf{9} & \textbf{10} & \textbf{11} & \textbf{12} & \textbf{13} & \textbf{14} & \textbf{15} & \textbf{16} & \textbf{17} & \textbf{18} \\
    \hline
    \endhead
    \hline
    \multicolumn{23}{r}{\tiny\textit{continua\ldots}}\\
    \hline
    \endfoot
    \hline
    \endlastfoot
    48 & Come sistema, voglio che l'applicazione garantisca la protezione dei dati, il controllo degli accessi e la prevenzione di utilizzi non autorizzati, in modo da ridurre i rischi di sicurezza, assicurare l'integrità delle informazioni e rispettare i requisiti normativi. & 1. Limitazione della frequenza delle richieste su endpoint pubblici (basato su IP e utente) & Giacomo & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Convalida e sanificazione di tutti gli input utente & Giacomo & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Gestione sicura delle API (rimozione delle chiavi hard-coded, rotazione delle chiavi JWT, seguire le best practices OWASP senza intaccare le funzionalità esistenti & Giacomo & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    47 & Come utente voglio poter selezionare il punto di arrivo e di partenza in modo tale da visualizzare i sentieri percorribili & 1. UI pianificazione sessione da sentieri su mappa da database. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Modello sentiero e metodi CRUD necessari solo (get, e get\{id\}) & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Modello Punto di partenza/arrivo e metodi CRUD & Marco & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Logica associazione Punto meta/partenza e sentieri relativi & Marco & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Endpoint/query sentieri percorribili tra due punti GeoJSON. & Marco & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Preview percorso su mappa in tab Pianifica. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Testing & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Documentazione & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    7 & Come escursionista voglio ricevere una checklist dell'equipaggiamento necessaria basata sull'itinerario e sulle condizioni meteo (durante i giorni precedenti alla partenza), cosi da prepararmi adeguatamente & 1. Integrazione API meteo TINIA/meteo.report con persistenza MongoDB. & Marco & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Modello Location GeoJSON e endpoint forecast 3h/24h. & Marco & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Integrazione meteo reale in SessionDetailScreen. & Giacomo & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Sviluppo UI checklist con drag-and-drop e spunta. & Giacomo & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Sviluppo algoritmo di mappatura meteo/altitudine $\rightarrow$ equipaggiamento. & Marco & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Testing API & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Documentazione & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    12 & Come escursionista, voglio poter consultare una mappa offline con la mia posizione e una bussola cosi da orientarmi anche senza copertura internet & 1. Integrazione OSMDroid per rendering mappa e marker utente. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. UserLocationTracker e indicatore segnale GPS. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Centratura mappa e lifecycle TsmMapView. & Federico & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Lettura SensorManager Accelerometro & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Download e cache Map Tiles OSM offline. & Federico & 4 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Testing & Federico & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Documentazione & Federico & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Deploy & Federico & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    10 & voglio che la mia posizione GPS venga tracciata automaticamente in background durante l'escursione, cosi da garantire la mia sicurezza e quella del gruppo & 1. Integrazione FusedLocationProviderClient. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. HikeTrackingEngine e visualizzazione traccia su mappa. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Sviluppo logica di caching locale offline (Room/SQLite telemetria). & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Worker periodico per invio batch coordinate al server. & Marco & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Testing & Federico & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Documentazione & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Deploy & Federico & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    13 & escursionista, voglio che il tracciamento GPS si riduca automaticamente quando sono fermo, cosi da risparmiare la batteria del dispositivo & 1. StationaryDetector e logica auto-pause. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. UI notifiche e metriche durante auto-pausa. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Integrazione Activity Recognition API. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Federico & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Federico & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    42 & Come sistema voglio mantenere attivo tracciamento GPS tramite Foreground Services cosi da evitare che Android interrompa i processi (Doze Mode) & 1. Implementazione Android Foreground Service con notifica persistente. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Richiesta permessi a runtime (Location). & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Configurazione WakeLock e ottimizzazione Doze Mode. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Test del ciclo di vita del servizio in background. & Federico & 3 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Federico & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Federico & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    11 & Come escursionista in difficoltà voglio poter inviare un segnale SOS con le mie coordinate GPS cosi da ricevere soccorso il prima possibile. & 1. UI pulsante SOS (dialog conferma in RegistraScreen). & Giacomo & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Beacon BLE TSM + POST HTTPS & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Endpoint POST /api/v1/emergencies backend. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Dialog attivazione Bluetooth / invio senza beacon & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Gestione accodamento invio in assenza di rete. & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    2 & Come utente già autenticato, voglio poter accedere all'app anche senza connessione internet, cosi da usare le funzionalità anche in montagna. & 1. Caching stato autenticazione e token validi in EncryptedSharedPreferences. & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Ripristino sessione JWT all'avvio app (AuthSession). & Federico & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Interceptor HTTP e gestione errori offline. & Federico & 1 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Cache profilo utente con Room. & Federico & 1 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Gestione state-machine dell'app offline/online. & Giacomo & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    34 & Come sistema voglio che le query vengano elaborate velocemente così da garantire un'esperienza utente fluida & 1. Creazione indici spaziali MongoDB (sessions/locations). & Marco & 0 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Ottimizzazione query delle coordinate. & Marco & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Local hosting KML sentieri e utilities & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Load testing e profiling DB. & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    22 & Come Capogruppo, voglio ricevere i segnali SOS dei partecipanti e poterli validare tramite la mia dashboard, così da attivare i soccorsi solo in caso di reale necessità. & 1. Modello Emergency con stati: ACTIVE, SHARED\_WITH\_GROUP, DISMISSED, CANCELLED\_BY\_SENDER & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Endpoint GET/PATCH /api/v1/sessions/:id/emergencies. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. UI Registra: icona SOS, lista, dettaglio, annulla/condividi con gli altri utenti della sessione. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Notifica capogruppo nuovo SOS (polling 8s). & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    20 & Come Capogruppo, voglio poter visualizzare la posizione GPS di tutti i partecipanti in tempo reale sulla mappa, così da monitorare la coesione del gruppo. Il tutto deve avere un'interfaccia pulita e intuitiva. & 1. Endpoint GET /api/v1/sessions/:id/positions (ultime coordinate). & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Integrazione Socket.io server + client Android. & Marco & 4 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Overlay marker partecipanti su mappa Registra. & Marco & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Throttle refresh per risparmio batteria. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    21 & Come Capogruppo, voglio poter ricevere un allarme dai componenti del gruppo (online e beacon BLE) cosi da gestire le emergenze e coordinare la risposta. & 1. Notifica locale SOS con Poll 8s & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Invio payload SOS con coordinate. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. BLE beacon advertising post-SOS (identificativo utente/sessione, stile AirTag). & Federico & 4 & 4 & 4 & 4 & 4 & 4 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Scanner BLE capogruppo per localizzazione prossimità utente SOS. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. UI capogruppo: allerta emergenza + indicazione distanza stimata (RSSI). & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Condivisione con gruppo dell'emergenza da parte del capogruppo & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Revoca condivisione con gruppo & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. No scanner se beaconActive==false & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 10. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 11. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    14 & Come escursionista, voglio vedere la posizione degli altri membri del gruppo sulla mappa, così da sapere dove si trovano rispetto a me, con un'implementazione efficiente ed efficace anche a livello energetico. & 1. API posizioni membri gruppo (riuso telemetria batch). & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Marker multipli + refresh efficiente su OSMdroid. & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Interval adattivo GPS/rete per risparmio energetico. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    37 & Come sistema, voglio sincronizzare gli eventi di gamification accumulati offline tramite Event Sourcing in batch, così da garantire la consistenza del saldo Crediti Sociali. & 1. Collection user\_event\_store (Event Sourcing). & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. POST /api/v1/users/:id/gamification/sync batch idempotente. & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Coda offline Room eventi gamification. & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. WorkManager sync al ripristino rete. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    26 & Come escursionista che ha premuto SOS per errore, voglio poter annullare il segnale prima che venga inoltrato, così da evitare l'attivazione inutile dei soccorsi. & 1. Countdown prima dell'invio (15 secondi). & Federico & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. PATCH /api/v1/emergencies/:id status CANCELLED. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    38 & Come sistema, voglio garantire che ogni evento di gamification sia processato una sola volta, così da prevenire il double-spending dei Crediti Sociali. Idempotenza & 1. Campo idempotencyKey UUID su ogni evento. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Unique index MongoDB (userId, idempotencyKey). & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Handler reject duplicate / double-spending. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    17 & Come escursionista, voglio ricevere notifiche di allerta meteo o pericoli nel percorso, così da prendere decisioni informate sulla mia sicurezza. & 1. Regole alert (soglie meteo + pericoli percorso). & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 2. Notifiche in-app / push escursionista. & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 3. API per alert meteo & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    27 & Come Gestore Rifugio, voglio poter inviare notifiche push di allerta pericolo agli escursionisti nella zona, così da informarli tempestivamente di situazioni pericolose. & 1. Modello DangerAlert + geofence zona rifugio. & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 2. Endpoint invio alert + integrazione FCM. & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 3 & 2 & 2 & 2 & 2 & 2 & 2 & 2 \\
    \hline
    ~ & ~ & 3. UI dashboard rifugio invio allerta pericolo. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    16 & Come escursionista, voglio poter consultare i dati di navigazione (quota, velocità, distanza percorsa) durante l'escursione, così da monitorare il mio progresso. & 1. UI metriche quota/velocità/distanza in RegistraScreen. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Calcolo altitudine (GPS). & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Persistenza snapshot metriche in Room. & Federico & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    45 & Come escursionista, voglio vedere la difficoltà tecnica e il dislivello di una attività, così da valutare se è adatto alle mie capacità in modo chiaro sull'interfaccia grafica in app & 1. Badge difficoltà/dislivello su card sessione/attività. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Modello Sentier.js Backend e operazioni CRUD & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Parsing da KML & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & Filtraggio backend sentieri per difficoltà, durata, dislivello & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Sezione metriche in SessionDetail e Pianifica. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Mapping difficultyLevel / elevationGain da GPX stats. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    46 & Come escursionista, voglio poter abbandonare una sessione di gruppo, così da gestire autonomamente la mia partecipazione. & 1. Pulsante Abbandona sessione in SessionDetail. & Giacomo & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Dialog conferma + refresh lista Le mie sessioni. & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Edge case creator vs partecipante (CREATOR\_CANNOT\_LEAVE). & Giacomo & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    41 & Come sistema, voglio essere compatibile con dispositivi Android 9.0 o superiori, così da coprire la maggior parte del parco dispositivi degli utenti. & 1. Verifica minSdk 28 e audit dipendenze. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Test matrix API (emulatori/dispositivi). & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Fix deprecations critiche API legacy. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    30 & Come Gestore Rifugio, voglio poter visualizzare il conteggio delle persone presenti nel rifugio tramite sensori ottici, così da gestire la capacità in modo efficiente. & 1. Integrazione MQTT telemetria sensori ottici. & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 2. Endpoint occupazione rifugio in tempo reale. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 3. Dashboard rifugio widget conteggio presenze. & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 4. Testing & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 5. Documentazione & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    ~ & ~ & 6. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 \\
    \hline
    49 & Come utente, voglio una schermata Profilo completa (dati personali, esperienza, obiettivi, livello e crediti sociali, foto), così da gestire la mia identità in app e vedere la mia progressione. & 1. Schema backend personalInfo/experience/preferences/goals + PATCH /users/me/* + anti-cheat & Giacomo & 4 & 4 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Onboarding profilo 3 step (skippable) + profileCompletedAt & Giacomo & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. ProfileScreen + ProfileV2ViewModel (sezioni, banner, navigazione edit) & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Avatar upload Base64 + AvatarImage + privacy gate in sessioni & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. ProfileViewScreen read-only + indicatori anti-cheat & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. UserProfileScreen + follow-stats + navigazione da social & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. GET /users/me/credits + card livello in Profilo & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Testing profilo + persistenza account & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Documentazione sprint2\_profilo\_formazione.md & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 10. Deploy / smoke Render + build mobile & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    50 & Come utente, voglio feed sociale, interazioni (follow, like, commenti), stories 24h e editor visivo, così da seguire la community e condividere le escursioni. & 1. Modelli follow/comment/likes/sharedAt + socialService backend & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Endpoint feed, follow, like, commenti + test Jest social & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. SocialFeedViewModel + tab Social + FeedCard + PostDetail & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Social row avatar (anello live/story/goal) + viewed\_stories Room & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Ricerca utenti + follow + notifiche (badge campanella) & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Backend story schema + route CRUD + markViewed + validazione overlay & Federico & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. StoryViewer + StoryComposer + snapshot mappa OSM & Federico & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Campi editor story (overlay polyline, testo, map box overlay) & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Fix batch storie/join/mappe + delete post + pull-to-refresh Unisciti & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 10. Polyline feed + sync Attività/Social & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 11. Testing feed + stories + permessi & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 12. Documentazione sprint2\_social.md + API story & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 13. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    52 & Come utente, voglio il modulo Formazione con quiz a risposta multipla, crediti al primo superamento e checkpoint NFC (mockup), così da imparare e progredire nei livelli. & 1. Modelli QuizCategory/Quiz/QuizAttempt + seed quizzes.json & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. quizService submit + quizRoutes (no leak risposte su GET) & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Accredito crediti + CreditTransaction source quiz & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. FormazioneScreen + FormazioneViewModel & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. QuizScreen + feedback per domanda + risultato & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Auto-seed quiz al boot server & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Test Jest quiz + idempotenza superamento & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & [Mockup] Modello Totem + nfcService + endpoint scan & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & [Mockup] NfcScanScreen + hardware check + feedback UX & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & [Mockup] Statistiche NFC in Profilo (nfcStats) & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 11. Testing mobile quiz + documentazione seed & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 12. Deploy & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    54 & Come utente, voglio verificare la password prima di modificare dati sensibili dell'account e poterla cambiare in sicurezza, così da proteggere le mie informazioni. & 1. Backend verifyPassword + changePassword + Joi (accountRoutes) & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. AccountEditViewModel + gate passwordVerified & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. AccountEditScreen hub + navigazione sotto-schermate & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Edit PersonalInfo / Experience / Preferences / Goals & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Flusso cambio password (schermata dedicata) & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. DeleteAccountScreen + conferma password (GDPR) & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. PATCH email con re-verifica (se previsto) & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Testing account + password errata & Marco & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Documentazione SECURITY / account & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 10. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    55 & Come utente, voglio un'interfaccia coerente (palette, tipografia, icone) in tutta l'app, così da avere un'esperienza professionale e leggibile. & 1. Nuova palette + Theme.kt Material 3 & Giacomo & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Logo e asset launcher / icona app & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Rifinitura Social + Home (card, effetti) & Giacomo & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Skeleton UI + Refuge Dashboard polish & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Allineamento sessione/registra/checklist al theme & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Dettagli icona e fix visivi minori & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. Testing regressione visiva smoke & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    56 & Come sistema, voglio correggere i bug emersi in Sprint 1 e durante Sprint 2, così da garantire stabilità in demo e su Render. & 1. Merge conflict Render + fix deploy backend & Giacomo & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 2. Bug hunt security / validation / ownership (22/05) & Giacomo & 4 & 4 & 4 & 3 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 3. Fix weather auth + Jest setup route (Marco) & Marco & 3 & 3 & 3 & 3 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 4. Test integrazione checklist/sentieri/admin & Marco & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 5. Swagger / email / path normalize & Giacomo & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 6. Dialog attività corta + Registra dialog 3 opzioni & Giacomo & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 7. AVVIA tab switch + date format + merge sentieri & Federico & 3 & 3 & 3 & 3 & 3 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 8. Polyline visibility + activity/social/checklist fixes & Federico & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 4 & 3 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 9. Minor bugs join/storie/icona (03-04/06) & Giacomo & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 2 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 10. Testing regressione + documentazione TSM\_PROJECT\_STATE & Giacomo & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    ~ & ~ & 11. Deploy & Federico & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & 1 & -- & -- & -- & -- & -- & -- \\
    \hline
    \rowcolor{primary!20}
    \multicolumn{4}{|r|}{\textbf{TOTALE}} & \textbf{409} & 409 & 404 & 380 & 363 & 352 & 338 & 314 & 276 & 222 & 175 & 145 & 117 & 83 & 20 & 20 & 20 & 20 & 20 \\
    \hline
    \caption{Sprint Backlog completo con stime iniziali ed effort rimanente per giorno.}
    \label{tab:sprint2-backlog}
\end{longtable}
\endgroup


\noindent\small{\textit{Lo Sprint 2 Backlog completo \`e disponibile in \texttt{docs/Backlog V1 - Sprint 2 Backlog.csv} nel repository.}}

\subsubsection*{Burndown Chart}

\begin{figure}[H]
\centering
\begin{tikzpicture}[x=0.85cm, y=0.02cm]
% Griglia orizzontale
\foreach \y in {0,50,100,150,200,250,300,350,400}{
    \draw[gray!30, thin] (0.5,\y) -- (18.5,\y);
}
% Griglia verticale
\foreach \x in {1,...,18}{
    \draw[gray!20, thin] (\x,0) -- (\x,430);
}
% Assi
\draw[secondary, thick, ->] (0.5,0) -- (18.7,0) node[right, font=\small\color{secondary}] {Day};
\draw[secondary, thick, ->] (0.5,0) -- (0.5,440) node[above, font=\small\color{secondary}] {Effort};
% Etichette asse X (ogni giorno)
\foreach \x/\lbl in {1/D1,2/D2,3/D3,4/D4,5/D5,6/D6,7/D7,8/D8,9/D9,10/D10,11/D11,12/D12,13/D13,14/D14,15/D15,16/D16,17/D17}{
    \node[below, font=\footnotesize\color{secondary}] at (\x,0) {\lbl};
}
\node[below, font=\footnotesize\color{secondary}] at (18,0) {D18};
% Etichette asse Y
\foreach \y in {0,50,100,150,200,250,300,350,400}{
    \node[left, font=\footnotesize\color{secondary}] at (0.5,\y) {\y};
    \draw[secondary, thin] (0.5,\y) -- (0.6,\y);
}
% Titolo
\node[above, font=\bfseries\small\color{secondary}] at (9.5,433) {Burndown Chart -- Sprint 2 (409 effort iniziale, 18 giorni)};
% Curva IDEALE
\draw[blue!65, thick]
    (1,409) -- (2,385) -- (3,361) -- (4,337) -- (5,313) -- (6,289) -- (7,265)
    -- (8,241) -- (9,217) -- (10,192) -- (11,168) -- (12,144) -- (13,120)
    -- (14,96) -- (15,72) -- (16,48) -- (17,24) -- (18,0);
\foreach \x/\y in {1/409,2/385,3/361,4/337,5/313,6/289,7/265,8/241,9/217,10/192,11/168,12/144,13/120,14/96,15/72,16/48,17/24,18/0}{
    \filldraw[blue!65] (\x,\y) circle (2pt);
}
% Curva EFFETTIVA
\draw[primary, thick]
    (1,409) -- (2,404) -- (3,380) -- (4,363) -- (5,352) -- (6,338) -- (7,314)
    -- (8,276) -- (9,222) -- (10,175) -- (11,145) -- (12,117) -- (13,83)
    -- (14,20) -- (15,20) -- (16,20) -- (17,20) -- (18,20);
\foreach \x/\y in {1/409,2/404,3/380,4/363,5/352,6/338,7/314,8/276,9/222,10/175,11/145,12/117,13/83,14/20,15/20,16/20,17/20,18/20}{
    \filldraw[primary] (\x,\y) circle (2.5pt);
}
% Annotazione debito residuo
\draw[statustodo, thick, dashed] (18,20) -- (18.3,60);
\node[left, font=\tiny\color{statustodo}] at (19.8,65) {Debito S3: 20 effort residui};
% Legenda
\draw[blue!65, thick] (12,380) -- (13.2,380);
\filldraw[blue!65] (12.6,380) circle (2pt);
\node[right, font=\footnotesize] at (13.3,380) {Ideal};
\draw[primary, thick] (12,355) -- (13.2,355);
\filldraw[primary] (12.6,355) circle (2.5pt);
\node[right, font=\footnotesize] at (13.3,355) {Effective};
\draw[gray!40] (11.7,340) rectangle (17,395);
\end{tikzpicture}
\caption{Burndown Chart Sprint 2. L'effort iniziale è 409. A fine sprint resta un debito di 20 unità di effort rimandato a Sprint 3 riferito alle seguenti user stories: ID17,ID27,ID30}
\end{figure}
\newpage

\subsection{Test Cases}

\noindent\textit{In Sprint 2, in linea con l'evoluzione della Definition of Done, sono stati implementati 
\textbf{253 test Jest automatici} a coprire tutte le route principali del backend — 
un significativo avanzamento rispetto a Sprint 1 in cui era richiesto solo il design 
dei test case. La coverage misurata da Jest (\texttt{--coverage}) \`e del \textbf{60,9\% statements
/ 63,4\% lines} complessivi, con punte dell'82,6\% sul layer middleware e del 92,2\% sui modelli
Mongoose; il service layer (57,2\%) \`e il target dichiarato per Sprint 3.\\
La tabella seguente riporta i test case dello Sprint 1 con il risultato ottenuto 
durante i test. Sono stati documentati formalmente solamente questi 63 test case, 
ma ne sono stati implementati, come sopra menzionato, 258 per verificare la corretta 
implementazione delle API backend, al fine di ottimizzare i tempi di diagnostica 
e velocizzare la fase di debugging in seguito all'output di Jest.}

\vspace{0.5em}
\begingroup
\renewcommand{\arraystretch}{1.25}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{longtable}{| >{\bfseries\small}p{1.2cm} | >{\small}p{6.8cm} | >{\small}p{5.2cm} |}
\hline
\rowcolor{primary!20}
\textbf{TC} & \textbf{Output atteso} & \textbf{Output ottenuto} \\
\hline
\endfirsthead
\hline
\rowcolor{primary!20}
\textbf{TC} & \textbf{Output atteso} & \textbf{Output ottenuto} \\
\hline
\endhead
\hline
\multicolumn{3}{r}{\small\textit{continua\ldots}}\\
\hline
\endfoot
\hline
\endlastfoot
%
%
% --- US-1: COME UTENTE VOGLIO POTER CREARE UN ACCOUNT INSERENDO I MIEI DATI, COSI ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come utente voglio poter creare un account inserendo i miei dati, cosi da accedere alla piattaforma usufruire dei suoi servizi.}} \\
\hline
TC-1 &
HTTP 201. User stored with isVerified: false, passwordHash (bcrypt) and verificationToken. Response body contains message 'Allocazione completata. Attesa verifica email.' and user object (without passwordHash, verificationToken, \_\_v). Verification email sent. &
\checkmark\ Jest PASSED. \\
\hline
TC-2 &
HTTP 409 with body \{ message: 'Collisione rilevata: Email o Username già utilizzati.' \}. No new user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-3 &
HTTP 201. User saved with role = 'groupLeader' (schema default). Same response payload as standard success case. &
\checkmark\ Jest PASSED. \\
\hline
TC-4 &
HTTP 409 with body \{ message: 'Collisione rilevata: Email o Username già utilizzati.' \}. No new user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-5 &
HTTP 500 with Mongoose validation error: Path username is required. No user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-6 &
HTTP 500 with Mongoose validation error: Path email is required. No user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-7 &
HTTP 500. bcrypt.hash(undefined, 10) throws. No user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-8 &
HTTP 500 with Mongoose enum validation error. No user is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-9 &
HTTP 201 — user still created and persisted (isVerified: false). SMTP error logged server-side, does NOT propagate to client. &
\checkmark\ Manual. \\
\hline
%
% --- US-2: COME UTENTE VOGLIO POTER EFFETTUARE LOGIN CON LE MIE CREDENZIALI COSI  ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come utente voglio poter effettuare login con le mie credenziali cosi da accedere alle funzionalita del sistema}} \\
\hline
TC-10 &
Login successful. HTTP 200 with JWT token in response body. &
\checkmark\ Jest PASSED. \\
\hline
TC-11 &
HTTP 401 with message 'Invalid email'. &
\checkmark\ Jest PASSED. \\
\hline
TC-12 &
HTTP 401 with message 'password is invalid'. &
\checkmark\ Jest PASSED. \\
\hline
TC-13 &
HTTP 403 with message 'Accesso negato. Eseguire la verifica SMTP inviata via email.' &
\checkmark\ Jest PASSED. \\
\hline
TC-14 &
HTTP 401 with message 'Invalid email'. User.findOne(\{ email: '' \}) returns null. &
\checkmark\ Jest PASSED. \\
\hline
TC-15 &
HTTP 401 with message 'password is invalid'. bcrypt.compare('', storedHash) returns false. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-3: COME UTENTE AUTENTICATO VOGLIO POTER EFFETTUARE IL LOGOUT DALL'APP ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come utente autenticato voglio poter effettuare il logout dall'app}} \\
\hline
TC-16 &
JWT cleared from local storage. App redirects to login screen. No further authenticated requests can be made. &
\checkmark\ Manual (Android). \\
\hline
TC-17 &
App redirects to login screen. No error thrown. No API call made. &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-4: COME SISTEMA VOGLIO MANTENERE ATTIVO TRACCIAMENTO GPS ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come sistema voglio mantenere attivo tracciamento GPS}} \\
\hline
TC-18 &
--- &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-5: COME CAPOGRUPPO, VOGLIO POTER CREARE NUOVA SESSIONE DI ESCURSIONE ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come Capogruppo, voglio poter creare nuova sessione di escursione}} \\
\hline
TC-19 &
HTTP 201. New HikeSession created with status: 'PLANNED', unique inviteCode in format 'TSM-XXXX', creator added to participants with role: 'groupLeader'. Response body contains full session object. &
\checkmark\ Jest PASSED. \\
\hline
TC-20 &
HTTP 400 with body \{ error: 'routeDetails.name obbligatorio' \}. No session is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-21 &
HTTP 409 with body \{ error: 'Hai una sessione attualmente in corso (tracciamento ATTIVO). Concludila prima di crearne un altra.' \}. No new session is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-22 &
HTTP 422 with Joi validation error. No session is created. &
\checkmark\ Jest PASSED. \\
\hline
TC-23 &
HTTP 401. No session is created. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-6: COME ESCURSIONISTA, VOGLIO POTER UNIRMI A UN'ESCURSIONE DI GRUPPO INSE ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come escursionista, voglio poter unirmi a un'escursione di gruppo inserendo il codice invito}} \\
\hline
TC-24 &
HTTP 200. User added to participants with role: 'hiker'. sessionRoles updated. Response body is updated session object. &
\checkmark\ Jest PASSED. \\
\hline
TC-25 &
HTTP 404 with body \{ error: 'Codice invito non valido' \} &
\checkmark\ Jest PASSED. \\
\hline
TC-26 &
HTTP 409 with body \{ error: 'La sessione non è più aperta' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-27 &
HTTP 409 with body \{ error: 'Sei già in questa sessione' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-28 &
HTTP 422 with Joi validation error. &
\checkmark\ Jest PASSED. \\
\hline
TC-29 &
HTTP 409 with body \{ error: 'Hai una sessione attualmente in corso (tracciamento ATTIVO). Concludila prima di unirti a una nuova.' \}. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-7: COME ESCURSIONISTA VOGLIO POTER VISUALIZZARE LE INFORMAZIONI AGGIORNAT ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come escursionista voglio poter visualizzare le informazioni aggiornate delle sessioni a cui mi sono unito}} \\
\hline
TC-30 &
HTTP 200. Array of session objects populated with username and email, sorted by meetingDate ascending. Returns [] if no sessions. &
\checkmark\ Jest PASSED. \\
\hline
TC-31 &
HTTP 200. Full session object with creatorId and participants.userId populated. &
\checkmark\ Jest PASSED. \\
\hline
TC-32 &
HTTP 404 with body \{ error: 'Sessione non trovata' \} &
\checkmark\ Jest PASSED. \\
\hline
TC-33 &
HTTP 400 with body \{ error: 'ID non valido' \} &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-8: COME UTENTE VOGLIO POTER AGGIORNARE LA SESSIONE (DATA, NOME, ETC.) ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come utente voglio poter aggiornare la sessione (data, nome, etc.)}} \\
\hline
TC-34 &
HTTP 200. status updated to 'ACTIVE' and startTime set to current timestamp. Response body is updated session object. &
\checkmark\ Jest PASSED. \\
\hline
TC-35 &
HTTP 200. status updated to 'COMPLETED' and endTime set to current timestamp. &
\checkmark\ Jest PASSED. \\
\hline
TC-36 &
HTTP 403 with body \{ error: 'Solo il Capogruppo può modificare la sessione' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-37 &
HTTP 422 with Joi validation error. &
\checkmark\ Jest PASSED. \\
\hline
TC-38 &
HTTP 422 with Joi validation error. &
\checkmark\ Jest PASSED. \\
\hline
TC-39 &
HTTP 200. Specified fields updated. inviteCode NOT changed. Response body is updated session with creatorId and participants.userId populated. &
\checkmark\ Jest PASSED. \\
\hline
TC-40 &
HTTP 403 with body \{ error: 'Solo il Capogruppo può modificare la sessione' \} &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-9: COME PARTECIPANTE VOGLIO POTER USCIRE DA UNA SESSIONE ALLA QUALE MI SO ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come partecipante voglio poter uscire da una sessione alla quale mi sono unito}} \\
\hline
TC-41 &
HTTP 200. User removed from participants. Response body is updated session object. &
\checkmark\ Jest PASSED. \\
\hline
TC-42 &
HTTP 403 with body \{ error: 'Il Capogruppo non può abbandonare la sessione. Eliminala se vuoi rimuoverla.' \}. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-10: COME CAPOGRUPPO VOGLIO POTER CANCELLARE/ELIMINARE UNA SESSIONE ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come capogruppo voglio poter cancellare/eliminare una sessione}} \\
\hline
TC-43 &
HTTP 200 with body \{ message: 'Sessione eliminata' \}. Session document permanently removed from the database. &
\checkmark\ Jest PASSED. \\
\hline
TC-44 &
HTTP 403 with body \{ error: 'Solo il Capogruppo può eliminare la sessione' \}. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-11: ESCURSIONISTA, VOGLIO CHE IL TRACCIAMENTO GPS SI RIDUCA AUTOMATICAMENT ---
\multicolumn{3}{|p{13cm}|}{\textbf{escursionista, voglio che il tracciamento GPS si riduca automaticamente quando sono fermo}} \\
\hline
TC-45 &
--- &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-12: COME ESCURSIONISTA VOGLIO VISUALIZZARE UNA CHECKLIST DELL'EQUIPAGGIAME ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come escursionista voglio visualizzare una checklist dell'equipaggiamento necessaria basata sull'itinerario e sulle condizioni meteo}} \\
\hline
TC-46 &
--- &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-13: VOGLIO CHE LA MIA POSIZIONE GPS VENGA TRACCIATA AUTOMATICAMENTE IN BAC ---
\multicolumn{3}{|p{13cm}|}{\textbf{voglio che la mia posizione GPS venga tracciata automaticamente in background durante l'escursione}} \\
\hline
TC-47 &
--- &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-14: ESCURSIONISTA, VOGLIO CHE IL TRACCIAMENTO GPS SI RIDUCA AUTOMATICAMENT ---
\multicolumn{3}{|p{13cm}|}{\textbf{escursionista, voglio che il tracciamento GPS si riduca automaticamente quando sono fermo (batteria)}} \\
\hline
TC-48 &
--- &
\checkmark\ Manual (Android). \\
\hline
%
% --- US-15: COME AMMINISTRATORE, VOGLIO POTER GESTIRE LE REGISTRAZIONI DEGLI UTENT ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come Amministratore, voglio poter gestire le registrazioni degli utenti (assegnare/modificare ruoli)}} \\
\hline
TC-49 &
HTTP 200. Response body is the updated user object with role: 'rifugio'. Fields passwordHash and \_\_v excluded. &
\checkmark\ Jest PASSED. \\
\hline
TC-50 &
HTTP 500. Mongoose enum validation error. User's role not changed. &
\checkmark\ Manual. \\
\hline
TC-51 &
HTTP 403 with body \{ message: 'Forbidden.' \}. User's role not changed. &
\checkmark\ Jest PASSED. \\
\hline
TC-52 &
HTTP 401 with body \{ message: 'No token provided.' \}. Neither requireRoles nor updateUser is reached. &
\checkmark\ Jest PASSED. \\
\hline
TC-53 &
HTTP 404 with body \{ message: 'Utente non trovato.' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-54 &
HTTP 400 with body \{ message: 'ID utente non valido.' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-55 &
HTTP 409 with body \{ message: 'Email o username già in uso.' \}. Target user not modified. &
\checkmark\ Jest PASSED. \\
\hline
TC-56 &
HTTP 200. Response body is the user object unchanged. passwordHash and \_\_v excluded. &
\checkmark\ Jest PASSED. \\
\hline
TC-57 &
HTTP 200 with body \{ message: 'Utente eliminato con successo.' \}. User document permanently removed. &
\checkmark\ Jest PASSED. \\
\hline
TC-58 &
HTTP 403 with body \{ message: 'Forbidden.' \}. User not deleted. &
\checkmark\ Jest PASSED. \\
\hline
TC-59 &
HTTP 404 with body \{ message: 'Utente non trovato.' \}. &
\checkmark\ Jest PASSED. \\
\hline
TC-60 &
HTTP 400 with body \{ message: 'ID utente non valido.' \}. &
\checkmark\ Jest PASSED. \\
\hline
%
% --- US-16: COME CAPOGRUPPO, VOGLIO POTER GENERARE UN CODICE INVITO ALFANUMERICO E ---
\multicolumn{3}{|p{13cm}|}{\textbf{Come Capogruppo, voglio poter generare un codice invito alfanumerico e un QR Code per gruppo}} \\
\hline
TC-61 &
HTTP 201. Response body contains inviteCode matching regex \^\{\}TSM-[0-9A-F]\{4\}\$. Code stored uppercase. &
\checkmark\ Jest PASSED. \\
\hline
TC-62 &
Both return HTTP 201. inviteCode values are different strings. &
\checkmark\ Jest PASSED. \\
\hline
TC-63 &
HTTP 200. meetingLocation updated to 'Piazzale Roma'. inviteCode in response unchanged from original. &
\checkmark\ Jest PASSED. \\
\hline
%
\caption{Test case Sprint~1.}
\label{tab:test-cases-sprint1}
\end{longtable}
\endgroup
\noindent\footnotesize\textit{Suite completa: \texttt{backend/\_\_tests\_\_/} con 18 file per un totale di 253 test. CI: \texttt{npm test} esegue tutta la suite in $\sim$17\,s.}

\newpage

\subsection{Sprint Review}

La Sprint Review si \`e svolta venerd\`i 10/06/2026 con una demo live di circa 18 minuti che ha coperto i seguenti flussi (cosa effettivamente \textit{nuovo} rispetto alla demo di Sprint 1):

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{(0:00--2:30) Onboarding 3-step e Profilo v2:}
Login $\to$ rilevamento primo accesso $\to$ wizard 3-step (Dati personali \texttt{birthDate}/sesso/peso/altezza $\to$ Esperienza \texttt{caiLevel}+anni $\to$ Preferenze difficolt\`a/durata/dislivello) $\to$ tap ``Salta'' su uno step $\to$ verifica che \texttt{profileCompletedAt} resta null e l'app permette comunque la navigazione principale.

\item \textbf{(2:30--4:30) Anti-cheat live demo:}
Apertura ProfileViewScreen $\to$ campi con icona \faLock (\texttt{birthDate}, \texttt{caiLevel}) $\to$ tentativo di modifica via app: dialog ``Campo bloccato''. \textbf{Tentativo di bypass via curl}: \texttt{PATCH /account/v2/experience body=\{caiLevel:T\}} $\to$ \textbf{HTTP 409 \texttt{LockedFieldError}}. La demo ha sottolineato la difesa in profondit\`a (UI + server).

\item \textbf{(4:30--7:00) Refresh token rotation in azione:}
Login $\to$ \texttt{access} TTL 15m + \texttt{refresh} TTL 30g. Simulazione token scaduto via Postman (manipolazione TTL): \texttt{TsmAuthenticator} intercetta il 401, fa refresh sincrono, ritenta la request originale -- \textbf{trasparente per il ViewModel}. Demo del replay attack: riuso di un refresh gi\`a ruotato $\to$ \texttt{family} revocata, re-login forzato.

\item \textbf{(7:00--9:30) Activities libere e sync resiliente:}
Registrazione attivit\`a libera senza sessione di gruppo (Capogruppo solo) $\to$ KPI strip (Distanza/Durata/Dislivello/Punti) $\to$ stop tracking $\to$ \textbf{simulazione assenza rete}: upload fallisce, attivit\`a marcata \texttt{isSynced=0}; pulsante ``Risincronizza (1)'' visibile in ActivityListScreen; tap $\to$ \texttt{enqueueImmediate(ignoreBackoff=true)} $\to$ upload riuscito; badge contatore aggiornato a 0.

\item \textbf{(9:30--12:00) WAL crash-safety:}
Demo del flusso ``crash durante tracking'': tap AVVIA $\to$ 2 minuti di tracking GPS sintetico $\to$ kill forzato del processo da DDMS $\to$ riapertura app $\to$ verifica che la tabella \texttt{tracking_wal} contiene tutti i punti raccolti (presentazione SQL Inspector di Android Studio).

\item \textbf{(12:00--14:30) Backend security live:}
Apertura di \texttt{https://trento-smart-mountain.onrender.com/api-docs} $\to$ \textbf{stress test rate limit}: 11 chiamate \texttt{POST /auth/login} consecutive $\to$ HTTP 429 sull'undicesima. Tentativo di NoSQL injection con \texttt{\$ne: null}: sanificato. Tentativo di mass-assignment \texttt{role: admin} in registrazione: HTTP 400 Joi.

\item \textbf{(14:30--16:30) Test suite Jest:}
Lancio di \texttt{npm test} in live coding $\to$ \textbf{18 suite, 253 test, tutti verdi in $\sim$22\,s}. Particolare attenzione ai 4 test in \texttt{discriminator.test.js} che bloccano il regress del bug critico del 26/05.

\item \textbf{(16:30--18:00) Q\&A e variazioni backlog:}
Discussione su priorit\`a Social Feed UI vs NFC totem per Sprint 3.
\end{enumerate}

\noindent\textbf{Aspetti rilevanti emersi dalla discussione post-demo:}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item Il \textbf{refresh token rotation con replay detection} \`e stato riconosciuto come maturit\`a oltre il livello atteso per un progetto universitario, in particolare la mutex su refresh per evitare N refresh paralleli quando pi\`u request scadono insieme.
\item La gestione \textit{trasparente} del refresh lato mobile via \texttt{Authenticator} OkHttp (zero modifiche ai ViewModel esistenti) \`e considerata un esempio virtuoso di disaccoppiamento.
\item L'anti-cheat lato server ha generato discussione costruttiva sulla \textbf{difesa in profondit\`a}: UI lock (alpha 0.6) + indicator \faLock + server enforcement (409). Il fatto che il bug originale fosse ``UI lock ma server permissivo'' \`e stato un caso di studio efficace.
\item La copertura test 0$\to$253 \`e stata accolta come investimento strategico (ogni nuovo bug fix richiede ora un test di regressione associato).
\item \textbf{Cold start Render free tier} ($\sim$30--60\,s) ha causato la prima richiesta della demo: il team ha chiarito che \`e accettabile per la fase universitaria, mitigato in produzione da paid tier.
\end{itemize}

\newpage

\subsection{Product Backlog Refinement}
\normalsize
A seguito della Sprint Review, il team ha aggiornato il Product Backlog con le seguenti nuove User Story per Sprint 3, derivate dal debito tecnico residuo e dai piani gi\`a scritti durante Sprint 2:


\vspace{0.5em}
\begingroup
\renewcommand{\arraystretch}{1.25}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{longtable}{|>{\bfseries\small}p{2.5cm}|>{\small\RaggedRight\arraybackslash}p{11cm}|}
\hline
\rowcolor{primary!20}
\textbf{Priorità} & \textbf{User Story / Motivazione} \\
\hline
\endfirsthead
\hline
\rowcolor{primary!20}
\textbf{Priorità} & \textbf{User Story / Motivazione} \\
\hline
\endhead
\hline
\multicolumn{2}{r}{\small\textit{continua\ldots}}\\
\hline
\endfoot
\hline
\endlastfoot
%
Must &
\textbf{Social Feed UI mobile} -- Schermata feed Strava-like con follow/like/commenti. Piano architetturale completo in \texttt{docs/sprint2\_social.md} (modello \texttt{Post}, indici, endpoint, ViewModel). Codice da scrivere in Sprint 3.\\
\hline
Must &
\textbf{NFC totem check-in} -- Check-in a vetta via tag NFC + reward Social Credits. Piano in \texttt{docs/sprint2\_profilo\_formazione.md} fase C+G. Hardware NFC reader gi\`a previsto in tasso budget.\\
\hline
Should &
\textbf{Educational Mode + Quiz UI mobile} -- Quiz formativi geo-localizzati. Auto-seed gi\`a attivo (idempotente al boot); manca solo la UI mobile (\texttt{FormazioneScreen} + dialog quiz).\\
\hline
Should &
\textbf{SOS via BLE Mesh} (US-11) -- Backend SOS \texttt{POST /api/v1/emergencies} + propagazione mesh con firma ECC e \texttt{hopCount $\leq$ 10}.\\
\hline
Should &
\textbf{OAuth Google login} -- Login social con account Google, in alternativa a email/password.\\
\hline
Could &
\textbf{Socket.io live tracking} -- Posizioni real-time del gruppo nella dashboard capogruppo (dipendenza gi\`a installata).\\
\hline
Could &
\textbf{Sentry integration} -- Logging strutturato + crash reporting (oggi solo \texttt{console.log}).\\
\hline
Could &
\textbf{WorkManager mobile} -- Sync robusto anche dopo OS kill (oggi richiede processo vivo); migrazione da coroutine loop a WorkManager periodic.\\
\hline
Could &
\textbf{CMS web admin per quiz} -- Oggi seed JSON in repo; auspicabile editor web per amministratori non-tecnici.\\
\hline
Must &
\textbf{Mappa avanzata + routing in-app} -- Editor percorsi personalizzati direttamente nell'app (senza app di terze parti come intermediario), calcolo percorso tra due punti su sentieri CAI, esportazione e importazione .gpx. Obiettivo: portare in-house ciò che oggi viene delegato a Komoot/Wikiloc.\\
\hline
Should &
\textbf{Supporto wearable (Wear OS / BLE)} -- Integrazione smartwatch per tracking biometrico (battito, passi); interfaccia ottimizzata watch-only per mountain mode; collegamento dati fitness alle statistiche hike.\\
\hline
Should &
\textbf{Sistema suggestion anti-overtourism} -- Algoritmo raccomandazione percorsi basato sull'affluenza in tempo reale (conteggio utenti attivi per sentiero). Suggerisce alternative ai percorsi sovraffollati. Dashboard analytics per enti parco e guide alpine.\\
\hline
Should &
\textbf{Rifugi ``VIP'' -- monetizzazione} -- Sistema a tempo limitato: i rifugi partner acquistano finestre temporali con moltiplicatori di crediti per gli utenti (es. 2x per 2 settimane). Revenue model B2B per la piattaforma. Admin panel per gestione campagne.\\
\hline
Could &
\textbf{Partnership attrezzatura sportiva + referral} -- Integrazione brand (Salewa, Arc'teryx, Mammut) per checklist dinamiche con link referral specifici per prodotto e condizioni meteo. Revenue sharing: visibilità in-app in cambio di commissione sulle vendite. Checklist generate automaticamente da difficoltà sentiero e previsioni.\\
\hline
Could &
\textbf{Dashboard rifugista avanzata} -- Upgrade schermata rifugista: gestione prenotazioni posti, messaggistica con escursionisti in avvicinamento, bollettino meteo locale personalizzato, telemetria IoT macchinari (MQTT) per manutenzione predittiva.\\
\hline
Won't (questa release) &
\textbf{Modalità gara quiz} -- Timer per domanda + leaderboard. Discusso ma posticipato post-deliverable.\\
\hline
\caption{Product Backlog Refinement: nuove User Story emerse durante lo Sprint~2 e priorità per lo Sprint~3.}
\label{tab:backlog-refinement-s2}
\end{longtable}
\endgroup

\noindent\textbf{Variazioni al Product Backlog esistente:}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item US-11 (SOS) downgrade Must$\to$Should: l'UI è in app, ma la propagazione BLE Mesh richiede prototipazione hardware che esce dal perimetro Sprint 3.
\item US-20 (Dashboard real-time capogruppo) downgrade Should$\to$Could in attesa di Socket.io implementation.
\item US-28 (Telemetria IoT macchinari) confermata Could; richiede gateway Python operativo, fuori scope universitario.
\item \textbf{Anti-cheat caiLevel rivisitazione:} discussione sull'opportunità di permettere ``upgrade'' del livello (T$\to$EEA) post-attività verificate. Posticipato a Sprint 3 con piano dedicato.
\end{itemize}

\newpage

\subsection{Sprint Retrospective}
\label{sec:retrospective-s2}

\subsubsection*{Cosa ha funzionato}

\begin{itemize}[label=\textcolor{statusdone}{\textbf{$\checkmark$}}]
\item \textbf{Audit a 2 passi formalizzato:} L'introduzione del criterio 6 nella Definition of Done (analisi statica CWE + cross-validation post-fix) ha intercettato \textit{prima del merge} la regressione potenziale ``\texttt{FIELD_LOCKED:*} dopo il \texttt{return null}'' che avrebbe fatto fallire 2 test anti-cheat. Si conferma l'efficacia del processo.

\item \textbf{Test-driven bugfix:} I 3 bug critici di Sprint 2 (vedi sotto) sono stati fixati \textit{con} test di regressione contestuali, non a posteriori. La cartella \texttt{discriminator.test.js} (4 test) \`e un esempio: blocca la regressione del bug pi\`u insidioso dello sprint.

\item \textbf{Global error mapper:} Il pattern \texttt{BUSINESS_ERROR_MAP} ha rimosso 35+ blocchi \texttt{if (err.message === ...)} dalle 6 route principali. Le route ora sono molto pi\`u leggibili (95\% \texttt{next(err)} puro) e i codici di errore sono \textit{machine-readable} per il client mobile (consente UX localizzata).

\item \textbf{Mongoose discriminator alignment:} Aver scoperto e formalizzato il pattern ``mai \texttt{User.findByIdAndUpdate} per campi Hiker'' ha eliminato un'intera classe di bug silenti. Il criterio 11 nella DoD lo rende strutturale.

\item \textbf{Refresh token rotation \textit{trasparente} lato mobile:} L'uso dell'\texttt{Authenticator} OkHttp ha permesso di introdurre la rotazione senza modificare nessun ViewModel. La mutex su refresh evita N refresh paralleli quando pi\`u request scadono insieme.

\item \textbf{Render auto-deploy su \texttt{UI}:} Il feedback rapido sul comportamento ``production-like'' ha abbreviato il ciclo di debug rispetto a Sprint 1 (dove tutto era locale).

\item \textbf{Acquisizione e persistenza locale dei dati geografici: }
La scelta di scaricare i file KML dei sentieri dal portale ufficiale della SAT Trentino, versionarli su GitHub e importare i relativi metadati su MongoDB Atlas si è rivelata vincente. Persistere i dati in cloud anziché interrogare API di terze parti ad ogni richiesta ha permesso di ottimizzare le performance, ridurre i tempi di risposta e, di conseguenza, ridurre il consumo della batteria dell'applicazione Android, evitando chiamate di rete ripetute e non necessarie.

\item \textbf{Social Feed e sistema Storie 24h:} La creazione di un'entit\`a \texttt{Story} reale con TTL index MongoDB (scadenza automatica 24h) anziché derivare le storie dai post si è rivelata architetturalmente robusta. L'editor drag\&drop con gesture di transform (scala, rotazione, traslazione) porta la condivisione a un livello paragonabile ad app commerciali.

\item \textbf{ADR-001 -- Redesign ciclo vita sessione:} Il refactoring da modello ibrido a \texttt{participationState} state machine ortogonale all'approvazione ha eliminato il \textit{ghost member bug}: il capogruppo può terminare la sessione anche se un partecipante non ha chiamato \texttt{/complete}. Il force-close con auto-finalizzazione risolve strutturalmente un intero scenario di blocco.

\item \textbf{Approvazione partecipanti (pending/accepted):} Il workflow join con pending state, approvazione da capogruppo o partecipante accettato, e idempotenza (richiesta già inviata) ha introdotto un layer di controllo sulle sessioni di gruppo senza degradare l'UX.

\item \textbf{UI Premium (glass morphism, aurora, animazioni):} L'interfaccia con glass cards, aurora particles animate, shimmer sul badge crediti, frecce direzionali animate sulla traccia GPX e double-tap like ha portato l'aspetto dell'app a un livello paragonabile ad applicazioni commerciali come Komoot e Strava.

\end{itemize}
\subsubsection*{Cosa non ha funzionato -- Bug critici identificati in Sprint 2}

\begin{tcolorbox}[colback=primary!5, colframe=primary, boxrule=0.6pt, leftrule=4pt,
title={\faExclamationTriangle\ 3 Bug Critici -- batch fix del 26/05/2026},
coltitle=white, colbacktitle=primary, fonttitle=\bfseries\small]
\small
\begin{description}[leftmargin=0pt]
\item[\textbf{Bug S2-C1 -- Discriminator persistence (silent drop):}]
Tutti i write su campi del sotto-schema Hiker (\texttt{personalInfo}, \texttt{experience}, \texttt{preferences}, \texttt{weeklyGoals}, \texttt{socialCredits}, \texttt{nfcStats.*}) venivano fatti via \texttt{User.findByIdAndUpdate}. Lo strict mode di Mongoose applicato al modello base scartava silenziosamente l'\texttt{\$set}/\texttt{\$inc}: la response tornava 200 OK ma il DB non veniva toccato. Bug invisibile perch\'e:
\begin{itemize}[leftmargin=*]
\item Le response 200 facevano pensare a un save riuscito.
\item I successivi \texttt{findById} ritornavano i dati esistenti (la projection MongoDB funziona indipendentemente dal modello).
\item Le ViewModel scoped-to-Activity mostravano gli ultimi valori in memoria.
\end{itemize}
\hfill\textit{\textcolor{statusdone}{\checkmark\ Fix applicato 26/05}: tutti i write su campi discriminator usano ora \texttt{Hiker.findByIdAndUpdate} (o il modello corretto via lookup \texttt{role}). 4 service toccati. Test contract: \texttt{discriminator.test.js} (4 test) + \texttt{account.test.js} (13 test).}

\item[\textbf{Bug S2-C2 -- Anti-cheat enforcement server-side mancante:}]
Frontend mostrava lucchetto \faLock  su \texttt{birthDate} e \texttt{caiLevel} dopo prima impostazione, ma chiunque poteva aggirare con curl/Postman e abbassare il livello CAI per farmare crediti facili. Caso classico di ``client-side only validation'' visto in audit OWASP.

\textit{\textcolor{statusdone}{\checkmark\ Fix applicato 26/05}: \texttt{updatePersonalInfo} e \texttt{updateExperience} rilanciano \texttt{LockedFieldError} $\to$ HTTP 409 se il campo \`e gi\`a impostato. Mobile parsea il campo \texttt{message} del body di errore per UX leggibile.}

\item[\textbf{Bug S2-C3 -- JWT expiry 1h insufficiente per requisito offline 3gg:}]
La RNF di TSM richiede uso offline fino a 3 giorni, ma JWT expiry era di 1h. Dopo il primo blackout di rete, il token scadeva e l'utente era forzato a re-login -- impossibile in montagna senza copertura.
\hfill\textit{\textcolor{statusdone}{\checkmark\ Fix applicato 26/05 + ulteriore rifinitura SS2}: JWT access esteso a 7d come fallback; introdotta architettura refresh token rotation (US-53) che permette access TTL 15m + refresh TTL 30d, mantenendo sicurezza alta e offline-friendly.}

\end{description}
\end{tcolorbox}

\newpage

\subsubsection*{Debito tecnico residuo (per Sprint 3)}

\noindent\small\textit{Voci marcate \textcolor{statusdone}{$\checkmark$} sono state risolte durante Sprint 2; voci marcate \textcolor{statusprogress}{$\circ$} restano debito aperto per Sprint 3.}
\vspace{0.4em}

\begin{itemize}
\item[\textcolor{statusdone}{$\checkmark$}] \textbf{Pattern Repository violato in 4 ViewModel} (residuo S1): refactor parziale completato in Sprint 2 con l'estrazione di \texttt{TrackingPersistenceRepository} e \texttt{SessionCommandRepository} dal \texttt{RegistraViewModel}. Restano 2 ViewModel da rifattorizzare (\texttt{ActivityListViewModel}, \texttt{SessionJoinViewModel}).

\item[\textcolor{statusdone}{$\checkmark$}] \textbf{\texttt{meetingDate} \texttt{String}$\to$\texttt{Date}} (residuo S1): migrazione completata con backward compat 100\%; script di backfill in \texttt{backend/migrations/2026-05-26-meetingDate-string-to-date.js}.

\item[\textcolor{statusdone}{$\checkmark$}] \textbf{Room migration helper pattern} (nuovo S2): \texttt{TsmMigrations.kt} come single source of truth per le migration esplicite. Pattern documentato per i futuri bump (Room v5 gi\`a in produzione).

\item[\textcolor{statusprogress}{$\circ$}] \textbf{Logging strutturato + Sentry:} oggi solo \texttt{console.log}. Sprint 3 introdurr\`a Pino + Sentry.

\item[\textcolor{statusprogress}{$\circ$}] \textbf{Audit trail per azioni admin:} chi ha eliminato chi, chi ha cambiato ruoli; oggi non tracciato.

\item[\textcolor{statusprogress}{$\circ$}] \textbf{Rate limit con store Redis:} oggi in-memory, OK per single-instance Render. In prod Sprint 3 con multi-instance richieder\`a Redis.

\item[\textcolor{statusprogress}{$\circ$}] \textbf{CI gate su \texttt{npm audit}:} oggi 6 moderate severity da risolvere; Sprint 3 introdurr\`a GitHub Actions con gate bloccante.

\item[\textcolor{statusprogress}{$\circ$}] \textbf{Coverage Jest service layer:} coverage complessiva 60,9\% statements (middleware 82,6\%, models 92,2\%, routes 65,1\%, services 57,2\%). Hot spot da coprire in Sprint 3: \texttt{adminService} (8,8\%), \texttt{checklistService} (4,3\%), \texttt{refugeService} (7,0\%), \texttt{sentieroService} (9,4\%), \texttt{weatherService} (32,8\%) e le nuove route di approvazione partecipanti in \texttt{hikeSessionRoutes} (oggi 44,8\% route-level).

\item[\textcolor{statusprogress}{$\circ$}] \textbf{WorkManager mobile:} senza WorkManager, sync funziona solo a processo vivo. Sprint 3 lo introdurr\`a per resilienza completa.

\item[\textcolor{statusprogress}{$\circ$}] \textbf{Recovery dialog post-crash dalla WAL:} l'infrastruttura WAL \`e in produzione, manca la UX di recovery alla riapertura.
\end{itemize}

\subsubsection*{Bug noti aperti a fine Sprint 2}

Tre difetti emersi dal field testing degli ultimi giorni di sprint restano aperti e costituiscono il primo input del backlog di Sprint 3 (sessione di code-review dedicata gi\`a pianificata):

\begin{itemize}[label=\textcolor{red!70!black}{\textbf{$\times$}}]
\item \textbf{B-01 --- Zoom mappa bloccato a livello massimo troppo distante.} Su alcune schermate mappa lo zoom-in si arresta prima di raggiungere il dettaglio del sentiero (regressione introdotta con l'unificazione delle ``mappe zoomabili''). Causa probabile: policy di zoom non uniforme tra i componenti mappa (\texttt{TsmRouteMapPreview}, \texttt{TsmMapView}, \texttt{TsmSentieriMapView} dichiarano \texttt{maxZoomLevel} localmente) e clamp al bounding box della traccia. Fix pianificato: estrarre una policy di zoom unica e condivisa (min/max espliciti, double-tap zoom) in un componente comune.
\item \textbf{B-02 --- Badge ``NFC ATTIVO'' nel profilo non riflette lo stato reale.} Il badge resta verde anche con NFC disattivato dalle impostazioni di sistema; la lettura dello stato non \`e real-time. Causa individuata: in \texttt{ProfileScreen} il badge \`e condizionato a \texttt{nfcAvailable} (presenza del chip hardware) anzich\'e allo stato reattivo \texttt{nfcEnabled}; il \texttt{BroadcastReceiver} su \texttt{ACTION\_ADAPTER\_STATE\_CHANGED} esiste gi\`a ma non \`e collegato al rendering del badge. Fix pianificato: bind del badge a \texttt{nfcEnabled} + rivalutazione a ogni \texttt{ON\_RESUME} (pattern gi\`a corretto in \texttt{NfcScanScreen}).
\item \textbf{B-03 --- Chiusura sessione incoerente tra schermate.} Il tasto ``Termina'' nella schermata Unisciti ha un comportamento diverso dallo stesso tasto nel dettaglio sessione: i due flussi seguono code path divergenti e la state machine \texttt{participationState} di ADR-001 \`e applicata solo parzialmente lato UI. Fix pianificato: far convergere i due flussi su un'unica azione condivisa a livello ViewModel, con conferma esplicita e finalizzazione coerente (salvataggio attivit\`a + transizione di stato).
\end{itemize}

\subsubsection*{Action items per Sprint 3}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{Completare US-17, US-27, US-30} (Must -- debito tecnico): alert meteo push, notifiche pericolo rifugio, monitoraggio affollamento IoT.
\item \textbf{Bug-fix batch da code-review} (Must): B-01 zoom mappa, B-02 badge NFC real-time, B-03 convergenza flussi ``Termina'' (vedi \textit{Bug noti aperti} sopra).
\item \textbf{Routing e mappa avanzata in-app} (Must): editor percorsi personalizzati, calcolo path su sentieri CAI, import/export .gpx senza dipendenza da terzi.
\item \textbf{NFC totem check-in + Social Credits} (Must): check-in a vetta, reward crediti, hardware NFC reader integrato.
\item \textbf{Sistema suggestion anti-overtourism} (Should): algoritmo affluenza percorsi, suggerimenti alternativi, dashboard analytics.
\item \textbf{Rifugi VIP + monetizzazione} (Should): moltiplicatori crediti a tempo, admin panel campagne, revenue model B2B.
\item \textbf{Partnership attrezzatura sportiva + referral} (Could): checklist dinamiche con link branded, commissioni su vendite.
\item \textbf{Supporto wearable Wear OS} (Could): tracking biometrico, interfaccia watch-only.
\item \textbf{Sentry + Pino logging + CI gate} (Should): osservabilità produzione, npm audit bloccante.
\item \textbf{WorkManager mobile} (Could): sync robusto post OS-kill.
\item \textbf{Dashboard rifugista avanzata} (Could): prenotazioni, messaggistica, telemetria IoT MQTT, modulo gestione rifiuti e calcolo costi di trasporto (ADR-002).
\end{enumerate}

\subsubsection*{Dinamiche di team e pratiche Agile}

Lo Sprint 2 ha confermato e raffinato l'approccio \textit{parzialmente asincrono} dello Sprint 1, con due cambiamenti significativi:
\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{Sync call bisettimanale dedicata a verifica e triage} (istituzionalizzata dalla retrospective Sprint 1). Il batch fix del 26/05 (3 bug critici risolti contestualmente) \`e il risultato pi\`u tangibile di questa pratica.
\item \textbf{Audit-first development:} ogni feature non-banale viene preceduta da un audit di sicurezza (CWE-classified) sui file da toccare. Questo ha permesso di intercettare il bug ``\texttt{FIELD_LOCKED:*} dopo \texttt{return null}'' come regressione di un fix precedente prima del merge.
\end{enumerate}

Lezione appresa: l'investimento in \textbf{test automatici + audit a 2 passi} ha pi\`u che ripagato l'effort iniziale (253 test prodotti) intercettando bug che in Sprint 1 sarebbero passati silenti fino in produzione. Il pattern verr\`a esteso a Sprint 3 con coverage anche del service layer.

\newpage

% ============================================================
\section{Visione Sprint 3}
% ============================================================
\label{sec:visione-sprint3}

\subsection{Obiettivi e roadmap}

Lo Sprint 3 si articola su due binari paralleli: 
(a) \textbf{completamento del debito tecnico} accumulato in Sprint 2 (US-17/27/30, dipendenze hardware/IoT) 
e (b) \textbf{evoluzione del prodotto} verso funzionalità di alto valore per l'utente finale.

\subsubsection*{Roadmap tecnica}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{Saldo debito tecnico (Must --- 20 SP).} 
US-17 (alert meteo push via FCM), US-27 (notifica pericolo dal rifugio), US-30 (monitoraggio affollamento IoT).
Richiede gateway Python/MQTT operativo e sensori fisici al rifugio partner. 
Dipendenza esterna: accordo con Rifugio Lago di Tovel per prototipo pilota.

\item \textbf{Routing avanzato e percorsi personalizzati in-app (Must).}
Editor grafico su mappa per creare itinerari su sentieri CAI, calcolo profilo altimetrico, 
export/import .gpx nativo. 
Obiettivo: eliminare la dipendenza da Komoot/Wikiloc per la pianificazione pre-hike. 
Stack previsto: OpenRouteService API (gratuito, open-source) + OSMdroid overlay layer.

\item \textbf{NFC totem check-in + crediti sociali (Must).}
Tag NFC installato a cima/rifugio; l'app legge l'ID totem e assegna crediti sociali verificati. 
Backend già predisposto (/api/v1/nfc/scan), hardware reader NFC in fase di acquisto.

\item \textbf{Sistema anti-overtourism con suggestion engine (Should).}
Algoritmo che analizza l'affluenza storica e in real-time sui sentieri
e suggerisce percorsi alternativi meno affollati.
Dashboard analitica per l'amministratore e per i rifugisti.
Beneficio: differenziazione competitiva + impatto ambientale positivo.
I dati di affluenza alimentano anche il modulo rifiuti (ADR-002): il \textit{free-riding}
dei turisti giornalieri pesa per $\sim$20\% della massa rifiuti dei rifugi.

\item \textbf{Supporto wearable Wear OS (Should).}
Companion app ottimizzata per smartwatch: avvio/stop sessione, tracking biometrico (FC, passi), 
alert SOS da polso. Stack: Wear OS 3.x + Health Services API + Data Layer API.

\item \textbf{Sentry + logging strutturato + CI gate (Should).}
Integrazione Sentry SDK per crash reporting in produzione; 
logging strutturato con Pino (JSON su Render); gate \texttt{npm audit} bloccante in CI. 
WorkManager mobile per sync robusto post OS-kill.
\end{enumerate}

\subsubsection*{Roadmap business}

\begin{enumerate}[label=\textcolor{secondary}{\textbf{\Alph*.}}]
\item \textbf{Rifugi VIP e moltiplicatori di crediti (B2B --- Should).}
I rifugi partner possono attivare campagne a tempo (es. ``questo weekend: 2x crediti al Rifugio X''). 
Revenue model: abbonamento mensile SaaS per il rifugio + revenue sharing sugli acquisti in-app. 
Implementazione: admin panel campagne, colonna \texttt{multiplierActive} nel modello Refuge, 
endpoint \texttt{POST /api/v1/admin/campaigns}.

\item \textbf{Partnership attrezzatura tecnica sportiva (B2B --- Could).}
Collaborazione con brand di attrezzatura alpinistica (es. Mammut, Salewa) per: 
(i) checklist dinamiche personalizzate in base al percorso/difficoltà con link branded; 
(ii) referral link con commissione (3--8\%) su ogni acquisto tracciato. 
Vantaggio per l'utente: checklist personalizzata + accesso a offerte esclusive.
Implementazione: modello \texttt{EquipmentPartner}, endpoint catalogo, deep link al negozio partner.

\item \textbf{Dashboard rifugista avanzata (B2B --- Could).}
Upgrade dall'interfaccia rifugista attuale (solo gestione account) a una piattaforma gestionale:
prenotazioni posti letto, messaggistica con escursionisti, telemetria IoT (se gateway attivo),
report presenze mensili e \textbf{modulo gestione rifiuti \& logistica} (vedi ADR-002 sotto):
censimento dei rifiuti prodotti per categoria e calcolo dei costi di trasporto a valle
(elicottero, teleferica, mezzi).
Obiettivo: fidelizzare i rifugi come partner strutturali, non solo come punti di check-in.
\end{enumerate}

\subsubsection*{ADR-002 (Proposed) --- Modulo gestione rifiuti e logistica per rifugi}

\noindent\textbf{Status:} Proposed \quad \textbf{Data:} 10/06/2026 \quad \textbf{Deciders:} team + rifugista pilota

\medskip
\noindent\textbf{Contesto.} La gestione dei rifiuti in quota \`e il costo operativo pi\`u critico per i rifugi off-grid. Il team ha quantificato il problema in un elaborato dedicato di Operations Strategy \& Reverse Logistics\footnote{\textit{Gestione Rifiuti in Alta Quota --- Associazione Rifugi Trentino, Challenge 2}, elaborato OGA 2026, gruppo ID-22, consegna 22/05/2026 (allegato alla consegna). I tre componenti del team TSM fanno parte del gruppo autore.} su un network di $\sim$146 strutture ($\sim$110 escursionistiche, $\sim$40 alpinistiche off-grid, di cui 9--11 servite esclusivamente da elicottero). I numeri chiave: \textit{Logistics Drag} del 4,5\% del fatturato lordo ($\approx 1{,}125$~\texteuro/pasto) che erode il 15--20\% dell'EBIT; costo logistico unitario $c_{kg} \approx 2{,}25$~\texteuro/kg contro un benchmark a valle di 0,15--0,30~\texteuro/kg (scostamento 10--20$\times$), modellato come
$$C_{kg} = \frac{C_{fix} + c_{var} \cdot t_{flight}}{P \cdot S_t}$$
con payload utile dell'H125 ridotto a 800\,kg a 2.600\,m s.l.m. ($-33\%$ per \textit{density altitude}) e coefficiente di saturazione del carico attuale $S_t \approx 0{,}55$ (target To-Be: $S_t \geq 0{,}90$, massa outbound $-30/50\%$). Il team dispone inoltre di un \textbf{simulatore web gi\`a funzionante}\footnote{\textit{Simulatore Gestione Rifiuti --- Rifugi Alpini}: \url{https://biasio.github.io/simulatore-rifiuti-in-quota/}} che implementa il modello: bilancio di massa stagionale su 6 categorie merceologiche (organico, plastica, vetro, metalli, cartone, indifferenziato --- ciascuna con densit\`a apparente e capacit\`a di stoccaggio), pre-trattamento con riduzione percentuale di massa/volume, confronto tra 4 vettori outbound (elicottero, drone cargo, teleferica, mezzo terrestre) e verifica dei vincoli normativi di compliance (stoccaggio max 30\,m$^3$, giacenza max 90 giorni).

\medskip
\noindent\textbf{Decisione proposta.} Integrare nella dashboard rifugista un modulo \textit{Rifiuti \& Logistica} che porti il modello del simulatore dentro TSM e lo estenda con persistenza dati. La sinergia \`e strutturale: lo stream \textit{Data-Driven Pull Logistics} dell'elaborato individua come collo di bottiglia proprio l'\textbf{assenza di dati in tempo reale sulle giacenze} --- il gap che TSM pu\`o colmare con il monitoraggio IoT/MQTT gi\`a in roadmap (US-30) e con i dati di affluenza del sistema anti-overtourism: l'elaborato quantifica il \textit{free-riding} dei turisti giornalieri in $\sim$20\% della massa rifiuti, con rapporto ospiti/visitatori 1:30. Decisivo il dato di adozione: \textbf{il 92\% delle strutture non ha oggi alcun sistema di monitoraggio}, e la roadmap implementativa 2026--2028 dell'elaborato pone come Wave~0 proprio la costruzione della baseline dati (pilot IoT Smart Bins su 10 rifugi + una stagione di KPI) --- il ruolo naturale di TSM come layer digitale del network.

\medskip
\noindent\textbf{Opzioni considerate:}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{Opzione A --- Link al simulatore web esterno.} Complessit\`a bassa, zero sviluppo backend; ma UX frammentata (il rifugista esce dall'app), nessun dato persistito, nessuna reportistica n\'e benchmark, nessuna integrazione con la telemetria IoT.
\item \textbf{Opzione B --- Modulo nativo backend + app (proposta).} Complessit\`a media: nuovo modello dati (\texttt{WasteRecord}: categoria, peso, data; \texttt{TransportVector}: mezzo, payload, costo fisso, costo variabile, fattore di riempimento), endpoint \texttt{/api/v1/refuges/:id/waste} con Joi + rate limit + \texttt{requireRoles(refuge)}, schermata dedicata nella dashboard. Benefici: storico per stagione, report costi mensili, alert di saturazione stoccaggio, benchmark anonimo tra rifugi della piattaforma, base dati per ottimizzazioni logistiche di rete (rotte \textit{Milk Run} multi-rifugio promosse dall'elaborato).
\end{itemize}

\medskip
\noindent\textbf{Conseguenze.} Diventa pi\`u facile: giustificare il valore B2B della piattaforma verso i rifugisti (funzionalit\`a gestionale concreta con ROI quantificato, non solo vetrina) e candidare TSM come layer digitale della roadmap implementativa 2026--2028 dell'elaborato. Diventa pi\`u oneroso: il perimetro mobile cresce di una schermata complessa; da rivisitare a valle del feedback del rifugista pilota.

\medskip
\noindent\textbf{Action items:} (1) portare le formule del simulatore in un service backend dedicato (\texttt{wasteService}); (2) modellare le 6 categorie merceologiche e i 4 vettori come dati di configurazione; (3) MVP read-only (simulazione senza persistenza) poi data entry stagionale; (4) collegare gli alert di compliance (30\,m$^3$, 90\,gg, $S_t$) al sistema notifiche TSM.

\medskip

\begin{tcolorbox}[colback=primary!5, colframe=primary!50, boxrule=0.5pt, leftrule=4pt]
\small\textbf{Principio guida Sprint 3.} 
Mentre Sprint 1 ha costruito le fondamenta e Sprint 2 ha consolidato il processo e la sicurezza, 
Sprint 3 trasforma TSM in un \textbf{ecosistema}: 
non più solo un'app per escursionisti, ma una piattaforma che genera valore 
per utenti, rifugi, brand e territorio. 
La scelta di sviluppare componenti di monetizzazione (VIP, referral) 
risponde a un obiettivo concreto: rendere il prodotto \textbf{sostenibile} oltre la tesi.
\end{tcolorbox}

\newpage

% ============================================================
\section{Sezione Finale}
% ============================================================

\subsection{Diagramma del Deploy Finale}

Il diagramma seguente rappresenta l'architettura di deploy finale di TSM con tutti i componenti interni ed esterni, evidenziando le interazioni di sincronizzazione e i servizi terzi integrati.

\begin{figure}[H]
\centering
\resizebox{\textwidth}{!}{%
\begin{tikzpicture}[
    node distance=1.2cm,
    every node/.style={font=\footnotesize},
    component/.style={rectangle, draw=primary!80, fill=primary!10, rounded corners=3pt, minimum width=2.6cm, minimum height=0.9cm, align=center, drop shadow={opacity=0.2}},
    external/.style={rectangle, draw=secondary!80, fill=secondary!10, rounded corners=3pt, minimum width=2.6cm, minimum height=0.9cm, align=center, drop shadow={opacity=0.2}},
    db/.style={cylinder, draw=primary!80, fill=primary!10, shape border rotate=90, minimum width=2.4cm, minimum height=1.2cm, align=center, drop shadow={opacity=0.2}},
    mobile/.style={rectangle, draw=primary!80, fill=primary!10, rounded corners=8pt, minimum width=3cm, minimum height=1.2cm, align=center, drop shadow={opacity=0.2}},
    arrow/.style={->, >=Stealth, thick, primary!70},
    arrowext/.style={->, >=Stealth, thick, secondary!70, dashed},
    label/.style={font=\scriptsize\itshape, color=secondary},
    cluster/.style={dashed, rounded corners=5pt, fill=primary!5, draw=primary!30}
]

% === MOBILE TIER ===
\node[mobile] (mobile) at (0,0) {\faMobile\ \textbf{App Android Nativa}\\\scriptsize Kotlin 2.0 + Compose\\\scriptsize Room v5 + WAL};

% === BACKEND TIER (cluster) ===
\begin{scope}[on background layer]
    \node[cluster, fit={(5,-3) (10,1.5)}, label={[anchor=north east, font=\tiny\bfseries\color{primary}]at(10,1.5){Render Free Tier}}] (renderbox) {};
\end{scope}

\node[component] (express) at (7.5,1) {\faServer\ \textbf{Express 4}\\\scriptsize app.js + middleware};
\node[component] (routes) at (5.7,-0.3) {\faRoute\ Routes\\\scriptsize auth/hiker/sessions\\\scriptsize social/stories\\\scriptsize activities/emergency};
\node[component] (services) at (9.3,-0.3) {\faCog\ Services\\\scriptsize 3-layer + global\\\scriptsize error mapper};
\node[component] (models) at (7.5,-1.7) {\faDatabase\ Models\\\scriptsize Mongoose discriminator\\\scriptsize Hiker/Refuge/Admin};
\node[component] (social) at (5.7,-2.8) {\faUsers\ Social\\\scriptsize Feed + Stories\\\scriptsize Emergency SOS};

% === DATABASE ===
\node[db] (mongo) at (14,-0.5) {\textbf{MongoDB}\\\textbf{Atlas}\\\scriptsize Free M0\\\scriptsize 2dsphere indexes\\\scriptsize Story TTL 24h};

% === EXTERNAL SERVICES ===
\node[external] (tinia) at (14,3) {\faCloudSun\ \textbf{TINIA}\\\scriptsize meteo.report API\\\scriptsize Prov. Bolzano};
\node[external] (brevo) at (14,1.5) {\faEnvelope\ \textbf{Brevo SMTP}\\\scriptsize Email transazionali\\\scriptsize verify/reset};
\node[external] (osm) at (-4.5,-1.5) {\faMap\ \textbf{OpenStreetMap}\\\scriptsize Tile server\\\scriptsize OpenTopoMap};
\node[external] (gnss) at (-4.5,1.5) {\faSatellite\ \textbf{GNSS}\\\scriptsize Hardware GPS\\\scriptsize FusedLocation};

% === FUTURE (placeholder) ===
\node[external, opacity=0.5] (mqtt) at (-4.5,-3.5) {\faWifi\ \textbf{MQTT}\\\scriptsize (Sprint 3)\\\scriptsize Mosquitto + gateway};
\node[external, opacity=0.5] (ble) at (0,-3.5) {\faBluetoothB\ \textbf{BLE Mesh}\\\scriptsize (Sprint 3)\\\scriptsize SOS fallback};

% === ARROWS ===
% Mobile -> Backend (HTTPS)
\draw[arrow] (mobile.east) -- node[label, above]{HTTPS + JWT} (renderbox.west);
% Backend internal
\draw[arrow] (express) -- (routes);
\draw[arrow] (express) -- (services);
\draw[arrow] (routes) -- (services);
\draw[arrow] (services) -- (models);
\draw[arrow] (routes) -- (social);
\draw[arrow] (social) -- (models);
% Backend -> DB
\draw[arrow] (models.east) -- (mongo.west);
% Backend -> External
\draw[arrowext] (services.north east) to[bend left=10] node[label, above, sloped]{REST cache 1h} (tinia.south);
\draw[arrowext] (services.east) -- node[label, above, sloped]{SMTP} (brevo.west);
% Mobile -> External
\draw[arrowext] (mobile.west) to[bend left=10] node[label, above, sloped]{GPS hardware} (gnss.east);
\draw[arrowext] (mobile.west) to[bend right=10] node[label, below, sloped]{Tile XYZ} (osm.east);
% Future
\draw[arrowext, opacity=0.4] (mobile.south west) to[bend right=20] (mqtt.north east);
\draw[arrowext, opacity=0.4] (mobile.south) to[bend right=10] (ble.north);

% Legend
\node[font=\tiny, anchor=south west, align=left] at (-4.5,-5) {
    \textcolor{primary!80}{$\blacksquare$} TSM internal components\\
    \textcolor{secondary!80}{$\blacksquare$} External / third-party services\\
    Dashed arrows: external API calls\\
    Faded: Sprint 3 (not yet deployed)
};

\end{tikzpicture}%
}
\caption{Diagramma deploy finale di Trento Smart Mountain a fine Sprint 2. Componenti interni: app Android nativa (Kotlin/Compose), backend Express 4 su Render, MongoDB Atlas (con TTL index 24h per Stories). Routes copre auth/hiker/sessions/social/stories/emergency; servizi esterni: TINIA meteo, Brevo SMTP, OSM tile server, hardware GNSS. I componenti in trasparenza (MQTT, BLE Mesh) sono placeholder per Sprint 3.}
\label{fig:deploy-diagram}
\end{figure}

\subsubsection*{Componenti del deploy}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{Mobile (client):} APK Android nativo distribuito sideload (Debug); production-ready per Google Play Internal Testing (futuro). Comunica con il backend via HTTPS + JWT Bearer; con \texttt{TsmAuthenticator} per refresh rotation trasparente.
\item \textbf{Backend (Render Free):} servizio Node.js 18 + Express 4 deployato su \texttt{trento-smart-mountain.onrender.com}. Auto-deploy da branch \texttt{UI}. Limitazioni: single instance, cold start $\sim$30--60\,s dopo inattivit\`a.
\item \textbf{Database (MongoDB Atlas Free M0):} cluster shared M0 con 512\,MB storage; indici 2dsphere su \texttt{location}, \texttt{activity.startPoint}; indice composto \texttt{(status, meetingDate)} su \texttt{hikesessions}; TTL su \texttt{refreshtokens} (30g). \textbf{Nuovo Sprint 2:} TTL index \texttt{expiresAt} su collection \texttt{stories} (scadenza automatica a 24h).
\item \textbf{TINIA / meteo.report:} API meteo della Provincia Autonoma di Bolzano, con cache 1\,h su MongoDB e refresh on-demand admin-only.
\item \textbf{Brevo SMTP:} email transazionali (verifica registrazione, reset password).
\item \textbf{OpenStreetMap + OpenTopoMap:} tile server pubblici usati da OSMdroid per la mappa offline-friendly.
\item \textbf{Hardware GNSS:} GPS del dispositivo Android via \texttt{FusedLocationProviderClient}; \texttt{ACCESS_BACKGROUND_LOCATION} + \texttt{WAKE_LOCK} per tracking continuo.
\item \textbf{Sprint 3 (placeholder):} MQTT (gateway Python per IoT macchinari rifugio) e BLE Mesh (SOS offline) sono pianificati ma non ancora deployati.
\end{itemize}

\newpage

\subsection{Stack Tecnologico}

Lista completa di librerie, framework e pacchetti utilizzati nello sviluppo di TSM, divisi per tier.

\subsubsection*{Backend (Node.js)}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.3}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|>{\bfseries\small}l >{\small}c >{\small\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\textbf{Pacchetto} & \textbf{Versione} & \textbf{Scopo} \\
\hline
node & 18+ & Runtime JavaScript server-side. \\
\hline
express & 4 & Framework HTTP. \\
\hline
mongoose & 8 & ODM MongoDB con discriminator support. \\
\hline
jsonwebtoken & 9 & Generazione e verifica JWT (access + refresh). \\
\hline
bcrypt & 5 & Hashing password con cost adattivo. \\
\hline
joi & 17 & Validazione schemi request body / query. \\
\hline
helmet & 7 & Security HTTP headers (CSP, HSTS). \\
\hline
express-rate-limit & 7 & Rate limit a 5 livelli. \\
\hline
express-mongo-sanitize & 2 & Anti NoSQL injection. \\
\hline
hpp & 0.2 & HTTP Parameter Pollution protection. \\
\hline
cors & 2 & CORS con allow-list. \\
\hline
nodemailer & 6 & SMTP client (Brevo). \\
\hline
swagger-ui-express & 5 & UI Swagger su \texttt{/api-docs}. \\
\hline
swagger-autogen & 2 & Generazione automatica \texttt{swagger-output.json}. \\
\hline
jest & 29 & Test framework. \\
\hline
supertest & 6 & HTTP integration testing. \\
\hline
mongodb-memory-server & 9 & MongoDB in-memory per i test Jest. \\
\hline
dotenv & 16 & Caricamento env vars. \\
\hline
axios & 1 & Client HTTP per API esterne (TINIA, meteo.report). \\
\hline
\end{tabularx}
\caption{Stack backend: dipendenze chiave del package.json.}
\label{tab:stack-backend}
\end{table}

\subsubsection*{Mobile (Android nativo Kotlin)}

\begin{table}[H]
\centering
\renewcommand{\arraystretch}{1.3}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{tabularx}{\textwidth}{|>{\bfseries\small}l >{\small}c >{\small\RaggedRight}X|}
\hline
\rowcolor{primary!20}
\textbf{Pacchetto} & \textbf{Versione} & \textbf{Scopo} \\
\hline
kotlin & 2.0.21 & Linguaggio principale. \\
\hline
gradle & 8.x & Build system. \\
\hline
compose-bom & 2024.12.01 & UI declarativa Jetpack Compose. \\
\hline
material3 & 1.3.x & Design system Material You. \\
\hline
navigation-compose & 2.8.x & Navigazione tra schermate. \\
\hline
room & 2.6.1 & Database locale SQLite (v5 con WAL). \\
\hline
retrofit & 2.11 & Client HTTP REST. \\
\hline
okhttp & 4.12 & HTTP stack (+ \texttt{Authenticator} per refresh). \\
\hline
gson & 2.10 & Serializzazione JSON. \\
\hline
security-crypto & 1.1.0 & \texttt{EncryptedSharedPreferences} per JWT. \\
\hline
play-services-location & 21.x & FusedLocationProviderClient (GPS). \\
\hline
osmdroid & 6.1.x & Mappa offline-friendly + OpenTopoMap. \\
\hline
zxing & 3.5 & Generazione QR codici invito. \\
\hline
coil-compose & 2.x & Caricamento immagini async. \\
\hline
coroutines-android & 1.7 & Async + StateFlow. \\
\hline
junit & 4.13 & Unit test (modulo). \\
\hline
\end{tabularx}
\caption{Stack mobile: dipendenze chiave di \texttt{mobile/app/build.gradle.kts}.}
\label{tab:stack-mobile}
\end{table}

\subsubsection*{Infrastruttura / DevOps}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{MongoDB Atlas} (Free Tier M0, cluster shared) -- database production.
\item \textbf{Render} (Free Tier) -- hosting backend Node.js con auto-deploy GitHub.
\item \textbf{Docker Compose} -- ambiente di sviluppo locale (MongoDB + Mosquitto MQTT placeholder).
\item \textbf{GitHub} -- VCS, code review, issue tracking, branch protection rules.
\item \textbf{Swagger / SwaggerHub} -- API documentation, alternativa ad Apiary.
\item \textbf{Brevo} -- transactional email provider (300 email/giorno free).
\item \textbf{TINIA / meteo.report} -- API meteo gratuita PAB.
\end{itemize}

\newpage

\subsection{Conclusioni}

Il Milestone 4 chiude un ciclo Agile SCRUM completo di Trento Smart Mountain, articolato su 2 sprint, di cui $\sim$650 ore di lavoro di team nel solo Sprint 2. I numeri salienti dello \textbf{Sprint 2}:

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
\item \textbf{78 SP pianificati, 71 completati (91\%)}; 7 SP in debito tecnico per Sprint 3 (US-17 alert meteo, US-27 allerta pericolo push, US-30 monitoraggio affollamento -- dipendenze hardware/IoT esterno).
\item \textbf{253 test Jest verdi}, suite eseguibile in $\sim$22\,s (\texttt{npm test}).
\item \textbf{3 bug critici identificati e fixati in-sprint} (batch del 26/05): discriminator persistence, anti-cheat server-side, JWT expiry insufficiente.
\item \textbf{24 codici di errore} centralizzati in \texttt{BUSINESS_ERROR_MAP}, rimossi 35+ blocchi \texttt{if (err.message === ...)} dalle 6 route principali.
\item \textbf{Refresh token rotation con replay detection} trasparente lato mobile (\texttt{TsmAuthenticator} OkHttp con mutex).
\item \textbf{Room v5 con WAL crash-safety} per i punti GPS durante tracking attivo.
\item \textbf{Security stack OWASP-compliant}: helmet, mongo-sanitize, hpp, rate limit 5 livelli, Joi su 100\% degli endpoint POST/PATCH/DELETE.
\end{itemize}

\textbf{Risultato strategico.} Pi\`u che la quantit\`a di feature, lo Sprint 2 ha consolidato il \textit{processo}: la Definition of Done estesa (audit a 2 passi + test automatici + discriminator alignment) ha intercettato \textit{strutturalmente} una classe intera di bug silenti che in Sprint 1 erano sfuggiti fino all'audit di fine sprint. Il team chiude il deliverable con una piattaforma robusta e una base di test che protegge le evoluzioni di Sprint 3.

\textbf{Lezione di processo.} L'investimento iniziale in audit-first development e test-driven bugfix ha pi\`u che ripagato l'effort. Il batch del 26/05 (3 bug critici risolti contestualmente con 17 test di regressione) \`e l'esempio pi\`u tangibile: senza il criterio 6 della DoD, il bug ``discriminator persistence'' sarebbe rimasto silente in produzione (l'API ritornava 200 OK!) finch\'e un utente non avesse notato che i suoi dati non venivano mai salvati. La pratica verr\`a estesa al service layer in Sprint 3.

\textbf{Prossimi passi (Sprint 3).} Sprint 3 si articola su tre priorità principali (dettaglio in \S\,\ref{sec:visione-sprint3}): (1) \textbf{saldo debito tecnico} US-17/27/30 (alert meteo push, notifiche pericolo, IoT affollamento) non completabile in Sprint 2 per dipendenze hardware; (2) \textbf{mappa avanzata e routing in-app} con editor itinerari su sentieri CAI, check-in NFC a vetta e sistema anti-overtourism; (3) \textbf{piattaforma B2B} con rifugi VIP a moltiplicatori di crediti, partnership brand sportivi con referral link, e dashboard gestionale rifugista con modulo rifiuti \& logistica (ADR-002). Infine, osservabilità di produzione con Sentry + logging strutturato + CI gate \texttt{npm audit}.
\vspace{1mm}

\textbf{Note del gruppo.}\\
Come gruppo abbiamo scelto di andare oltre i requisiti d'esame, non per dimostrare 
qualcosa, ma perché credevamo nel prodotto che stavamo costruendo.
La decisione di sviluppare un'app Android, più complessa rispetto a una web app, 
ma necessaria per garantire performance e accessibilità reali, nasce da questa 
stessa motivazione.
L'intelligenza artificiale, di cui il gruppo ha fatto uso come descritto nella 
sezione  \ref{appendix:AI}, ha contribuito a rendere questo percorso sostenibile senza 
comprometterne la qualità.
Alla fine del secondo sprint, il risultato è più di un semplice mockup, 
più di un semplice progetto universitario: è un inizio, un affacciarsi 
al mondo della tecnologia e decidere di farne parte gettando delle basi solide per continuare a sviluppare il prodotto in un futuro prossimo.

\newpage

\vfill

% ============================================================
\appendix
\section{- API implementate Sprint 1 + Sprint 2}
% ============================================================

Tutti gli endpoint sono documentati in Swagger (\texttt{swagger-output.json} nel repository; UI su \texttt{/api-docs}; documentazione integrale su SwaggerHub).

\vspace{0.5em}
\begingroup
\renewcommand{\arraystretch}{1.25}
\arrayrulecolor{primary!90}
\rowcolors{1}{primary!10}{white}
\begin{longtable}{|>{\small\ttfamily}l >{\small\ttfamily}p{6.0cm} >{\small}c >{\small}p{4.5cm}|}
\hline
\rowcolor{primary!20}
\textbf{Metodo} & \textbf{Route} & \textbf{Auth} & \textbf{Descrizione} \\
\hline
\endfirsthead
\hline
\rowcolor{primary!20}
\textbf{Metodo} & \textbf{Route} & \textbf{Auth} & \textbf{Descrizione} \\
\hline
\endhead
\hline
\multicolumn{4}{r}{\small\textit{continua\ldots}}\\
\hline
\endfoot
\hline
\endlastfoot
%
% === AUTH (S1 + S2 estensioni) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Auth (Sprint 1 + Sprint 2 extensions)}} \\
\hline
POST & /auth/register/hiker & No & Registrazione hiker (rate limit 5/h, Joi). \\
\hline
POST & /auth/register/refuge & No & Registrazione rifugio (Joi flat schema). \\
\hline
POST & /auth/login & No & Login; ritorna \texttt{accessToken}, \texttt{refreshToken}, \texttt{refreshExpiresAt}, alias \texttt{token} (backward compat). \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /auth/refresh & No (token in body) & \textbf{Refresh token rotation} con replay detection; revoca \texttt{family} su replay. \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /auth/logout & JWT & Revoca refresh token (idempotente). \textcolor{statusdone}{\faStar\ S2} \\
\hline
GET & /auth/verify/:token & No & Verifica email $\to$ deep link \texttt{tsm://}. \\
\hline
POST & /auth/forgot-password & No & Reset password (Brevo, rate limit 5/h). \\
\hline
GET / POST & /auth/reset-password/:token & No & Form HTML + JSON, monouso 1h. \\
\hline
%
% === ACCOUNT V2 (Sprint 2 nuovo) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Account v2 (Sprint 2)}} \\
\hline
GET & /account/v2/me & JWT & Profilo utente completo (con discriminator). \textcolor{statusdone}{\faStar\ S2} \\
\hline
PATCH & /account/v2/personal-info & JWT & Update \texttt{personalInfo}; lock su \texttt{birthDate}. \textcolor{statusdone}{\faStar\ S2} \\
\hline
PATCH & /account/v2/experience & JWT & Update \texttt{experience}; lock su \texttt{caiLevel}. \textcolor{statusdone}{\faStar\ S2} \\
\hline
PATCH & /account/v2/preferences & JWT & Update \texttt{preferences} (difficolt\`a, durata, dislivello). \textcolor{statusdone}{\faStar\ S2} \\
\hline
PATCH & /account/v2/goals & JWT & Update \texttt{weeklyGoals} (km, dislivello). \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /account/v2/mark-completed & JWT & Marca onboarding completato (\texttt{profileCompletedAt}). \textcolor{statusdone}{\faStar\ S2} \\
\hline
PATCH & /account/v2/change-password & JWT & Cambio password con vecchia password (rate limit). \textcolor{statusdone}{\faStar\ S2} \\
\hline
%
% === SESSIONS (S1 + S2 estensioni) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Sessions}} \\
\hline
POST & /api/v1/sessions & JWT & Crea sessione (GPX stats + invite code TSM-XXXX). \\
\hline
GET & /api/v1/sessions/my & JWT & Sessioni dell'utente (creator + partecipante). \\
\hline
GET & /api/v1/sessions/stats?year= & JWT & Statistiche aggregate annuali (HikeSession + Activity). \textcolor{statusdone}{\faStar\ S2} \\
\hline
GET & /api/v1/sessions/:id & JWT & Dettaglio sessione populated. \\
\hline
POST & /api/v1/sessions/join & JWT & Join con codice TSM-XXXX; il partecipante entra in stato \texttt{pending}. \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /api/v1/sessions/:id/participants/:uid/approve & JWT & Approva richiesta di join (capogruppo o partecipante accettato). \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /api/v1/sessions/:id/participants/:uid/reject & JWT & Rifiuta richiesta di join pendente. \textcolor{statusdone}{\faStar\ S2} \\
\hline
DELETE & /api/v1/sessions/:id/participants/:uid & JWT (creator) & Rimozione definitiva partecipante (ban per la sessione). \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /api/v1/sessions/:id/leave & JWT & Abbandona sessione (non-creator). \\
\hline
DELETE & /api/v1/sessions/:id & JWT (creator) & Elimina sessione. \\
\hline
PATCH & /api/v1/sessions/:id & JWT (creator) & Modifica sessione. \\
\hline
PATCH & /api/v1/sessions/:id/status & JWT (creator) & Lifecycle PLANNED$\to$ACTIVE$\to$ COMPLETED. \\
\hline
PATCH & /api/v1/sessions/:id/complete & JWT (creator) & Complete con \texttt{actualStats}. \textcolor{statusdone}{\faStar\ S2} \\
\hline
%
% === ACTIVITIES LIBERE (Sprint 2 nuovo) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Activities libere (Sprint 2)}} \\
\hline
POST & /api/v1/activities & JWT & Crea attivit\`a libera personale. \textcolor{statusdone}{\faStar\ S2} \\
\hline
GET & /api/v1/activities & JWT & Lista attivit\`a per utente. \textcolor{statusdone}{\faStar\ S2} \\
\hline
GET & /api/v1/activities/:id & JWT (owner) & Dettaglio attivit\`a. \textcolor{statusdone}{\faStar\ S2} \\
\hline
DELETE & /api/v1/activities/:id & JWT (owner) & Elimina attivit\`a. \textcolor{statusdone}{\faStar\ S2} \\
\hline
%
% === WEATHER (S1 + S2 fix) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Weather}} \\
\hline
GET & /weather/locations/nearby & JWT & Stazioni meteo vicine (2dsphere). \\
\hline
GET & /weather/locations/search & JWT & Ricerca stazioni per nome. \\
\hline
GET & /weather/forecast/:externalId & JWT & Forecast 3h+24h (cache 1h). \\
\hline
POST & /weather/seed & JWT+admin & Seed towns + POI da TINIA. \textcolor{statusdone}{Fix S1-C2} \\
\hline
POST & /weather/forecast/:id/refresh & JWT+admin & Force refresh forecast. \textcolor{statusdone}{Fix S1-C2} \\
\hline
%
% === NFC (preparazione Sprint 2 + 3) ===
% === SOCIAL FEED & FOLLOW (Sprint 2) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Social Feed \& Follow (Sprint 2)}} \\
\hline
GET & /api/v1/users/search & JWT & Ricerca utenti per username/nome. \\
\hline
POST & /api/v1/users/:id/follow & JWT & Segui utente (+ notifica al target). \\
\hline
DELETE & /api/v1/users/:id/follow & JWT & Smetti di seguire. \\
\hline
GET & /api/v1/users/me/feed & JWT & Feed sociale paginato (post propri + seguiti). \\
\hline
GET & /api/v1/users/me/social-row & JWT & Stato anelli avatar (storie attive, sessioni live). \\
\hline
GET & /api/v1/users/me/weekly-leaderboard & JWT & Classifica settimanale crediti tra seguiti. \\
\hline
GET & /api/v1/users/:id/posts & JWT & Post pubblicati di un profilo (privacy-gated). \\
\hline
GET & /api/v1/users/me/notifications & JWT & Notifiche (follow, like, commenti, approvazioni); varianti \texttt{unread-count}, \texttt{read}, \texttt{DELETE}. \\
\hline
POST & /api/v1/activities/:id/share & JWT & Condivide l'attivit\`a come post nel feed (DELETE per ritirarla). \\
\hline
POST & /api/v1/activities/:id/like & JWT & Like al post (DELETE per rimuoverlo). \\
\hline
POST & /api/v1/activities/:id/comments & JWT & Commenta il post (Joi; GET lista, DELETE proprio commento). \\
\hline
%
% === STORIE 24H (Sprint 2) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Storie 24h (Sprint 2)}} \\
\hline
POST & /api/v1/stories & JWT & Crea storia (media Base64 image/video, caption, overlay editor); scadenza automatica via TTL index 24h. \\
\hline
GET & /api/v1/stories/user/:userId & JWT & Storie attive (non scadute) di un autore, privacy-gated. \\
\hline
GET & /api/v1/stories/:id & JWT & Dettaglio singola storia. \\
\hline
POST & /api/v1/stories/:id/view & JWT & Marca la storia come vista (idempotente). \\
\hline
DELETE & /api/v1/stories/:id & JWT (autore) & Elimina la storia (403 per non-owner). \\
\hline
%
% === EMERGENCY SOS (Sprint 2) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Emergency SOS (Sprint 2 backend)}} \\
\hline
POST & /api/v1/emergencies & JWT & Crea segnalazione SOS con posizione GPS (Joi). \\
\hline
GET & /api/v1/emergencies/:id & JWT & Stato della segnalazione. \\
\hline
PATCH & /api/v1/emergencies/:id & JWT & Aggiorna/annulla la segnalazione (US-26). \\
\hline
%
% === NFC (Sprint 2 backend, UI Sprint 3) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small NFC (Sprint 2 backend, UI Sprint 3)}} \\
\hline
POST & /api/v1/nfc/scan & JWT & Registra scan NFC totem; \$inc \texttt{nfcStats.scansCount}. \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /api/v1/nfc/admin/totem & JWT+admin & Crea totem NFC con \texttt{tagId} univoco. \textcolor{statusdone}{\faStar\ S2} \\
\hline
%
% === QUIZ (Sprint 2 backend, UI Sprint 3) ===
\multicolumn{4}{|l|}{\cellcolor{primary!15}\textbf{\small Quiz (Sprint 2 backend, UI Sprint 3)}} \\
\hline
GET & /api/v1/quiz/categories & JWT & Lista categorie quiz disponibili (auto-seed idempotente). \textcolor{statusdone}{\faStar\ S2} \\
\hline
GET & /api/v1/quiz/:categoryId/questions & JWT & Domande di una categoria. \textcolor{statusdone}{\faStar\ S2} \\
\hline
POST & /api/v1/quiz/submit & JWT & Submit risposte e calcolo crediti. \textcolor{statusdone}{\faStar\ S2} \\
\hline

\caption{Endpoint API implementati negli Sprint 1 + 2. Marker \textcolor{statusdone}{\faStar\ S2} = nuovo o significativamente modificato in Sprint 2.}
\label{tab:api-endpoints-s2}

\end{longtable}

\endgroup

\newpage
\vspace*{\fill}
\section{Utilizzo Intelligenza Artificiale nello Sviluppo}
\label{appendix:AI}
In continuit\`a con il deliverable D3, lo sviluppo dello Sprint 2 \`e stato affiancato da strumenti di intelligenza artificiale generativa (Gemini, Claude Code), utilizzati come supporto alla progettazione, alla stesura del codice, all'audit di sicurezza e alla documentazione. Tale scelta non ha sostituito le decisioni architetturali n\'e la responsabilit\`a del team sul prodotto finale: ogni output prodotto dall'AI \`e stato \textbf{revisionato, integrato e validato manualmente}, in particolare nelle aree security-sensitive (refresh token rotation, anti-cheat server-side, audit del bug ``discriminator persistence''). Tutti i test di regressione sono stati progettati per fissare i contratti scoperti durante l'audit, indipendentemente dall'origine dell'implementazione.

L'IA \`e stata particolarmente utile in diverse aree:
\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
\item \textbf{Audit di sicurezza a 2 passi:} analisi statica CWE-classified su file modificati prima del merge.
\item \textbf{Refactor a rischio:} estrazione di \texttt{TrackingPersistenceRepository} e \texttt{SessionCommandRepository} dal \texttt{RegistraViewModel} (547$\to$501 righe) eseguita in worktree isolata.
\item \textbf{Test coverage espansione:} dal generation iniziale di 60 test  all'estensione a 258.
\item \textbf{Ingnegnerizziazione architettura backend: } consultazione per l'ingegnerizzazione efficiente ed efficace delle collections, in particolare l'integrazione della checklist dinamica all'interno della collection Hikesession.
\item \textbf{Consultazione pre sviluppo:} durante il secondo sprint ci sono state diverse idee che rendevano l'implementazione delle user stories presenti più solide.
Come nel caso della user story sulla selezione dei sentieri nell'app, sono stati integrati i filtri per raggruppare i sentieri e rendere la fase di pianificazione più accessibile e intuitiva. L'AI è stata utilizzata per consolidare le nostre idee prima di partire con l'implementazione vera e propria.
\end{enumerate}


\vspace*{\fill}
\newpage

\section{Schermate Android}

% Nota: gli screenshot di Sprint 1 (Welcome, Login, Home, Sessione) restano validi e sono riportati nel D3.
% Qui includiamo SOLO le nuove schermate introdotte in Sprint 2.
\subsection{Nuova palette e pivot grafico}

\vfill

\begin{figure}[H]
    \centering
    \includegraphics[width=0.6\textwidth]{TSM_logo_Light.png}
    \caption{Nuovo Logo Trento Smart Mountain}
\end{figure}

\vfill

\begin{figure}[H]
    \centering
    \includegraphics[width=\textwidth]{TSM_palette.png}
    \caption{Palette primaria dell'applicazione}
\end{figure}

\vfill

\subsection{Onboarding 3-step e Profilo v2}

\begin{figure}[H]
    \centering
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{Onboarding-Step1.png}
        \captionof{figure}{Onboarding -- Step 1 (Dati personali)}
        \label{fig:mockup-onb-1}
    \end{minipage}
    \hfill
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{Onboarding-Step2.png}
        \captionof{figure}{Onboarding -- Step 2 (Esperienza CAI)}
        \label{fig:mockup-onb-2}
    \end{minipage}
\end{figure}

\vspace{1em}

\begin{figure}[H]
    \centering
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{Onboarding-Step3.png}
        \captionof{figure}{Onboarding -- Step 3 (Preferenze)}
        \label{fig:mockup-onb-3}
    \end{minipage}
    \hfill
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{ProfileViewScreen.png}
        \captionof{figure}{ProfileViewScreen (read-only con lock \faLock)}
        \label{fig:mockup-profile-view}
    \end{minipage}
\end{figure}

\subsection{Activities libere e Sync Engine}

\begin{figure}[H]
    \centering
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{ActivityList-YearlyStats.png}
        \captionof{figure}{ActivityList -- Yearly Stats card}
        \label{fig:mockup-activity-stats}
    \end{minipage}
    \hfill
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{ActivityList-Risincronizza.png}
        \captionof{figure}{Pulsante ``Risincronizza (n)'' con bypass backoff}
        \label{fig:mockup-resync}
    \end{minipage}
\end{figure}

\subsection{Save Activity Dialogs (Sprint 2 UX)}

\begin{figure}[H]
    \centering
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{SaveActivity-KPIStrip.png}
        \captionof{figure}{Dialog ``Salva Attivit\`a'' con KPI strip}
        \label{fig:mockup-save-kpi}
    \end{minipage}
    \hfill
    \begin{minipage}[t]{0.44\textwidth}
        \centering
        \includegraphics[width=\textwidth]{SaveActivity-TooShort.png}
        \captionof{figure}{Dialog ``Attivit\`a troppo corta'' (3 opzioni)}
        \label{fig:mockup-too-short}
    \end{minipage}
\end{figure}

\end{document}
