import AVFoundation

/// Pulls interleaved Int16 stereo @ 48 kHz from EmulatorCore's audio ring and plays it through an
/// AVAudioSourceNode. Mirrors the Android Oboe callback (oboe-audio.cpp), converting to Float32.
final class EmulatorAudio {
    private let engine = AVAudioEngine()
    private let core: EmulatorCore
    private var sourceNode: AVAudioSourceNode?

    init(core: EmulatorCore) { self.core = core }

    func start() {
        let format = AVAudioFormat(commonFormat: .pcmFormatFloat32,
                                   sampleRate: 48000, channels: 2, interleaved: false)!
        let core = self.core
        let node = AVAudioSourceNode(format: format) { _, _, frameCount, ablPtr -> OSStatus in
            let abl = UnsafeMutableAudioBufferListPointer(ablPtr)
            let n = Int(frameCount)
            var tmp = [Int16](repeating: 0, count: n * 2)
            let got = tmp.withUnsafeMutableBufferPointer { buf in
                core.readAudio(into: buf.baseAddress!, frames: n)   // stereo frames written
            }
            let left = abl[0].mData!.assumingMemoryBound(to: Float.self)
            let right = abl[1].mData!.assumingMemoryBound(to: Float.self)
            for i in 0..<n {
                if i < got {
                    left[i] = Float(tmp[i * 2]) / 32768.0
                    right[i] = Float(tmp[i * 2 + 1]) / 32768.0
                } else {
                    left[i] = 0; right[i] = 0
                }
            }
            return noErr
        }
        engine.attach(node)
        engine.connect(node, to: engine.mainMixerNode, format: format)
        sourceNode = node
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            try engine.start()
        } catch {
            print("EmulatorAudio start failed: \(error)")
        }
    }

    func stop() {
        engine.stop()
    }
}
