import XCTest
@testable import RosaParks

final class RosaParksTests: XCTestCase {
    // MARK: - ParkingSession Cost Calculations

    func testCostForMinutesSimple() {
        let cost = ParkingSession.costForMinutes(60, pricePerHour: 660)
        XCTAssertEqual(cost, 660)
    }

    func testCostForMinutesPartialHour() {
        let cost = ParkingSession.costForMinutes(30, pricePerHour: 660)
        XCTAssertEqual(cost, 330)
    }

    func testCostForZeroMinutes() {
        let cost = ParkingSession.costForMinutes(0, pricePerHour: 660)
        XCTAssertEqual(cost, 0)
    }

    func testEasyParkSurchargeMinimum() {
        // 15% of 100 = 15, but minimum is 75
        let surcharge = ParkingSession.easyParkSurchargeForCost(100)
        XCTAssertEqual(surcharge, 75)
    }

    func testEasyParkSurchargePercentage() {
        // 15% of 1000 = 150
        let surcharge = ParkingSession.easyParkSurchargeForCost(1000)
        XCTAssertEqual(surcharge, 150)
    }

    func testP1SessionCostMaxHours() {
        let zone = ParkingZone.reykjavikZones.first { $0.id == "P1" }!
        let session = ParkingSession(zone: zone, plate: "AB123", startTime: Date())
        // P1 has maxHours=3, so 4 hours should be capped at 3
        let cost = session.costForMinutes(240) // 4 hours
        let expectedCost = session.costForMinutes(180) // 3 hours (capped)
        XCTAssertEqual(cost, expectedCost)
    }

    func testP3SessionCostTieredPricing() {
        let zone = ParkingZone.reykjavikZones.first { $0.id == "P3" }!
        let session = ParkingSession(zone: zone, plate: "AB123", startTime: Date())
        // P3: 240 kr/hr for first 2 hours, then 70 kr/hr
        let cost = session.costForMinutes(180) // 3 hours
        let expected = 240 * 2 + 70 // 2h at 240 + 1h at 70
        XCTAssertEqual(cost, expected)
    }

    // MARK: - ParkingZone Data

    func testReykjavikZonesCount() {
        XCTAssertEqual(ParkingZone.reykjavikZones.count, 4)
    }

    func testZoneIDs() {
        let ids = ParkingZone.reykjavikZones.map(\.id)
        XCTAssertEqual(ids, ["P1", "P2", "P3", "P4"])
    }

    func testParkaSurchargeFixed() {
        let zone = ParkingZone.reykjavikZones[0]
        let session = ParkingSession(zone: zone, plate: "AB123", startTime: Date())
        XCTAssertEqual(session.parkaSurcharge, 89)
    }
}
