import SwiftUI

struct PlateInput: View {
    @Binding var plate: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Bílnúmer")
                .font(.caption)
                .foregroundColor(Theme.textSecondary)

            TextField("AB123", text: $plate)
                .font(.title3.monospaced())
                .foregroundColor(Theme.textPrimary)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .padding()
                .background(Theme.surface)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Theme.outline, lineWidth: 1)
                )
        }
    }
}
