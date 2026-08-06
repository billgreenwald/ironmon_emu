import SwiftUI
import TrackerCore

/// Root tracker panel — the SwiftUI counterpart of Android `TrackerPanel.kt`. Thin renderer of the
/// shared `TrackerState`; all logic lives in :tracker-core.
struct TrackerPanel: View {
    let state: TrackerState?
    @State private var tab: Int = 0

    var body: some View {
        ZStack {
            TrackerTheme.panelBg.ignoresSafeArea()
            content
        }
        .foregroundColor(TrackerTheme.textPrimary)
    }

    @ViewBuilder private var content: some View {
        if let s = state, let active = TrackerStateSwift.shared.active(state: s) {
            ActivePanel(active: active, tab: $tab)
        } else if let s = state, TrackerStateSwift.shared.isNoGameLoaded(state: s) {
            StatusView(text: "No game loaded", symbol: "gamecontroller")
        } else {
            StatusView(text: "Waiting for game…", symbol: "bolt.horizontal.circle")
        }
    }
}

struct StatusView: View {
    let text: String
    let symbol: String
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol).font(.largeTitle)
            Text(text).font(.footnote)
        }
        .foregroundColor(TrackerTheme.textSecondary)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ActivePanel: View {
    let active: ActiveState
    @Binding var tab: Int
    @StateObject private var router = SheetRouter()

    private var doubles: Bool { active.battle.isDoubles && active.battle.enemy2 != nil }

    var body: some View {
        panel
            .environmentObject(router)
            .sheet(item: $router.current) { sheet in sheetContent(for: sheet) }
    }

    private var panel: some View {
        VStack(spacing: 0) {
            PanelHeader(active: active)
            if active.isGameOver { GameOverBanner(runAttempts: Int(active.runAttempts)) }
            RouteStrip(active: active)
            if active.showBallPicker {
                BallPickerView(chosen: Int(active.chosenBall))
            } else {
                TrackerTabBar(tab: $tab, doubles: doubles)
                TabView(selection: $tab) {
                    ScrollView { MainView(active: active).padding(6) }.tag(0)
                    ScrollView { EnemyView(active: active).padding(6) }.tag(1)
                    ScrollView { RouteView(active: active).padding(6) }.tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            if active.isGameOver && active.logAvailable { ReviewLogsBanner() }
        }
    }
}

private struct BallPickerView: View {
    let chosen: Int   // 1=Left, 2=Middle, 3=Right; 0=unset
    private let labels = ["LEFT", "MIDDLE", "RIGHT"]
    var body: some View {
        VStack(spacing: 14) {
            Text("Pick your starter ball").font(.system(size: 13, weight: .semibold))
            HStack(spacing: 22) {
                ForEach(1...3, id: \.self) { i in
                    VStack(spacing: 4) {
                        Text(chosen == i ? "▼" : " ").foregroundColor(TrackerTheme.accentRed)
                        PokeBallView(size: 40, dimmed: chosen != 0 && chosen != i)
                        Text(labels[i - 1]).font(.system(size: 9)).foregroundColor(TrackerTheme.textSecondary)
                    }
                }
            }
            Button { IosTracker.shared.rerollBall() } label: {
                Text("↺ Reroll").font(.system(size: 12, weight: .semibold)).foregroundColor(.white)
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .background(TrackerTheme.accentBlue, in: Capsule())
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

// ── Chrome ───────────────────────────────────────────────────────────────────────────────────────

private struct PanelHeader: View {
    let active: ActiveState
    var body: some View {
        HStack(spacing: 6) {
            Text("IRONMON")
                .font(.system(size: 10, weight: .heavy)).foregroundColor(.white)
                .padding(.horizontal, 5).padding(.vertical, 2)
                .background(TrackerTheme.accentRed, in: RoundedRectangle(cornerRadius: 4))
            Text("\(active.game.displayName) v1.\(Int(active.romVersion))")
                .font(.system(size: 10)).foregroundColor(TrackerTheme.textSecondary)
                .lineLimit(1)
            Spacer()
            Text("Run \(Int(active.runAttempts) + 1)")
                .font(.system(size: 10, weight: .bold)).foregroundColor(TrackerTheme.accentRed)
        }
        .padding(.horizontal, 8).padding(.vertical, 6)
        .background(TrackerTheme.headerBg)
    }
}

private struct GameOverBanner: View {
    let runAttempts: Int
    var body: some View {
        Button { IosTracker.shared.manualNextRun() } label: {
            HStack {
                Text("GAME OVER").font(.system(size: 12, weight: .heavy)).foregroundColor(.white)
                Spacer()
                Text("Run \(runAttempts + 1) →").font(.system(size: 11, weight: .bold)).foregroundColor(.white)
            }
            .padding(.horizontal, 8).padding(.vertical, 5)
            .frame(maxWidth: .infinity)
            .background(Color(hex: 0xB00020))
        }
        .buttonStyle(.plain)
    }
}

private struct RouteStrip: View {
    let active: ActiveState
    @EnvironmentObject var router: SheetRouter
    var body: some View {
        if let route = active.currentRoute, !route.name.isEmpty {
            let mapId = Int(route.mapLayoutId)
            HStack {
                Text(route.name).font(.system(size: 11, weight: .semibold)).foregroundColor(TrackerTheme.accentBlue)
                    .lineLimit(1)
                if GallerySwift.shared.hasImages(routeName: route.name) {
                    Button { router.present(.gallery(route.name)) } label: {
                        Text("↗").font(.system(size: 11, weight: .bold)).foregroundColor(TrackerTheme.accentBlue)
                    }.buttonStyle(.plain)
                }
                Spacer()
                if TrackerStateSwift.shared.routeHasTrainers(active: active, mapId: Int32(mapId)) {
                    let d = Int(TrackerStateSwift.shared.trainerDefeated(active: active, mapId: Int32(mapId)))
                    let t = Int(TrackerStateSwift.shared.trainerTotal(active: active, mapId: Int32(mapId)))
                    Text("Trainers \(d)/\(t)")
                        .font(.system(size: 10))
                        .foregroundColor(d >= t && t > 0 ? TrackerTheme.hpHigh : TrackerTheme.textSecondary)
                }
            }
            .padding(.horizontal, 8).padding(.vertical, 4)
        }
    }
}

private struct ReviewLogsBanner: View {
    var body: some View {
        HStack {
            Spacer()
            Text("📖 Review Logs").font(.system(size: 11, weight: .bold)).foregroundColor(.white)
            Spacer()
        }
        .padding(.vertical, 6)
        .background(TrackerTheme.accentBlue)
    }
}

private struct TrackerTabBar: View {
    @Binding var tab: Int
    let doubles: Bool
    private var titles: [String] {
        doubles ? ["MY MONS", "OPPONENTS", "ROUTES"] : ["MY MON", "OPPONENT", "ROUTES"]
    }
    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<3, id: \.self) { i in
                Button { tab = i } label: {
                    Text(titles[i])
                        .font(.system(size: 10, weight: tab == i ? .bold : .regular))
                        .foregroundColor(tab == i ? .white : TrackerTheme.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                        .background(tab == i ? TrackerTheme.accentRed.opacity(0.85) : Color.clear)
                }
            }
        }
        .background(TrackerTheme.headerBg)
    }
}
