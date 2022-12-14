import Foundation
@testable import Jigra

enum BridgeTypeError: Error {
    case badCast
}

@objc class BridgedTypesHelper: NSObject {
    @objc static let shared = BridgedTypesHelper()

    var untypedArray: [Any] {
        return []
    }

    @objc func validTransformationOf(array: [Any]) -> [Any] {
        let result = JSTypes.coerceArrayToJSArray(array)!.jigra.replacingNullValues()
        return result.jigra.replacingOptionalValues() as [Any]
    }

    @objc func invalidTransformationOf(array: [Any]) -> [Any] {
        let result = JSTypes.coerceArrayToJSArray(array)!.jigra.replacingNullValues()
        return result as [Any]
    }

    @objc func testCast(of array: [Any], atIndex index: Int) throws -> Any {
        if let castArray = array as? [JSValue] {
            return castArray[index] as Any
        }
        throw BridgeTypeError.badCast
    }
}
