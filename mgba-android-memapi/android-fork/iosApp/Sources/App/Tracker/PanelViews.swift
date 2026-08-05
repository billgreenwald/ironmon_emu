import SwiftUI
import TrackerCore

// MY MON / OPPONENT / ROUTES pages. Mirror Android MainView/EnemyView/RouteView. All Kotlin ints
// arrive as Int32 — convert with Int(...) for Swift use, pass Int32(...) into Kotlin.

// ── MY MON ──────────────────────────────────────────────────────────────────────────────────────

struct MainView: View {
    let active: ActiveState

    var body: some View {
        if let mon = active.leadPokemon {
            TrackerCard {
                header(mon)
                if Int(mon.starRating) > 0 { StarRatingRow(rating: Int(mon.starRating), score: Int(mon.ratingScore)) }
                TypeChips(type1: Int(mon.type1), type2: Int(mon.type2))
                HpBar(percent: mon.hpPercent, cur: Int(mon.currentHp), maxHp: Int(mon.maxHp))
                if let bag = active.bagDetail, Int(bag.hpHealCount) > 0 {
                    InfoRow(label: "Heals",
                            value: "\(Int(bag.hpHealPercent))% HP (\(Int(bag.hpHealCount)))",
                            valueColor: TrackerTheme.hpHigh)
                }
                Divider().background(Color.white.opacity(0.1))
                gameStatsRow
                InfoRow(label: "Ability", value: AbilityTable.shared.name(abilityId: mon.abilityId, isMaxFr: active.isMaxFr))
                InfoRow(label: "Nature",
                        value: "\(NatureTable.shared.get(natureId: mon.nature).name) \(NatureTable.shared.modifier(natureId: mon.nature))")
                InfoRow(label: "Item", value: Int(mon.heldItemId) == 0 ? "None" : ItemTable.shared.get(itemId: mon.heldItemId))
                LearnsetRowView(info: active.playerLearnset, speciesId: Int(mon.speciesId), level: Int(mon.level),
                                bst: Int(mon.bst), isMaxFr: active.isMaxFr)
                if active.battle.isActive, let stages = TrackerStateSwift.shared.playerStatStages(battle: active.battle), stages.anyChanged {
                    StatStagesRow(stages: stages)
                }
                StatsTableView(mon: mon)
                MoveTableView(moves: Array(mon.moves), monType1: Int(mon.type1), monType2: Int(mon.type2), active: active)
                if let note = TrackerStateSwift.shared.note(active: active, speciesId: mon.speciesId), !note.isEmpty {
                    Text("📝 \(note)").font(.system(size: 10)).foregroundColor(TrackerTheme.noteYellow)
                }
            }
        } else {
            Text("No Pokémon yet").font(.footnote).foregroundColor(TrackerTheme.textSecondary)
                .frame(maxWidth: .infinity).padding(.top, 20)
        }
    }

    private func header(_ mon: PokemonData) -> some View {
        HStack(alignment: .top, spacing: 8) {
            PokemonSprite(natDexId: Int(mon.natDexId), size: 48)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(mon.displayName).font(.system(size: 14, weight: .bold)).lineLimit(1)
                    MonMarkers(genderSymbol: TrackerStateSwift.shared.genderSymbol(mon: mon),
                               isShiny: mon.isShiny, hasPokerus: mon.hasPokerus)
                }
                HStack(spacing: 6) {
                    Text("Lv.\(Int(mon.level))").font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary)
                    StatusBadge(status: Int(mon.statusCondition))
                }
            }
            Spacer()
        }
    }

    private var gameStatsRow: some View {
        Group {
            if let s = active.stats {
                Text("\(s.steps) steps · \(s.totalBattles) battles · \(s.pokemonCenterVisits) centers")
                    .font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
            }
        }
    }
}

