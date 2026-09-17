// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorSsoWebview",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorSsoWebview",
            targets: ["SSOWebViewPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "SSOWebViewPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/SSOWebViewPlugin"),
        .testTarget(
            name: "SSOWebViewPluginTests",
            dependencies: ["SSOWebViewPlugin"],
            path: "ios/Tests/SSOWebViewPluginTests")
    ]
)