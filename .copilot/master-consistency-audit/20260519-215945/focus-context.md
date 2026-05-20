## Focus Problem Statement
The JavaFX 'IllegalArgumentException: BorderPane is already set as root of another scene' error in DesktopUi.showDashboard was able to pass the pipeline undetected. Research why current tests (LoginInteractionTest, FrontendStartupSmokeTest) missed this runtime transition error. Propose a testing pattern that would have caught this and apply corrections in a new PR.