struct StarRatingRow: View {
    let rating: Int
    let score: Int
    var body: some View {
        HStack(spacing: 1) {
            if rating >= 6 {
                ForEach(0..<5, id: \.self) { _ in Text("★").foregroundColor(TrackerTheme.star) }
                Text("+").foregroundColor(TrackerTheme.star)
            } else {
                ForEach(0..<5, id: \.self) { i in
                    Text(i < rating ? "★" : "☆").foregroundColor(TrackerTheme.star)
                }
            }
            Text("(\(score))").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
        }
        .font(.system(size: 11))
    }
}

// ── OPPONENT ────────────────────────────────────────────────────────────────────────────────────

struct EnemyView: View {
    let active: ActiveState

    var body: some View {
        if active.battle.isActive, let enemy = active.battle.enemy {
            TrackerCard {
                HStack(alignment: .top, spacing: 8) {
                    PokemonSprite(natDexId: Int(enemy.natDexId), size: 48)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(enemy.name).font(.system(size: 16, weight: .bold)).lineLimit(1)
                        Text("Lv.\(Int(enemy.level))").font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary)
                    }
                    Spacer()
                }
                TypeChips(type1: Int(enemy.type1), type2: Int(enemy.type2))
                HpBar(percent: enemy.hpPercent, showNumbers: false)
                LearnsetRowView(info: active.enemyLearnset, speciesId: Int(enemy.speciesId), level: Int(enemy.level),
                                bst: Int(enemy.bst), isMaxFr: active.isMaxFr)
                battleInfo
                sideConditions
                if let stages = TrackerStateSwift.shared.enemyStatStages(enemy: enemy), stages.anyChanged {
                    StatStagesRow(stages: stages)
                }
                Text("Revealed Moves:").font(.system(size: 10, weight: .semibold)).foregroundColor(TrackerTheme.textSecondary)
                EnemyMoveTableView(enemy: enemy, playerType1: playerType1, playerType2: playerType2, active: active)
            }
        } else {
            Text("Not in battle").font(.footnote).foregroundColor(TrackerTheme.textSecondary)
                .frame(maxWidth: .infinity).padding(.top, 20)
        }
    }

    private var playerType1: Int { Int(active.leadPokemon?.type1 ?? -1) }
    private var playerType2: Int { Int(active.leadPokemon?.type2 ?? -1) }

    private var battleInfo: some View {
        let b = active.battle
        return HStack(spacing: 8) {
            Text(b.isWild ? "WILD" : "TRAINER").font(.system(size: 10, weight: .bold)).foregroundColor(TrackerTheme.accentRed)
            if !b.isWild && Int(b.trainerOpponentId) > 0 {
                Text("Trainer #\(Int(b.trainerOpponentId))").font(.system(size: 10)).foregroundColor(TrackerTheme.textSecondary)
            }
            if b.weather.displayName != "Clear" {
                Text(b.weather.displayName).font(.system(size: 10)).foregroundColor(TrackerTheme.accentBlue)
            }
            Spacer()
        }
    }

    @ViewBuilder private var sideConditions: some View {
        let b = active.battle
        let items: [String] = {
            var out: [String] = []
            if Int(b.playerReflect) > 0 { out.append("Reflect (\(Int(b.playerReflect))t)") }
            if Int(b.playerLightScreen) > 0 { out.append("Light Screen (\(Int(b.playerLightScreen))t)") }
            if Int(b.enemySpikes) > 0 { out.append("Spikes ×\(Int(b.enemySpikes))") }
            if Int(b.playerSafeguard) > 0 { out.append("Safeguard (\(Int(b.playerSafeguard))t)") }
            return out
        }()
        if !items.isEmpty {
            Text(items.joined(separator: " · ")).font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
        }
    }
}

// ── ROUTES (basic; full slot-padding in Phase B) ─────────────────────────────────────────────────

struct RouteView: View {
    let active: ActiveState

    private var orderedMapIds: [Int] { active.routeVisitOrder.map { Int(truncating: $0) } }

