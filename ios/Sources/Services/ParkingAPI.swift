import Foundation

enum ParkingAPI {
    static let baseURL = "https://rosa-parks.gudrodur.workers.dev"
    private static let timeoutInterval: TimeInterval = 10

    // MARK: - Response Types

    struct ZonesResponse: Decodable {
        let zones: [APIZone]
    }

    struct APIZone: Decodable {
        let id: String
        let name: String
        let coordinates: [[[Double]]]
        let pricing: [APIPricing]
        let fee: String?
    }

    struct APIPricing: Decodable {
        let validDescription: String
        let prices: [APIPrice]

        enum CodingKeys: String, CodingKey {
            case validDescription = "valid_description"
            case prices
        }
    }

    struct APIPrice: Decodable {
        let info: String?
        let price: Int
        let extraInfo: String?
        let daily: Bool?
    }

    struct StartSessionResponse: Decodable {
        let sessionId: String
        let plate: String
        let zoneId: String
        let startTime: String
        let status: String
    }

    struct StopSessionResponse: Decodable {
        let sessionId: String
        let plate: String
        let zoneId: String
        let startTime: String
        let endTime: String
        let durationMinutes: Int
        let status: String
    }

    // MARK: - API Calls

    static func fetchZones() async throws -> ZonesResponse {
        let url = URL(string: "\(baseURL)/api/zones")!
        var request = URLRequest(url: url, timeoutInterval: timeoutInterval)
        request.httpMethod = "GET"
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.httpError
        }
        return try JSONDecoder().decode(ZonesResponse.self, from: data)
    }

    static func startSession(plate: String, zoneId: String) async throws -> StartSessionResponse {
        let url = URL(string: "\(baseURL)/api/parking/start")!
        var request = URLRequest(url: url, timeoutInterval: timeoutInterval)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body = ["plate": plate, "zoneId": zoneId]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.httpError
        }
        return try JSONDecoder().decode(StartSessionResponse.self, from: data)
    }

    static func stopSession(sessionId: String) async throws -> StopSessionResponse {
        let uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/
            .ignoresCase()
        guard sessionId.wholeMatch(of: uuidRegex) != nil else {
            throw APIError.invalidSessionId
        }
        let url = URL(string: "\(baseURL)/api/parking/stop/\(sessionId)")!
        var request = URLRequest(url: url, timeoutInterval: timeoutInterval)
        request.httpMethod = "DELETE"
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw APIError.httpError
        }
        return try JSONDecoder().decode(StopSessionResponse.self, from: data)
    }

    enum APIError: Error {
        case httpError
        case invalidSessionId
    }
}
