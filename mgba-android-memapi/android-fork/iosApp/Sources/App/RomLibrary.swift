import Foundation
import TrackerCore

/// The ROM library: remembers a user-picked folder (security-scoped bookmark), scans it for
/// .gba/.gb ROMs, groups them into families via the shared logic, and prepares a chosen ROM for
/// loading. iOS counterpart of Android's GameListViewmodel/QuickloadManager (BATCH mode only).
@MainActor
final class RomLibrary: ObservableObject {
    @Published private(set) var families: [RomFamilyGroup] = []
    @Published private(set) var folderName: String?

    private let bookmarkKey = "rom_folder_bookmark"
    private var folderURL: URL?

    init() {
        resolveBookmark()
        scan()
    }

    var hasFolder: Bool { folderURL != nil }

    func setFolder(_ url: URL) {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        if let data = try? url.bookmarkData(options: [], includingResourceValuesForKeys: nil, relativeTo: nil) {
            UserDefaults.standard.set(data, forKey: bookmarkKey)
        }
        folderURL = url
        folderName = url.lastPathComponent
        scan()
    }

    private func resolveBookmark() {
        guard let data = UserDefaults.standard.data(forKey: bookmarkKey) else { return }
        var stale = false
        if let url = try? URL(resolvingBookmarkData: data, options: [], relativeTo: nil, bookmarkDataIsStale: &stale) {
            folderURL = url
            folderName = url.lastPathComponent
        }
    }

    func lastPlayed(_ prefix: String) -> Int { UserDefaults.standard.integer(forKey: "lastnum_\(prefix)") }
    func setLastPlayed(_ prefix: String, _ number: Int) { UserDefaults.standard.set(number, forKey: "lastnum_\(prefix)") }

    func scan() {
        guard let folder = folderURL else { families = []; return }
        let scoped = folder.startAccessingSecurityScopedResource()
        defer { if scoped { folder.stopAccessingSecurityScopedResource() } }
        var entries: [RomFileEntry] = []
        if let en = FileManager.default.enumerator(at: folder, includingPropertiesForKeys: nil) {
            for case let fileURL as URL in en {
                let ext = fileURL.pathExtension.lowercased()
                if ext == "gba" || ext == "gb" {
                    entries.append(RomFileEntry(name: fileURL.lastPathComponent, path: fileURL.path))
                }
            }
        }
        families = RomFamilyUtils.shared.buildFamilies(files: entries)
    }

    /// Copy the chosen family ROM into the sandbox and return its local path (nil on failure).
    /// [number] selects a specific member; nil uses last-played (falling back to the first).
    func prepareROM(for group: RomFamilyGroup, number: Int? = nil) -> String? {
        guard let folder = folderURL else { return nil }
        let scoped = folder.startAccessingSecurityScopedResource()
        defer { if scoped { folder.stopAccessingSecurityScopedResource() } }

        let paths = RomFamilySwift.shared.memberPaths(group: group)
        let want = number ?? lastPlayed(group.prefix)
        let srcPath = paths.first { Int(RomFamilySwift.shared.numberOfPath(path: $0)) == want } ?? paths.first
        guard let src = srcPath else { return nil }

        let srcURL = URL(fileURLWithPath: src)
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dest = docs.appendingPathComponent(srcURL.lastPathComponent)
        if FileManager.default.fileExists(atPath: dest.path) { try? FileManager.default.removeItem(at: dest) }
        do { try FileManager.default.copyItem(at: srcURL, to: dest) } catch { return nil }

        setLastPlayed(group.prefix, Int(RomFamilySwift.shared.numberOfPath(path: src)))
        return dest.path
    }

    /// Ordered member numbers for a family (for quickload cycling).
    func memberNumbers(for group: RomFamilyGroup) -> [Int] {
        RomFamilySwift.shared.memberPaths(group: group).map { Int(RomFamilySwift.shared.numberOfPath(path: $0)) }
    }
}
