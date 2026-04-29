import SwiftUI

struct SummaryView: View {
    @EnvironmentObject var viewModel: ParkingViewModel
    @Binding var path: NavigationPath

    var body: some View {
        if let session = viewModel.completedSession {
            ScrollView {
                VStack(spacing: 20) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 64))
                        .foregroundColor(Theme.zoneGreen)

                    Text("Lotu lokið")
                        .font(.title.bold())

                    VStack(spacing: 8) {
                        row("Svæði", session.zone.name)
                        row("Bílnúmer", session.plate)
                        row("Tími", "\(session.durationMinutes) mín.")
                        Divider()
                        row("Rósa Parks", "\(session.totalCostKr) kr.")
                            .font(.headline)
                    }
                    .padding()
                    .background(Theme.gray50)
                    .cornerRadius(12)

                    SurchargeTable(session: session)

                    Button {
                        viewModel.reset()
                        path = NavigationPath()
                    } label: {
                        Text("Aftur á forsíðu")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Theme.red900)
                            .cornerRadius(12)
                    }
                }
                .padding()
            }
            .navigationTitle("Yfirlit")
            .navigationBarBackButtonHidden(true)
        }
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .foregroundColor(Theme.gray600)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}
