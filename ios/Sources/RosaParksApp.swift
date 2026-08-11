import SwiftUI
import FirebaseCore

@main
struct RosaParksApp: App {
    @StateObject private var viewModel = ParkingViewModel()

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
        }
    }
}
