package hh.game.mgba_android.tracker.tables

/**
 * Static BST (Base Stat Total) lookup table extracted from the Ironmon Tracker Lua source
 * (PokemonData.lua, PokemonData.Pokemon table).
 *
 * 0-indexed: index 0 unused, index 1 = Bulbasaur (318), …, index 411 = Chimecho (425).
 * Blank placeholder slots (252–276) hold 0.
 */
object BstTable {

    private val TABLE = intArrayOf(
        0, // index 0 unused
        318, 405, 525, 309, 405, 534, 314, 405, 530, 195, // 1-10
        205, 385, 195, 205, 385, 251, 349, 469, 253, 413, // 11-20
        262, 442, 288, 438, 300, 475, 300, 450, 275, 365, // 21-30
        495, 273, 365, 495, 323, 473, 299, 505, 270, 425, // 31-40
        245, 455, 320, 395, 480, 285, 405, 305, 450, 265, // 41-50
        405, 290, 440, 320, 500, 305, 455, 350, 555, 300, // 51-60
        385, 500, 310, 400, 490, 305, 405, 505, 300, 390, // 61-70
        480, 335, 515, 300, 390, 485, 410, 500, 315, 490, // 71-80
        325, 465, 352, 310, 460, 325, 475, 325, 500, 305, // 81-90
        525, 310, 405, 500, 385, 328, 483, 325, 475, 330, // 91-100
        480, 325, 520, 320, 425, 455, 455, 385, 340, 490, // 101-110
        345, 485, 450, 435, 490, 295, 440, 320, 450, 340, // 111-120
        520, 460, 500, 455, 490, 495, 500, 490, 200, 540, // 121-130
        535, 288, 325, 525, 525, 525, 395, 355, 495, 355, // 131-140
        495, 515, 540, 580, 580, 580, 300, 420, 600, 680, // 141-150
        600, 318, 405, 525, 309, 405, 534, 314, 405, 530, // 151-160
        215, 415, 262, 442, 265, 390, 250, 390, 535, 330, // 161-170
        460, 205, 218, 210, 245, 405, 320, 470, 280, 365, // 171-180
        500, 480, 250, 410, 410, 500, 250, 340, 450, 360, // 181-190
        180, 425, 390, 210, 430, 525, 525, 405, 490, 435, // 191-200
        336, 405, 455, 290, 465, 415, 430, 510, 300, 450, // 201-210
        430, 500, 505, 500, 430, 330, 500, 250, 410, 250, // 211-220
        450, 380, 300, 480, 330, 465, 465, 330, 500, 540, // 221-230
        330, 500, 515, 465, 250, 210, 455, 305, 360, 365, // 231-240
        490, 540, 580, 580, 580, 300, 410, 600, 680, 680, // 241-250
        600, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 251-260
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 261-270
        0, 0, 0, 0, 0, 0, 310, 405, 530, 310, // 271-280
        405, 530, 310, 405, 535, 220, 420, 240, 420, 195, // 281-290
        205, 385, 205, 385, 220, 340, 480, 220, 340, 480, // 291-300
        266, 456, 236, 270, 430, 295, 460, 360, 270, 430, // 301-310
        269, 414, 400, 500, 260, 380, 440, 300, 500, 375, // 311-320
        470, 380, 288, 468, 330, 308, 468, 200, 540, 305, // 321-330
        460, 290, 340, 520, 237, 474, 295, 475, 305, 460, // 331-340
        290, 410, 530, 335, 475, 300, 480, 440, 440, 190, // 341-350
        330, 470, 405, 405, 380, 280, 410, 310, 490, 260, // 351-360
        295, 455, 400, 280, 440, 670, 302, 467, 460, 240, // 361-370
        360, 480, 345, 485, 485, 465, 295, 455, 458, 458, // 371-380
        485, 330, 430, 530, 420, 400, 400, 355, 495, 355, // 381-390
        495, 198, 278, 518, 300, 420, 600, 300, 420, 600, // 391-400
        580, 580, 580, 670, 670, 680, 600, 600, 600, 600, // 401-410
        425, // 411
    )

