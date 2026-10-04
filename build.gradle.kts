plugins {
    java
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.appium:java-client:9.4.0")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.22.0")
}

tasks.test {
    maxHeapSize = "256m"
    jvmArgs("-XX:MaxMetaspaceSize=128m")

    // Forward -Dappium.* from the Gradle command line into the forked test JVM. BaseTest
    // reads these with System.getProperty, and Gradle does not propagate its own JVM's
    // system properties to the fork - so without this, overriding the target package or
    // device silently has no effect and every run goes to the defaults.
    listOf(
        "appium.worker",
        "appium.appPackage",
        "appium.appActivity",
        "appium.deviceName",
        "appium.serverUrl",
        "appium.udid",
        "appium.bstackApp",
        "appium.osVersion",
        "appium.bstackProject",
        "appium.bstackBuild",
        "appium.bstackSession",
        "appium.bstackLogin",
    ).forEach { key ->
        System.getProperty(key)?.let { value -> systemProperty(key, value) }
    }

    // Phase 4: when -Dappium.worker=N is supplied, this run is one shard of a parallel
    // execution and must not write into the shared, fixed result paths - two shards would
    // otherwise overwrite each other's JUnit XML and the run would look green while half
    // its results were lost. Absent the property nothing changes, so sequential runs keep
    // today's layout and regressionReport keeps working untouched.
    System.getProperty("appium.worker")?.let { worker ->
        val base = "parallel-results/worker-$worker"
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("$base/test-results"))
        reports.html.outputLocation.set(layout.buildDirectory.dir("$base/html"))
        binaryResultsDirectory.set(layout.buildDirectory.dir("$base/binary"))
    }

    finalizedBy("regressionReport")
}

// Build-gate smoke suite: only tests tagged @Category(P0.class). Same device/Appium
// properties as `test`, so e.g. -Dappium.udid=... works unchanged.
tasks.register<Test>("p0Smoke") {
    group = "verification"
    description = "Runs the P0 smoke suite (tests tagged @Category(P0))."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnit { includeCategories("com.shriaikyam.aikyam.categories.P0") }
    maxHeapSize = "256m"
    jvmArgs("-XX:MaxMetaspaceSize=128m")
    listOf("appium.worker", "appium.appPackage", "appium.appActivity", "appium.deviceName",
        "appium.serverUrl", "appium.udid").forEach { key ->
        System.getProperty(key)?.let { value -> systemProperty(key, value) }
    }
}

