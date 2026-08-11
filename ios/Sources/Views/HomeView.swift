import SwiftUI

struct HomeView: View {
    @EnvironmentObject var viewModel: ParkingViewModel
    @Binding var path: NavigationPath

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("Rósa Parks")
                    .font(.largeTitle.bold())
                    .foregroundColor(Theme.brandRed)

                Text("Gjaldfrjálst bílastæðaapp")
                    .font(.subheadline)
                    .foregroundColor(Theme.textSecondary)

                PlateInput(plate: $viewModel.plate)
                    .padding(.horizontal)

                ForEach(viewModel.zones) { zone in
                    ZoneCard(
                        zone: zone,
                        isSelected: viewModel.selectedZone?.id == zone.id,
                        onTap: { viewModel.selectZone(zone) }
                    )
                }

                Button {
                    viewModel.startParking()
                    path.append(AppScreen.activeParking)
                } label: {
                    Text("Leggja")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(canStart ? Theme.brandRed : Theme.fillDisabled)
                        .cornerRadius(12)
                }
                .disabled(!canStart)

                HStack {
                    Button { path.append(AppScreen.zoneMap) } label: {
                        Label("Kort", systemImage: "map")
                    }
                    Spacer()
                    Button { path.append(AppScreen.about) } label: {
                        Label("Um appið", systemImage: "info.circle")
                    }
                }
                .padding(.horizontal)
            }
            .padding()
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
    }

    private var canStart: Bool {
        viewModel.selectedZone != nil && viewModel.plate.count >= 3
    }
}
