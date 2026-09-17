import Foundation

@objc public class SSOWebView: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
