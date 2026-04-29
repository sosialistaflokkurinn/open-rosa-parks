import SwiftUI

struct ParkingZone: Identifiable, Hashable {
    let id: String
    let name: String
    let color: Color
    let pricePerHour: Int
    let description: String
    let maxHours: Int?
    let pricePerHourAfterInitial: Int?
    let initialHours: Int?
    let weekdayHours: String?
    let weekendHours: String?

    init(
        id: String,
        name: String,
        color: Color,
        pricePerHour: Int,
        description: String,
        maxHours: Int? = nil,
        pricePerHourAfterInitial: Int? = nil,
        initialHours: Int? = nil,
        weekdayHours: String? = nil,
        weekendHours: String? = nil
    ) {
        self.id = id
        self.name = name
        self.color = color
        self.pricePerHour = pricePerHour
        self.description = description
        self.maxHours = maxHours
        self.pricePerHourAfterInitial = pricePerHourAfterInitial
        self.initialHours = initialHours
        self.weekdayHours = weekdayHours
        self.weekendHours = weekendHours
    }
}

extension ParkingZone {
    static let reykjavikZones: [ParkingZone] = [
        ParkingZone(
            id: "P1",
            name: "P1 — Rautt svæði",
            color: Theme.zoneRed,
            pricePerHour: 660,
            description: "Miðborg innri (Bankastræti, Laugavegur)",
            maxHours: 3,
            weekdayHours: "09:00–21:00",
            weekendHours: "10:00–21:00"
        ),
        ParkingZone(
            id: "P2",
            name: "P2 — Blátt svæði",
            color: Theme.zoneBlue,
            pricePerHour: 240,
            description: "Miðborg ytri (Hlemmur, Hljómskálagarður)",
            weekdayHours: "09:00–21:00",
            weekendHours: "10:00–21:00"
        ),
        ParkingZone(
            id: "P3",
            name: "P3 — Grænt svæði",
            color: Theme.zoneGreen,
            pricePerHour: 240,
            description: "Vesturbær, Þingholt",
            pricePerHourAfterInitial: 70,
            initialHours: 2,
            weekdayHours: "09:00–18:00"
        ),
        ParkingZone(
            id: "P4",
            name: "P4 — Gult svæði",
            color: Theme.zoneYellow,
            pricePerHour: 240,
            description: "Laugardalur, Háskóli",
            weekdayHours: "08:00–16:00"
        ),
    ]
}
