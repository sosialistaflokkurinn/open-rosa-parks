import SwiftUI

struct TimerDisplay: View {
    let elapsedSeconds: Int

    var body: some View {
        Text(formatted)
            .font(.system(size: 64, weight: .light, design: .monospaced))
            .foregroundColor(Theme.gray900)
    }

    private var formatted: String {
        let minutes = elapsedSeconds / 60
        let seconds = elapsedSeconds % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}
