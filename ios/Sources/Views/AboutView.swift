import SwiftUI
import FirebaseCrashlytics

struct AboutView: View {
    #if DEBUG
    @State private var testEventSent = false
    #endif

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Spacer()
                    VStack {
                        Text("Rósa Parks")
                            .font(.largeTitle.bold())
                            .foregroundColor(Theme.brandRed)
                        Text("Gjaldfrjálst bílastæðaapp")
                            .font(.subheadline)
                            .foregroundColor(Theme.textSecondary)
                    }
                    Spacer()
                }

                Divider()

                section("Af hverju?",
                    "Reykjavíkurborg rukkar íbúa fyrir bílastæði á eigin landi. " +
                    "Einkafyrirtæki eins og EasyPark og Parka innheimta " +
                    "þjónustugjöld ofan á gjöld borgarinnar. Borgin býður eigin " +
                    "greiðsluleið á vefnum en hefur ekkert eigið app.")

                section("Hvað er þetta?",
                    "Þetta app sýnir að borgin gæti auðveldlega byggt sitt eigið " +
                    "app án álags. Engin 15% þjónustugjöld. Engin 89 kr. per færslu. " +
                    "Bara borgi fyrir stæðið og ekkert annað.")

                section("Hver smíðaði þetta?",
                    "Samtakamáttur svf. Opinn hugbúnaður, án álags. " +
                    "Kóðinn er á GitHub.")

                Divider()

                Text("Samtakamáttur svf.")
                    .font(.caption)
                    .foregroundColor(Theme.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                #if DEBUG
                    // Mirrors the Android debug-only trigger (#110/#112): long-press
                    // the footer to send a non-fatal test event to Crashlytics.
                    .onLongPressGesture {
                        Crashlytics.crashlytics().record(
                            error: NSError(
                                domain: "is.rosaparks",
                                code: 0,
                                userInfo: [NSLocalizedDescriptionKey: "crashlytics-test-non-fatal"]
                            )
                        )
                        testEventSent = true
                    }
                #endif

                #if DEBUG
                if testEventSent {
                    Text("Crashlytics test event sent")
                        .font(.caption2)
                        .foregroundColor(Theme.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                #endif
            }
            .padding()
        }
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle("Um appið")
    }

    private func section(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline)
                .foregroundColor(Theme.accentText)
            Text(body)
                .foregroundColor(Theme.textPrimary)
        }
    }
}
