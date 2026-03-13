package hh.game.mgba_android.tracker.tables

object EvolutionTable {

    // Map of speciesId -> full evolution chain list (base to final) for that species' specific path
    private val chains: Map<Int, List<Int>> = buildMap {
        // Bulbasaur line
        val bulba = listOf(1, 2, 3)
        put(1, bulba); put(2, bulba); put(3, bulba)

        // Charmander line
        val charm = listOf(4, 5, 6)
        put(4, charm); put(5, charm); put(6, charm)

        // Squirtle line
        val squirt = listOf(7, 8, 9)
        put(7, squirt); put(8, squirt); put(9, squirt)

        // Caterpie line
        val cater = listOf(10, 11, 12)
        put(10, cater); put(11, cater); put(12, cater)

        // Weedle line
        val weed = listOf(13, 14, 15)
        put(13, weed); put(14, weed); put(15, weed)

        // Pidgey line
        val pidgey = listOf(16, 17, 18)
        put(16, pidgey); put(17, pidgey); put(18, pidgey)

        // Rattata line
        val ratta = listOf(19, 20)
        put(19, ratta); put(20, ratta)

        // Spearow line
        val spear = listOf(21, 22)
        put(21, spear); put(22, spear)

        // Ekans line
        val ekans = listOf(23, 24)
        put(23, ekans); put(24, ekans)

        // Pichu -> Pikachu -> Raichu
        val pika = listOf(172, 25, 26)
        put(172, pika); put(25, pika); put(26, pika)

        // Sandshrew line
        val sand = listOf(27, 28)
        put(27, sand); put(28, sand)

        // Nidoran-F line
        val nidoF = listOf(29, 30, 31)
        put(29, nidoF); put(30, nidoF); put(31, nidoF)

        // Nidoran-M line
        val nidoM = listOf(32, 33, 34)
        put(32, nidoM); put(33, nidoM); put(34, nidoM)

        // Cleffa -> Clefairy -> Clefable
        val clef = listOf(173, 35, 36)
        put(173, clef); put(35, clef); put(36, clef)

        // Vulpix line
        val vul = listOf(37, 38)
        put(37, vul); put(38, vul)

        // Igglybuff -> Jigglypuff -> Wigglytuff
        val jigg = listOf(174, 39, 40)
        put(174, jigg); put(39, jigg); put(40, jigg)

        // Zubat -> Golbat -> Crobat
        val zubat = listOf(41, 42, 169)
        put(41, zubat); put(42, zubat); put(169, zubat)

        // Oddish -> Gloom -> Vileplume (branch)
        val gloomVile = listOf(43, 44, 45)
        put(43, gloomVile); put(44, gloomVile); put(45, gloomVile)
        // Oddish -> Gloom -> Bellossom (branch)
        val gloomBell = listOf(43, 44, 182)
        put(182, gloomBell)

        // Paras line
        val paras = listOf(46, 47)
        put(46, paras); put(47, paras)

        // Venonat line
        val veno = listOf(48, 49)
        put(48, veno); put(49, veno)

        // Diglett line
        val dig = listOf(50, 51)
        put(50, dig); put(51, dig)

        // Meowth line
        val meow = listOf(52, 53)
        put(52, meow); put(53, meow)

        // Psyduck line
        val psy = listOf(54, 55)
        put(54, psy); put(55, psy)

        // Mankey line
        val mank = listOf(56, 57)
        put(56, mank); put(57, mank)

        // Growlithe line
        val growl = listOf(58, 59)
        put(58, growl); put(59, growl)

        // Poliwag -> Poliwhirl -> Poliwrath (branch)
        val poliWrath = listOf(60, 61, 62)
        put(60, poliWrath); put(61, poliWrath); put(62, poliWrath)
        // Poliwag -> Poliwhirl -> Politoed (branch)
        val poliToed = listOf(60, 61, 186)
        put(186, poliToed)

        // Abra line
        val abra = listOf(63, 64, 65)
        put(63, abra); put(64, abra); put(65, abra)

        // Machop line
        val mach = listOf(66, 67, 68)
        put(66, mach); put(67, mach); put(68, mach)

        // Bellsprout line
        val bell = listOf(69, 70, 71)
        put(69, bell); put(70, bell); put(71, bell)

        // Tentacool line
        val tent = listOf(72, 73)
        put(72, tent); put(73, tent)

        // Geodude line
        val geo = listOf(74, 75, 76)
        put(74, geo); put(75, geo); put(76, geo)

        // Ponyta line
        val pon = listOf(77, 78)
        put(77, pon); put(78, pon)

        // Slowpoke -> Slowbro (branch)
        val slowBro = listOf(79, 80)
        put(79, slowBro); put(80, slowBro)
        // Slowpoke -> Slowking (branch)
        val slowKing = listOf(79, 199)
        put(199, slowKing)

        // Magnemite line
        val mag = listOf(81, 82)
        put(81, mag); put(82, mag)

        // Doduo line
        val dod = listOf(84, 85)
        put(84, dod); put(85, dod)

        // Seel line
        val seel = listOf(86, 87)
        put(86, seel); put(87, seel)

        // Grimer line
        val grim = listOf(88, 89)
        put(88, grim); put(89, grim)

        // Shellder line
        val shell = listOf(90, 91)
        put(90, shell); put(91, shell)

        // Gastly line
        val gast = listOf(92, 93, 94)
        put(92, gast); put(93, gast); put(94, gast)

        // Onix -> Steelix
        val onix = listOf(95, 208)
        put(95, onix); put(208, onix)

        // Drowzee line
        val drow = listOf(96, 97)
        put(96, drow); put(97, drow)

        // Krabby line
        val krab = listOf(98, 99)
        put(98, krab); put(99, krab)

        // Voltorb line
        val volt = listOf(100, 101)
        put(100, volt); put(101, volt)

        // Exeggcute line
        val egg = listOf(102, 103)
        put(102, egg); put(103, egg)

        // Cubone line
        val cub = listOf(104, 105)
        put(104, cub); put(105, cub)

        // Tyrogue branches
        val tyroLee = listOf(236, 106)
        put(106, tyroLee)
        val tyroChan = listOf(236, 107)
        put(107, tyroChan)
        val tyroTop = listOf(236, 237)
        put(236, tyroLee) // default chain for Tyrogue itself
        put(237, tyroTop)

        // Koffing line
        val koff = listOf(109, 110)
        put(109, koff); put(110, koff)

        // Rhyhorn line
        val rhy = listOf(111, 112)
        put(111, rhy); put(112, rhy)

        // Chansey -> Blissey
        val chan = listOf(113, 242)
        put(113, chan); put(242, chan)

        // Horsea -> Seadra -> Kingdra
        val horse = listOf(116, 117, 230)
        put(116, horse); put(117, horse); put(230, horse)

        // Goldeen line
        val gold = listOf(118, 119)
        put(118, gold); put(119, gold)

        // Staryu line
        val star = listOf(120, 121)
        put(120, star); put(121, star)

        // Scyther -> Scizor
        val scy = listOf(123, 212)
        put(123, scy); put(212, scy)

        // Smoochum -> Jynx
        val smooch = listOf(238, 124)
        put(238, smooch); put(124, smooch)

        // Elekid -> Electabuzz
        val elek = listOf(239, 125)
        put(239, elek); put(125, elek)

        // Magby -> Magmar
        val magby = listOf(240, 126)
        put(240, magby); put(126, magby)

        // Magikarp line
        val magi = listOf(129, 130)
        put(129, magi); put(130, magi)

        // Eevee branches
        val eeveeVap = listOf(133, 134)
        put(134, eeveeVap)
        val eeveeJolt = listOf(133, 135)
        put(135, eeveeJolt)
        val eeveeFlar = listOf(133, 136)
        put(136, eeveeFlar)
        val eeveeEsp = listOf(133, 196)
        put(196, eeveeEsp)
        val eeveeUmb = listOf(133, 197)
        put(197, eeveeUmb)
        put(133, eeveeVap) // default for Eevee itself

        // Porygon -> Porygon2
        val pory = listOf(137, 233)
        put(137, pory); put(233, pory)

        // Omanyte line
        val oman = listOf(138, 139)
        put(138, oman); put(139, oman)

        // Kabuto line
        val kabu = listOf(140, 141)
        put(140, kabu); put(141, kabu)

        // Dratini line
        val drat = listOf(147, 148, 149)
        put(147, drat); put(148, drat); put(149, drat)

        // Chikorita line
        val chiko = listOf(152, 153, 154)
        put(152, chiko); put(153, chiko); put(154, chiko)

        // Cyndaquil line
        val cynda = listOf(155, 156, 157)
        put(155, cynda); put(156, cynda); put(157, cynda)

        // Totodile line
        val toto = listOf(158, 159, 160)
        put(158, toto); put(159, toto); put(160, toto)

        // Sentret line
        val sent = listOf(161, 162)
        put(161, sent); put(162, sent)

        // Hoothoot line
        val hoot = listOf(163, 164)
        put(163, hoot); put(164, hoot)

        // Ledyba line
        val ledy = listOf(165, 166)
        put(165, ledy); put(166, ledy)

        // Spinarak line
        val spin = listOf(167, 168)
        put(167, spin); put(168, spin)

        // Chinchou line
        val chin = listOf(170, 171)
        put(170, chin); put(171, chin)

        // Togepi line
        val toge = listOf(175, 176)
        put(175, toge); put(176, toge)

        // Natu line
        val natu = listOf(177, 178)
        put(177, natu); put(178, natu)

        // Mareep line
        val mare = listOf(179, 180, 181)
        put(179, mare); put(180, mare); put(181, mare)

        // Hoppip line
        val hopp = listOf(187, 188, 189)
        put(187, hopp); put(188, hopp); put(189, hopp)

        // Sunkern line
        val sunk = listOf(191, 192)
        put(191, sunk); put(192, sunk)

        // Wooper line
        val woop = listOf(194, 195)
        put(194, woop); put(195, woop)

        // Wynaut -> Wobbuffet
        val wyWobb = listOf(360, 202)
        put(360, wyWobb); put(202, wyWobb)

        // Pineco line
        val pine = listOf(204, 205)
        put(204, pine); put(205, pine)

        // Snubbull line
        val snub = listOf(209, 210)
        put(209, snub); put(210, snub)

        // Teddiursa line
        val tedd = listOf(216, 217)
        put(216, tedd); put(217, tedd)

        // Slugma line
        val slug = listOf(218, 219)
        put(218, slug); put(219, slug)

        // Swinub line
        val swin = listOf(220, 221)
        put(220, swin); put(221, swin)

        // Remoraid line
        val rem = listOf(223, 224)
        put(223, rem); put(224, rem)

        // Houndour line
        val hound = listOf(228, 229)
        put(228, hound); put(229, hound)

        // Phanpy line
        val phan = listOf(231, 232)
        put(231, phan); put(232, phan)

        // Larvitar line
        val larvi = listOf(246, 247, 248)
        put(246, larvi); put(247, larvi); put(248, larvi)

        // Azurill -> Marill -> Azumarill
        val azu = listOf(298, 183, 184)
        put(298, azu); put(183, azu); put(184, azu)

        // Treecko line
        val tree = listOf(252, 253, 254)
        put(252, tree); put(253, tree); put(254, tree)

        // Torchic line
        val torch = listOf(255, 256, 257)
        put(255, torch); put(256, torch); put(257, torch)

        // Mudkip line
        val mud = listOf(258, 259, 260)
        put(258, mud); put(259, mud); put(260, mud)

        // Poochyena line
        val pooch = listOf(261, 262)
        put(261, pooch); put(262, pooch)

        // Zigzagoon line
        val zigz = listOf(263, 264)
        put(263, zigz); put(264, zigz)

        // Wurmple -> Silcoon -> Beautifly (branch)
        val wurmBeaut = listOf(265, 266, 267)
        put(265, wurmBeaut); put(266, wurmBeaut); put(267, wurmBeaut)
        // Wurmple -> Cascoon -> Dustox (branch)
        val wurmDust = listOf(265, 268, 269)
        put(268, wurmDust); put(269, wurmDust)

        // Lotad line
        val lota = listOf(270, 271, 272)
        put(270, lota); put(271, lota); put(272, lota)

        // Seedot line
        val seed = listOf(273, 274, 275)
        put(273, seed); put(274, seed); put(275, seed)

        // Taillow line
        val tail = listOf(276, 277)
        put(276, tail); put(277, tail)

        // Wingull line
        val wing = listOf(278, 279)
        put(278, wing); put(279, wing)

        // Ralts line
        val ralts = listOf(280, 281, 282)
        put(280, ralts); put(281, ralts); put(282, ralts)

        // Surskit line
        val surs = listOf(283, 284)
        put(283, surs); put(284, surs)

        // Shroomish line
        val shroom = listOf(285, 286)
        put(285, shroom); put(286, shroom)

        // Slakoth line
        val slak = listOf(287, 288, 289)
        put(287, slak); put(288, slak); put(289, slak)

        // Nincada -> Ninjask (branch)
        val nincNinjask = listOf(290, 291)
        put(290, nincNinjask); put(291, nincNinjask)
        // Nincada -> Shedinja (branch)
        val nincShed = listOf(290, 292)
        put(292, nincShed)

        // Whismur line
        val whis = listOf(293, 294, 295)
        put(293, whis); put(294, whis); put(295, whis)

        // Makuhita line
        val maku = listOf(296, 297)
        put(296, maku); put(297, maku)

        // Skitty line
        val skit = listOf(300, 301)
        put(300, skit); put(301, skit)

        // Aron line
        val aron = listOf(304, 305, 306)
        put(304, aron); put(305, aron); put(306, aron)

        // Meditite line
        val medi = listOf(307, 308)
        put(307, medi); put(308, medi)

        // Electrike line
        val elec = listOf(309, 310)
        put(309, elec); put(310, elec)

        // Gulpin line
        val gulp = listOf(316, 317)
        put(316, gulp); put(317, gulp)

        // Carvanha line
        val carv = listOf(318, 319)
        put(318, carv); put(319, carv)

        // Wailmer line
        val wail = listOf(320, 321)
        put(320, wail); put(321, wail)

        // Numel line
        val numel = listOf(322, 323)
        put(322, numel); put(323, numel)

        // Spoink line
        val spoink = listOf(325, 326)
        put(325, spoink); put(326, spoink)

        // Trapinch line
        val trap = listOf(328, 329, 330)
        put(328, trap); put(329, trap); put(330, trap)

        // Cacnea line
        val cacn = listOf(331, 332)
        put(331, cacn); put(332, cacn)

        // Swablu line
        val swab = listOf(333, 334)
        put(333, swab); put(334, swab)

        // Barboach line
        val barb = listOf(339, 340)
        put(339, barb); put(340, barb)

        // Corphish line
        val corf = listOf(341, 342)
        put(341, corf); put(342, corf)

        // Baltoy line
        val balt = listOf(343, 344)
        put(343, balt); put(344, balt)

        // Lileep line
        val lilee = listOf(345, 346)
        put(345, lilee); put(346, lilee)

        // Anorith line
        val anor = listOf(347, 348)
        put(347, anor); put(348, anor)

        // Feebas line
        val feeb = listOf(349, 350)
        put(349, feeb); put(350, feeb)

        // Shuppet line
        val shupp = listOf(353, 354)
        put(353, shupp); put(354, shupp)

        // Duskull line
        val dusk = listOf(355, 356)
        put(355, dusk); put(356, dusk)

        // Snorunt line
        val snor = listOf(361, 362)
        put(361, snor); put(362, snor)

        // Spheal line
        val spheal = listOf(363, 364, 365)
        put(363, spheal); put(364, spheal); put(365, spheal)

        // Clamperl -> Huntail (branch)
        val clamHunt = listOf(366, 367)
        put(366, clamHunt); put(367, clamHunt)
        // Clamperl -> Gorebyss (branch)
        val clamGore = listOf(366, 368)
        put(368, clamGore)

        // Bagon line
        val bagon = listOf(371, 372, 373)
        put(371, bagon); put(372, bagon); put(373, bagon)

        // Beldum line
        val beld = listOf(374, 375, 376)
        put(374, beld); put(375, beld); put(376, beld)
    }

    /**
     * Returns the full evolution chain for the given species ID, from base form to final form
     * along the specific evolutionary path of that species.
     * If the species has no evolutions (legendary, single-form, or not found), returns listOf(speciesId).
     */
    fun getChain(speciesId: Int): List<Int> {
        return chains[speciesId] ?: listOf(speciesId)
    }
}
