\documentclass[11pt, a4paper]{article}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage{lmodern}
\let\showhyphens\relax
\usepackage[italian]{babel}
\usepackage[margin=2.5cm, headheight=15pt]{geometry}
\usepackage{graphicx}
\usepackage{float}
\usepackage[hidelinks]{hyperref}
\usepackage{tabularx}
\usepackage{longtable}
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
\usetikzlibrary{trees, positioning}
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
\fancyhead[R]{\textcolor{secondary}{\small Trento Smart Mountain \faMountain\ -- D3}}
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

% ============================================================
% FRONTESPIZIO
% ============================================================
\begin{titlepage}
    \centering
    \vspace*{3cm}
    {\Huge \textbf{\textcolor{primary}{Trento Smart Mountain \faMountain}}}\\[1cm]
    {\LARGE Deliverable D3 -- Running the Sprint!}\\[2cm]

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

    \includegraphics[width=0.3\textwidth]{logo_unitn.png}

    \vspace{1.5cm}

    {\large \textbf{Scadenza:} 17/05/2026}\\[1cm]
    {\normalsize Anno Accademico 2025/2026}
\end{titlepage}

% ============================================================
% PAGINA ABSTRACT
% ============================================================
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
    Il Deliverable D3 descrive l'adozione del framework Agile SCRUM per lo sviluppo di Trento Smart Mountain. Vengono formalizzate le dinamiche del team durante lo \textit{Sprint 1}, focalizzato sulla creazione delle fondamenta architetturali (Backend Node.js + MongoDB), dei servizi core di autenticazione JWT, della gestione completa delle sessioni escursione con tracking GPS, profilo altimetrico reale da GPX e integrazione meteo live (TINIA/meteo.report). Il documento dettaglia Product Backlog, strategia di branching, risultati degli Sprint meetings e i 3 bug critici identificati nell'audit interno di fine sprint -- a dimostrazione di un processo Agile maturo basato sull'auto-critica continua.
