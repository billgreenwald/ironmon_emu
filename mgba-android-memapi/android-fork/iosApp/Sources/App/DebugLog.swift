import Foundation

/// Tiny append-only debug log written to Documents/ironmon_debug.log, exportable from the home
/// screen (share button). Lets us diagnose device-only issues (folder scan, ROM open) by having
/// the user upload the log. Serialized on its own queue so it's safe to call from anywhere.
enum DebugLog {
    private static let queue = DispatchQueue(label: "ironmon.debuglog")
    private static let formatter: DateFormatter = {
        let f = DateFormatter(); f.dateFormat = "HH:mm:ss.SSS"; return f
    }()

    static var fileURL: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("ironmon_debug.log")
    }

    static func log(_ message: String) {
        let line = "[\(formatter.string(from: Date()))] \(message)\n"
        queue.async {
            let url = fileURL
            if let data = line.data(using: .utf8) {
                if let handle = try? FileHandle(forWritingTo: url) {
                    handle.seekToEndOfFile(); handle.write(data); try? handle.close()
                } else {
                    try? data.write(to: url)   // first write creates the file
                }
            }
        }
        NSLog("IRONMON %@", message)   // also surface in Console/Xcode
    }

    static func clear() {
        queue.async { try? FileManager.default.removeItem(at: fileURL) }
    }
}