    // NatDex ROM hack species (IDs 412–1235) — from CyanSMP64/NatDexExtension natDexMons table
    private val NATDEX_BST: Map<Int, Int> = mapOf(
    412 to 318,  // Turtwig
    413 to 405,  // Grotle
    414 to 525,  // Torterra
    415 to 309,  // Chimchar
    416 to 405,  // Monferno
    417 to 534,  // Infernape
    418 to 314,  // Piplup
    419 to 405,  // Prinplup
    420 to 530,  // Empoleon
    421 to 245,  // Starly
    422 to 340,  // Staravia
    423 to 485,  // Staraptor
    424 to 250,  // Bidoof
    425 to 410,  // Bibarel
    426 to 194,  // Kricketot
    427 to 384,  // Kricketune
    428 to 263,  // Shinx
    429 to 363,  // Luxio
    430 to 523,  // Luxray
    431 to 280,  // Budew
    432 to 515,  // Roserade
    433 to 350,  // Cranidos
    434 to 495,  // Rampardos
    435 to 350,  // Shieldon
    436 to 495,  // Bastiodon
    437 to 224,  // Burmy
    438 to 424,  // Wormadam
    439 to 424,  // Mothim
    440 to 244,  // Combee
    441 to 474,  // Vespiquen
    442 to 405,  // Pachirisu
    443 to 330,  // Buizel
    444 to 495,  // Floatzel
    445 to 275,  // Cherubi
    446 to 450,  // Cherrim
    447 to 325,  // Shellos
    448 to 475,  // Gastrodon
    449 to 482,  // Ambipom
    450 to 348,  // Drifloon
    451 to 498,  // Drifblim
    452 to 350,  // Buneary
    453 to 480,  // Lopunny
    454 to 495,  // Mismagius
    455 to 505,  // Honchkrow
    456 to 310,  // Glameow
    457 to 452,  // Purugly
    458 to 285,  // Chingling
    459 to 329,  // Stunky
    460 to 479,  // Skuntank
    461 to 300,  // Bronzor
    462 to 500,  // Bronzong
    463 to 290,  // Bonsly
    464 to 310,  // Mime Jr.
    465 to 220,  // Happiny
    466 to 411,  // Chatot
    467 to 485,  // Spiritomb
    468 to 300,  // Gible
    469 to 410,  // Gabite
    470 to 600,  // Garchomp
    471 to 390,  // Munchlax
    472 to 285,  // Riolu
    473 to 525,  // Lucario
    474 to 330,  // Hippopotas
    475 to 525,  // Hippowdon
    476 to 330,  // Skorupi
    477 to 500,  // Drapion
    478 to 300,  // Croagunk
    479 to 490,  // Toxicroak
    480 to 454,  // Carnivine
    481 to 330,  // Finneon
    482 to 460,  // Lumineon
    483 to 345,  // Mantyke
    484 to 334,  // Snover
    485 to 494,  // Abomasnow
    486 to 510,  // Weavile
    487 to 535,  // Magnezone
    488 to 515,  // Lickilicky
    489 to 535,  // Rhyperior
    490 to 535,  // Tangrowth
    491 to 540,  // Electivire
    492 to 540,  // Magmortar
    493 to 545,  // Togekiss
    494 to 515,  // Yanmega
    495 to 525,  // Leafeon
    496 to 525,  // Glaceon
    497 to 510,  // Gliscor
    498 to 530,  // Mamoswine
    499 to 535,  // Porygon-Z
    500 to 518,  // Gallade
    501 to 525,  // Probopass
    502 to 525,  // Dusknoir
    503 to 480,  // Froslass
    504 to 440,  // Rotom
    505 to 580,  // Uxie
    506 to 580,  // Mesprit
    507 to 580,  // Azelf
    508 to 680,  // Dialga
    509 to 680,  // Palkia
    510 to 600,  // Heatran
    511 to 670,  // Regigigas
    512 to 680,  // Giratina
    513 to 580,  // Cresselia
    514 to 480,  // Phione
    515 to 600,  // Manaphy
    516 to 600,  // Darkrai
    517 to 600,  // Shaymin
    518 to 720,  // Arceus
    519 to 600,  // Victini
    520 to 308,  // Snivy
    521 to 413,  // Servine
    522 to 528,  // Serperior
    523 to 308,  // Tepig
    524 to 418,  // Pignite
    525 to 528,  // Emboar
    526 to 308,  // Oshawott
    527 to 413,  // Dewott
    528 to 528,  // Samurott
    529 to 255,  // Patrat
    530 to 420,  // Watchog
    531 to 275,  // Lillipup
    532 to 370,  // Herdier
    533 to 500,  // Stoutland
    534 to 281,  // Purrloin
    535 to 446,  // Liepard
    536 to 316,  // Pansage
    537 to 498,  // Simisage
    538 to 316,  // Pansear
    539 to 498,  // Simisear
    540 to 316,  // Panpour
    541 to 498,  // Simipour
    542 to 292,  // Munna
    543 to 487,  // Musharna
    544 to 264,  // Pidove
    545 to 358,  // Tranquill
    546 to 488,  // Unfezant
    547 to 295,  // Blitzle
    548 to 497,  // Zebstrika
    549 to 280,  // Roggenrola
    550 to 390,  // Boldore
    551 to 515,  // Gigalith
    552 to 323,  // Woobat
    553 to 425,  // Swoobat
    554 to 328,  // Drilbur
    555 to 508,  // Excadrill
    556 to 445,  // Audino
    557 to 305,  // Timburr
    558 to 405,  // Gurdurr
    559 to 505,  // Conkeldurr
    560 to 294,  // Tympole
    561 to 384,  // Palpitoad
    562 to 509,  // Seismitoad
    563 to 465,  // Throh
    564 to 465,  // Sawk
    565 to 310,  // Sewaddle
    566 to 380,  // Swadloon
    567 to 500,  // Leavanny
    568 to 260,  // Venipede
    569 to 360,  // Whirlipede
    570 to 485,  // Scolipede
    571 to 280,  // Cottonee
    572 to 480,  // Whimsicott
    573 to 280,  // Petilil
    574 to 480,  // Lilligant
    575 to 460,  // Basculin
    576 to 292,  // Sandile
    577 to 351,  // Krokorok
    578 to 519,  // Krookodile
    579 to 315,  // Darumaka
    580 to 480,  // Darmanitan
    581 to 461,  // Maractus
    582 to 325,  // Dwebble
    583 to 485,  // Crustle
    584 to 348,  // Scraggy
    585 to 488,  // Scrafty
    586 to 490,  // Sigilyph
    587 to 303,  // Yamask
    588 to 483,  // Cofagrigus
    589 to 355,  // Tirtouga
    590 to 495,  // Carracosta
    591 to 401,  // Archen
    592 to 567,  // Archeops
    593 to 329,  // Trubbish
    594 to 474,  // Garbodor
    595 to 330,  // Zorua
    596 to 510,  // Zoroark
    597 to 300,  // Minccino
    598 to 470,  // Cinccino
    599 to 290,  // Gothita
    600 to 390,  // Gothorita
    601 to 490,  // Gothitelle
    602 to 290,  // Solosis
    603 to 370,  // Duosion
    604 to 490,  // Reuniclus
    605 to 305,  // Ducklett
    606 to 473,  // Swanna
    607 to 305,  // Vanillite
    608 to 395,  // Vanillish
    609 to 535,  // Vanilluxe
    610 to 335,  // Deerling
    611 to 475,  // Sawsbuck
    612 to 428,  // Emolga
    613 to 315,  // Karrablast
    614 to 495,  // Escavalier
    615 to 294,  // Foongus
    616 to 464,  // Amoonguss
    617 to 335,  // Frillish
    618 to 480,  // Jellicent
    619 to 470,  // Alomomola
    620 to 319,  // Joltik
    621 to 472,  // Galvantula
    622 to 305,  // Ferroseed
    623 to 489,  // Ferrothorn
    624 to 300,  // Klink
    625 to 440,  // Klang
    626 to 520,  // Klinklang
    627 to 275,  // Tynamo
    628 to 405,  // Eelektrik
    629 to 515,  // Eelektross
    630 to 335,  // Elgyem
    631 to 485,  // Beheeyem
    632 to 275,  // Litwick
    633 to 370,  // Lampent
    634 to 520,  // Chandelure
    635 to 320,  // Axew
    636 to 410,  // Fraxure
    637 to 540,  // Haxorus
    638 to 305,  // Cubchoo
    639 to 505,  // Beartic
    640 to 515,  // Cryogonal
    641 to 305,  // Shelmet
    642 to 495,  // Accelgor
    643 to 471,  // Stunfisk
    644 to 350,  // Mienfoo
    645 to 510,  // Mienshao
    646 to 485,  // Druddigon
    647 to 303,  // Golett
    648 to 483,  // Golurk
    649 to 340,  // Pawniard
    650 to 490,  // Bisharp
    651 to 490,  // Bouffalant
    652 to 350,  // Rufflet
    653 to 510,  // Braviary
    654 to 370,  // Vullaby
    655 to 510,  // Mandibuzz
    656 to 484,  // Heatmor
    657 to 484,  // Durant
    658 to 300,  // Deino
    659 to 420,  // Zweilous
    660 to 600,  // Hydreigon
    661 to 360,  // Larvesta
    662 to 550,  // Volcarona
    663 to 580,  // Cobalion
    664 to 580,  // Terrakion
    665 to 580,  // Virizion
    666 to 580,  // Tornadus
    667 to 580,  // Thundurus
    668 to 680,  // Reshiram
    669 to 680,  // Zekrom
    670 to 600,  // Landorus
    671 to 660,  // Kyurem
    672 to 580,  // Keldeo
    673 to 600,  // Meloetta
    674 to 600,  // Genesect
    675 to 313,  // Chespin
    676 to 405,  // Quilladin
    677 to 530,  // Chesnaught
    678 to 307,  // Fennekin
    679 to 409,  // Braixen
    680 to 534,  // Delphox
    681 to 314,  // Froakie
    682 to 405,  // Frogadier
    683 to 530,  // Greninja
    684 to 237,  // Bunnelby
    685 to 423,  // Diggersby
    686 to 278,  // Fletchling
    687 to 382,  // Fletchinder
    688 to 499,  // Talonflame
    689 to 200,  // Scatterbug
    690 to 213,  // Spewpa
    691 to 411,  // Vivillon
    692 to 369,  // Litleo
    693 to 507,  // Pyroar
    694 to 303,  // Flabébé
    695 to 371,  // Floette
    696 to 552,  // Florges
    697 to 350,  // Skiddo
    698 to 531,  // Gogoat
    699 to 348,  // Pancham
    700 to 495,  // Pangoro
    701 to 472,  // Furfrou
    702 to 355,  // Espurr
    703 to 466,  // Meowstic
    704 to 325,  // Honedge
    705 to 448,  // Doublade
    706 to 500,  // Aegislash
    707 to 341,  // Spritzee
    708 to 462,  // Aromatisse
    709 to 341,  // Swirlix
    710 to 480,  // Slurpuff
    711 to 288,  // Inkay
    712 to 482,  // Malamar
    713 to 306,  // Binacle
    714 to 500,  // Barbaracle
    715 to 320,  // Skrelp
    716 to 494,  // Dragalge
    717 to 330,  // Clauncher
    718 to 500,  // Clawitzer
    719 to 289,  // Helioptile
    720 to 481,  // Heliolisk
    721 to 362,  // Tyrunt
    722 to 521,  // Tyrantrum
    723 to 362,  // Amaura
    724 to 521,  // Aurorus
    725 to 525,  // Sylveon
    726 to 500,  // Hawlucha
    727 to 431,  // Dedenne
    728 to 500,  // Carbink
    729 to 300,  // Goomy
    730 to 452,  // Sliggoo
    731 to 600,  // Goodra
    732 to 470,  // Klefki
    733 to 309,  // Phantump
    734 to 474,  // Trevenant
    735 to 335,  // Pumpkaboo
    736 to 494,  // Gourgeist
    737 to 304,  // Bergmite
    738 to 514,  // Avalugg
    739 to 245,  // Noibat
    740 to 535,  // Noivern
    741 to 680,  // Xerneas
    742 to 680,  // Yveltal
    743 to 600,  // Zygarde
    744 to 600,  // Diancie
    745 to 600,  // Hoopa
    746 to 600,  // Volcanion
    747 to 320,  // Rowlet
    748 to 420,  // Dartrix
    749 to 530,  // Decidueye
    750 to 320,  // Litten
    751 to 420,  // Torracat
    752 to 530,  // Incineroar
    753 to 320,  // Popplio
    754 to 420,  // Brionne
    755 to 530,  // Primarina
    756 to 265,  // Pikipek
    757 to 355,  // Trumbeak
    758 to 485,  // Toucannon
    759 to 253,  // Yungoos
    760 to 418,  // Gumshoos
    761 to 300,  // Grubbin
    762 to 400,  // Charjabug
    763 to 500,  // Vikavolt
    764 to 338,  // Crabrawler
    765 to 478,  // Crabominable
    766 to 476,  // Oricorio
    767 to 304,  // Cutiefly
    768 to 464,  // Ribombee
    769 to 280,  // Rockruff
    770 to 487,  // Lycanroc
    771 to 175,  // Wishiwashi
    772 to 305,  // Mareanie
    773 to 495,  // Toxapex
    774 to 385,  // Mudbray
    775 to 500,  // Mudsdale
    776 to 269,  // Dewpider
    777 to 454,  // Araquanid
    778 to 250,  // Fomantis
    779 to 480,  // Lurantis
    780 to 285,  // Morelull
    781 to 405,  // Shiinotic
    782 to 320,  // Salandit
    783 to 480,  // Salazzle
    784 to 340,  // Stufful
    785 to 500,  // Bewear
    786 to 210,  // Bounsweet
    787 to 290,  // Steenee
    788 to 510,  // Tsareena
    789 to 485,  // Comfey
    790 to 490,  // Oranguru
    791 to 490,  // Passimian
    792 to 230,  // Wimpod
    793 to 530,  // Golisopod
    794 to 320,  // Sandygast
    795 to 480,  // Palossand
    796 to 410,  // Pyukumuku
    797 to 534,  // Type: Null
    798 to 570,  // Silvally
    799 to 440,  // Minior
    800 to 480,  // Komala
    801 to 485,  // Turtonator
    802 to 435,  // Togedemaru
    803 to 476,  // Mimikyu
    804 to 475,  // Bruxish
    805 to 485,  // Drampa
    806 to 517,  // Dhelmise
    807 to 300,  // Jangmo-o
    808 to 420,  // Hakamo-o
    809 to 600,  // Kommo-o
    810 to 570,  // Tapu Koko
    811 to 570,  // Tapu Lele
    812 to 570,  // Tapu Bulu
    813 to 570,  // Tapu Fini
    814 to 200,  // Cosmog
    815 to 400,  // Cosmoem
    816 to 680,  // Solgaleo
    817 to 680,  // Lunala
    818 to 570,  // Nihilego
    819 to 570,  // Buzzwole
    820 to 570,  // Pheromosa
    821 to 570,  // Xurkitree
    822 to 570,  // Celesteela
    823 to 570,  // Kartana
    824 to 570,  // Guzzlord
    825 to 600,  // Necrozma
    826 to 600,  // Magearna
    827 to 600,  // Marshadow
    828 to 420,  // Poipole
    829 to 540,  // Naganadel
    830 to 570,  // Stakataka
    831 to 570,  // Blacephalon
    832 to 600,  // Zeraora
    833 to 300,  // Meltan
    834 to 600,  // Melmetal
    835 to 310,  // Grookey
    836 to 420,  // Thwackey
    837 to 530,  // Rillaboom
    838 to 310,  // Scorbunny
    839 to 420,  // Raboot
    840 to 530,  // Cinderace
    841 to 310,  // Sobble
    842 to 420,  // Drizzile
    843 to 530,  // Inteleon
    844 to 275,  // Skwovet
    845 to 460,  // Greedent
    846 to 245,  // Rookidee
    847 to 365,  // Corvisquire
    848 to 495,  // Corviknight
    849 to 180,  // Blipbug
    850 to 335,  // Dottler
    851 to 505,  // Orbeetle
    852 to 245,  // Nickit
    853 to 455,  // Thievul
    854 to 250,  // Gossifleur
    855 to 460,  // Eldegoss
    856 to 270,  // Wooloo
    857 to 490,  // Dubwool
    858 to 284,  // Chewtle
    859 to 485,  // Drednaw
    860 to 270,  // Yamper
    861 to 490,  // Boltund
    862 to 240,  // Rolycoly
    863 to 410,  // Carkol
    864 to 510,  // Coalossal
    865 to 260,  // Applin
    866 to 485,  // Flapple
    867 to 485,  // Appletun
    868 to 315,  // Silicobra
    869 to 510,  // Sandaconda
    870 to 475,  // Cramorant
    871 to 280,  // Arrokuda
    872 to 490,  // Barraskewda
    873 to 242,  // Toxel
    874 to 502,  // Toxtricity
    875 to 305,  // Sizzlipede
    876 to 525,  // Centiskorch
    877 to 310,  // Clobbopus
    878 to 480,  // Grapploct
    879 to 308,  // Sinistea
    880 to 508,  // Polteageist
    881 to 265,  // Hatenna
    882 to 370,  // Hattrem
    883 to 510,  // Hatterene
    884 to 265,  // Impidimp
    885 to 370,  // Morgrem
    886 to 510,  // Grimmsnarl
    887 to 520,  // Obstagoon
    888 to 440,  // Perrserker
    889 to 510,  // Cursola
    890 to 507,  // Sirfetch'd
    891 to 520,  // Mr. Rime
    892 to 483,  // Runerigus
    893 to 270,  // Milcery
    894 to 495,  // Alcremie
    895 to 470,  // Falinks
    896 to 435,  // Pincurchin
    897 to 185,  // Snom
    898 to 475,  // Frosmoth
    899 to 470,  // Stonjourner
    900 to 470,  // Eiscue
    901 to 475,  // Indeedee
    902 to 436,  // Morpeko
    903 to 330,  // Cufant
    904 to 500,  // Copperajah
    905 to 505,  // Dracozolt
    906 to 505,  // Arctozolt
    907 to 505,  // Dracovish
    908 to 505,  // Arctovish
    909 to 535,  // Duraludon
    910 to 270,  // Dreepy
    911 to 410,  // Drakloak
    912 to 600,  // Dragapult
    913 to 660,  // Zacian
    914 to 660,  // Zamazenta
    915 to 690,  // Eternatus
    916 to 385,  // Kubfu
    917 to 550,  // Urshifu
    918 to 600,  // Zarude
    919 to 580,  // Regieleki
    920 to 580,  // Regidrago
    921 to 580,  // Glastrier
    922 to 580,  // Spectrier
    923 to 500,  // Calyrex
    924 to 525,  // Wyrdeer
    925 to 500,  // Kleavor
    926 to 550,  // Ursaluna
    927 to 530,  // Basculegion
    928 to 510,  // Sneasler
    929 to 510,  // Overqwil
    930 to 580,  // Enamorus
    931 to 310,  // Sprigatito
    932 to 410,  // Floragato
    933 to 530,  // Meowscarada
    934 to 310,  // Fuecoco
    935 to 411,  // Crocalor
    936 to 530,  // Skeledirge
    937 to 310,  // Quaxly
    938 to 410,  // Quaxwell
    939 to 530,  // Quaquaval
    940 to 254,  // Lechonk
    941 to 489,  // Oinkologne
    942 to 210,  // Tarountula
    943 to 404,  // Spidops
    944 to 210,  // Nymble
    945 to 450,  // Lokix
    946 to 240,  // Pawmi
    947 to 350,  // Pawmo
    948 to 490,  // Pawmot
    949 to 305,  // Tandemaus
    950 to 470,  // Maushold
    951 to 312,  // Fidough
    952 to 477,  // Dachsbun
    953 to 260,  // Smoliv
    954 to 354,  // Dolliv
    955 to 510,  // Arboliva
    956 to 417,  // Squawkabilly
    957 to 280,  // Nacli
    958 to 355,  // Naclstack
    959 to 500,  // Garganacl
    960 to 255,  // Charcadet
    961 to 525,  // Armarouge
    962 to 525,  // Ceruledge
    963 to 272,  // Tadbulb
    964 to 495,  // Bellibolt
    965 to 280,  // Wattrel
    966 to 490,  // Kilowattrel
    967 to 340,  // Maschiff
    968 to 505,  // Mabosstiff
    969 to 290,  // Shroodle
    970 to 485,  // Grafaiai
    971 to 275,  // Bramblin
    972 to 480,  // Brambleghast
    973 to 335,  // Toedscool
    974 to 515,  // Toedscruel
    975 to 450,  // Klawf
    976 to 304,  // Capsakid
    977 to 486,  // Scovillain
    978 to 270,  // Rellor
    979 to 470,  // Rabsca
    980 to 255,  // Flittle
    981 to 481,  // Espathra
    982 to 297,  // Tinkatink
    983 to 380,  // Tinkatuff
    984 to 506,  // Tinkaton
    985 to 245,  // Wiglett
    986 to 425,  // Wugtrio
    987 to 485,  // Bombirdier
    988 to 315,  // Finizen
    989 to 457,  // Palafin
    990 to 300,  // Varoom
    991 to 500,  // Revavroom
    992 to 501,  // Cyclizar
    993 to 480,  // Orthworm
    994 to 350,  // Glimmet
    995 to 525,  // Glimmora
    996 to 290,  // Greavard
    997 to 488,  // Houndstone
    998 to 500,  // Flamigo
    999 to 334,  // Cetoddle
    1000 to 521,  // Cetitan
    1001 to 478,  // Veluza
    1002 to 530,  // Dondozo
    1003 to 475,  // Tatsugiri
    1004 to 535,  // Annihilape
    1005 to 430,  // Clodsire
    1006 to 520,  // Farigiraf
    1007 to 520,  // Dudunsparce
    1008 to 550,  // Kingambit
    1009 to 570,  // Great Tusk
    1010 to 570,  // Scream Tail
    1011 to 570,  // Brute Bonnet
    1012 to 570,  // Flutter Mane
    1013 to 570,  // Slither Wing
    1014 to 570,  // Sandy Shocks
    1015 to 570,  // Iron Treads
    1016 to 570,  // Iron Bundle
    1017 to 570,  // Iron Hands
    1018 to 570,  // Iron Jugulis
    1019 to 570,  // Iron Moth
    1020 to 570,  // Iron Thorns
    1021 to 320,  // Frigibax
    1022 to 423,  // Arctibax
    1023 to 600,  // Baxcalibur
    1024 to 300,  // Gimmighoul
    1025 to 550,  // Gholdengo
    1026 to 570,  // Wo-Chien
    1027 to 570,  // Chien-Pao
    1028 to 570,  // Ting-Lu
    1029 to 570,  // Chi-Yu
    1030 to 590,  // Roaring Moon
    1031 to 590,  // Iron Valiant
    1032 to 670,  // Koraidon
    1033 to 670,  // Miraidon
    1034 to 590,  // Walking Wake
    1035 to 590,  // Iron Leaves
    1036 to 485,  // Dipplin
    1037 to 308,  // Poltchageist
    1038 to 508,  // Sinistcha
    1039 to 555,  // Okidogi
    1040 to 555,  // Munkidori
    1041 to 555,  // Fezandipiti
    1042 to 550,  // Ogerpon
    1043 to 600,  // Archaludon
    1044 to 540,  // Hydrapple
    1045 to 590,  // Gouging Fire
    1046 to 590,  // Raging Bolt
    1047 to 590,  // Iron Boulder
    1048 to 590,  // Iron Crown
    1049 to 450,  // Terapagos
    1050 to 600,  // Pecharunt
    1051 to 625,  // Venusaur M
    1052 to 634,  // Charizard X
    1053 to 634,  // Charizard Y
    1054 to 630,  // Blastoise M
    1055 to 495,  // Beedrill M
    1056 to 579,  // Pidgeot M
    1057 to 600,  // Alakazam M
    1058 to 590,  // Slowbro M
    1059 to 600,  // Gengar M
    1060 to 590,  // Kangaskhan M
    1061 to 600,  // Pinsir M
    1062 to 640,  // Gyarados M
    1063 to 615,  // Aerodactyl M
    1064 to 780,  // Mewtwo X
    1065 to 780,  // Mewtwo Y
    1066 to 610,  // Ampharos M
    1067 to 610,  // Steelix M
    1068 to 600,  // Scizor M
    1069 to 600,  // Heracross M
    1070 to 600,  // Houndoom M
    1071 to 700,  // Tyranitar M
    1072 to 630,  // Sceptile M
    1073 to 630,  // Blaziken M
    1074 to 635,  // Swampert M
    1075 to 618,  // Gardevoir M
    1076 to 480,  // Sableye M
    1077 to 480,  // Mawile M
    1078 to 630,  // Aggron M
    1079 to 510,  // Medicham M
    1080 to 575,  // Manectric M
    1081 to 560,  // Sharpedo M
    1082 to 560,  // Camerupt M
    1083 to 590,  // Altaria M
    1084 to 555,  // Banette M
    1085 to 565,  // Absol M
    1086 to 580,  // Glalie M
    1087 to 700,  // Salamence M
    1088 to 700,  // Metagross M
    1089 to 700,  // Latias M
    1090 to 700,  // Latios M
    1091 to 580,  // Lopunny M
    1092 to 700,  // Garchomp M
    1093 to 625,  // Lucario M
    1094 to 594,  // Abomasnow M
    1095 to 618,  // Gallade M
    1096 to 545,  // Audino M
    1097 to 700,  // Diancie M
    1098 to 780,  // Rayquaza M
    1099 to 770,  // Kyogre P
    1100 to 770,  // Groudon P
    1101 to 253,  // Rattata A
    1102 to 413,  // Raticate A
    1103 to 485,  // Raichu A
    1104 to 300,  // Sandshrew A
    1105 to 450,  // Sandslash A
    1106 to 299,  // Vulpix A
    1107 to 505,  // Ninetales A
    1108 to 265,  // Diglett A
    1109 to 425,  // Dugtrio A
    1110 to 290,  // Meowth A
    1111 to 440,  // Persian A
    1112 to 300,  // Geodude A
    1113 to 390,  // Graveler A
    1114 to 495,  // Golem A
    1115 to 325,  // Grimer A
    1116 to 500,  // Muk A
    1117 to 530,  // Exeggutor A
    1118 to 425,  // Marowak A
    1119 to 290,  // Meowth G
    1120 to 410,  // Ponyta G
    1121 to 500,  // Rapidash G
    1122 to 315,  // Slowpoke G
    1123 to 490,  // Slowbro G
    1124 to 377,  // Farfetch'd G
    1125 to 490,  // Weezing G
    1126 to 460,  // Mr. Mime G
    1127 to 580,  // Articuno G
    1128 to 580,  // Zapdos G
    1129 to 580,  // Moltres G
    1130 to 490,  // Slowking G
    1131 to 410,  // Corsola G
    1132 to 240,  // Zigzagoon G
    1133 to 420,  // Linoone G
    1134 to 315,  // Darumaka G
    1135 to 480,  // Darmanitan G
    1136 to 303,  // Yamask G
    1137 to 471,  // Stunfisk G
    1138 to 350,  // Growlithe H
    1139 to 555,  // Arcanine H
    1140 to 330,  // Voltorb H
    1141 to 490,  // Electrode H
    1142 to 534,  // Typhlosion H
    1143 to 440,  // Qwilfish H
    1144 to 430,  // Sneasel H
    1145 to 528,  // Samurott H
    1146 to 480,  // Lilligant H
    1147 to 330,  // Zorua H
    1148 to 510,  // Zoroark H
    1149 to 510,  // Braviary H
    1150 to 452,  // Sliggoo H
    1151 to 600,  // Goodra H
    1152 to 514,  // Avalugg H
    1153 to 530,  // Decidueye H
    1154 to 490,  // Tauros P
    1155 to 210,  // Wooper P
    1156 to 320,  // Pikachu C
    1157 to 430,  // Pikachu P
    1158 to 490,  // Tauros P F
    1159 to 490,  // Tauros P W
    1160 to 435,  // Eevee P
    1161 to 205,  // Pichu S
    1162 to 420,  // Castform F
    1163 to 420,  // Castform W
    1164 to 420,  // Castform I
    1165 to 600,  // Deoxys Atk
    1166 to 600,  // Deoxys Def
    1167 to 600,  // Deoxys Spe
    1168 to 224,  // Burmy S
    1169 to 224,  // Burmy T
    1170 to 424,  // Wormadam S
    1171 to 424,  // Wormadam T
    1172 to 519,  // Cherrim S
    1173 to 520,  // Rotom Heat
    1174 to 520,  // Rotom Wash
    1175 to 520,  // Rotom Frost
    1176 to 520,  // Rotom Fan
    1177 to 520,  // Rotom Mow
    1178 to 680,  // Dialga O
    1179 to 680,  // Palkia O
    1180 to 680,  // Giratina O
    1181 to 600,  // Shaymin S
    1182 to 460,  // Basculin B
    1183 to 460,  // Basculin W
    1184 to 540,  // Darmanitan Z
    1185 to 540,  // Darmanitan Z G
    1186 to 580,  // Tornadus T
    1187 to 580,  // Thundurus T
    1188 to 600,  // Landorus T
    1189 to 700,  // Kyurem W
    1190 to 700,  // Kyurem B
    1191 to 600,  // Meloetta P
    1192 to 640,  // Greninja A
    1193 to 551,  // Floette E
    1194 to 466,  // Meowstic F
    1195 to 500,  // Aegislash B
    1196 to 335,  // Pumpkaboo S
    1197 to 335,  // Pumpkaboo L
    1198 to 335,  // Pumpkaboo X
    1199 to 494,  // Gourgeist S
    1200 to 494,  // Gourgeist L
    1201 to 494,  // Gourgeist X
    1202 to 486,  // Zygarde 10
    1203 to 708,  // Zygarde C
    1204 to 680,  // Hoopa U
    1205 to 476,  // Oricorio E
    1206 to 476,  // Oricorio P
    1207 to 476,  // Oricorio G
    1208 to 487,  // Lycanroc M
    1209 to 487,  // Lycanroc D
    1210 to 620,  // Wishiwashi S
    1211 to 500,  // Minior C
    1212 to 680,  // Necrozma DM
    1213 to 680,  // Necrozma DW
    1214 to 754,  // Necrozma U
    1215 to 502,  // Toxtricity L
    1216 to 470,  // Eiscue N
    1217 to 475,  // Indeedee F
    1218 to 436,  // Morpeko H
    1219 to 700,  // Zacian C
    1220 to 700,  // Zamazenta C
    1221 to 1125,  // Eternatus E
    1222 to 550,  // Urshifu R
    1223 to 680,  // Calyrex I
    1224 to 680,  // Calyrex S
    1225 to 555,  // Ursaluna B
    1226 to 530,  // Basculegion F
    1227 to 580,  // Enamorus T
    1228 to 489,  // Oinkologne F
    1229 to 650,  // Palafin H
    1230 to 300,  // Gimmighoul R
    1231 to 550,  // Ogerpon W
    1232 to 550,  // Ogerpon F
    1233 to 550,  // Ogerpon R
    1234 to 600,  // Terapagos T
    1235 to 700,  // Terapagos S
    )

    /** Returns the BST for the given species ID, or 0 if out of range. */
    fun bst(speciesId: Int): Int =
        if (speciesId in 1 until TABLE.size) TABLE[speciesId]
        else NATDEX_BST[speciesId] ?: 0
}