\end{abstractbox}
\end{center}
\vspace*{\fill}
\begin{center}
    \textcolor{secondary}{\rule{0.3\textwidth}{0.4pt}}\\
    \vspace{0.3cm}
    \footnotesize\textcolor{secondary}{Trento Smart Mountain \faMountain\ --- Running the Sprint!}
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
    \arrayrulecolor{gray!30}
    \rowcolors{2}{secondary!5}{white}
    \begin{tabularx}{\textwidth}{l l c X}
        \toprule
        \thead{Nome} & \thead{Cognome} & \thead{Matricola} & \thead{Account GitHub} \\
        \midrule
        Federico         & Cattelan & 242111 & \href{https://github.com/federicocattelan}{\texttt{@federicocattelan}} \\
        Marco Christian  & Stoica   & 246443 & \href{https://github.com/STUSSY-user}{\texttt{@STUSSY-user}} \\
        Giacomo          & Radin    & 242907 & \href{https://github.com/giacomoradin}{\texttt{@giacomoradin}} \\
        \bottomrule
    \end{tabularx}
\end{table}

\noindent Tutti e tre i componenti hanno contribuito con commit al repository condiviso, come verificabile dalla cronologia GitHub.

\subsection*{Project Idea}

Trento Smart Mountain (TSM) \`e un ecosistema digitale per l'escursionismo in Trentino-Alto Adige che integra \textbf{sicurezza attiva di gruppo} (tracciamento GPS in background, codici invito sessione, fallback SOS), \textbf{gamification educativa} (modello CAI di stima sforzo, punti per attivit\`a completate, futuri quiz NFC ai checkpoint di vetta) e \textbf{gestione rifugi} (account dedicati, telemetria IoT prevista). Il sistema combina un'app Android nativa (Kotlin 2.0 / Jetpack Compose, architettura MVVM, offline-first) con un backend Node.js + MongoDB (indici geospaziali 2dsphere, JWT) e un'integrazione meteo reale (TINIA / meteo.report) per supportare l'escursionista dalla pianificazione fino al ritorno a valle.

\subsection*{Links}

\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item \textbf{Repository GitHub:}
          \href{https://github.com/giacomoradin/Trento_Smart_Mountain}%
               {https://github.com/giacomoradin/Trento\_Smart\_Mountain}
    \item \textbf{Swagger / API Docs:} \texttt{http://<host>:3000/api-docs}
          (file \texttt{swagger-output.json} incluso nel repository)
    \item \textbf{Apiary:}
          \href{https://trentosmartmountain.docs.apiary.io}%
               {https://trentosmartmountain.docs.apiary.io}
\end{itemize}

\newpage

% ============================================================
\section{Sezione Generale}
% ============================================================

\subsection{Strategia di Branching}

Il team ha adottato una strategia basata su \textit{Git Flow} semplificata, evitando rigorosamente la logica \textit{``Master only strategy''}.

\subsubsection*{Branch attivi e storici}

\begin{table}[H]
    \centering
    \arrayrulecolor{gray!30}
    \rowcolors{2}{secondary!5}{white}
    \begin{tabularx}{\textwidth}{>{\ttfamily}l X l}
        \toprule
        \thead{Branch} & \thead{Scopo} & \thead{Stato} \\
        \midrule
        main                            & Release stabile; solo merge da PR approvate. Nessun push diretto. & Attivo \\
        UI                              & Branch di integrazione Sprint 1: convergenza feature mobile e API. & Attivo \\
        API-Meteo-Integration           & Feature branch integrazione meteo TINIA. & Mergiato \\
        auth-login-jwt                  & Feature branch autenticazione JWT. & Mergiato \\
        crud-mongodb                    & Feature branch User CRUD MongoDB. & Mergiato \\
        Swagger-setup                   & Feature branch documentazione OpenAPI/Swagger. & Mergiato \\
        18-gestione-sessione-escursione & Feature branch Issue \#18 (sessione escursione). & Mergiato \\
        Reorganizatio-Repo-Structure    & Refactor struttura cartelle repository. & Mergiato \\
        bugfix/*                        & Branch dedicati alla risoluzione di bug critici (Sprint 2). & Pianificati \\
        \bottomrule
    \end{tabularx}
\end{table}

\subsubsection*{Convenzioni operative}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
    \item Una branch per ogni Issue GitHub (formato \texttt{<numero>-<slug>} oppure \texttt{<feature>-<area>}).
    \item PR obbligatoria verso \texttt{UI} (Sprint 1) o \texttt{main} -- mai push diretto su \texttt{main}.
    \item Commit semantici: \texttt{feat:}, \texttt{fix:}, \texttt{refactor:}, \texttt{docs:}, \texttt{chore:}.
    \item Merge tramite Pull Request con revisione esplicita di almeno un altro membro del team.
\end{enumerate}

\begin{tcolorbox}[colback=primary!5, colframe=primary!40, boxrule=0.5pt, leftrule=4pt]
    \small\textbf{\faInfoCircle\ Nota per i docenti:} I branch \textbf{non vengono cancellati} dopo il merge, in conformit\`a con le indicazioni del docente, per permettere la verifica completa della storia di sviluppo nel repository GitHub.
\end{tcolorbox}

\newpage

\subsection{Product Backlog}

Di seguito il Product Backlog in formato User Story agile (priorit\`a MoSCoW). Lo stato riflette la situazione a fine Sprint 1 (16/05/2026).

\vspace{0.4em}
\noindent\small\textit{Legenda:}
\textcolor{statusdone}{\textbf{Done}} \;|\;
\textcolor{statusprogress}{\textbf{Done*}} (done con bug noto) \;|\;
\textcolor{statusprogress}{\textbf{UI-only}} \;|\;
\textcolor{statustodo}{\textbf{Sprint 2+}}
\vspace{0.4em}

\arrayrulecolor{gray!30}
\begin{longtable}{>{\bfseries\color{primary!80!black}\small}c >{\small}p{8cm} >{\small}c >{\small}c >{\small}p{2.2cm}}
    \toprule
    \textbf{ID} & \textbf{User Story} & \textbf{Priorit\`a} & \textbf{RF} & \textbf{Stato} \\
    \midrule
    \endfirsthead
    \toprule
    \textbf{ID} & \textbf{User Story} & \textbf{Priorit\`a} & \textbf{RF} & \textbf{Stato} \\
    \midrule
    \endhead
    \midrule\multicolumn{5}{r}{\small\textit{continua\ldots}}\\
    \endfoot
    \bottomrule
    \endlastfoot
    %
    US-01 & Come escursionista voglio registrarmi e verificare la mia email cos\`i da accedere al sistema in sicurezza.
           & Must & RF0 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-02 & Come escursionista voglio fare login con JWT persistito offline cos\`i da non reinserire credenziali.
           & Must & RF0 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    US-03 & Come rifugista voglio un flusso di registrazione dedicato (nome, CAI, quota, posti, coordinate) cos\`i che il backend riconosca il mio ruolo.
           & Should & RF0 ext. & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-04 & Come escursionista voglio poter resettare la password via email se la dimentico.
           & Should & RF0 ext. & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    US-05 & Come capogruppo voglio pianificare un'escursione importando un GPX e generare un codice invito \texttt{TSM-XXXX}.
           & Must & RF11 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-06 & Come partecipante voglio unirmi a una sessione inserendo il codice invito fornito dal capogruppo.
           & Must & RF7 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    US-07 & Come utente voglio vedere il dettaglio sessione con profilo altimetrico reale, meteo TINIA e checklist gestibile.
           & Must & RF1 + RF11 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-08 & Come capogruppo voglio modificare i dettagli della sessione (data, ora, difficolt\`a, partecipanti max).
           & Should & RF11 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    US-09 & Come capogruppo voglio avviare la sessione passando lo status a \texttt{ACTIVE} e attivando il tracking GPS.
           & Must & RF8 & \textcolor{statusprogress}{\textbf{Done*}} \\[2pt]
    \rowcolor{secondary!5}
    US-10 & Come utente voglio che il tracking GPS registri distanza, dislivello, quota e tempo anche con schermo spento.
           & Must & RF8, RNF9 & \textcolor{statusprogress}{\textbf{Done*}} \\[2pt]
    US-11 & Come utente voglio una mappa con la mia posizione live (OSMdroid) durante l'escursione.
           & Must & RF10 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-12 & Come utente in difficolt\`a voglio premere un SOS dalla schermata di tracciamento per ricevere soccorso.
           & Could & RF9 & \textcolor{statusprogress}{\textbf{UI-only}} \\[2pt]
    US-13 & Come utente voglio vedere la lista delle mie attivit\`a completate con statistiche per anno.
           & Should & nuovo & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    \rowcolor{secondary!5}
    US-14 & Come utente voglio vedere il dettaglio di un'attivit\`a completata (metriche + punti CAI).
           & Should & nuovo & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
    US-15 & Come utente voglio vedere il meteo reale per la localit\`a della sessione (TINIA API).
           & Should & RF6 & \textcolor{statusdone}{\textbf{Done}} \\[2pt]
\end{longtable}

\noindent\small\textit{* Done con bug noto documentato in \S\ref{sec:retrospective}. Il Product Backlog completo (con How to Demo, importanza numerica e tutti gli RF) \`e disponibile nel file \texttt{docs/Backlog V1 - Product Backlog.csv} nel repository.}

\newpage

\subsection{Definizione di ``Done''}

Una User Story \`e dichiarata \textbf{Done} se soddisfa \textit{tutti} i seguenti criteri:

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
    \item \textbf{Completamento funzionale:} Il comportamento descritto \`e implementato ed \`e verificabile sul ramo di integrazione (\texttt{UI}).
    \item \textbf{Qualit\`a del codice:} Il codice \`e completo di commenti dove necessario (logica non ovvia, scelte architetturali, edge case) e segue gli standard concordati dal team.
    \item \textbf{Integrazione nel repository:} Il lavoro \`e mergiato nel branch condiviso, senza conflitti irrisolti.
    \item \textbf{Build e stabilit\`a:} Il progetto Android compila senza errori (\texttt{./gradlew assembleDebug}); il backend parte senza eccezioni (\texttt{npm run dev}).
    \item \textbf{Verifica manuale:} \`E stata eseguita una prova del flusso principale della story (da chi ha sviluppato o tramite peer-review). Bug evidenti al primo utilizzo invalidano lo stato di Done.
    \item \textbf{Tracciabilit\`a:} La story \`e aggiornata nello strumento di sprint (Product/Sprint Backlog CSV).
    \item \textbf{Documentazione leggera:} Se la story introduce setup nuovo o variabili d'ambiente, \`e aggiornato almeno un documento del team (\texttt{docs/setup\_mobile.md} o \texttt{TSM\_PROJECT\_STATE.md}).
    \item \textbf{API contract:} Ogni nuovo endpoint \`e descritto in Swagger (\texttt{swagger-output.json}); ogni endpoint che tocca dati utente \`e protetto dal middleware \texttt{authenticate}.
\end{enumerate}

\newpage

% ============================================================
\section{Sezione Sprint \#1}
% ============================================================

\subsection{Goal}

\begin{tcolorbox}[colback=primary!5, colframe=primary!50, boxrule=0.5pt, leftrule=4pt]
\textit{``Consegnare un'app TSM dimostrabile end-to-end che permetta a un capogruppo di pianificare un'escursione da GPX, condividere un codice invito, accogliere partecipanti, avviare il tracking GPS, completare l'attivit\`a e ritrovarla in `Le mie attivit\`a', con un backend Node.js + MongoDB autenticato via JWT e integrato con un servizio meteo reale (TINIA).''}
\end{tcolorbox}

\subsection{Sprint Planning (Sprint Backlog)}

\subsubsection*{Parametri dello Sprint}

\begin{table}[H]
    \centering
    \arrayrulecolor{gray!30}
    \rowcolors{2}{secondary!5}{white}
    \begin{tabularx}{0.80\textwidth}{l X}
        \toprule
        \textbf{Durata}                  & Una settimana (09/05/2026 -- 17/05/2026) \\
        \textbf{Capacit\`a team}         & 3 membri $\times$ \mbox{$\sim$70h/settimana} $\approx$ 210h totali \\
        \textbf{Story points pianificati} & 55 \\
        \textbf{Story points completati}  & $\sim$50 (US-12 lasciata UI-only; US-09/US-10 done con bug noti) \\
        \bottomrule
    \end{tabularx}
\end{table}

\subsubsection*{Andamento dello sprint}

\begin{table}[H]
    \centering
    \arrayrulecolor{gray!30}
    \rowcolors{2}{secondary!5}{white}
    \begin{tabularx}{\textwidth}{l c c X}
        \toprule
        \thead{Settimana} & \thead{SP Pianificati} & \thead{SP Completati} & \thead{Note principali} \\
        \midrule
        SS1 & 13 & 12 & Auth completo (register/login JWT/verify email); registrazione rifugio in progress \\
        SS2 & 16 & 17 & SessionHub PIANIFICA + UNISCITI; integrazione meteo TINIA; generazione QR \\
        SS3 & 14 & 14 & SessionDetail + edit mode + profilo altimetrico Canvas + GPS tracking engine \\
        SS4 & 12 & 7 & ActivityList + Room + bugfix Sprint; 5 SP in debito (SOS backend, refinements) \\
        \bottomrule
    \end{tabularx}
\end{table}
\footnotesize{\textit{SS* $\rightarrow$ sub-sprint numero.}}

\newpage

\subsubsection*{Sprint Backlog}

\arrayrulecolor{gray!30}
\begin{longtable}{>{\bfseries\color{primary!80!black}\small}c >{\small}p{7.2cm} >{\small}l >{\small}c >{\small}p{2cm}}
    \toprule
    \textbf{US} & \textbf{Sprint Task} & \textbf{Volunteer} & \textbf{Stima} & \textbf{Status} \\
    \midrule
    \endfirsthead
    \toprule
    \textbf{US} & \textbf{Sprint Task} & \textbf{Volunteer} & \textbf{Stima} & \textbf{Status} \\
    \midrule
    \endhead
    \midrule\multicolumn{5}{r}{\small\textit{continua\ldots}}\\
    \endfoot
    \bottomrule
    \endlastfoot
    %
    US-01 & UI registrazione con step indicator, checkbox ToS GPS e validazione & Federico & 2 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-01 & Endpoint \texttt{POST /users} con hashing bcrypt e ruoli & Marco & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-01 & Invio email verifica SMTP con retry esponenziale (3x) + deep link \texttt{tsm://} & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-01 & Collezione utente MongoDB + RBAC middleware & Marco & 3 & \textcolor{statusdone}{$\checkmark$} \\
    US-03 & UI registrazione rifugio: campi CAI, quota, posti, coordinate & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-02 & UI form login con icone campo, toggle password, offline badge & Federico & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-02 & JWT Bearer token + \texttt{AuthInterceptor} OkHttp & Federico & 1 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-02 & Ripristino sessione JWT all'avvio (\texttt{AuthSession} + \texttt{TokenStorage}) & Federico & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-04 & Reset password via email: form HTML responsivo + token monouso 1h & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-05 & Schema DB Sessione (GeoJSON) + endpoint \texttt{POST /api/v1/sessions} & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    US-05 & Parser GPX: smoothing MA(5), valley-peak 10m, campionamento 50 punti & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-05 & UI PIANIFICA: form, DatePicker, TimePicker, QR preview ZXing & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    US-05 & Generazione codice invito \texttt{TSM-XXXX} (4 hex uppercase) & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-06 & UI UNISCITI: OTP boxes con \texttt{TextFieldValue} + lista sessioni & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-06 & Endpoint \texttt{POST /sessions/join} + validazione codice server & Marco & 2 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-06 & Leave/delete sessione (RemovalMode creator vs partecipante) & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-07 & Profilo altimetrico reale (Canvas normalizzato min/max, area fill) & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-07 & Checklist drag-and-drop (\texttt{ReorderableColumn}, toggle, add/remove) & Giacomo & 2 & \textcolor{statusdone}{$\checkmark$} \\
    US-07 & Integrazione meteo TINIA: modello Location 2dsphere + endpoint forecast & Marco & 4 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-08 & Endpoint \texttt{PATCH /sessions/:id} con populate fix (Gson crash fix) & Marco & 3 & \textcolor{statusdone}{$\checkmark$} \\
    US-08 & Edit mode UI (solo creator) con auto-close su successo & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-09 & Flusso AVVIA + \texttt{PATCH /sessions/:id/status} (PLANNED$\to$ACTIVE) & Giacomo & 2 & \textcolor{statusprogress}{Bug C1} \\
    US-09 & \texttt{SessionStartCoordinator} bus singleton AVVIA$\to$Registra & Federico & 1 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-10 & \texttt{FusedLocationProviderClient} + \texttt{HikeTrackingEngine} & Federico & 4 & \textcolor{statusdone}{$\checkmark$} \\
    US-10 & \texttt{ForegroundTrackingService} con notifica persistente & Federico & 4 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-10 & \texttt{StationaryDetector} auto-pause da accelerometro & Federico & 3 & \textcolor{statusdone}{$\checkmark$} \\
    US-11 & OSMdroid + marker posizione live + metriche GPS in \texttt{RegistraScreen} & Federico & 5 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-12 & UI dialog SOS con FAB in \texttt{RegistraScreen} (UI-only, backend Sprint 2) & Giacomo & 2 & \textcolor{statusprogress}{UI-only} \\
    US-13 & \texttt{ActivityListScreen} + \texttt{CompletedActivityEntity} Room & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    US-14 & Activity Detail + metriche + punti CAI calcolati (\texttt{HikeEstimation.kt}) & Giacomo & 3 & \textcolor{statusdone}{$\checkmark$} \\
    -- & Setup Swagger + Docker Compose (MongoDB + Mosquitto) + boilerplate & Marco/Giacomo & 5 & \textcolor{statusdone}{$\checkmark$} \\
    \rowcolor{secondary!5}
    -- & Bottom navigation app + ristrutturazione repository backend & Federico/Marco & 5 & \textcolor{statusdone}{$\checkmark$} \\
\end{longtable}

\subsubsection*{Burndown Chart}

\begin{figure}[H]
    \centering
    % Burndown Chart in TikZ puro (nessuna dipendenza da pgfplots)
    % Dati: riga TOTALE da Backlog V1 - Sprint 1 Backlog.csv
    % Ideale: da 200 a 0 in 7 settimane (linea retta)
    % Effettivo: 200, 181, 155, 124, 79, 30, 0
    \begin{tikzpicture}[x=1.8cm, y=0.030cm]
        % Griglia orizzontale
        \foreach \y in {0,50,100,150,200}{
            \draw[gray!30, thin] (0.5,\y) -- (7.5,\y);
        }
        % Griglia verticale
        \foreach \x in {1,...,7}{
            \draw[gray!20, thin] (\x,0) -- (\x,210);
        }
        % Assi
        \draw[secondary, thick, ->] (0.5,0) -- (7.7,0) node[right, font=\small\color{secondary}] {Settimana};
        \draw[secondary, thick, ->] (0.5,0) -- (0.5,218) node[above, font=\small\color{secondary}] {Effort};
        % Etichette asse X
        \foreach \x/\lbl in {1/D1,2/D2,3/D3,4/D4,5/D5,6/D6,7/D7}{
            \node[below, font=\footnotesize\color{secondary}] at (\x,0) {\lbl};
        }
        % Etichette asse Y
        \foreach \y in {0,50,100,150,200}{
            \node[left, font=\footnotesize\color{secondary}] at (0.5,\y) {\y};
            \draw[secondary, thin] (0.5,\y) -- (0.55,\y);
        }
        % Titolo
        \node[above, font=\bfseries\small\color{secondary}] at (4,215) {Burndown Chart -- Sprint 1};
        % Curva IDEALE (blu, linea retta 200->0)
        \draw[blue!65, thick] (1,200) -- (2,167) -- (3,133) -- (4,100) -- (5,67) -- (6,33) -- (7,0);
        \foreach \x/\y in {1/200,2/167,3/133,4/100,5/67,6/33,7/0}{
            \filldraw[blue!65] (\x,\y) rectangle ++(0.08,5) node {};
            \filldraw[blue!65] (\x,\y) circle (2.5pt);
        }
        % Curva EFFETTIVA (rosso primary, dati CSV)
        \draw[primary, thick] (1,200) -- (2,181) -- (3,155) -- (4,124) -- (5,79) -- (6,30) -- (7,10);
        \foreach \x/\y in {1/200,2/181,3/155,4/124,5/79,6/30,7/10}{
            \filldraw[primary] (\x,\y) circle (3pt);
        }
        % Legenda
        \draw[blue!65, thick] (5.2,185) -- (5.8,185);
        \filldraw[blue!65] (5.5,185) circle (2.5pt);
        \node[right, font=\footnotesize] at (5.85,185) {Ideal Work Remaining};
        \draw[primary, thick] (5.2,170) -- (5.8,170);
        \filldraw[primary] (5.5,170) circle (3pt);
        \node[right, font=\footnotesize] at (5.85,170) {Estimate Work Remaining};
        % Box legenda
        \draw[gray!40] (5.1,160) rectangle (7.4,195);
    \end{tikzpicture}
    \caption{Burndown Chart Sprint 1. L'effort iniziale \`e normalizzato a 200 unit\`a. Il team ha mantenuto un ritmo leggermente superiore all'ideale nei primi 4--5 giorni, completando circa 50/55 story points pianificati.}
\end{figure}

\newpage

\subsection{Test Cases}

\noindent\textit{Per Sprint 1 \`e sufficiente il solo \textbf{design} dei test case. La tabella seguente copre i flussi principali e documenta esplicitamente anche i bug critici identificati (TC-07, TC-08, TC-11).}

\vspace{0.5em}
\arrayrulecolor{gray!30}
\begin{longtable}{>{\bfseries\small}l >{\small}c >{\small}c >{\small}p{5cm} >{\small}p{5cm}}
    \toprule
    \textbf{TC} & \textbf{US} & \textbf{Tipo} & \textbf{Passi / Input} & \textbf{Output atteso} \\
    \midrule
    \endfirsthead
    \toprule
    \textbf{TC} & \textbf{US} & \textbf{Tipo} & \textbf{Passi / Input} & \textbf{Output atteso} \\
    \midrule
    \endhead
    \midrule\multicolumn{5}{r}{\small\textit{continua\ldots}}\\
    \endfoot
    \bottomrule
    \endlastfoot
    %
    TC-01 & US-01 & E2E manuale &
        1) Tap ``Registrati'' \newline
        2) Inserire email/pwd validi \newline
        3) Ricevere mail SMTP \newline
        4) Tap link \texttt{tsm://auth/verify/...} \newline
        5) Login &
        Login riuscito; JWT salvato in \texttt{Encrypted\-Shared\-Preferences}. \\
    \rowcolor{secondary!5}
    TC-02 & US-04 & E2E manuale &
        1) Tap ``Password dimenticata'' \newline
        2) Inserire email account esistente \newline
        3) Aprire form HTML dal link ricevuto \newline
        4) Impostare nuova password \newline
        5) Login con nuova password &
        Login riuscito con nuova password. \\
    TC-03 & US-05 & E2E manuale &
        1) Sessione $\to$ tab PIANIFICA \newline
        2) Importa file \texttt{.gpx} valido \newline
        3) Compila form (data, ora, difficolt\`a) \newline
        4) Tap ``Crea'' &
        Codice \texttt{TSM-XXXX} visualizzato; QR generato; sessione visibile in tab UNISCITI. \\
    \rowcolor{secondary!5}
    TC-04 & US-06 & E2E manuale &
        2 account: account B \newline
        1) Login \newline
        2) Tab UNISCITI \newline
        3) Inserisce codice \texttt{TSM-XXXX} &
        Sessione appare nella lista di B con dettagli corretti. \\
    TC-05 & US-07 & UI manuale &
        Sessione con GPX caricato \newline
        1) Tap sulla sessione &
        Profilo altimetrico reale (Canvas), card meteo TINIA 3h/24h, lista partecipanti con avatar, checklist drag-and-drop. \\
    \rowcolor{secondary!5}
    TC-06 & US-09 & E2E manuale &
        Sessione \texttt{PLANNED}, utente = creator \newline
        1) Apri sessione \newline
        2) Tap ``AVVIA'' &
        Status $\to$ \texttt{ACTIVE}; switch automatico a tab Registra; GPS tracking avviato. \\
    TC-07 & US-09 & E2E manuale &
        Sessione \texttt{PLANNED}, utente $\neq$ creator \newline
        1) Join tramite codice \newline
        2) Tap ``AVVIA'' &
        \textbf{Atteso:} status \texttt{ACTIVE}. \textbf{\textcolor{primary}{Attuale -- Bug C1:}} 403 silent, caricamento perpetuo. \\
    \rowcolor{secondary!5}
    TC-08 & US-10 & Device fisico &
        Tracking attivo \newline
        1) Tap AVVIA \newline
        2) Blocca schermo per 5 min \newline
        3) Sblocca schermo &
        Distanza/quota aggiornate correttamente. \textbf{\textcolor{primary}{Attuale -- Bug C3:}} tracciato GPS troncato (permesso \texttt{ACCESS\_BACKGROUND\_LOCATION} mancante). \\
    TC-09 & US-13 & UI manuale &
        Almeno 1 attivit\`a \texttt{COMPLETED} \newline
        1) Home $\to$ ``Le mie attivit\`a'' &
        Card statistiche per anno; lista attivit\`a con metriche. \\
    \rowcolor{secondary!5}
    TC-10 & US-15 & E2E manuale &
        Sessione con coordinate in Trentino \newline
        1) Apri \texttt{SessionDetailScreen} &
        Card meteo con forecast 3h e 24h (dati TINIA reali; emoji skyCondition). \\
    TC-11 & Sic. C2 & API (Postman) &
        \texttt{POST /weather/seed} SENZA header \texttt{Authorization} &
        \textbf{Atteso:} 401 Unauthorized. \textbf{\textcolor{primary}{Attuale -- Bug C2:}} 200 OK (endpoint pubblico). \\
    \rowcolor{secondary!5}
    TC-12 & US-08 & UI manuale &
        Creator su sessione \texttt{PLANNED} \newline
        1) Apri edit mode \newline
        2) Modifica nome/data \newline
        3) Tap ``Salva'' &
        Pannello edit si chiude automaticamente; dati persistiti su MongoDB. \\
    TC-13 & US-03 & E2E manuale &
        App pulita \newline
        1) Tap ``Registra Rifugio'' \newline
        2) Compila campi (nome, CAI, quota, posti, coordinate) \newline
        3) Submit &
        Account creato con \texttt{role=rifugio}; redirect a schermata verifica email. \\
\end{longtable}

\subsection{Sprint Review}

La Sprint Review si \`e svolta alla fine della settimana con una demo live di circa 15 minuti che ha coperto il seguente flusso completo:

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
    \item \textbf{(0:00--1:30) Autenticazione completa:}
          Registrazione nuovo utente $\to$ ricezione mail SMTP $\to$ tap su deep link \texttt{tsm://auth/verify/...} $\to$ login con JWT $\to$ accesso all'area principale con bottom navigation.

    \item \textbf{(1:30--5:30) Pianificazione escursione:}
          Import file GPX reale (es.\ \textit{Catinaccio.gpx}) $\to$ profilo altimetrico generato automaticamente (Canvas con area fill gradient) $\to$ stima punti CAI $\to$ generazione codice \texttt{TSM-XXXX}.

    \item \textbf{(5:30--8:00) Join partecipante (secondo account):}
          Login su secondo dispositivo $\to$ tab UNISCITI $\to$ inserimento codice $\to$ apertura \texttt{SessionDetailScreen}: meteo reale TINIA (forecast 3h/24h), checklist drag-and-drop, lista partecipanti con avatar.

    \item \textbf{(8:00--9:00) Avvio e tracking GPS:}
          Capogruppo: tap ``AVVIA'' $\to$ switch automatico a tab Registra $\to$ mappa OSMdroid con traccia GPS in tempo reale $\to$ metriche (distanza, dislivello, quota, tempo) $\to$ Stop $\to$ status \texttt{COMPLETED} persistito su MongoDB.

    \item \textbf{(9:00--12:30) Le mie attivit\`a:}
          Home $\to$ tab ``Le mie attivit\`a'' $\to$ card statistiche annuali $\to$ dettaglio attivit\`a con metriche e punti CAI calcolati con formula Naismith + modello TSM.

    \item \textbf{(12:30--15:00) Swagger API Docs:}
          Apertura di \texttt{http://localhost:3000/api-docs} $\to$ esplorazione degli endpoint implementati (auth, users, sessions, weather) con schemi request/response.
\end{enumerate}

\noindent\textbf{Aspetti rilevanti emersi dalla discussione post-demo:}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item Il profilo altimetrico reale dal GPX \`e stato il feature pi\`u apprezzato: differenzia concretamente TSM da Komoot/AllTrails su sessioni proprie del team.
    \item Il modello di stima CAI (\texttt{HikeEstimation.kt}: formula polinomiale su pendenza + equivalenza Naismith) \`e stato riconosciuto come valore differenziale rispetto alle app concorrenti.
    \item Sono stati identificati 3 bug critici (Bug C1, C2, C3) che saranno corretti come prima priorit\`a assoluta di Sprint 2.
    \item Il sistema meteo TINIA -- integrazione con geospatial 2dsphere e cache MongoDB 1h -- ha funzionato correttamente su dati reali del Trentino.
\end{itemize}

\newpage

\subsection{Product Backlog Refinement}

A seguito della Sprint Review, il team ha aggiornato il Product Backlog con le seguenti nuove User Story per Sprint 2, derivate dai bug critici e dal debito tecnico identificato nell'audit interno:

\begin{table}[H]
    \centering
    \arrayrulecolor{gray!30}
    \rowcolors{2}{secondary!5}{white}
    \begin{tabularx}{\textwidth}{>{\bfseries\color{primary!80!black}}c c X}
        \toprule
        \thead{ID} & \thead{Priorit\`a} & \thead{User Story / Motivazione} \\
        \midrule
        US-16 & Must &
            \textbf{Fix Bug C1} (\textcolor{statusdone}{\checkmark}\,\textit{applicato 17/05}) -- Pulsante AVVIA ora visibile solo al creator (\texttt{isCreator} check in \texttt{SessionDetailScreen}); i partecipanti vedono un chip informativo ``In attesa del Capogruppo''. \\
        US-17 & Must &
            \textbf{Fix Bug C2} (\textcolor{statusdone}{\checkmark}\,\textit{applicato 17/05}) -- \texttt{POST /seed} e \texttt{POST /refresh} ora richiedono \texttt{authenticate + requireRoles(''admin'')}; endpoint debug \texttt{GET /test} rimosso. \\
        US-18 & Must &
            \textbf{Fix Bug C3} (\textcolor{statusdone}{\checkmark}\,\textit{applicato 17/05}) -- \texttt{ACCESS\_BACKGROUND\_LOCATION} e \texttt{WAKE\_LOCK} aggiunti all'AndroidManifest. Flusso runtime Android 10+ da implementare in Sprint 2. \\
        US-19 & Should &
            \textbf{Backend SOS} -- Implementare \texttt{POST /api/v1/emergencies} con firma ECC per chiudere RF9 (oggi solo UI). \\
        US-20 & Could &
            \textbf{HomeScreen Social Feed} -- Feed community e attivit\`a sociali (schermata oggi placeholder). \\
        US-21 & Should &
            \textbf{WorkManager store-and-forward} -- Upload batch telemetria GPS offline al ripristino rete. \\
        US-22 & Could &
            \textbf{Socket.io real-time} -- Posizioni live del gruppo nella dashboard capogruppo (RF12). \\
        US-23 & Could &
            \textbf{BLE Mesh fallback SOS} -- Propagazione SOS offline via BLE Mesh (RF13). \\
        \bottomrule
    \end{tabularx}
\end{table}

\noindent\textbf{Variazioni al Product Backlog esistente:}
\begin{itemize}[label=\textcolor{primary}{$\bullet$}]
    \item I task relativi alla raccolta fisica dei rifiuti (RF15--RF18 di D1) erano gi\`a stati soppressi in D2 e rimangono fuori scope.
    \item La generazione di coppie di chiavi ECC lato Android Keystore (originariamente nella bozza di Sprint 1) viene posticipata a Sprint 3 per complessit\`a crittografica superiore alle stime iniziali.
    \item US-07 viene ri-prioritizzata (Should $\to$ Must) per Sprint 2 in funzione del completamento dell'algoritmo meteo/equipaggiamento automatico.
\end{itemize}

\subsection{Sprint Retrospective}
\label{sec:retrospective}

\subsubsection*{Cosa ha funzionato}

\begin{itemize}[label=\textcolor{statusdone}{\textbf{$\checkmark$}}]
    \item \textbf{Integrazione GPX $\to$ altimetria $\to$ modello CAI:} La collaborazione tra Marco (modello Location GeoJSON + meteo TINIA) e Giacomo (parser GPX con smoothing + Canvas altimetrico) si \`e incastrata efficacemente, producendo la feature pi\`u innovativa dello sprint.

    \item \textbf{Refactor backend populate simmetrico:} Il fix di \texttt{creatorId + participants.userId} con \texttt{Response<ApiMessageBody>} ha sbloccato l'edit mode PATCH che crashava per deserializzazione Gson (bug gi\`a risolto durante lo sprint).

    \item \textbf{\texttt{SessionStartCoordinator} bus singleton:} L'introduzione del coordinator ha eliminato il coupling diretto tra \texttt{SessionDetailScreen} e \texttt{RegistraScreen}, rendendo il flusso AVVIA $\to$ GPS tracking pulito e disaccoppiato.

    \item \textbf{Dark theme Material3 coerente:} Token di design condivisi (\texttt{TsmPrimary} \#3F7020, \texttt{TsmAccent} \#4FC3F7, \texttt{TsmSos} \#6B0D0D) hanno garantito coerenza visiva senza overhead.

    \item \textbf{Strategia di branching:} L'adozione di feature branches per ogni issue ha prevenuto conflitti accidentali e mantenuto \texttt{main} stabile per tutta la durata dello sprint.
\end{itemize}

\subsubsection*{Cosa non ha funzionato -- Bug Critici identificati}

\begin{tcolorbox}[colback=primary!5, colframe=primary, boxrule=0.6pt, leftrule=4pt,
    title={\faExclamationTriangle\ 3 Bug Critici -- audit interno fine sprint},
    coltitle=white, colbacktitle=primary, fonttitle=\bfseries\small]
\small
\begin{description}[leftmargin=0pt]
    \item[\textbf{Bug C1 -- Partecipante non-creator non pu\`o AVVIA:}]
        Il service \texttt{hikeSessionService.js:229} (\texttt{updateSessionStatus}) blocca il \texttt{PATCH} con \texttt{403 FORBIDDEN} se \texttt{creatorId $\neq$ userId}. Il client mostra caricamento perpetuo (silent 403). Solo il creator pu\`o passare a \texttt{ACTIVE}. Contraddice il flusso \texttt{SessionStartCoordinator} descritto nell'architettura.
        \hfill\textit{Fix: US-16 (Sprint 2)}

    \item[\textbf{Bug C2 -- Endpoint Weather non protetti:}]
        Il router \texttt{weatherRoutes.js} non chiama mai \texttt{authenticate}. \texttt{POST /weather/seed} e \texttt{POST /forecast/:id/refresh} (operazioni pesanti verso API TINIA) sono pubblici $\to$ possibile DoS economico e blacklist IP del server.
        \hfill\textit{Fix: US-17 (Sprint 2)}

    \item[\textbf{Bug C3 -- \texttt{ACCESS\_BACKGROUND\_LOCATION} mancante:}]
        Il permesso non \`e dichiarato nell'AndroidManifest. Su Android 10+, \texttt{FusedLocationProvider} interrompe gli update GPS quando lo schermo si spegne $\to$ tracciato GPS troncato $\to$ metriche e punti CAI errati. Viola RNF9 (``tracking continuo background'').
        \hfill\textit{Fix: US-18 (Sprint 2)}
\end{description}
\end{tcolorbox}

\subsubsection*{Debito tecnico noto (non bloccante)}

\begin{itemize}[label=\textcolor{statusprogress}{$\circ$}]
    \item \textbf{Pattern Repository violato in 4 ViewModel:} \texttt{ActivityListViewModel}, \texttt{SessionPlanViewModel}, \texttt{SessionJoinViewModel} e \texttt{SessionDetailViewModel} chiamano \texttt{TsmApiClient.service()} direttamente, bypassando il layer Repository. Impatta testabilit\`a e manutenibilit\`a.

    \item \textbf{\texttt{User.sessionRoles} non nello schema Mongoose:} Il service fa \texttt{\$push} su un campo non dichiarato $\to$ Mongoose scarta silenziosamente il write con \texttt{strict: true}. Il sistema dei ruoli per-sessione non viene persistito.

    \item \textbf{\texttt{meetingDate} come \texttt{String} invece di \texttt{Date}:} L'ordinamento \texttt{sort(\{meetingDate: 1\})} \`e lessicografico, non temporale. Instabile cross-client se il formato cambia.

    \item \textbf{Zero unit test scritti:} Sprint 2 introdurr\`a almeno una test class JUnit per i ViewModel e un test Jest per \texttt{hikeSessionService}.

    \item \textbf{Codice morto da rimuovere:} \texttt{AppRepository.kt} (interfaccia vuota), \texttt{LocalDataSource.kt} (singleton vuoto), endpoint \texttt{GET /weather/test} (debug esposto).
\end{itemize}

\subsubsection*{Action items per Sprint 2}

\begin{enumerate}[label=\textcolor{primary}{\textbf{\arabic*.}}]
    \item \textbf{Triage bug critici} \textcolor{statusdone}{(\checkmark\ completato 17/05 -- audit interno)}: C1 $\to$ gating AVVIA al creator; C2 $\to$ \texttt{authenticate + requireRoles(''admin'')} su \texttt{/weather/seed} e \texttt{/refresh}; C3 $\to$ \texttt{ACCESS\_BACKGROUND\_LOCATION} aggiunto al manifest.
    \item \textbf{Pulizia codice morto} \textcolor{statusdone}{(\checkmark\ completato 17/05)}: cancellati \texttt{AppRepository.kt}, \texttt{LocalDataSource.kt} e l'endpoint \texttt{GET /weather/test}.
    \item \textbf{Setup CI minimale:} GitHub Actions con build APK (\texttt{./gradlew assembleDebug}) + ESLint backend, per intercettare regressioni automaticamente.
    \item \textbf{Aggiornare KDoc orfani:} commenti obsoleti in \texttt{TsmApplication.kt} e \texttt{TsmApiService.kt} identificati durante l'audit.
    \item \textbf{Migrare \texttt{meetingDate} a \texttt{Date}:} script di backfill MongoDB per i documenti esistenti (ordinamento temporale vs lessicografico).
\end{enumerate}

\subsubsection*{Dinamiche di team e pratiche Agile}

Il team ha adottato un approccio \textit{parzialmente asincrono} per il Daily Scrum (via messaggistica), con sync call vocale settimanale. La principale lezione appresa: \textbf{3 bug critici scoperti solo nell'audit di fine sprint} evidenziano la necessit\`a di un \textit{Definition of Done check} sistematico al termine di ogni settimana, non solo a chiusura sprint. Questa pratica verr\`a istituzionalizzata in Sprint 2 con una \textit{sync call} bisettimanale dedicata a verifica e triage.

La separazione rigida dei compiti (backend Marco; mobile Federico/Giacomo; integrazione meteo Marco) ha permesso di accelerare nelle prime settimane senza conflitti, ma ha evidenziato la necessit\`a di allineare meglio gli \textit{standard architetturali} (pattern Repository) prima di iniziare un nuovo sprint.

% ============================================================
\appendix
\section{Appendice A -- API implementate Sprint 1}
% ============================================================

Tutti gli endpoint sono documentati in Swagger (\texttt{swagger-output.json} nel repository; UI su \texttt{/api-docs}).

\vspace{0.5em}
\arrayrulecolor{gray!30}
\begin{longtable}{>{\small\ttfamily}l >{\small\ttfamily}p{6.5cm} >{\small}c >{\small}p{4.5cm}}
    \toprule
    \textbf{Metodo} & \textbf{Route} & \textbf{Auth} & \textbf{Descrizione} \\
    \midrule
    \endfirsthead
    \toprule
    \textbf{Metodo} & \textbf{Route} & \textbf{Auth} & \textbf{Descrizione} \\
    \midrule
    \endhead
    \midrule\multicolumn{4}{r}{\small\textit{continua\ldots}}\\
    \endfoot
    \bottomrule
    \endlastfoot
    %
    POST   & /auth/login                        & No  & Login con email/password; ritorna JWT. \\
    \rowcolor{secondary!5}
    GET    & /auth/verify/:token                & No  & Verifica email $\to$ redirect deep link \texttt{tsm://}. \\
    POST   & /auth/forgot-password              & No  & Richiesta reset password (invio link via SMTP). \\
    \rowcolor{secondary!5}
    GET    & /auth/reset-password/:token        & No  & Form HTML responsive reset password. \\
    POST   & /auth/reset-password/:token        & No  & Salva nuova password (accetta JSON o form). \\
    \rowcolor{secondary!5}
    POST   & /users                             & No  & Registrazione utente o rifugio (\texttt{rifugioDetails}). \\
    GET    & /users/:id                         & JWT & Profilo utente. \\
    \rowcolor{secondary!5}
    PUT    & /users/:id                         & JWT+admin & Aggiorna dati utente. \\
    DELETE & /users/:id                         & JWT+admin & Elimina utente. \\
    \rowcolor{secondary!5}
    POST   & /api/v1/sessions                   & JWT & Crea sessione (GPX stats + \texttt{estimatedPoints} CAI). \\
    GET    & /api/v1/sessions/my                & JWT & Le mie sessioni (populate creator + partecipanti). \\
    \rowcolor{secondary!5}
    GET    & /api/v1/sessions/:id               & JWT & Dettaglio sessione fully populated. \\
    POST   & /api/v1/sessions/join              & JWT & Join con codice \texttt{TSM-XXXX}. \\
    \rowcolor{secondary!5}
    POST   & /api/v1/sessions/:id/leave         & JWT & Abbandona sessione (non-creator). \\
    DELETE & /api/v1/sessions/:id               & JWT (creator) & Elimina sessione e rimuove partecipanti. \\
    \rowcolor{secondary!5}
    PATCH  & /api/v1/sessions/:id               & JWT (creator) & Modifica dettagli (populate fix Gson-safe). \\
    PATCH  & /api/v1/sessions/:id/status        & JWT (creator) & Ciclo vita: PLANNED$\to$ACTIVE$\to$COMPLETED. \\
    \rowcolor{secondary!5}
    GET    & /weather/locations/nearby          & No$^{*}$ & Stazioni meteo vicine per coordinate (2dsphere). \\
    GET    & /weather/locations/search          & No$^{*}$ & Ricerca stazioni per nome (case-insensitive). \\
    \rowcolor{secondary!5}
    GET    & /weather/forecast/:externalId      & No$^{*}$ & Forecast 3h + 24h (cache MongoDB 1h). \\
    POST   & /weather/forecast/:externalId/refresh & JWT+admin & Forza refresh forecast. \textcolor{statusdone}{\checkmark\ Fix C2 17/05} \\
    \rowcolor{secondary!5}
    POST   & /weather/seed                      & JWT+admin & Popola DB con towns + POI da API TINIA. \textcolor{statusdone}{\checkmark\ Fix C2 17/05} \\
\end{longtable}

\noindent\small\textit{Tutti gli endpoint \texttt{POST /weather/seed} e \texttt{POST /weather/forecast/:id/refresh} richiedono ora \texttt{Bearer JWT + role=admin} (fix Bug C2 applicato 17/05). I \texttt{GET /weather/*} rimangono pubblici (dati meteo non sensibili).}

\end{document}
