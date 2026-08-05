import SwiftUI
import TrackerCore

// Tap-to-open detail sheets (Android `TrackerBottomSheet` set). A SheetRouter injected via
// @EnvironmentObject lets any deep row present a sheet without threading bindings down.

final class SheetRouter: ObservableObject {
    @Published var current: TrackerSheet?
    func present(_ s: TrackerSheet) { current = s }
}

enum TrackerSheet: Identifiable {
    case move(MoveData, Int)                 // move, category
    case moveById(Int, Bool, Bool)           // moveId, isMaxFr, isNatDex (enemy revealed moves)
    case ability(Int, Bool)                  // abilityId, isMaxFr
    case typeDefense(Int, Int, Bool)         // type1, type2, isNatDex
    case learnset(LearnsetInfo, Int)         // info, currentLevel
    case evo(Int, Bool)                      // speciesId, isMaxFr
    case ivStat(PokemonData)
    case bag(BagDetailInfo)
    case note(Int, String)                   // speciesId, currentNote
    case moveHistory([TrackedMove], Bool)    // tracked moves, isMaxFr
    case routeMon(Int, ActiveState)          // speciesId, active
    case coverage(PokemonData, Bool)         // mon, isNatDex
    case gallery(String)                     // routeName

    var id: String {
        switch self {
        case .move(let m, _): return "move-\(m.moveId)"
        case .moveById(let id, _, _): return "moveid-\(id)"
        case .ability(let a, _): return "ability-\(a)"
        case .typeDefense(let a, let b, _): return "typedef-\(a)-\(b)"
        case .learnset: return "learnset"
        case .evo(let s, _): return "evo-\(s)"
        case .ivStat: return "ivstat"
        case .bag: return "bag"
        case .note(let s, _): return "note-\(s)"
        case .moveHistory: return "movehistory"
        case .routeMon(let s, _): return "routemon-\(s)"
        case .coverage: return "coverage"
        case .gallery(let r): return "gallery-\(r)"
        }
    }
}

/// Standard sheet chrome: dark background + a Done button.
struct SheetChrome<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) { content }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(TrackerTheme.panelBg.ignoresSafeArea())
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }
        .navigationViewStyle(.stack)
        .foregroundColor(TrackerTheme.textPrimary)
    }
}

@ViewBuilder
func sheetContent(for sheet: TrackerSheet) -> some View {
    switch sheet {
    case .move(let m, let cat):        MoveDetailSheet(move: m, category: cat)
    case .moveById(let id, let mx, let nd): MoveByIdDetailSheet(moveId: id, isMaxFr: mx, isNatDex: nd)
    case .ability(let id, let mx):     AbilityDetailSheet(abilityId: id, isMaxFr: mx)
    case .typeDefense(let t1, let t2, let nd): TypeDefenseSheet(type1: t1, type2: t2, isNatDex: nd)
    case .learnset(let info, let lvl): LearnsetSheet(info: info, level: lvl)
    case .evo(let sid, let mx):        EvoDetailSheet(speciesId: sid, isMaxFr: mx)
    case .ivStat(let mon):             IVStatSheet(mon: mon)
    case .bag(let bag):                BagDetailSheet(bag: bag)
    case .note(let sid, let note):     NoteEditSheet(speciesId: sid, initial: note)
    case .moveHistory(let moves, let mx): MoveHistorySheet(moves: moves, isMaxFr: mx)
    case .routeMon(let sid, let active):  RouteMonSheet(speciesId: sid, active: active)
    case .coverage(let mon, let nd):   CoverageSheet(mon: mon, isNatDex: nd)
    case .gallery(let route):          GalleryView(routeName: route)
    }
}

// ── Individual sheets ──────────────────────────────────────────────────────────────────────────

struct MoveDetailSheet: View {
    let move: MoveData
    let category: Int
    var body: some View {
        SheetChrome(title: move.moveName) {
            HStack { TypeChip(typeId: Int(move.moveType)); CategoryIcon(category: category) }
            InfoRow(label: "Power", value: Int(move.power) == 0 ? "—" : "\(Int(move.power))")
            InfoRow(label: "Accuracy", value: Int(move.accuracy) == 0 ? "—" : "\(Int(move.accuracy))")
            InfoRow(label: "PP", value: "\(Int(move.pp))/\(Int(move.maxPp))")
            Text(MoveDescTable.shared.get(moveId: move.moveId))
                .font(.system(size: 12)).foregroundColor(TrackerTheme.textSecondary)
        }
    }
}

