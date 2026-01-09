# OpenSensorHub Add-ons

This repository contains various OpenSensorHub add-on modules including sensor drivers, database connectors, processing algorithms, additional services, etc.

Please clone this repository with the `--recursive` option (`git clone --recursive`) as it contains submodules.

For more information about OpenSensorHub, see the [OSH Core Readme](https://github.com/opensensorhub/osh-core).

## Building

Please set environment variables `GITHUB_ACTOR` and `GITHUB_TOKEN` to your GitHub username and PAT respectively, and run `./gradlew build` from the root directory

## Installing

If using a build system, binaries for osh-core are stored in GitHub Packages. Use the following repository setup to resolve dependencies:

### Gradle

```groovy
//build.gradle
repositories {
  maven {
    url = uri("https://maven.pkg.github.com/opensensorhub/osh-addons")
    credentials {
      username = System.getenv("GITHUB_ACTOR")
      password = System.getenv("GITHUB_TOKEN")
    }
  }
}
  ```
### Maven
```xml
<!--pom.xml-->
<repositories>
    <repository>
        <id>osh-addons</id>
        <name>osh-addons</name>
        <url>https://maven.pkg.github.com/opensensorhub/osh-addons</url>
    </repository>
</repositories>
```
```xml
<!--~/.m2/settings.xml-->
<servers>
    <server>
        <id>osh-addons</id>
        <username>${env.GITHUB_ACTOR}</username>
        <password>${env.GITHUB_TOKEN}</password>
    </server>
</servers>
```
Where environment variables `GITHUB_ACTOR` and `GITHUB_TOKEN` are your GitHub username and PAT respectively.<br>

[Instructions on how to generate PATs](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)

