resolvers += Resolver.url("Upstart Commerce", url("https://upstartcommerce.bintray.com/generic/"))(
  Resolver.ivyStylePatterns
)

addSbtPlugin("org.upstartcommerce" % "sbt-openapi-generator" % "0.1-SNAPSHOT") // Use the latest version from the badge above
addSbtPlugin("com.twilio" % "sbt-guardrail" % "0.55.0")