struct MoveByIdDetailSheet: View {
    let moveId: Int
    let isMaxFr: Bool
    let isNatDex: Bool
    var body: some View {
        let stats = MoveStatsTable.shared.get(moveId: Int32(moveId), isMaxFr: isMaxFr, isNatDex: isNatDex)
        SheetChrome(title: MoveNames.shared.get(id: Int32(moveId), isMaxFr: isMaxFr)) {
            TypeChip(typeId: Int(stats.type))
            InfoRow(label: "Power", value: Int(stats.power) == 0 ? "—" : "\(Int(stats.power))")
            InfoRow(label: "Accuracy", value: Int(stats.accuracy) == 0 ? "—" : "\(Int(stats.accuracy))")
            InfoRow(label: "PP", value: "\(Int(stats.pp))")
            Text(MoveDescTable.shared.get(moveId: Int32(moveId)))
                .font(.system(size: 12)).foregroundColor(TrackerTheme.textSecondary)
        }
    }
}

struct AbilityDetailSheet: View {
    let abilityId: Int
    let isMaxFr: Bool
    var body: some View {
        let info = AbilityTable.shared.get(abilityId: Int32(abilityId), isMaxFr: isMaxFr)
        SheetChrome(title: info.name) {
            Text(info.desc).font(.system(size: 12)).foregroundColor(TrackerTheme.textSecondary)
        }
    }
}

struct TypeDefenseSheet: View {
    let type1: Int
    let type2: Int
    let isNatDex: Bool
    var body: some View {
        let rows = TrackerStateSwift.shared.defenseChart(type1: Int32(type1), type2: Int32(type2), isNatDex: isNatDex)
        SheetChrome(title: "Type Defenses") {
            if rows.isEmpty {
                Text("All matchups neutral (1×).").font(.system(size: 12)).foregroundColor(TrackerTheme.textSecondary)
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                HStack {
                    TypeChip(typeId: Int(r.typeId))
                    Spacer()
                    Text(multiplierLabel(r.mult)).font(.system(size: 12, weight: .bold))
                        .foregroundColor(multiplierColor(r.mult))
                }
            }
        }
    }
}

struct LearnsetSheet: View {
    let info: LearnsetInfo
    let level: Int
    var body: some View {
        let levels = info.allMoveLevels.map { Int(truncating: $0) }
        SheetChrome(title: "Learnset") {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 40), spacing: 6)], spacing: 6) {
                ForEach(Array(levels.enumerated()), id: \.offset) { _, lv in
                    let isNext = lv == Int(info.nextMoveLevel)
                    Text("\(lv)")
                        .font(.system(size: 12, weight: isNext ? .bold : .regular))
                        .foregroundColor(lv <= level ? TrackerTheme.textSecondary : (isNext ? TrackerTheme.noteYellow : TrackerTheme.textPrimary))
                        .frame(maxWidth: .infinity).padding(.vertical, 4)
                        .background(TrackerTheme.cardBg, in: RoundedRectangle(cornerRadius: 5))
                }
            }
        }
    }
}

struct EvoDetailSheet: View {
    let speciesId: Int
    let isMaxFr: Bool
    var body: some View {
        let lvl = Int(EvolutionLevel.shared.get(speciesId: Int32(speciesId), isMaxFr: isMaxFr))
        let method = EvolutionLevel.shared.getMethod(speciesId: Int32(speciesId), isMaxFr: isMaxFr)
        SheetChrome(title: "Evolution") {
            if lvl > 0 {
                Text("Evolves at level \(lvl).").font(.system(size: 13))
            } else if let m = method, !m.isEmpty {
                Text("Evolves via \(m).").font(.system(size: 13))
            } else {
                Text("Does not evolve, or evolution is unknown.").font(.system(size: 13)).foregroundColor(TrackerTheme.textSecondary)
            }
        }
    }
}

struct IVStatSheet: View {
    let mon: PokemonData
    private var rows: [(String, Int, Int)] {  // label, IV, EV
        [("HP", Int(mon.ivHp), Int(mon.evHp)), ("Atk", Int(mon.ivAtk), Int(mon.evAtk)),
         ("Def", Int(mon.ivDef), Int(mon.evDef)), ("SpA", Int(mon.ivSpA), Int(mon.evSpA)),
         ("SpD", Int(mon.ivSpD), Int(mon.evSpD)), ("Spe", Int(mon.ivSpe), Int(mon.evSpe))]
    }
    var body: some View {
        SheetChrome(title: "Stats Detail") {
            HStack { Text("Stat").frame(width: 40, alignment: .leading); Spacer(); Text("IV").frame(width: 40); Text("EV").frame(width: 44) }
                .font(.system(size: 10, weight: .bold)).foregroundColor(TrackerTheme.textSecondary)
            ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                HStack {
                    Text(r.0).frame(width: 40, alignment: .leading)
                    Spacer()
                    Text("\(r.1)").frame(width: 40)
                    Text("\(r.2)").frame(width: 44)
                }.font(.system(size: 12))
            }
            Divider().background(Color.white.opacity(0.1))
            InfoRow(label: "Friendship", value: "\(Int(mon.friendship))/255")
            HStack { Text("Hidden Power").font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary); Spacer(); TypeChip(typeId: Int(mon.hiddenPowerType)) }
        }
    }
}

