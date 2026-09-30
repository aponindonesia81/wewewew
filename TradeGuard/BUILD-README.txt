TradeGuard Real Quotex Test Build

1) Open this folder in Android Studio.
2) Let Gradle sync/download dependencies.
3) Build > Build APK(s).
4) Install the debug APK on the Android phone.
5) Open TradeGuard -> Enable Accessibility -> enable TradeGuard.
6) Tap Start Screen Sensor and approve screen capture.
7) Open official Quotex Android app (package io.quotex.x).
8) Keep the Result ROI pointed at the RESULT (P/L) area. Default ROI is L45/T35/R100/B75 percent; adjust if needed.
9) For a safe first test, use the Quotex demo account. The app never connects to Quotex servers and never places trades.
10) After 4 detected losses (0.00 counts as loss by default), a local 3-minute lock is stored and a touch-blocking overlay appears only while Quotex is foreground.

Important Android limitation: this is an overlay/accessibility protection. It does not disable or close Quotex. A normal third-party Android app cannot guarantee an unbypassable block if its own services are force-stopped/uninstalled. For production-grade server-authoritative licensing/lock persistence, Firebase/Cloud Functions should be added later.
