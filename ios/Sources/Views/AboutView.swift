import SwiftUI

struct AboutView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Spacer()
                    VStack {
                        Text("Rósa Parks")
                            .font(.largeTitle.bold())
                            .foregroundColor(Theme.red900)
                        Text("Gjaldfrjálst bílastæðaapp")
                            .font(.subheadline)
                            .foregroundColor(Theme.gray600)
                    }
                    Spacer()
                }

                Divider()

                section("Af hverju?",
                    "Reykjavíkurborg rukkar íbúa fyrir bílastæði á eigin landi " +
                    "— og gefur einkafyrirtækjum eins og EasyPark og Parka " +
                    "einkarétt á að innheimta þjónustugjöld ofan á.")

                section("Hvað er þetta?",
                    "Þetta app sýnir að borgin gæti auðveldlega byggt sitt eigið " +
                    "app án álags. Engin 15% þjónustugjöld. Engin 89 kr. per færslu. " +
                    "Bara borgi fyrir stæðið og ekkert annað.")

                section("Hver smíðaði þetta?",
                    "Sósíalistaflokkurinn. Opinn hugbúnaður, án álags. " +
                    "Kóðinn er á GitHub.")

                Divider()

                Text("Sósíalistaflokkurinn")
                    .font(.caption)
                    .foregroundColor(Theme.gray600)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            .padding()
        }
        .navigationTitle("Um appið")
    }

    private func section(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline)
                .foregroundColor(Theme.red900)
            Text(body)
                .foregroundColor(Theme.gray900)
        }
    }
}
