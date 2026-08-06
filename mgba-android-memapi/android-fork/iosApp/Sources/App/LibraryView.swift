import SwiftUI
import UniformTypeIdentifiers
import TrackerCore

/// ROM library home screen (Android GameListMaterialActivity, BATCH mode). Lists ROM families;
/// tap one to play. Folder is picked once and remembered via a security-scoped bookmark.
struct LibraryView: View {
    @ObservedObject var library: RomLibrary
    let onPlay: (RomFamilyGroup) -> Void
    @State private var showFolderPicker = false
    @State private var showRomPicker = false
    @State private var showSettings = false
    let onPlayFile: (URL) -> Void

    var body: some View {
        NavigationView {
            content
                .navigationTitle("ROM Library")
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        HStack(spacing: 14) {
                            Button { library.scan() } label: { Image(systemName: "arrow.clockwise") }
                            Button { showRomPicker = true } label: { Image(systemName: "doc.badge.plus") }
                            Button { showFolderPicker = true } label: { Image(systemName: "folder") }
                        }
                    }
                }
        }
        .navigationViewStyle(.stack)
        .fileImporter(isPresented: $showFolderPicker, allowedContentTypes: [.folder]) { result in
            if case .success(let url) = result { library.setFolder(url) }
        }
        .fileImporter(isPresented: $showRomPicker, allowedContentTypes: [.data]) { result in
            if case .success(let url) = result { onPlayFile(url) }
        }
        .sheet(isPresented: $showSettings) { SettingsView() }
    }

    @ViewBuilder private var content: some View {
        if !library.hasFolder {
            emptyState
        } else if library.families.isEmpty {
            VStack(spacing: 8) {
                Text("No .gba/.gb ROMs found in this folder.").foregroundColor(.secondary)
                Button("Choose a different folder") { showFolderPicker = true }
            }
        } else {
            List {
                if let name = library.folderName {
                    Text(name).font(.system(size: 11)).foregroundColor(.secondary)
                }
                ForEach(Array(library.families.enumerated()), id: \.offset) { _, g in
                    FamilyRowView(library: library, group: g)
                        .contentShape(Rectangle())
                        .onTapGesture { onPlay(g) }
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "folder.badge.plus").font(.largeTitle).foregroundColor(.secondary)
            Text("Pick a folder of GBA ROMs to build your library, or open a single ROM.")
                .multilineTextAlignment(.center).foregroundColor(.secondary).padding(.horizontal)
            Button("Choose ROM Folder") { showFolderPicker = true }
                .padding(.horizontal, 20).padding(.vertical, 10)
                .background(TrackerTheme.accentBlue, in: Capsule()).foregroundColor(.white)
            Button("Open a single ROM") { showRomPicker = true }.font(.footnote)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct FamilyRowView: View {
    @ObservedObject var library: RomLibrary
    let group: RomFamilyGroup
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text(group.prefix.isEmpty ? "(unnamed)" : group.prefix.prefix(1).uppercased() + group.prefix.dropFirst())
                    .font(.headline)
                Text("ROM family · \(Int(group.totalCount)) ROM\(group.totalCount == 1 ? "" : "s")")
                    .font(.system(size: 12)).foregroundColor(.secondary)
                Text("Last: ROM \(library.lastPlayed(group.prefix))")
                    .font(.system(size: 12)).foregroundColor(TrackerTheme.accentBlue)
            }
            Spacer()
            Text(RomFamilySwift.shared.ext(group: group).uppercased())
                .font(.system(size: 11, weight: .bold)).foregroundColor(.white)
                .padding(.horizontal, 8).padding(.vertical, 3)
                .background(TrackerTheme.hpHigh, in: Capsule())
        }
        .padding(.vertical, 4)
    }
}
