import SwiftUI
import TrackerCore

/// Randomizer `.log` viewer — 5 tabs (Pokémon / Trainers / Routes / TMs / Misc). Mirrors Android
/// LogOverlay. Data is parsed by the shared `RandomizerLog`; iOS imports the file via a picker.
struct LogViewerOverlay: View {
    let data: LogData
    let active: ActiveState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            TabView {
                LogPokemonTab(data: data, active: active).tabItem { Label("Pokémon", systemImage: "list.bullet") }
                LogTrainersTab(data: data, active: active).tabItem { Label("Trainers", systemImage: "person.3") }
                LogRoutesTab(data: data).tabItem { Label("Routes", systemImage: "map") }
                LogTMsTab(data: data).tabItem { Label("TMs", systemImage: "square.stack") }
                LogMiscTab(data: data).tabItem { Label("Misc", systemImage: "info.circle") }
            }
            .navigationTitle("Randomizer Log")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
        .navigationViewStyle(.stack)
    }
}

// ── Pokémon ─────────────────────────────────────────────────────────────────────────────────────

private struct LogPokemonTab: View {
    let data: LogData
    let active: ActiveState
    @State private var query = ""

    private var mons: [LogPokemon] {
        let all = LogDataSwift.shared.pokemon(data: data)
        guard !query.isEmpty else { return all }
        return all.filter { $0.name.lowercased().contains(query.lowercased()) }
    }

    var body: some View {
        List(Array(mons.enumerated()), id: \.offset) { _, p in
            NavigationLink(destination: LogPokemonDetail(p: p, data: data, active: active)) {
                HStack {
                    PokemonSprite(natDexId: Int(p.id), size: 30)
                    Text(p.name)
                    Spacer()
                    Text("BST \(Int(p.bst))").font(.system(size: 11)).foregroundColor(.secondary)
                }
            }
        }
        .searchable(text: $query)
    }
}

private struct LogPokemonDetail: View {
    let p: LogPokemon
    let data: LogData
    let active: ActiveState

    var body: some View {
        List {
            Section {
                HStack {
                    PokemonSprite(natDexId: Int(p.id), size: 56)
                    VStack(alignment: .leading) {
                        Text(p.name).font(.headline)
                        HStack {
                            if let t1 = p.type1 { TypeChip(typeId: Int(truncating: t1)) }
                            if let t2 = p.type2 { TypeChip(typeId: Int(truncating: t2)) }
                        }
                    }
                }
            }
            Section("Base Stats (BST \(Int(p.bst)))") {
                statBar("HP", Int(p.baseStats.hp))
                statBar("Atk", Int(p.baseStats.atk))
                statBar("Def", Int(p.baseStats.def))
                statBar("SpA", Int(p.baseStats.spa))
                statBar("SpD", Int(p.baseStats.spd))
                statBar("Spe", Int(p.baseStats.spe))
            }
            Section("Abilities") {
                Text(AbilityTable.shared.name(abilityId: p.ability1, isMaxFr: active.isMaxFr))
                if let a2 = p.ability2 { Text(AbilityTable.shared.name(abilityId: Int32(truncating: a2), isMaxFr: active.isMaxFr)) }
            }
            let evos = LogDataSwift.shared.evolutions(p: p).map { Int(truncating: $0) }
            if !evos.isEmpty {
                Section("Evolutions") {
                    ForEach(Array(evos.enumerated()), id: \.offset) { _, eid in
                        Text(TrackerStateSwift.shared.speciesName(active: active, speciesId: Int32(eid)))
                    }
                }
            }
            let moves = LogDataSwift.shared.moveSet(p: p)
            if !moves.isEmpty {
                Section("Level-up Moves") {
                    ForEach(Array(moves.enumerated()), id: \.offset) { _, m in
                        HStack { Text("Lv.\(Int(m.level))").frame(width: 44, alignment: .leading).foregroundColor(.secondary); Text(m.name) }
                    }
                }
            }
        }
        .navigationTitle(p.name).navigationBarTitleDisplayMode(.inline)
    }

