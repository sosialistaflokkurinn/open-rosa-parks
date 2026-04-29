import SwiftUI

struct ActiveParkingView: View {
    @EnvironmentObject var viewModel: ParkingViewModel
    @Binding var path: NavigationPath

    var body: some View {
        VStack(spacing: 24) {
            if let session = viewModel.activeSession {
                Text(session.zone.name)
                    .font(.title2.bold())
                    .foregroundColor(session.zone.color)

                Text(session.plate)
                    .font(.title3.monospaced())
                    .foregroundColor(Theme.gray600)

                TimerDisplay(elapsedSeconds: viewModel.elapsedSeconds)

                Text("\(viewModel.runningCost) kr.")
                    .font(.system(size: 48, weight: .bold, design: .rounded))
                    .foregroundColor(Theme.red900)

                Spacer()

                Button {
                    viewModel.stopParking()
                    path.removeLast()
                    path.append(AppScreen.summary)
                } label: {
                    Text("Stöðva")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Theme.red900)
                        .cornerRadius(12)
                }

                Button("Hætta við") {
                    viewModel.cancelParking()
                    path.removeLast()
                }
                .foregroundColor(Theme.gray600)
            }
        }
        .padding()
        .navigationTitle("Virk lota")
        .navigationBarBackButtonHidden(true)
    }
}