    var body: some View {
        let isHoenn = TrackerStateSwift.shared.isHoenn(active: active)
        if orderedMapIds.isEmpty {
            Text("No encounters recorded yet.").font(.footnote).foregroundColor(TrackerTheme.textSecondary)
                .frame(maxWidth: .infinity).padding(.top, 20)
        } else {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(orderedMapIds, id: \.self) { mapId in
                    let species = TrackerStateSwift.shared.encountersForRoute(active: active, mapId: Int32(mapId)).map { Int(truncating: $0) }
                    if !species.isEmpty {
                        TrackerCard {
                            Text(RouteNames.shared.get(mapLayoutId: Int32(mapId), isHoenn: isHoenn))
                                .font(.system(size: 12, weight: .semibold)).foregroundColor(TrackerTheme.accentBlue)
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 46), spacing: 4)], spacing: 6) {
                                ForEach(species, id: \.self) { sid in
                                    VStack(spacing: 1) {
                                        PokemonSprite(natDexId: Int(TrackerStateSwift.shared.natDexId(active: active, speciesId: Int32(sid))), size: 34)
                                        Text(TrackerStateSwift.shared.speciesName(active: active, speciesId: Int32(sid)))
                                            .font(.system(size: 8)).lineLimit(1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared rows ───────────────────────────────────────────────────────────────────────────────────

struct LearnsetRowView: View {
    let info: LearnsetInfo?
    let speciesId: Int
    let level: Int
    let bst: Int
    let isMaxFr: Bool

    var body: some View {
        HStack(spacing: 8) {
            if let info = info {
                let soon = !info.allLearned && Int(info.nextMoveLevel) <= level + 1
                Text("Moves \(Int(info.learnedCount))/\(Int(info.totalCount))")
                    .font(.system(size: 10)).foregroundColor(TrackerTheme.textSecondary)
                if !info.allLearned {
                    Text("(Lv.\(Int(info.nextMoveLevel)))")
                        .font(.system(size: 10)).foregroundColor(soon ? TrackerTheme.noteYellow : TrackerTheme.textSecondary)
                }
            }
            let evo = Int(EvolutionLevel.shared.get(speciesId: Int32(speciesId), isMaxFr: isMaxFr))
            if evo > 0 {
                Text("Evo Lv.\(evo)")
                    .font(.system(size: 10)).foregroundColor(level >= evo - 2 ? TrackerTheme.noteYellow : TrackerTheme.textSecondary)
            }
            Spacer()
            Text("BST \(bst)").font(.system(size: 10)).foregroundColor(TrackerTheme.textSecondary)
        }
    }
}

struct StatsTableView: View {
    let mon: PokemonData

    private var cells: [(String, Int, Int)] {  // label, value, natureIndex (-1 = HP/none)
        [("HP", Int(mon.maxHp), -1), ("Atk", Int(mon.attack), 0), ("Def", Int(mon.defense), 1),
         ("SpA", Int(mon.spAtk), 2), ("SpD", Int(mon.spDef), 3), ("Spe", Int(mon.speed), 4)]
    }

    var body: some View {
        let nat = NatureTable.shared.get(natureId: mon.nature)
        HStack(spacing: 4) {
            ForEach(cells, id: \.0) { cell in
                let color: Color = cell.2 == Int(nat.boostedStat) ? TrackerTheme.statBoost
                    : (cell.2 == Int(nat.reducedStat) ? TrackerTheme.statReduce : TrackerTheme.textPrimary)
                VStack(spacing: 0) {
                    Text(cell.0).font(.system(size: 8)).foregroundColor(TrackerTheme.textSecondary)
                    Text("\(cell.1)").font(.system(size: 11, weight: .semibold)).foregroundColor(color)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

struct StatStagesRow: View {
    let stages: StatStages
    var body: some View {
        HStack(spacing: 6) {
            ForEach(Array(stages.deltas().enumerated()), id: \.offset) { _, d in
                let delta = Int(d.delta)
                if delta != 0 {
                    Text("\(d.label) \(delta > 0 ? "+" : "")\(delta)")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundColor(delta > 0 ? TrackerTheme.statBoost : TrackerTheme.statReduce)
                }
            }
        }
    }
}

// ── Move tables ───────────────────────────────────────────────────────────────────────────────────

private func deriveCategory(active: ActiveState, moveId: Int32, moveType: Int32, power: Int32) -> Int {
    let mc = Int(TrackerStateSwift.shared.moveCategory(active: active, moveId: moveId))
    if mc != 0 { return mc }
    if power == 0 { return 3 }
    return TrackerTheme.typeIsPhysical(Int(moveType)) ? 1 : 2
}

struct MoveTableView: View {
    let moves: [MoveData]
    let monType1: Int
    let monType2: Int
    let active: ActiveState

    private var enemy: EnemyData? { active.battle.isActive ? active.battle.enemy : nil }

    var body: some View {
        VStack(spacing: 2) {
            ForEach(Array(moves.enumerated()), id: \.offset) { _, m in
                if !m.isEmpty {
                    moveRow(m)
                }
            }
        }
    }

    private func moveRow(_ m: MoveData) -> some View {
        let type = Int(m.moveType)
        let isStab = type == monType1 || type == monType2
        let cat = deriveCategory(active: active, moveId: m.moveId, moveType: m.moveType, power: m.power)
        return HStack(spacing: 4) {
            CategoryIcon(category: cat)
            Circle().fill(TrackerTheme.typeColor(type)).frame(width: 6, height: 6)
            Text(m.moveName).font(.system(size: 10)).foregroundColor(isStab ? TrackerTheme.hpHigh : TrackerTheme.textPrimary).lineLimit(1)
            Spacer()
            Text(Int(m.power) == 0 ? "—" : "\(Int(m.power))").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary).frame(width: 24, alignment: .trailing)
            if let e = enemy, Int(m.power) > 0 {
                EffectivenessArrow(mult: TypeChart.shared.effectiveness(attType: m.moveType, defType1: e.type1, defType2: e.type2))
                    .frame(width: 12)
            } else {
                Spacer().frame(width: 12)
            }
            Text("\(Int(m.pp))").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary).frame(width: 18, alignment: .trailing)
        }
    }
}

struct EnemyMoveTableView: View {
    let enemy: EnemyData
    let playerType1: Int
    let playerType2: Int
    let active: ActiveState

    private var moveIds: [Int] {
        (enemy.fourConfirmedThisBattle ?? enemy.revealedMoveIds).map { Int(truncating: $0) }
    }

    var body: some View {
        VStack(spacing: 2) {
            ForEach(Array(moveIds.enumerated()), id: \.offset) { idx, mid in
                row(moveId: Int32(mid), slot: idx)
            }
            if moveIds.isEmpty {
                Text("— none seen —").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
            }
        }
    }

    private func row(moveId: Int32, slot: Int) -> some View {
        let stats = MoveStatsTable.shared.get(moveId: moveId, isMaxFr: active.isMaxFr, isNatDex: active.isNatDex)
        let name = MoveNames.shared.get(id: moveId, isMaxFr: active.isMaxFr)
        let type = Int(stats.type)
        let cat = deriveCategory(active: active, moveId: moveId, moveType: stats.type, power: stats.power)
        let stale = slot < enemy.moveStaleFlags.count && (enemy.moveStaleFlags[slot].boolValue)
        let pp = Int(TrackerStateSwift.shared.enemyPp(enemy: enemy, moveId: moveId))
        return HStack(spacing: 4) {
            CategoryIcon(category: cat)
            Circle().fill(TrackerTheme.typeColor(type)).frame(width: 6, height: 6)
            Text(name + (stale ? "*" : "")).font(.system(size: 10)).lineLimit(1)
            Spacer()
            Text(Int(stats.power) == 0 ? "—" : "\(Int(stats.power))").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary).frame(width: 24, alignment: .trailing)
            if Int(stats.power) > 0 && playerType1 >= 0 {
                EffectivenessArrow(mult: TypeChart.shared.effectiveness(attType: stats.type, defType1: Int32(playerType1), defType2: Int32(playerType2)))
                    .frame(width: 12)
            } else {
                Spacer().frame(width: 12)
            }
            Text(pp >= 0 ? "\(pp)" : "—").font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary).frame(width: 18, alignment: .trailing)
        }
    }
}