struct BagDetailSheet: View {
    let bag: BagDetailInfo
    var body: some View {
        SheetChrome(title: "Bag") {
            section("HP", bag.hpItems)
            section("PP", bag.ppItems)
            section("Status", bag.statusItems)
            section("Battle", bag.battleItems)
        }
    }
    @ViewBuilder private func section(_ title: String, _ items: [BagItemEntry]) -> some View {
        if !items.isEmpty {
            Text(title).font(.system(size: 12, weight: .bold)).foregroundColor(TrackerTheme.accentBlue)
            ForEach(Array(items.enumerated()), id: \.offset) { _, it in
                InfoRow(label: it.name, value: "×\(Int(it.quantity))")
            }
        }
    }
}

struct NoteEditSheet: View {
    let speciesId: Int
    let initial: String
    @State private var text: String = ""
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        SheetChrome(title: "Note") {
            TextField("Add a note…", text: $text)
                .textFieldStyle(.roundedBorder).foregroundColor(.black)
            Button("Save") {
                IosTracker.shared.saveNote(speciesId: Int32(speciesId), note: text)
                dismiss()
            }
            .font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
            .padding(.horizontal, 16).padding(.vertical, 8)
            .background(TrackerTheme.accentBlue, in: Capsule())
        }
        .onAppear { text = initial }
    }
}

struct MoveHistorySheet: View {
    let moves: [TrackedMove]
    let isMaxFr: Bool
    var body: some View {
        let sorted = moves.sorted { Int($0.minLv) > Int($1.minLv) }
        SheetChrome(title: "Move History") {
            HStack { Text("Move").frame(maxWidth: .infinity, alignment: .leading); Text("Min").frame(width: 36); Text("Max").frame(width: 36) }
                .font(.system(size: 10, weight: .bold)).foregroundColor(TrackerTheme.textSecondary)
            ForEach(Array(sorted.enumerated()), id: \.offset) { _, m in
                HStack {
                    Text(MoveNames.shared.get(id: m.id, isMaxFr: isMaxFr)).frame(maxWidth: .infinity, alignment: .leading)
                    Text("\(Int(m.minLv))").frame(width: 36)
                    Text("\(Int(m.maxLv))").frame(width: 36)
                }.font(.system(size: 12))
            }
        }
    }
}

struct RouteMonSheet: View {
    let speciesId: Int
    let active: ActiveState
    var body: some View {
        let name = TrackerStateSwift.shared.speciesName(active: active, speciesId: Int32(speciesId))
        let ndex = Int(TrackerStateSwift.shared.natDexId(active: active, speciesId: Int32(speciesId)))
        let bst = Int(BstTable.shared.bst(speciesId: Int32(speciesId)))
        let evo = Int(EvolutionLevel.shared.get(speciesId: Int32(speciesId), isMaxFr: active.isMaxFr))
        SheetChrome(title: name) {
            HStack {
                PokemonSprite(natDexId: ndex, size: 56)
                VStack(alignment: .leading) {
                    Text(name).font(.system(size: 15, weight: .bold))
                    Text("BST \(bst)").font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary)
                    if evo > 0 { Text("Evolves at Lv.\(evo)").font(.system(size: 11)).foregroundColor(TrackerTheme.textSecondary) }
                }
                Spacer()
            }
        }
    }
}

struct CoverageSheet: View {
    let mon: PokemonData
    let isNatDex: Bool
    var body: some View {
        // Read-only coverage from the mon's damaging move types.
        let attackTypes = Array(mon.moves).filter { Int($0.power) > 0 }.map { Int($0.moveType) }
        let allDef = isNatDex ? Array(0...18) : Array(0...17)
        SheetChrome(title: "Coverage") {
            if attackTypes.isEmpty {
                Text("No damaging moves.").font(.system(size: 12)).foregroundColor(TrackerTheme.textSecondary)
            }
            ForEach(allDef, id: \.self) { def in
                if def != 9 {
                    let best = attackTypes.map { Float(TypeChart.shared.effectiveness(attType: Int32($0), defType1: Int32(def), defType2: Int32(def))) }.max() ?? 1
                    HStack {
                        TypeChip(typeId: def)
                        Spacer()
                        Text(multiplierLabel(best)).font(.system(size: 12, weight: .bold)).foregroundColor(multiplierColor(best))
                    }
                }
            }
        }
    }
}

// ── shared multiplier formatting ─────────────────────────────────────────────────────────────
func multiplierLabel(_ m: Float) -> String {
    switch m {
    case 0: return "0×"
    case 0.25: return "¼×"
    case 0.5: return "½×"
    case 2: return "2×"
    case 4: return "4×"
    default: return "1×"
    }
}
func multiplierColor(_ m: Float) -> Color {
    switch m {
    case 0: return Color(hex: 0x888888)
    case 0.25, 0.5: return TrackerTheme.hpLow
    case 2, 4: return TrackerTheme.hpHigh
    default: return TrackerTheme.textPrimary
    }
}
