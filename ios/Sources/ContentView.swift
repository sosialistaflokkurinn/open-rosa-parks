import SwiftUI

enum AppScreen: Hashable {
    case home
    case activeParking
    case summary
    case about
    case zoneMap
}

struct ContentView: View {
    @EnvironmentObject var viewModel: ParkingViewModel
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(path: $path)
                .navigationDestination(for: AppScreen.self) { screen in
                    switch screen {
                    case .home:
                        HomeView(path: $path)
                    case .activeParking:
                        ActiveParkingView(path: $path)
                    case .summary:
                        SummaryView(path: $path)
                    case .about:
                        AboutView()
                    case .zoneMap:
                        ZoneMapView()
                    }
                }
        }
        .tint(Theme.red900)
    }
}
