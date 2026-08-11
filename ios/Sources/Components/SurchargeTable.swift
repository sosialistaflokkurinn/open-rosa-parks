import SwiftUI

struct SurchargeTable: View {
    let session: ParkingSession

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Samanburður")
                .font(.headline)
                .foregroundColor(Theme.accentText)

            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 8) {
                GridRow {
                    Text("App").fontWeight(.semibold)
                    Text("Stæði").fontWeight(.semibold)
                    Text("Gjald").fontWeight(.semibold)
                    Text("Samtals").fontWeight(.semibold)
                }
                .font(.caption)
                .foregroundColor(Theme.textSecondary)

                Divider()

                GridRow {
                    Text("Rósa Parks").foregroundColor(Theme.accentText)
                    Text("\(session.totalCostKr) kr.")
                    Text("0 kr.").foregroundColor(Theme.positive)
                    Text("\(session.totalCostKr) kr.").fontWeight(.bold)
                }

                GridRow {
                    Text("EasyPark")
                    Text("\(session.totalCostKr) kr.")
                    Text("+\(session.easyParkSurcharge) kr.").foregroundColor(Theme.negative)
                    Text("\(session.totalCostKr + session.easyParkSurcharge) kr.").fontWeight(.bold)
                }

                GridRow {
                    Text("Parka")
                    Text("\(session.totalCostKr) kr.")
                    Text("+\(session.parkaSurcharge) kr.").foregroundColor(Theme.negative)
                    Text("\(session.totalCostKr + session.parkaSurcharge) kr.").fontWeight(.bold)
                }
            }
            .font(.subheadline)
            .foregroundColor(Theme.textPrimary)
        }
        .padding()
        .background(Theme.surface)
        .cornerRadius(12)
    }
}
