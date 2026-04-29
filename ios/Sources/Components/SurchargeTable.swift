import SwiftUI

struct SurchargeTable: View {
    let session: ParkingSession

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Samanburður")
                .font(.headline)
                .foregroundColor(Theme.red900)

            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 8) {
                GridRow {
                    Text("App").fontWeight(.semibold)
                    Text("Stæði").fontWeight(.semibold)
                    Text("Gjald").fontWeight(.semibold)
                    Text("Samtals").fontWeight(.semibold)
                }
                .font(.caption)
                .foregroundColor(Theme.gray600)

                Divider()

                GridRow {
                    Text("Rósa Parks").foregroundColor(Theme.red900)
                    Text("\(session.totalCostKr) kr.")
                    Text("0 kr.").foregroundColor(Theme.zoneGreen)
                    Text("\(session.totalCostKr) kr.").fontWeight(.bold)
                }

                GridRow {
                    Text("EasyPark")
                    Text("\(session.totalCostKr) kr.")
                    Text("+\(session.easyParkSurcharge) kr.").foregroundColor(Theme.zoneRed)
                    Text("\(session.totalCostKr + session.easyParkSurcharge) kr.").fontWeight(.bold)
                }

                GridRow {
                    Text("Parka")
                    Text("\(session.totalCostKr) kr.")
                    Text("+\(session.parkaSurcharge) kr.").foregroundColor(Theme.zoneRed)
                    Text("\(session.totalCostKr + session.parkaSurcharge) kr.").fontWeight(.bold)
                }
            }
            .font(.subheadline)
        }
        .padding()
        .background(Theme.gray50)
        .cornerRadius(12)
    }
}
