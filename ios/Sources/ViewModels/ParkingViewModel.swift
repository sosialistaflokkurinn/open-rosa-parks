import Foundation
import SwiftUI

@MainActor
class ParkingViewModel: ObservableObject {
    @Published var zones: [ParkingZone] = ParkingZone.reykjavikZones
    @Published var selectedZone: ParkingZone?
    @Published var plate: String = "" {
        didSet {
            let cleaned = String(plate.uppercased().prefix(6))
            if plate != cleaned { plate = cleaned }
            UserDefaults.standard.set(cleaned, forKey: Self.savedPlateKey)
        }
    }
    @Published var activeSession: ParkingSession?
    @Published var completedSession: ParkingSession?
    @Published var elapsedSeconds: Int = 0
    @Published var runningCost: Int = 0

    private var timer: Timer?
    private static let savedPlateKey = "saved_plate"

    init() {
        plate = UserDefaults.standard.string(forKey: Self.savedPlateKey) ?? ""
        Task { await loadZones() }
    }

    private func loadZones() async {
        do {
            let response = try await ParkingAPI.fetchZones()
            let mapped = mapAPIZones(response)
            if !mapped.isEmpty {
                zones = mapped
            }
        } catch {
            // Worker unavailable, keep hardcoded data
        }
    }

    private func mapAPIZones(_ response: ParkingAPI.ZonesResponse) -> [ParkingZone] {
        response.zones.compactMap { apiZone -> ParkingZone? in
            let color = Theme.zoneColor(for: apiZone.id)
            guard let pricing = apiZone.pricing.first,
                  let mainPrice = pricing.prices.first?.price else { return nil }

            let hasMaxHours = pricing.prices.contains { $0.extraInfo?.contains("hámark") == true }
            let maxHours: Int? = hasMaxHours
                ? pricing.prices.first?.extraInfo.flatMap { Int(String($0.filter(\.isNumber))) }
                : nil

            let tieredPrices = pricing.prices.count > 1
            let afterRate = tieredPrices ? pricing.prices[safe: 1]?.price : nil
            let initialHours: Int? = tieredPrices
                ? pricing.prices.first?.info.flatMap { Int(String($0.filter(\.isNumber))) }
                : nil

            return ParkingZone(
                id: apiZone.id,
                name: apiZone.name,
                color: color,
                pricePerHour: mainPrice,
                description: pricing.validDescription,
                maxHours: maxHours,
                pricePerHourAfterInitial: afterRate,
                initialHours: initialHours
            )
        }
    }

    func selectZone(_ zone: ParkingZone) {
        selectedZone = zone
    }

    func startParking() {
        guard let zone = selectedZone else { return }
        let session = ParkingSession(zone: zone, plate: plate, startTime: Date())
        activeSession = session
        startTimer()

        Task {
            do {
                let response = try await ParkingAPI.startSession(plate: plate, zoneId: zone.id)
                activeSession?.serverSessionId = response.sessionId
            } catch {
                // Server unavailable, continue with local session
            }
        }
    }

    func stopParking() {
        timer?.invalidate()
        timer = nil
        guard var session = activeSession else { return }
        session.endTime = Date()
        completedSession = session
        activeSession = nil
        elapsedSeconds = 0
        runningCost = 0

        if let serverId = session.serverSessionId {
            Task {
                try? await ParkingAPI.stopSession(sessionId: serverId)
            }
        }
    }

    func cancelParking() {
        timer?.invalidate()
        timer = nil
        let serverId = activeSession?.serverSessionId
        activeSession = nil
        elapsedSeconds = 0
        runningCost = 0

        if let serverId {
            Task {
                try? await ParkingAPI.stopSession(sessionId: serverId)
            }
        }
    }

    func reset() {
        selectedZone = nil
        completedSession = nil
        elapsedSeconds = 0
        runningCost = 0
    }

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self, let session = self.activeSession else { return }
                let elapsed = Date().timeIntervalSince(session.startTime)
                self.elapsedSeconds = Int(elapsed)
                self.runningCost = session.costForMinutes(Int(elapsed / 60))
            }
        }
    }
}

private extension Collection {
    subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
