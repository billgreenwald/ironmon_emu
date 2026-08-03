import SwiftUI
import MetalKit

/// Draws the emulator framebuffer (240×160 RGBA8) to the screen via Metal, sampled onto a
/// fullscreen quad each frame (nearest-neighbor). Pulls the latest complete frame from EmulatorCore.
struct GameMetalView: UIViewRepresentable {
    let core: EmulatorCore

    func makeCoordinator() -> Renderer { Renderer(core: core) }

    func makeUIView(context: Context) -> MTKView {
        let view = MTKView(frame: .zero, device: context.coordinator.device)
        view.delegate = context.coordinator
        view.colorPixelFormat = .bgra8Unorm
        view.framebufferOnly = true
        view.preferredFramesPerSecond = 60
        view.clearColor = MTLClearColorMake(0, 0, 0, 1)
        view.contentMode = .scaleAspectFit
        return view
    }

    func updateUIView(_ uiView: MTKView, context: Context) {}

    final class Renderer: NSObject, MTKViewDelegate {
        let device: MTLDevice
        private let queue: MTLCommandQueue
        private let pipeline: MTLRenderPipelineState
        private let sampler: MTLSamplerState
        private let core: EmulatorCore
        private var texture: MTLTexture?
        private var staging = [UInt8]()

        init(core: EmulatorCore) {
            self.core = core
            self.device = MTLCreateSystemDefaultDevice()!
            self.queue = device.makeCommandQueue()!
            let lib = device.makeDefaultLibrary()!
            let desc = MTLRenderPipelineDescriptor()
            desc.vertexFunction = lib.makeFunction(name: "vs_fullscreen")
            desc.fragmentFunction = lib.makeFunction(name: "fs_sample")
            desc.colorAttachments[0].pixelFormat = .bgra8Unorm
            self.pipeline = try! device.makeRenderPipelineState(descriptor: desc)
            let sd = MTLSamplerDescriptor()
            sd.minFilter = .nearest
            sd.magFilter = .nearest
            self.sampler = device.makeSamplerState(descriptor: sd)!
            super.init()
        }

        func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {}

        private func ensureTexture() {
            let w = Int(core.videoWidth), h = Int(core.videoHeight)
            guard w > 0, h > 0 else { return }
            if texture == nil || texture!.width != w || texture!.height != h {
                let td = MTLTextureDescriptor.texture2DDescriptor(
                    pixelFormat: .rgba8Unorm, width: w, height: h, mipmapped: false)
                td.usage = [.shaderRead]
                texture = device.makeTexture(descriptor: td)
                staging = [UInt8](repeating: 0, count: w * h * 4)
            }
        }

        func draw(in view: MTKView) {
            ensureTexture()
            guard
                let tex = texture,
                let drawable = view.currentDrawable,
                let rpd = view.currentRenderPassDescriptor,
                let cmd = queue.makeCommandBuffer()
            else { return }

            let w = tex.width, h = tex.height
            let got = staging.withUnsafeMutableBytes { ptr in
                core.copyFrame(into: ptr.baseAddress!, capacity: w * h * 4)
            }
            if got {
                staging.withUnsafeBytes { ptr in
                    tex.replace(region: MTLRegionMake2D(0, 0, w, h),
                                mipmapLevel: 0,
                                withBytes: ptr.baseAddress!,
                                bytesPerRow: w * 4)
                }
            }

            let enc = cmd.makeRenderCommandEncoder(descriptor: rpd)!
            enc.setRenderPipelineState(pipeline)
            enc.setFragmentTexture(tex, index: 0)
            enc.setFragmentSamplerState(sampler, index: 0)
            enc.drawPrimitives(type: .triangle, vertexStart: 0, vertexCount: 3)
            enc.endEncoding()
            cmd.present(drawable)
            cmd.commit()
        }
    }
}
