// Wraps the prebuilt liveness engine AAR in its own module. Android library modules
// cannot depend on a local .aar file directly, so ekyc-core depends on this project.
configurations.maybeCreate("default")
artifacts.add("default", file("faceidv5.aar"))
