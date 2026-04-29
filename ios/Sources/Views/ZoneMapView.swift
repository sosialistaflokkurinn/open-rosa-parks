import MapKit
import SwiftUI

struct ZoneMapView: View {
    @EnvironmentObject var viewModel: ParkingViewModel

    private let reykjavikCenter = CLLocationCoordinate2D(latitude: 64.1466, longitude: -21.9426)

    var body: some View {
        mapContent
            .navigationTitle("Svæðakort")
    }

    @ViewBuilder
    private var mapContent: some View {
        if #available(iOS 17.0, *) {
            ModernMapView(zones: viewModel.zones, center: reykjavikCenter)
        } else {
            LegacyMapView(center: reykjavikCenter)
        }
    }
}

@available(iOS 17.0, *)
private struct ModernMapView: View {
    let zones: [ParkingZone]
    let center: CLLocationCoordinate2D

    @State private var cameraPosition: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 64.1466, longitude: -21.9426),
            span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.03)
        )
    )

    private let zoneCenters: [String: CLLocationCoordinate2D] = [
        "P1": CLLocationCoordinate2D(latitude: 64.1460, longitude: -21.9330),
        "P2": CLLocationCoordinate2D(latitude: 64.1440, longitude: -21.9400),
        "P3": CLLocationCoordinate2D(latitude: 64.1455, longitude: -21.9230),
        "P4": CLLocationCoordinate2D(latitude: 64.1350, longitude: -21.9200),
    ]

    var body: some View {
        Map(position: $cameraPosition) {
            ForEach(zones) { zone in
                Annotation(zone.id, coordinate: zoneCenters[zone.id] ?? center) {
                    Text(zone.id)
                        .font(.caption.bold())
                        .padding(4)
                        .background(zone.color.opacity(0.8))
                        .foregroundColor(.white)
                        .cornerRadius(4)
                }
            }
        }
    }
}

private struct LegacyMapView: UIViewRepresentable {
    let center: CLLocationCoordinate2D

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.region = MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.03)
        )
        return mapView
    }

    func updateUIView(_ uiView: MKMapView, context: Context) {}
}
