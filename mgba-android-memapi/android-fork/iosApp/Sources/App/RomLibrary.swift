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
    private let looseKey = "loose_roms"
    private var folderURL: URL?

    private var docs: URL { FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0] }

    init() {
        resolveBookmark()
        scan()
    }

    var hasFolder: Bool { folderURL != nil }

    func setFolder(_ url: URL) {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        DebugLog.log("setFolder: \(url.path) scoped=\(scoped)")
        do {
            let data = try url.bookmarkData(options: [], includingResourceValuesForKeys: nil, relativeTo: nil)
            UserDefaults.standard.set(data, forKey: bookmarkKey)
            DebugLog.log("setFolder: bookmark saved (\(data.count) bytes)")
        } catch {
            DebugLog.log("setFolder: bookmark FAILED — \(error.localizedDescription)")
        }
        folderURL = url
        folderName = url.lastPathComponent
        scan()
    }

    private func resolveBookmark() {
        guard let data = UserDefaults.standard.data(forKey: bookmarkKey) else {
            DebugLog.log("resolveBookmark: none stored"); return
        }
        var stale = false
        if let url = try? URL(resolvingBookmarkData: data, options: [], relativeTo: nil, bookmarkDataIsStale: &stale) {
            folderURL = url
            folderName = url.lastPathComponent
            DebugLog.log("resolveBookmark: \(url.path) stale=\(stale)")
        } else {
            DebugLog.log("resolveBookmark: resolve FAILED")
        }
    }

    private var looseRoms: [String] {
        get { UserDefaults.standard.stringArray(forKey: looseKey) ?? [] }
        set { UserDefaults.standard.set(newValue, forKey: looseKey) }
    }

    func lastPlayed(_ prefix: String) -> Int { UserDefaults.standard.integer(forKey: "lastnum_\(prefix)") }
    func setLastPlayed(_ prefix: String, _ number: Int) { UserDefaults.standard.set(number, forKey: "lastnum_\(prefix)") }

    func scan() {
        var entries: [RomFileEntry] = []

        if let folder = folderURL {
            let scoped = folder.startAccessingSecurityScopedResource()
            defer { if scoped { folder.stopAccessingSecurityScopedResource() } }
            DebugLog.log("scan: folder=\(folder.path) scoped=\(scoped)")
            var seen = 0
            if let en = FileManager.default.enumerator(at: folder, includingPropertiesForKeys: nil,
                                                       options: [.skipsHiddenFiles]) {
                for case let fileURL as URL in en {
                    seen += 1
                    let ext = fileURL.pathExtension.lowercased()
                    if ext == "gba" || ext == "gb" {
                        entries.append(RomFileEntry(name: fileURL.lastPathComponent, path: fileURL.path))
                    } else if seen <= 40 {
                        DebugLog.log("scan:   skip \(fileURL.lastPathComponent) (ext=\(ext))")
                    }
                }
            } else {
                DebugLog.log("scan: enumerator was nil (couldn't open folder)")
            }
            DebugLog.log("scan: enumerated \(seen) items, \(entries.count) ROMs in folder")
        } else {
            DebugLog.log("scan: no folder set")
        }

        // Individually-added ROMs (Documents), so they persist on the home list (issue #4).
        for path in looseRoms where FileManager.default.fileExists(atPath: path) {
            entries.append(RomFileEntry(name: URL(fileURLWithPath: path).lastPathComponent, path: path))
        }

        families = RomFamilyUtils.shared.buildFamilies(files: entries)
        DebugLog.log("scan: \(families.count) families from \(entries.count) ROMs "
                   + "(\(looseRoms.count) loose)")
    }

    /// Copy a manually-picked single ROM into Documents, remember it so it shows on the home list,
    /// rescan, and return its runnable local path. iOS counterpart of Android adding a loose ROM.
    func addLooseROM(url: URL) -> String? {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        let dest = docs.appendingPathComponent(url.lastPathComponent)
        if !FileManager.default.fileExists(atPath: dest.path) {
            do { try FileManager.default.copyItem(at: url, to: dest) }
            catch { DebugLog.log("addLooseROM: copy FAILED — \(error.localizedDescription)"); return nil }
        }
        var loose = looseRoms
        if !loose.contains(dest.path) { loose.append(dest.path); looseRoms = loose }
        DebugLog.log("addLooseROM: \(dest.lastPathComponent) ready at \(dest.path)")
        scan()
        return dest.path
    }

    /// Copy the chosen family ROM into the sandbox and return its local path (nil on failure).
    /// [number] selects a specific member; nil uses last-played (falling back to the first).
    func prepareROM(for group: RomFamilyGroup, number: Int? = nil) -> String? {
        let paths = RomFamilySwift.shared.memberPaths(group: group)
        let want = number ?? lastPlayed(group.prefix)
        let srcPath = paths.first { Int(RomFamilySwift.shared.numberOfPath(path: $0)) == want } ?? paths.first
        guard let src = srcPath else { DebugLog.log("prepareROM: no member paths"); return nil }
        setLastPlayed(group.prefix, Int(RomFamilySwift.shared.numberOfPath(path: src)))

        // Loose ROM already inside Documents → run it in place (no copy, no scoping).
        if src.hasPrefix(docs.path) { DebugLog.log("prepareROM: run in place \(src)"); return src }

        // Folder ROM → copy into the sandbox under the folder's security scope.
        guard let folder = folderURL else { DebugLog.log("prepareROM: no folder for \(src)"); return nil }
        let scoped = folder.startAccessingSecurityScopedResource()
        defer { if scoped { folder.stopAccessingSecurityScopedResource() } }
        let srcURL = URL(fileURLWithPath: src)
        let dest = docs.appendingPathComponent(srcURL.lastPathComponent)
        if FileManager.default.fileExists(atPath: dest.path) { try? FileManager.default.removeItem(at: dest) }
        do { try FileManager.default.copyItem(at: srcURL, to: dest) }
        catch { DebugLog.log("prepareROM: copy FAILED — \(error.localizedDescription)"); return nil }
        DebugLog.log("prepareROM: copied \(dest.lastPathComponent) to sandbox")
        return dest.path
    }

    /// Ordered member numbers for a family (for quickload cycling).
    func memberNumbers(for group: RomFamilyGroup) -> [Int] {
        RomFamilySwift.shared.memberPaths(group: group).map { Int(RomFamilySwift.shared.numberOfPath(path: $0)) }
    }
}
