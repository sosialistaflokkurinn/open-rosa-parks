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
                        .foregroundColor(Theme.textPrimary)

                    Text(zone.description)
                        .font(.caption)
                        .foregroundColor(Theme.textSecondary)

                    HStack {
                        Text("\(zone.pricePerHour) kr./klst.")
                            .font(.subheadline.bold())
                            .foregroundColor(zone.color)

                        if let hours = zone.weekdayHours {
                            Text("• \(hours)")
                                .font(.caption)
                                .foregroundColor(Theme.textSecondary)
                        }
                    }
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(Theme.brandRed)
                }
            }
            .padding()
            .background(isSelected ? Theme.surfaceSelected : Theme.surface)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? Theme.brandRed : Theme.outline, lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}