// One-command regression report: parses every TEST-*.xml JUnit result produced by the
// `test` task into a single module-wise HTML summary (pass/fail/skip counts, per-test
// failure reasons, and a link to the screenshot/page-source BaseTest's TestWatcher
// captured for that failure) so `./gradlew test` alone answers "what broke" without
// opening each class's result file individually.
tasks.register("regressionReport") {
    group = "verification"
    description = "Aggregates the test task's JUnit XML results into one HTML summary."

    val resultsDir = layout.buildDirectory.dir("test-results/test")
    val diagnosticsDir = layout.buildDirectory.dir("test-results/diagnostics")
    val reportFile = layout.buildDirectory.file("reports/regression-summary.html")

    inputs.dir(resultsDir).skipWhenEmpty(false)
    outputs.file(reportFile)

    doLast {
        val resultsDirFile = resultsDir.get().asFile
        val diagnosticsDirFile = diagnosticsDir.get().asFile
        val xmlFiles = (resultsDirFile.listFiles { f: File -> f.name.startsWith("TEST-") && f.name.endsWith(".xml") }
            ?: arrayOf()).sortedBy { it.name }

        val docFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance()

        var total = 0
        var passed = 0
        var failed = 0
        var skipped = 0

        val html = StringBuilder()
        html.append("<!doctype html>\n<html><head><meta charset=\"utf-8\">")
        html.append("<title>Regression Summary</title><style>")
        html.append("body { font-family: -apple-system, Segoe UI, Arial, sans-serif; margin: 24px; color: #1a1a1a; }")
        html.append("h1 { margin-bottom: 4px; } .totals { margin-bottom: 24px; }")
        html.append(".totals span { display: inline-block; margin-right: 20px; font-size: 1.1em; }")
        html.append(".passed { color: #1a7f37; } .failed { color: #cf222e; } .skipped { color: #9a6700; }")
        html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 32px; }")
        html.append("th, td { border: 1px solid #d0d7de; padding: 6px 10px; text-align: left; font-size: 0.92em; vertical-align: top; }")
        html.append("th { background: #f6f8fa; } tr.FAILED, tr.ERROR { background: #fff0ef; } tr.SKIPPED { background: #fff8e6; }")
        html.append(".module-header { background: #eaeef2; font-weight: bold; font-size: 1.05em; } code { font-size: 0.85em; }")
        html.append("</style></head><body><h1>Regression Summary</h1>")
        html.append("<div id=\"totals-placeholder\"></div>")
        html.append("<table><tr><th>Module</th><th>Test Case</th><th>Status</th><th>Time (s)</th><th>Failure reason</th><th>Diagnostics</th></tr>\n")

        for (file in xmlFiles) {
            val doc = docFactory.newDocumentBuilder().parse(file)
            val suiteEl = doc.documentElement
            val className: String = suiteEl.getAttribute("name")
            val simpleClassName = className.substringAfterLast(".")
            val caseNodes = suiteEl.getElementsByTagName("testcase")

            html.append("<tr class=\"module-header\"><td colspan=\"6\">")
            html.append(simpleClassName)
            html.append(" (")
            html.append(caseNodes.length)
            html.append(" tests)</td></tr>\n")

            for (i in 0 until caseNodes.length) {
                val caseEl = caseNodes.item(i) as org.w3c.dom.Element
                val name: String = caseEl.getAttribute("name")
                val time: String = caseEl.getAttribute("time")
                val failureNodes = caseEl.getElementsByTagName("failure")
                val errorNodes = caseEl.getElementsByTagName("error")
                val skippedNodes = caseEl.getElementsByTagName("skipped")

                var status = "PASSED"
                var message = ""
                if (failureNodes.length > 0) {
                    status = "FAILED"
                    message = (failureNodes.item(0) as org.w3c.dom.Element).getAttribute("message")
                } else if (errorNodes.length > 0) {
                    status = "ERROR"
                    message = (errorNodes.item(0) as org.w3c.dom.Element).getAttribute("message")
                } else if (skippedNodes.length > 0) {
                    status = "SKIPPED"
                }

                total += 1
                if (status == "PASSED") passed += 1
                if (status == "FAILED" || status == "ERROR") failed += 1
                if (status == "SKIPPED") skipped += 1

                val safeName = name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                val safeMessage = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                var diagnostics = ""
                if (status == "FAILED" || status == "ERROR") {
                    val classDir = File(diagnosticsDirFile, simpleClassName)
                    val screenshot = File(classDir, "$name.png")
                    val pageSource = File(classDir, "${name}_pagesource.xml")
                    if (screenshot.exists()) {
                        diagnostics += "<a href=\"../test-results/diagnostics/$simpleClassName/$name.png\">screenshot</a> "
                    }
                    if (pageSource.exists()) {
                        diagnostics += "<a href=\"../test-results/diagnostics/$simpleClassName/${name}_pagesource.xml\">page source</a>"
                    }
                }

                html.append("<tr class=\"")
                html.append(status)
                html.append("\"><td></td><td><code>")
                html.append(safeName)
                html.append("</code></td><td>")
                html.append(status)
                html.append("</td><td>")
                html.append(time)
                html.append("</td><td>")
                html.append(safeMessage)
                html.append("</td><td>")
                html.append(diagnostics)
                html.append("</td></tr>\n")
            }
        }

        html.append("</table></body></html>")

        val totalsHtml = "<div class=\"totals\">" +
            "<span>Total: <b>$total</b></span>" +
            "<span class=\"passed\">Passed: <b>$passed</b></span>" +
            "<span class=\"failed\">Failed: <b>$failed</b></span>" +
            "<span class=\"skipped\">Skipped: <b>$skipped</b></span></div>"
        val finalHtml = html.toString().replace("<div id=\"totals-placeholder\"></div>", totalsHtml)

        val outFile = reportFile.get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(finalHtml)
        println("Regression summary: ${outFile.absolutePath}")
        println("Total: $total  Passed: $passed  Failed: $failed  Skipped: $skipped")
    }
}
