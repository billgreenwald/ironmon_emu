import SwiftUI
import TrackerCore

/// Route image gallery (Android GalleryOverlay): Maps / Hidden Items tabs, each a swipeable pager
/// of pinch-zoomable images loaded from the bundled `maps/` folder.
struct GalleryView: View {
    let routeName: String
    @Environment(\.dismiss) private var dismiss

    private var maps: [String] { GallerySwift.shared.routeMaps(routeName: routeName) }
    private var hidden: [String] { GallerySwift.shared.hiddenItems(routeName: routeName) }

    var body: some View {
        NavigationView {
            Group {
                if maps.isEmpty && hidden.isEmpty {
                    Text("No images for this area.").foregroundColor(.secondary)
                } else if maps.isEmpty {
                    ImagePager(paths: hidden)
                } else if hidden.isEmpty {
                    ImagePager(paths: maps)
                } else {
                    TabView {
                        ImagePager(paths: maps).tabItem { Label("Maps", systemImage: "map") }
                        ImagePager(paths: hidden).tabItem { Label("Hidden Items", systemImage: "sparkles") }
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black.ignoresSafeArea())
            .navigationTitle(routeName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
                ToolbarItem(placement: .bottomBar) {
                    Text("Images provided by Fellshadow").font(.system(size: 10)).foregroundColor(.secondary)
                }
            }
        }
        .navigationViewStyle(.stack)
    }
}

private struct ImagePager: View {
    let paths: [String]
    var body: some View {
        TabView {
            ForEach(Array(paths.enumerated()), id: \.offset) { _, p in
                ZoomableImage(path: p)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: paths.count > 1 ? .automatic : .never))
    }
}

private struct ZoomableImage: View {
    let path: String
    @State private var scale: CGFloat = 1
    @State private var offset: CGSize = .zero

    var body: some View {
        Group {
            if let img = GalleryImageLoader.image(path) {
                Image(uiImage: img).resizable().scaledToFit()
                    .scaleEffect(scale)
                    .offset(offset)
                    .gesture(
                        MagnificationGesture()
                            .onChanged { scale = Swift.max(1, $0) }
                            .onEnded { _ in if scale < 1.05 { scale = 1; offset = .zero } }
                    )
                    .simultaneousGesture(
                        DragGesture().onChanged { if scale > 1 { offset = $0.translation } }
                    )
                    .onTapGesture(count: 2) { scale = scale > 1 ? 1 : 2.5; offset = .zero }
            } else {
                Text("Image missing").foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

enum GalleryImageLoader {
    private static var cache: [String: UIImage] = [:]
    /// `relPath` like "maps/hidden_items/pewter_city.png" (relative to the bundled assets root).
    static func image(_ relPath: String) -> UIImage? {
        if let c = cache[relPath] { return c }
        let ns = relPath as NSString
        let dir = ns.deletingLastPathComponent
        let file = ns.lastPathComponent as NSString
        let name = file.deletingPathExtension
        let ext = file.pathExtension
        guard let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: dir),
              let img = UIImage(contentsOfFile: url.path) else { return nil }
        cache[relPath] = img
        return img
    }
}
