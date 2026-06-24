import SwiftUI
import UIKit
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Generado por Compose Multiplatform — ver iosMain/MainViewController.kt
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
