import SwiftUI

struct PlateInput: View {
    @Binding var plate: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Bílnúmer")
                .font(.caption)
                .foregroundColor(Theme.gray600)

            TextField("AB123", text: $plate)
                .font(.title3.monospaced())
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .padding()
                .background(Theme.gray50)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Theme.gray200, lineWidth: 1)
                )
        }
    }
}
