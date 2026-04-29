import SwiftUI

@main
struct RosaParksApp: App {
    @StateObject private var viewModel = ParkingViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(viewModel)
        }
    }
}
