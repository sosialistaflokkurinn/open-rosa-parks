import SwiftUI

struct ZoneCard: View {
    let zone: ParkingZone
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(zone.color)
                    .frame(width: 6)

                VStack(alignment: .leading, spacing: 4) {
                    Text(zone.name)
                        .font(.headline)
                        .foregroundColor(Theme.gray900)

                    Text(zone.description)
                        .font(.caption)
                        .foregroundColor(Theme.gray600)

                    HStack {
                        Text("\(zone.pricePerHour) kr./klst.")
                            .font(.subheadline.bold())
                            .foregroundColor(zone.color)

                        if let hours = zone.weekdayHours {
                            Text("• \(hours)")
                                .font(.caption)
                                .foregroundColor(Theme.gray600)
                        }
                    }
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Theme.red900)
                }
            }
            .padding()
            .background(isSelected ? Theme.red50 : .white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Theme.red900 : Theme.gray200, lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}
