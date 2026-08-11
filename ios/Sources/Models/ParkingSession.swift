import Foundation

struct ParkingSession {
    let zone: ParkingZone
    let plate: String
    let startTime: Date
    var endTime: Date?
    var serverSessionId: String?

    var isActive: Bool { endTime == nil }

    var durationMinutes: Int {
        let end = endTime ?? Date()
        return max(0, Int(end.timeIntervalSince(startTime) / 60))
    }

    var totalCostKr: Int {
        costForMinutes(durationMinutes)
    }

    /// EasyPark: 15% surcharge, minimum 75 kr
    var easyParkSurcharge: Int {
        Self.easyParkSurchargeForCost(totalCostKr)
    }

    /// Parka: 89 kr fixed fee per transaction (or 490 kr/month subscription)
    var parkaSurcharge: Int { 89 }

    func costForMinutes(_ minutes: Int) -> Int {
        if let initial = zone.initialHours,
           let afterRate = zone.pricePerHourAfterInitial,
           minutes > initial * 60 {
            let initialCost = zone.pricePerHour * initial
            let remainingMinutes = minutes - initial * 60
            return initialCost + Int((Double(remainingMinutes) / 60.0 * Double(afterRate)).rounded())
        }
        let maxMin = zone.maxHours.map { $0 * 60 }
        let capped = maxMin.map { min(minutes, $0) } ?? minutes
        return Self.costForMinutes(capped, pricePerHour: zone.pricePerHour)
    }

    static func costForMinutes(_ minutes: Int, pricePerHour: Int) -> Int {
        Int((Double(minutes) / 60.0 * Double(pricePerHour)).rounded())
    }

    static func easyParkSurchargeForCost(_ cost: Int) -> Int {
        max(75, Int((Double(cost) * 0.15).rounded()))
    }
}
