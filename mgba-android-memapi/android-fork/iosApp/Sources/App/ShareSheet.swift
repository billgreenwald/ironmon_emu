import SwiftUI
import UIKit

/// Thin SwiftUI wrapper over UIActivityViewController, for exporting the debug log (AirDrop, Files,
/// Mail, etc.). Presented via `.sheet`.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}
