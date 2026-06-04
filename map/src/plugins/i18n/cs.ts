function csPluralization(choice: number, choicesLength: number): number {
    // Special case for numbers between 2 and 4
    const some = choice > 1 && choice < 5
    if (some && choicesLength >= 2) {
        return 1
    }

    // Special case for number 1
    if (choice === 1 && choicesLength >= 3) {
        return 2
    }

    // Typical plural
    return 0
}

const csNumberFormats: Record<string, Intl.NumberFormatOptions> = {
    decimal: {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 12,
    },
}

const csDatetimeFormats: Record<string, Intl.DateTimeFormatOptions> = {
    short: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
    },
    long: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    },
    longWithDay: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        weekday: 'long',
    },
    timeShort: {
        hour: 'numeric',
        minute: 'numeric',
    },
}

const cs = {
    document: {
        title: 'Vizualizace veřejné dopravy',
        aboutPage: 'O aplikaci',
        mapPage: 'Mapa',
    },
    aboutPage: {
        title: 'Vizualizace veřejné dopravy',
        introduction: {
            p1: 'Tato webová aplikace vznikla jako výstup diplomové práce na {fitCvut} pod vedením projektu {odl}.',
            fitCvut: 'Fakultě informačních technologií ČVUT v Praze',
            odl: 'OpenDataLab',
            p2: 'Aplikace zpracovává jízdní řády veřejné hromadné dopravy zveřejňované v Celostátním informačním systému o jízdních řádech (CIS JŘ). Konkrétně se jedná o data publikovaná ve formátu NeTEx EPIP, jehož specifikace sice definuje povinnost uvádět polohy zastávek, avšak reálné datové sady tyto informace neobsahují. Z toho důvodu je nutné je získat z jiných zdrojů. V současné době aplikace integruje data z nástroje {jrUtil} vyvinutého Davidem Koňaříkem.',
            jrUtil: 'JrUtil',
        },
        tableOfContents: 'Obsah',
        appFeatures: {
            title: 'Co aplikace dělá',
            p1: 'Aplikace funguje jako funkční prototyp radaru veřejné dopravy. Na rozdíl od běžných sledovacích systémů, které jsou striktně vázány na GPS lokátory vozidel udávající jejich polohu v reálném čase (např. {pidMap}), tato aplikace odhaduje polohu vozů z jízdních řádů.',
            pidMap: 'Mapa PID',
            ul: 'Klíčové vlastnosti aplikace:',
            li1: {
                title: 'Vizualizace vozů a zastávek',
                text: 'Na interaktivním mapovém podkladu je zobrazena předpokládaná poloha vozů. Po otevření harmonogramu jízdy je zobrazena i její trasa včetně zastávek.',
            },
            li2: {
                title: 'Simulace v libovolném čase',
                text: 'Uživatel si může zvolit jakýkoliv čas a aplikace na základě jízdních řádů odhadne polohu vozidel na mapě. Aplikace dále umožňuje spustit simulaci pohybu vozů v čase.',
            },
            li3: {
                title: 'Digitální vývěsné jízdní řády',
                text: 'Součástí rozhraní je modul pro zobrazení klasických vývěsných jízdních řádů, které jsou propojeny s mapou, což uživatelům usnadňuje orientaci v geografickém i časovém kontextu.',
            },
        },
        manual: {
            title: 'Návod k použití',
            cityLines: {
                title: 'Zobrazení MHD',
                li1: 'Pro zobrazení linek MHD na mapě je nutné interaktivní mapu dostatečně přiblížit.',
                li2: 'Pro příblížení či oddálení použijte kolečko myši.',
            },
            search: {
                title: 'Vyhledání vývěsného jízdního řádu linky',
                li1: 'Do vyhledávacího pole zadejte kód linky. Pro přesné vyhledávání je nutné zadat celostátní šestimístný kód. Použitá zdrojová data veřejné dopravy mají pouze omezenou podporu pro zkrácené místní kódy.',
                li2: 'Vyberte linku ze seznamu. Jedna linka může být rozdělená do více záznamů podle období platnosti jízdního řádu.',
                li3: 'V otevřeném okně zvolte požadovaný směr jízdy v horní řadě záložek. Následně můžete v druhé řadě záložek procházet jednotlivé výpisy jízd rozdělené podle dnů, ve kterých jsou spoje provozovány.',
                li4: 'Vývěsný jízdní řád je možné otevřít i z okna harmonogramu jedné jízdy kliknutím na ikonu ',
            },
            journeyDetail: {
                title: 'Otevření harmonogramu jízdy',
                li1: 'Kliknutím na ikonu vozu zobrazíte jeho harmonogram a na mapě se vyznačí trasa, po které jede.',
                li2: 'Harmonogram vozu včetně jeho trasy lze zobrazit i kliknutím na odpovídající sloupec v okně vývěsného jízdního řádu.',
            },
            timeControls: {
                title: 'Ovládání času a animace pohybu',
                li1: 'V levém dolním rohu mapy se nachází ovládání času a animace.',
                li2: 'Okamžik zobrazení změníte zadáním požadované času a výběrem data z kalendářové nabídky.',
                li3: 'Požadovanou rychlost animace lze změnit výběrem z nabídky.',
                li4: 'Kliknutím na tlačítko START spustíte animaci. Opětovné stisknutí tlačítka animaci zastaví. Během aktivní animace není možné měnit čas zobrazení manuálně.',
            },
            openMap: {
                title: 'Otevření interaktivní mapy',
                li1: "Na interaktivní mapu přejdete kliknutím na záložku „@:{'document.mapPage'}“ v pravém horním rohu obrazovky.",
            },
            disclaimer: 'Uživatelské rozhraní aplikace je optimalizováno pro stolní počítače.',
        },
        dataSources: {
            title: 'Použitá data',
            p1: 'Zpracování dat v této aplikaci čelilo zásadní programátorské výzvě. Ministerstvo dopravy ČR sice zveřejňuje data z Celostátního informačního systému o jízdních řádech (CIS JŘ) v evropském formátu NeTEx. CIS JŘ však zcela postrádá geolokační údaje (souřadnice GPS) zastávek. Bez integrace dalších datových zdrojů tedy zobrazení na mapě není možné.',
            p2: 'Datová sada jízdních řádů neobsahuje ani jednoznačné identifikátory zastávek. Proto je nutné zastávky z CIS JŘ propojovat s jinými datovými sadami podle heuristik založených na jménu zastávky.',
            ul: 'Použité datové sady:',
            li1: {
                title: 'Jízdní řády z CIS JŘ',
                text: 'Byly použity {dataSource} ve formátu NeTEx EPIP pro veřejnou linkovou a městkou hromadnou dopravu. Vlaková doprava nebyla integrována.',
                dataSource: 'datové sady',
            },
            li2: {
                title: 'Polohové údaje z nástroje JrUtil',
                text: 'GPS souřadnice zastávek byly získávány z {dataSource} jízdních řádů vytvořené aplikací {jrUtil}.',
                dataSource: 'datové sady',
                jrUtil: 'JrUtil',
            },
            li3: {
                title: 'Polohové údaje z OpenStreetMap',
                text: 'Údaje o pozemních komunikacích pro generování trasy vozů byla čerpána z {osm}. Konkrétně byla použita {dataSource}. V současné době nejsou z této datové sady integrovány GPS souřadnice zastávek.',
                dataSource: 'data pro oblast ČR',
                osm: 'projektu OpenStreetMap',
            },
        },
        methods: {
            title: 'Použité metody a algoritmy',
            ul: 'Aby tato aplikace mohla vzniknout, bylo nutné implementovat komplexní logiku pro zpracování a vizualizaci dat:',
            li1: {
                title: 'Heuristické párování zastávek',
                text: 'Vzhledem k tomu, že data z CIS JŘ neobsahují jednoznačné identifikátory zastávek a jejich názvy jsou často nejednoznačné, využívá proces párování heuristiku založenou na (částečné) shodě textových řetězců. V případech, kdy ani tato shoda k jednoznačnému spárování nestačí, algoritmus zohledňuje kontext ostatních zastávek na dané lince.',
            },
            li2: {
                title: 'Výpočet tras po komunikacích',
                text: 'Namísto nereálného zobrazování tras pomocí přímých úseček (tzv. vzdušnou čarou) aplikace integruje mapové podklady z OpenStreetMap a využívá lokálně nasazenou směrovací službu {graphHopper}. V současné době jsou trasy generovány podle profilu pro osobní automobily.',
                graphHopper: 'GraphHopper',
            },
            li3: {
                title: 'Interpolace polohy vozidel',
                text: 'Algoritmus vezme geometricky trasy (posloupnost GPS bodů) a plánované časové průjezdy zastávkami. Následně je vypočítaná pozice v harmonogramu vozidla, která je následně lineárně interpolována na příslušnou část trasy. Pro výpočet směru jízdy je využita směrnice bodů trasy, mezi kterými se vůz nachází.',
            },
            li4: {
                title: 'Získávání dat pro vizualizaci',
                text: 'Aby se klientský prohlížeč nezahltil daty z celé republiky najednou, jsou data dynamicky načítána pouze pro viditelnou oblast na obrazovce (bounding box) a pouze pro hodinový interval odpovídající okamžiku zobrazení. Získaná data si klientský prohlížeč drží v paměti, aby nedocházelo k opakovanému přenosu stejných dat po síti.',
            },
            li5: {
                title: 'Animační smyčka',
                text: 'Každý snímek animace vyžaduje aktualizaci vizualizovaného času a přepočet poloh všech vozidel. Snímková frekvence (FPS) je však momentálně výrazně omezena, jelikož jsou tyto výpočetně náročné operace prováděny synchronně na hlavním vlákně webového prohlížeče, což může vést k jeho blokování a aplikace přestane reagovat na uživatelský vstup.',
            },
        },
    },
    footer: {
        openDataLabLogo: 'Logo OpenDataLab',
        fitCvutLogo: 'Logo FIT ČVUT',
        profinitLogo: 'Logo Profinit',
        gitHubLogo: 'Logo GitHub',
        year: '2026',
        author: 'David Gaier',
        disclaimer: 'Provozovatel neodpovídá za správnost a úplnost zpracovaných dat a informací, ani tato neověřuje a zříká se zodpovědnosti za veškeré škody a újmy, které by použitím těchto dat mohly vzniknout.',
        fitCvutAttribution: 'Aplikace vznikla jako diplomová práce na {0}.',
        fitCvut: 'FIT ČVUT',
    },
    searchLine: 'Zadejte kód linky',
    transportModes: {
        bus: 'Autobus',
        trolleybus: 'Trolejbus',
        rail: 'Vlak',
        funicular: 'Lanová dráha',
        tram: 'Tramvaj',
        metro: 'Metro',
    },
    daysOfWeek: {
        short: {
            monday: 'Po',
            tuesday: 'Út',
            wednesday: 'St',
            thursday: 'Čt',
            friday: 'Pá',
            saturday: 'So',
            sunday: 'Ne',
        },
    },
    lineVersion: {
        detour: 'Výlukový jízdní řád',
        inDirectionTo: 'Ve směru',
        operatingPeriod: 'Řád',
        validIn: 'Vydaný na:',
        activeIn: 'Platný v:',
        operatesRegularlyIn: 'Spoje pravidelně jezdí v:',
        alsoOperatesIn: 'Spoje také jedou v:',
        onlyOperatesIn: 'Spoje jedou pouze v:',
        doesNotOperateIn: 'Spoje nejedou v:',
    },
    journeyFacilities: {
        requiresOrdering: 'Spoj na objednávku',
        baggageStorage: 'Přeprava cestovních zavazadel',
        cyclesAllowed: 'Přeprava jízdních kol',
        lowFloorAccess: 'Bezbariérový přístup',
        reservationCompulsory: 'Nutné zakoupit místenku',
        reservationPossible: 'Možnost zakoupit místenku',
        snacksOnBoard: 'Občerstvení ve voze',
        unaccompaniedMinorAssistance: 'Částečně bezbariérový přístup, nutná dopomoc průvodce',
        noRoute: 'Pro tuto jízdu není trasa dostupná',
    },
    stopFacilities: {
        transportBan: 'Přeprava mezi shodně označenými zastávkami není povolena',
        onlyForBoarding: 'Zastávka je určena pouze pro nástup',
        onlyForAlighting: 'Zastávka je určena pouze pro výstup',
        requiresOrdering: 'Zastávka na objednávku',
        stopOnRequest: 'Zastávka na znamení',
        bistro: 'Občerstvení v zastávce',
        borderCrossing: 'Hraniční přechod, není zřízena zastávka',
        displaysForVisuallyImpaired: 'Úprava pro osoby se zrakovým postižením',
        lowFloorAccess: 'Bezbariérový přístup',
        parkAndRidePark: 'V okolí zastávky je P+R parkování',
        suitableForHeavilyDisabled: 'Zastávka je vhodná pro osoby s těžkým postižením',
        toilet: 'V zastávce se nachází toaleta',
        wheelChairAccessToilet: 'V zastávce se nachází toaleta s bezbariérovým přístupem',
    },
    connection: {
        cantReachServer: 'Nepovedlo se navázat spojení se serverem.',
        unknown: 'Při komunikaci se serverem došlo k neočekávané chybě.',
        badRequest: 'Byl poslán neplatný požadavek.',
    },
}

export { cs, csPluralization, csNumberFormats, csDatetimeFormats }