    private func statBar(_ label: String, _ value: Int) -> some View {
        HStack {
            Text(label).frame(width: 34, alignment: .leading).font(.system(size: 12))
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.gray.opacity(0.2))
                    Capsule().fill(TrackerTheme.accentBlue).frame(width: g.size.width * CGFloat(min(1, Double(value) / 200)))
                }
            }.frame(height: 8)
            Text("\(value)").frame(width: 34, alignment: .trailing).font(.system(size: 12))
        }
    }
}

// ── Trainers ────────────────────────────────────────────────────────────────────────────────────

private struct LogTrainersTab: View {
    let data: LogData
    let active: ActiveState
    var body: some View {
        List(Array(LogDataSwift.shared.trainers(data: data).enumerated()), id: \.offset) { _, t in
            NavigationLink(destination: LogTrainerDetail(t: t, active: active)) {
                VStack(alignment: .leading) {
                    Text(t.fullname.isEmpty ? t.name : t.fullname)
                    if !t.trainerClass.isEmpty { Text(t.trainerClass).font(.system(size: 11)).foregroundColor(.secondary) }
                }
            }
        }
    }
}

private struct LogTrainerDetail: View {
    let t: LogTrainer
    let active: ActiveState
    var body: some View {
        List(Array(LogDataSwift.shared.party(t: t).enumerated()), id: \.offset) { _, m in
            HStack {
                PokemonSprite(natDexId: Int(m.pokemonId), size: 34)
                VStack(alignment: .leading) {
                    Text(TrackerStateSwift.shared.speciesName(active: active, speciesId: m.pokemonId))
                    let moves = LogDataSwift.shared.partyMoveIds(m: m).map { Int(truncating: $0) }
                    if !moves.isEmpty {
                        Text(moves.map { MoveNames.shared.get(id: Int32($0), isMaxFr: active.isMaxFr) }.joined(separator: ", "))
                            .font(.system(size: 10)).foregroundColor(.secondary)
                    }
                }
                Spacer()
                Text("Lv.\(Int(m.level))").foregroundColor(.secondary)
            }
        }
        .navigationTitle(t.name).navigationBarTitleDisplayMode(.inline)
    }
}

// ── Routes / TMs / Misc ─────────────────────────────────────────────────────────────────────────

private struct LogRoutesTab: View {
    let data: LogData
    var body: some View {
        List(Array(LogDataSwift.shared.routes(data: data).enumerated()), id: \.offset) { _, r in
            VStack(alignment: .leading, spacing: 2) {
                Text(r.name).font(.system(size: 14, weight: .semibold))
                HStack(spacing: 12) {
                    if Int(r.numWilds) > 0 { Text("\(Int(r.numWilds)) wild").font(.system(size: 11)).foregroundColor(.secondary) }
                    if Int(r.numTrainers) > 0 { Text("\(Int(r.numTrainers)) trainers").font(.system(size: 11)).foregroundColor(.secondary) }
                }
            }
        }
    }
}

private struct LogTMsTab: View {
    let data: LogData
    var body: some View {
        List(Array(LogDataSwift.shared.tms(data: data).enumerated()), id: \.offset) { _, tm in
            HStack {
                Text("TM\(String(format: "%02d", Int(tm.tmNumber)))").frame(width: 56, alignment: .leading).foregroundColor(.secondary)
                Text(tm.name)
            }
        }
    }
}

private struct LogMiscTab: View {
    let data: LogData
    var body: some View {
        List {
            row("Game", data.settings.game)
            row("Version", data.settings.version)
            row("Seed", data.settings.randomSeed)
            if let s = data.settings.settingsString, !s.isEmpty {
                Section("Settings String") { Text(s).font(.system(size: 10)).foregroundColor(.secondary) }
            }
        }
    }
    @ViewBuilder private func row(_ label: String, _ value: String?) -> some View {
        if let v = value, !v.isEmpty { HStack { Text(label); Spacer(); Text(v).foregroundColor(.secondary) } }
    }
}
